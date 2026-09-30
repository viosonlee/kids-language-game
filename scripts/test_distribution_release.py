import hashlib
import json
from pathlib import Path
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import patch

from distribution.verify_release import signer_digests, validate


class DistributionReleaseTests(unittest.TestCase):
    def setUp(self):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        self.root = Path(directory.name)
        (self.root / "artifacts").mkdir()
        self.apk = b"prebuilt package test data"
        (self.root / "artifacts/app-debug.apk").write_bytes(self.apk)
        self.digest = "1234abcd" * 8
        (self.root / "signer-sha256.txt").write_text(self.digest)
        self.manifest = {"schemaVersion": 1, "packageName": "com.kidslanguage.companion.debug",
                         "versionCode": 2, "versionName": "1.0.1", "minSdk": 29,
                         "apkUrl": "https://github.com/owner/release/releases/download/v1.0.1-2/app-debug.apk",
                         "bytes": len(self.apk), "sha256": hashlib.sha256(self.apk).hexdigest(), "notes": "新版本"}
        self.write_manifest()

    def write_manifest(self):
        (self.root / "artifacts/update-debug.json").write_text(json.dumps(self.manifest), encoding="utf-8")

    def tools(self, args, **kwargs):
        if "verify" in args:
            return SimpleNamespace(stdout=f"Signer #1 certificate SHA-256 digest: {self.digest}\n")
        return SimpleNamespace(stdout="package: name='com.kidslanguage.companion.debug' versionCode='2' versionName='1.0.1'\nsdkVersion:'29'\n")

    def test_same_certificate_can_be_repeated_or_uppercase(self):
        text = f"Signer #1 certificate SHA-256 digest: {self.digest}\nSigner #1 certificate SHA-256 digest: {self.digest.upper()}\n"
        self.assertEqual({self.digest}, signer_digests(text))
        self.assertEqual(set(), signer_digests("certificate SHA-256 digest: invalid"))

    @patch("distribution.verify_release.subprocess.run")
    def test_valid_inputs_produce_matching_tag_and_notes(self, run):
        run.side_effect = self.tools
        manifest, tag = validate(self.root, "owner/release", self.root)
        self.assertEqual("v1.0.1-2", tag)
        self.assertEqual("新版本", (self.root / "release-notes.txt").read_text(encoding="utf-8"))
        self.assertEqual(self.manifest, manifest)

    def test_tampered_binary_is_rejected_before_tools_run(self):
        (self.root / "artifacts/app-debug.apk").write_bytes(self.apk[::-1])
        with patch("distribution.verify_release.subprocess.run") as run:
            with self.assertRaisesRegex(AssertionError, "hash mismatch"):
                validate(self.root, "owner/release", self.root)
            run.assert_not_called()

    @patch("distribution.verify_release.subprocess.run")
    def test_wrong_signer_is_rejected(self, run):
        run.side_effect = self.tools
        (self.root / "signer-sha256.txt").write_text("f" * 64)
        with self.assertRaisesRegex(AssertionError, "signer mismatch"):
            validate(self.root, "owner/release", self.root)

    @patch("distribution.verify_release.subprocess.run")
    def test_actual_apk_version_must_match_manifest(self, run):
        run.side_effect = self.tools
        self.manifest["minSdk"] = 30
        self.write_manifest()
        with self.assertRaises(AssertionError):
            validate(self.root, "owner/release", self.root)

    def test_different_destination_is_rejected(self):
        with self.assertRaisesRegex(AssertionError, "APK URL must match"):
            validate(self.root, "other/repository", self.root)


if __name__ == "__main__":
    unittest.main()
