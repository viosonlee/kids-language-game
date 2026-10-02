"""Short spoken reward preparation and release limits (Codex, 2026-10-03)."""
from pathlib import Path
import re
import tempfile

from check_audio_quality import (MAX_VOICE_SECONDS, SHORT_FEEDBACK_FILES, compliant, measure, normalize, run)


def trailing_end(stderr, duration):
    starts = re.findall(r"silence_start: ([0-9.]+)", stderr)
    ends = re.findall(r"silence_end: ([0-9.]+)", stderr)
    if not starts or not ends or abs(float(ends[-1]) - duration) > 0.03:
        return duration
    start = float(starts[-1])
    return min(duration, start + 0.12) if duration - start >= 0.3 else duration


def prepare_short_voice(source: Path, destination: Path):
    before = measure(source)
    detected = run("ffmpeg", "-nostdin", "-hide_banner", "-i", str(source),
                   "-af", "silencedetect=noise=-55dB:d=0.2", "-f", "null", "-").stderr
    end = trailing_end(detected, before["duration_seconds"])
    with tempfile.TemporaryDirectory(prefix="short-voice-", dir=destination.parent) as temp:
        output = Path(temp) / "voice.mp3"
        run("ffmpeg", "-nostdin", "-hide_banner", "-loglevel", "error", "-i", str(source),
            "-af", f"atrim=end={end:.6f},asetpts=PTS-STARTPTS", "-ar", str(before["sample_rate"]),
            "-ac", str(before["channels"]), "-codec:a", "libmp3lame", "-b:a", "96k", str(output))
        after = normalize(output, measure(output))
        if not compliant(after) or after["duration_seconds"] > MAX_VOICE_SECONDS:
            raise ValueError(f"Short reward exceeds audio limits: {destination.name}")
        output.replace(destination)
        return after
