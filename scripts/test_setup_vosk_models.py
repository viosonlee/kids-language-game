import hashlib
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest.mock import patch

import setup_vosk_models as setup


class ModelSetupTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)

    def archive(self, entries):
        path = self.root / "model.zip"
        with zipfile.ZipFile(path, "w") as zipped:
            for name in entries:
                zipped.writestr(name, b"model data")
        return path

    def test_checksum_rejects_truncated_or_changed_cache(self):
        archive = self.archive(["root/am/final.mdl"])
        metadata = {"bytes": archive.stat().st_size, "sha256": setup.sha256(archive)}
        setup.verify_archive(archive, metadata)
        archive.write_bytes(b"x" * metadata["bytes"])
        with self.assertRaises(ValueError):
            setup.verify_archive(archive, metadata)

    def test_traversal_never_escapes_staging(self):
        archive = self.archive(["../escaped"])
        with self.assertRaises(ValueError):
            setup.extract_model(archive, self.root / "cache", "model", "hash")
        self.assertFalse((self.root / "escaped").exists())
        self.assertFalse((self.root / "cache/model").exists())

    def test_incomplete_archive_never_replaces_existing_model(self):
        target = self.root / "cache/model"
        target.mkdir(parents=True)
        (target / "old").write_text("keep")
        with self.assertRaises(ValueError):
            setup.extract_model(self.archive(["root/am/final.mdl"]), target.parent, "model", "hash")
        self.assertEqual("keep", (target / "old").read_text())

    def test_complete_model_publishes_and_preserves_previous_directory(self):
        target = self.root / "cache/model"
        target.mkdir(parents=True)
        (target / "old").write_text("keep")
        archive = self.archive([f"root/{name}" for name in setup.REQUIRED])
        result = setup.extract_model(archive, target.parent, "model", "hash")
        self.assertTrue(setup.valid_model(result))
        backups = list(target.parent.glob("model.previous-*"))
        self.assertEqual(1, len(backups))
        self.assertEqual("keep", (backups[0] / "old").read_text())
        setup.extract_model(archive, target.parent, "model", "hash")
        self.assertEqual(1, len(list(target.parent.glob("model.previous-*"))))

    def test_device_selection_requires_unambiguous_online_device(self):
        for devices in ([], ["one", "two"]):
            with self.assertRaises(ValueError):
                setup.select_device(devices)
        with self.assertRaises(ValueError):
            setup.select_device(["one"], "offline")
        self.assertEqual("two", setup.select_device(["one", "two"], "two"))

    def test_setup_failure_returns_nonzero(self):
        with patch.object(setup, "get_archive", side_effect=ValueError("bad checksum")):
            self.assertEqual(1, setup.main(["--download-only", "--lang", "ja"]))


if __name__ == "__main__":
    unittest.main()
