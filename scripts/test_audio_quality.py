import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from check_audio_quality import gain_for, item_audio_files, normalize, parse_loudness


class AudioQualityTests(unittest.TestCase):
    def test_compliant_audio_is_not_reencoded(self):
        measured = {"lufs": -20.45, "true_peak_dbtp": -3}
        with patch("check_audio_quality.run") as command:
            self.assertEqual(measured, normalize(Path("unused.mp3"), measured))
            command.assert_not_called()

    def test_silence_and_missing_measurement_are_rejected(self):
        for text in ('no audio', '{"input_i":"-inf","input_tp":"-inf"}'):
            with self.assertRaises(ValueError):
                parse_loudness(text)

    def test_peak_limit_prevents_clipping_instead_of_silently_limiting(self):
        with self.assertRaises(ValueError):
            gain_for({"lufs": -25, "true_peak_dbtp": -4})
        self.assertEqual(3, gain_for({"lufs": -23, "true_peak_dbtp": -8}))

    def test_catalog_cannot_escape_audio_directory(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            (root / "content").mkdir()
            (root / "outside.mp3").write_bytes(b"outside")
            (root / "content/items_en.json").write_text(json.dumps([{"audio": "outside.mp3"}]))
            with self.assertRaises(ValueError):
                item_audio_files(root)

    def test_failed_output_validation_preserves_original(self):
        with tempfile.TemporaryDirectory() as temp:
            original = Path(temp) / "speech.mp3"
            original.write_bytes(b"original")
            before = {"lufs": -23, "true_peak_dbtp": -8, "sample_rate": 24000,
                      "channels": 1, "duration_seconds": 2}
            for after in (dict(before, lufs=-20, true_peak_dbtp=0), dict(before, lufs=-20, duration_seconds=1)):
                with patch("check_audio_quality.run"), patch("check_audio_quality.measure", return_value=after):
                    with self.assertRaises(ValueError):
                        normalize(original, before)
                self.assertEqual(b"original", original.read_bytes())
                self.assertEqual([original], list(Path(temp).iterdir()))
