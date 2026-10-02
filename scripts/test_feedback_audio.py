from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from feedback_audio import prepare_short_voice, trailing_end


class ShortFeedbackTests(unittest.TestCase):
    def test_only_trailing_silence_is_trimmed_with_a_tail(self):
        self.assertAlmostEqual(0.92, trailing_end('silence_start: 0.8\nsilence_end: 2.0', 2.0))
        self.assertEqual(2.0, trailing_end('silence_start: 0.8\nsilence_end: 1.2', 2.0))
        self.assertEqual(2.0, trailing_end('silence_start: 1.9\nsilence_end: 2.0', 2.0))
        self.assertEqual(2.0, trailing_end('', 2.0))

    def test_overlong_voice_does_not_replace_existing_asset(self):
        with tempfile.TemporaryDirectory() as temp:
            destination = Path(temp) / 'voice.mp3'
            destination.write_bytes(b'original')
            measured = {'lufs': -20, 'true_peak_dbtp': -6, 'duration_seconds': 2.0,
                        'sample_rate': 24000, 'channels': 1}
            with patch('feedback_audio.measure', return_value=measured), patch('feedback_audio.normalize', return_value=measured), \
                    patch('feedback_audio.run') as command:
                command.return_value.stderr = ''
                with self.assertRaises(ValueError):
                    prepare_short_voice(Path('source.mp3'), destination)
            self.assertEqual(b'original', destination.read_bytes())
