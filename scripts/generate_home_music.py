"""Generate the original, quiet home-screen music loop using only Python stdlib.
Author / Agent: Codex; Date: 2026-09-28.
"""
import math
import struct
import wave
from pathlib import Path

RATE = 22050
NOTES = (261.63, 329.63, 392.00, 440.00, 392.00, 329.63, 293.66, 261.63)


def generate():
    target = Path(__file__).resolve().parents[1] / "app/src/main/assets/audio/music/home.wav"
    target.parent.mkdir(parents=True, exist_ok=True)
    samples = bytearray()
    for frequency in NOTES:
        for index in range(RATE * 2):
            t = index / RATE
            envelope = math.sin(math.pi * t / 2) ** 2
            tone = math.sin(2 * math.pi * frequency * t) + 0.12 * math.sin(4 * math.pi * frequency * t)
            samples.extend(struct.pack("<h", round(32767 * 0.2 * envelope * tone)))
    with wave.open(str(target), "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(RATE)
        out.writeframes(samples)
    print(target)


if __name__ == "__main__":
    generate()
