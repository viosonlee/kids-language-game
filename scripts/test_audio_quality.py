import json
import io
from contextlib import redirect_stdout
from pathlib import Path
import tempfile
import unittest
import wave
from types import SimpleNamespace
from unittest.mock import patch

from check_audio_quality import gain_for, item_audio_files, normalize, parse_loudness, measure, quality_failures, main


class AudioQualityTests(unittest.TestCase):
    def test_cli_names_failed_voice_and_keeps_nonzero_exit_and_report(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp) / "assets"
            (root / "audio/sfx").mkdir(parents=True)
            with wave.open(str(root / "audio/sfx/success.wav"), "wb") as chime:
                chime.setnchannels(1)
                chime.setsampwidth(2)
                chime.setframerate(24000)
                chime.writeframes(b"\x00\x00" * 13200)
            report = Path(temp) / "report.json"
            def measured(path):
                return {"duration_seconds": 0.96 if path.name == "paopao_praise_effort.mp3" else 0.8,
                        "lufs": -20.45, "true_peak_dbtp": -5.89}
            output = io.StringIO()
            with patch("sys.argv", ["check_audio_quality.py", "--assets-root", str(root), "--report", str(report)]), \
                    patch("check_audio_quality.item_audio_files", return_value=[]), \
                    patch("check_audio_quality.measure", side_effect=measured), \
                    patch("check_audio_quality.run", return_value=SimpleNamespace(stdout="ffmpeg test")), \
                    redirect_stdout(output):
                self.assertEqual(1, main())
            self.assertIn("FAIL audio/paopao/paopao_praise_effort.mp3: decoded duration 0.960000s exceeds 0.950s", output.getvalue())
            result = json.loads(report.read_text(encoding="utf-8"))
            self.assertFalse(result["passed"])
            self.assertEqual(1, sum(not row["passed"] for row in result["files"]))

    def test_mp3_container_padding_does_not_change_decoded_duration(self):
        with tempfile.TemporaryDirectory() as temp:
            source = Path(temp) / "paopao_praise_effort.mp3"
            source.write_bytes(b"same encoded audio")
            for container_duration in ("0.960000", "0.911375"):
                def command(*args):
                    if args[0] == "ffprobe":
                        return SimpleNamespace(stdout=json.dumps({"streams": [{"codec_type": "audio",
                            "sample_rate": "24000", "channels": 1}], "format": {"duration": container_duration}}))
                    if "loudnorm=I=-20:TP=-1.5:LRA=7:print_format=json" in args:
                        return SimpleNamespace(stderr='{"input_i":"-20.45","input_tp":"-5.89"}')
                    with wave.open(str(args[-1]), "wb") as decoded:
                        decoded.setnchannels(1)
                        decoded.setsampwidth(2)
                        decoded.setframerate(24000)
                        decoded.writeframes(b"\x00\x00" * 21873)
                    return SimpleNamespace()
                with patch("check_audio_quality.run", side_effect=command):
                    values = measure(source)
                self.assertEqual(0.911375, values["duration_seconds"])
                self.assertEqual(float(container_duration), values["container_duration_seconds"])
                self.assertEqual([], quality_failures(source, values))

    def test_actual_overlong_voice_and_audio_limits_still_fail_with_reasons(self):
        failures = quality_failures(Path("paopao_praise_effort.mp3"),
            {"duration_seconds": 0.96, "lufs": -23, "true_peak_dbtp": -1})
        self.assertEqual(3, len(failures))
        self.assertIn("0.960000s exceeds 0.950s", failures[-1])
        self.assertEqual([], quality_failures(Path("paopao_praise_effort.mp3"),
            {"duration_seconds": 0.95, "lufs": -21, "true_peak_dbtp": -1.5}))

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
