from __future__ import annotations

import copy
import json
import sys
import unittest
from pathlib import Path


DEPLOY_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(DEPLOY_DIR))

from verify_image_pins import ValidationError, validate_compose, validate_registry_documents


class ImagePinVerificationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.lock = json.loads((DEPLOY_DIR / "image-lock.json").read_text(encoding="utf-8"))
        cls.entry = cls.lock["images"]["redis"]

    def exact_config(self) -> dict:
        return {
            "services": {
                service: {"image": entry["reference"]}
                for service, entry in self.lock["images"].items()
            }
        }

    def test_accepts_exact_compose_pin(self) -> None:
        config = self.exact_config()

        messages = validate_compose(self.lock, config)

        self.assertEqual(len(self.lock["images"]), len(messages))

    def test_rejects_mutable_tag(self) -> None:
        for service, entry in self.lock["images"].items():
            with self.subTest(service=service):
                config = self.exact_config()
                config["services"][service]["image"] = entry["tag"]

                with self.assertRaisesRegex(ValidationError, "must use"):
                    validate_compose(self.lock, config)

    def test_rejects_digest_mismatch(self) -> None:
        wrong_digest = "sha256:" + "0" * 64
        for service, entry in self.lock["images"].items():
            with self.subTest(service=service):
                config = self.exact_config()
                config["services"][service]["image"] = (
                    f"{entry['tag']}@{wrong_digest}"
                )

                with self.assertRaisesRegex(ValidationError, "must use"):
                    validate_compose(self.lock, config)

    def test_rejects_lock_without_provenance(self) -> None:
        lock = copy.deepcopy(self.lock)
        del lock["images"]["redis"]["provenance"]
        config = self.exact_config()

        with self.assertRaisesRegex(ValidationError, "missing provenance"):
            validate_compose(lock, config)

    def test_rejects_lock_without_platform_digest(self) -> None:
        lock = copy.deepcopy(self.lock)
        del lock["images"]["redis"]["provenance"]["platformManifestDigest"]

        with self.assertRaisesRegex(ValidationError, "invalid platform digest"):
            validate_compose(lock, self.exact_config())

    def test_rejects_attestation_without_reviewed_predicate(self) -> None:
        provenance = self.entry["provenance"]
        platform_digest = provenance["platformManifestDigest"]
        index = {
            "mediaType": "application/vnd.oci.image.index.v1+json",
            "manifests": [
                {
                    "digest": platform_digest,
                    "platform": provenance["platform"],
                },
                {
                    "digest": provenance["attestationManifestDigest"],
                    "annotations": {
                        "vnd.docker.reference.type": "attestation-manifest",
                        "vnd.docker.reference.digest": platform_digest,
                    },
                },
            ],
        }
        attestation = {
            "layers": [
                {
                    "mediaType": "application/vnd.in-toto+json",
                    "annotations": {"in-toto.io/predicate-type": "https://spdx.dev/Document"},
                }
            ]
        }

        with self.assertRaisesRegex(ValidationError, "missing predicate"):
            validate_registry_documents("redis", self.entry, index, attestation)

    def test_rejects_unreviewed_platform_manifest(self) -> None:
        provenance = self.entry["provenance"]
        actual_platform_digest = "sha256:" + "1" * 64
        index = {
            "mediaType": "application/vnd.oci.image.index.v1+json",
            "manifests": [
                {
                    "digest": actual_platform_digest,
                    "platform": provenance["platform"],
                }
            ],
        }

        with self.assertRaisesRegex(ValidationError, "reviewed platform"):
            validate_registry_documents("redis", self.entry, index, {"layers": []})


if __name__ == "__main__":
    unittest.main()
