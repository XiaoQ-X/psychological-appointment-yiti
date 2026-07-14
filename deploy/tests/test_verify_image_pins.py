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

    def test_accepts_exact_compose_pin(self) -> None:
        config = {"services": {"redis": {"image": self.entry["reference"]}}}

        messages = validate_compose(self.lock, config)

        self.assertEqual(1, len(messages))

    def test_rejects_mutable_tag(self) -> None:
        config = {"services": {"redis": {"image": self.entry["tag"]}}}

        with self.assertRaisesRegex(ValidationError, "must use"):
            validate_compose(self.lock, config)

    def test_rejects_digest_mismatch(self) -> None:
        wrong_digest = "sha256:" + "0" * 64
        config = {
            "services": {
                "redis": {"image": f"{self.entry['tag']}@{wrong_digest}"}
            }
        }

        with self.assertRaisesRegex(ValidationError, "must use"):
            validate_compose(self.lock, config)

    def test_rejects_lock_without_provenance(self) -> None:
        lock = copy.deepcopy(self.lock)
        del lock["images"]["redis"]["provenance"]
        config = {"services": {"redis": {"image": self.entry["reference"]}}}

        with self.assertRaisesRegex(ValidationError, "missing provenance"):
            validate_compose(lock, config)

    def test_rejects_attestation_without_reviewed_predicate(self) -> None:
        platform_digest = "sha256:" + "1" * 64
        provenance = self.entry["provenance"]
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


if __name__ == "__main__":
    unittest.main()
