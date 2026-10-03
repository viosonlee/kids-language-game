import hashlib
import json
from pathlib import Path
import struct
import unittest


class PracticeIconsTests(unittest.TestCase):
    def test_every_bundled_picture_and_distractor_is_offline_and_matches_its_manifest(self):
        assets = Path(__file__).resolve().parents[1] / "app/src/main/assets"
        root = assets / "images/practice"
        manifest = json.loads((root / "manifest.json").read_text(encoding="utf-8"))
        icons = {entry["asset"]: entry for entry in manifest["icons"]}
        self.assertEqual(len(icons), len(manifest["icons"]))
        used = set()
        for path in (assets / "content").glob("items_*.json"):
            for item in json.loads(path.read_text(encoding="utf-8")):
                for reference in [item["image"], *item["distractors"]]:
                    self.assertIn(reference, icons, item["id"])
                    used.add(reference)
        self.assertEqual(set(icons), used)
        for reference, entry in icons.items():
            self.assertTrue(reference.startswith("images/practice/emoji_u"))
            self.assertNotIn("..", reference)
            raw = (assets / reference).read_bytes()
            self.assertEqual(b"\x89PNG\r\n\x1a\n", raw[:8])
            self.assertEqual((512, 512), struct.unpack(">II", raw[16:24]))
            self.assertEqual(entry["sha256"], hashlib.sha256(raw).hexdigest())
        self.assertIn("Apache License", (root / "LICENSE.txt").read_text(encoding="utf-8"))
        self.assertIn(manifest["upstreamRevision"], (root / "NOTICE.txt").read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
