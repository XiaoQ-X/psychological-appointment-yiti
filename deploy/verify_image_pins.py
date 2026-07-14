#!/usr/bin/env python3
"""Fail-closed validation for production image pins and OCI provenance."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path
from typing import Any


DIGEST_PATTERN = re.compile(r"sha256:[0-9a-f]{64}\Z")
SCRIPT_DIR = Path(__file__).resolve().parent
DEFAULT_LOCK = SCRIPT_DIR / "image-lock.json"
DEFAULT_COMPOSE = SCRIPT_DIR / "docker-compose.prod.yml"
DEFAULT_ENV = SCRIPT_DIR / "production.env.example"


class ValidationError(RuntimeError):
    """Raised when an image integrity invariant is not satisfied."""


def read_json(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise ValidationError(f"cannot read JSON from {path}: {exc}") from exc
    if not isinstance(value, dict):
        raise ValidationError(f"expected a JSON object in {path}")
    return value


def validate_lock(lock: dict[str, Any]) -> dict[str, dict[str, Any]]:
    if lock.get("schemaVersion") != 1:
        raise ValidationError("image lock schemaVersion must be 1")
    images = lock.get("images")
    if not isinstance(images, dict) or not images:
        raise ValidationError("image lock must contain at least one image")

    for service, entry in images.items():
        if not isinstance(entry, dict):
            raise ValidationError(f"lock entry for {service} must be an object")
        reference = entry.get("reference")
        digest = entry.get("digest")
        tag = entry.get("tag")
        if not isinstance(reference, str) or not isinstance(digest, str):
            raise ValidationError(f"lock entry for {service} needs reference and digest")
        if not DIGEST_PATTERN.fullmatch(digest):
            raise ValidationError(f"lock entry for {service} has an invalid sha256 digest")
        if reference != f"{tag}@{digest}":
            raise ValidationError(f"lock entry for {service} reference does not match tag and digest")
        for field in ("version", "source", "sourceRevision", "reviewedAt"):
            if not isinstance(entry.get(field), str) or not entry[field].strip():
                raise ValidationError(f"lock entry for {service} is missing {field}")

        provenance = entry.get("provenance")
        if not isinstance(provenance, dict):
            raise ValidationError(f"lock entry for {service} is missing provenance")
        platform = provenance.get("platform")
        if not isinstance(platform, dict) or not platform.get("os") or not platform.get("architecture"):
            raise ValidationError(f"lock entry for {service} has an invalid provenance platform")
        if not DIGEST_PATTERN.fullmatch(str(provenance.get("platformManifestDigest", ""))):
            raise ValidationError(f"lock entry for {service} has an invalid platform digest")
        if not isinstance(provenance.get("predicateType"), str):
            raise ValidationError(f"lock entry for {service} is missing a provenance predicate type")
        if not DIGEST_PATTERN.fullmatch(str(provenance.get("attestationManifestDigest", ""))):
            raise ValidationError(f"lock entry for {service} has an invalid attestation digest")
    return images


def validate_compose(lock: dict[str, Any], compose_config: dict[str, Any]) -> list[str]:
    images = validate_lock(lock)
    services = compose_config.get("services")
    if not isinstance(services, dict):
        raise ValidationError("resolved Compose configuration has no services object")

    messages: list[str] = []
    for service, entry in images.items():
        service_config = services.get(service)
        if not isinstance(service_config, dict):
            raise ValidationError(f"Compose service {service} is missing")
        actual = service_config.get("image")
        expected = entry["reference"]
        if actual != expected:
            raise ValidationError(
                f"Compose service {service} must use {expected}; resolved image was {actual!r}"
            )
        messages.append(f"Compose service {service} is pinned to {entry['digest']}")
    return messages


def validate_registry_documents(
    service: str,
    entry: dict[str, Any],
    index: dict[str, Any],
    attestation: dict[str, Any],
) -> str:
    if index.get("mediaType") != "application/vnd.oci.image.index.v1+json":
        raise ValidationError(f"registry object for {service} is not an OCI image index")
    manifests = index.get("manifests")
    if not isinstance(manifests, list):
        raise ValidationError(f"registry index for {service} has no manifests")

    provenance = entry["provenance"]
    expected_platform = provenance["platform"]
    platform_manifest = next(
        (
            manifest
            for manifest in manifests
            if isinstance(manifest, dict)
            and manifest.get("platform", {}).get("os") == expected_platform["os"]
            and manifest.get("platform", {}).get("architecture")
            == expected_platform["architecture"]
        ),
        None,
    )
    if platform_manifest is None:
        raise ValidationError(f"registry index for {service} is missing the reviewed platform")

    expected_platform_digest = provenance["platformManifestDigest"]
    platform_digest = platform_manifest.get("digest")
    if platform_digest != expected_platform_digest:
        raise ValidationError(
            f"reviewed platform for {service} resolved to {platform_digest!r}, "
            f"expected {expected_platform_digest}"
        )

    expected_attestation_digest = provenance["attestationManifestDigest"]
    attestation_descriptor = next(
        (
            manifest
            for manifest in manifests
            if isinstance(manifest, dict)
            and manifest.get("digest") == expected_attestation_digest
            and manifest.get("annotations", {}).get("vnd.docker.reference.type")
            == "attestation-manifest"
            and manifest.get("annotations", {}).get("vnd.docker.reference.digest")
            == platform_digest
        ),
        None,
    )
    if attestation_descriptor is None:
        raise ValidationError(
            f"registry index for {service} does not bind the reviewed provenance attestation"
        )

    layers = attestation.get("layers")
    predicate_type = provenance["predicateType"]
    if not isinstance(layers, list) or not any(
        isinstance(layer, dict)
        and layer.get("mediaType") == "application/vnd.in-toto+json"
        and layer.get("annotations", {}).get("in-toto.io/predicate-type") == predicate_type
        for layer in layers
    ):
        raise ValidationError(
            f"reviewed attestation for {service} is missing predicate {predicate_type}"
        )
    return f"Registry index for {service} binds reviewed {predicate_type} provenance"


def run(command: list[str]) -> str:
    try:
        result = subprocess.run(
            command,
            check=True,
            capture_output=True,
            text=True,
            encoding="utf-8",
        )
    except FileNotFoundError as exc:
        raise ValidationError(f"required command is unavailable: {command[0]}") from exc
    except subprocess.CalledProcessError as exc:
        detail = (exc.stderr or exc.stdout or "unknown error").strip()
        raise ValidationError(f"command failed ({' '.join(command)}): {detail}") from exc
    return result.stdout.strip()


def resolve_compose(compose_path: Path, env_path: Path) -> dict[str, Any]:
    output = run(
        [
            "docker",
            "compose",
            "--env-file",
            str(env_path),
            "-f",
            str(compose_path),
            "config",
            "--format",
            "json",
        ]
    )
    try:
        config = json.loads(output)
    except json.JSONDecodeError as exc:
        raise ValidationError(f"docker compose returned invalid JSON: {exc}") from exc
    if not isinstance(config, dict):
        raise ValidationError("docker compose returned a non-object configuration")
    return config


def repository_from_reference(reference: str) -> str:
    tagged = reference.split("@", 1)[0]
    last_slash = tagged.rfind("/")
    last_colon = tagged.rfind(":")
    return tagged[:last_colon] if last_colon > last_slash else tagged


def verify_registry(service: str, entry: dict[str, Any]) -> str:
    reference = entry["reference"]
    resolved_digest = run(
        [
            "docker",
            "buildx",
            "imagetools",
            "inspect",
            reference,
            "--format",
            "{{.Manifest.Digest}}",
        ]
    )
    if resolved_digest != entry["digest"]:
        raise ValidationError(
            f"registry resolved {service} to {resolved_digest!r}, expected {entry['digest']}"
        )

    try:
        index = json.loads(
            run(["docker", "buildx", "imagetools", "inspect", reference, "--raw"])
        )
        attestation_digest = entry["provenance"]["attestationManifestDigest"]
        attestation_reference = f"{repository_from_reference(reference)}@{attestation_digest}"
        attestation = json.loads(
            run(
                [
                    "docker",
                    "buildx",
                    "imagetools",
                    "inspect",
                    attestation_reference,
                    "--raw",
                ]
            )
        )
    except json.JSONDecodeError as exc:
        raise ValidationError(f"registry returned invalid OCI JSON for {service}: {exc}") from exc
    return validate_registry_documents(service, entry, index, attestation)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--lock", type=Path, default=DEFAULT_LOCK)
    parser.add_argument("--compose", type=Path, default=DEFAULT_COMPOSE)
    parser.add_argument("--env-file", type=Path, default=DEFAULT_ENV)
    parser.add_argument(
        "--verify-registry",
        action="store_true",
        help="also verify the immutable registry digest and reviewed provenance attestation",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        lock = read_json(args.lock)
        compose_config = resolve_compose(args.compose, args.env_file)
        messages = validate_compose(lock, compose_config)
        if args.verify_registry:
            for service, entry in validate_lock(lock).items():
                messages.append(verify_registry(service, entry))
    except ValidationError as exc:
        print(f"IMAGE PIN VERIFICATION FAILED: {exc}", file=sys.stderr)
        return 1

    for message in messages:
        print(f"OK: {message}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
