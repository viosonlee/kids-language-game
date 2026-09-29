import hashlib
from pathlib import Path
import tempfile
import unittest

from prepare_app_update import create_manifest, parse_badging


class PrepareUpdateTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.apk = Path(self.directory.name) / "test.apk"
        self.apk.write_bytes(b"test-package-bytes")
        self.identity = {"packageName": "com.kidslanguage.companion.debug", "versionCode": 2,
                         "versionName": "1.0.1", "minSdk": 29}

    def test_hash_and_size_come_from_binary(self):
        result = create_manifest(self.apk, self.identity, "https://example.com/app.apk", "升级说明")
        self.assertEqual(hashlib.sha256(self.apk.read_bytes()).hexdigest(), result["sha256"])
        self.assertEqual(self.apk.stat().st_size, result["bytes"])
        self.assertEqual("升级说明", result["notes"])

    def test_parse_actual_aapt_identity(self):
        result = parse_badging("package: name='com.kidslanguage.companion.debug' versionCode='2' versionName='1.0.1' platformBuildVersionName='15'\nsdkVersion:'29'\n")
        self.assertEqual(self.identity, result)

    def test_missing_metadata_fails(self):
        for value in ("", "package: name='x' versionCode='2' versionName='1.0.1'"):
            with self.assertRaises(ValueError):
                parse_badging(value)

    def test_unsafe_urls_fail(self):
        for url in ("http://example.com/app.apk", "file:///app.apk", "https://user:pass@example.com/app.apk", "https:///app.apk", "https://example.com/app.apk#fragment"):
            with self.subTest(url=url), self.assertRaises(ValueError):
                create_manifest(self.apk, self.identity, url)

    def test_empty_binary_fails(self):
        self.apk.write_bytes(b"")
        with self.assertRaises(ValueError):
            create_manifest(self.apk, self.identity, "https://example.com/app.apk")

    def test_invalid_version_and_notes_fail(self):
        with self.assertRaises(ValueError):
            create_manifest(self.apk, {**self.identity, "versionCode": 0}, "https://example.com/app.apk")
        with self.assertRaises(ValueError):
            create_manifest(self.apk, self.identity, "https://example.com/app.apk", "x" * 8001)


if __name__ == "__main__":
    unittest.main()
