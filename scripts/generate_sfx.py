#!/usr/bin/env python3
"""
Procedural Sound Effects (SFX) Generator for Kids Language Companion
Synthesizes cute, gentle, low-latency WAV sound effects for preschooler feedback:
- pop.wav: bubble pop for card/button taps
- success.wav: cheerful chime chord for correct answer
- celebrate.wav: fanfare sparkle for round completion
- retry.wav: gentle warm tone encouraging child to try again

Author / Agent: Antigravity Agent
Date: 2026-09-28
"""

import math
import struct
import wave
from pathlib import Path

SAMPLE_RATE = 44100
BASE_DIR = Path(__file__).resolve().parent.parent
SFX_DIR = BASE_DIR / "app" / "src" / "main" / "assets" / "audio" / "sfx"

def save_wav(filepath: Path, samples: list[float]):
    filepath.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(filepath), "w") as wav_file:
        # 1 channel (mono), 2 bytes per sample (16-bit), sample_rate, uncompressed
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)
        wav_file.setframerate(SAMPLE_RATE)
        
        packed = bytearray()
        for s in samples:
            # clamp to -1.0 .. 1.0
            clamped = max(-1.0, min(1.0, s))
            # convert to 16-bit signed integer
            val = int(clamped * 32767.0)
            packed.extend(struct.pack("<h", val))
        wav_file.writeframes(packed)
    print(f"Generated {filepath.name} ({len(samples)} samples, {len(samples)/SAMPLE_RATE:.2f}s)")

def generate_pop() -> list[float]:
    """Short bubble pop: frequency sweep 400Hz -> 1400Hz, rapid decay, 90ms"""
    duration = 0.09
    num_samples = int(duration * SAMPLE_RATE)
    samples = []
    phase = 0.0
    for i in range(num_samples):
        t = i / SAMPLE_RATE
        progress = i / num_samples
        # exponential frequency rise
        freq = 400.0 + 1000.0 * (progress ** 1.8)
        phase += 2.0 * math.pi * freq / SAMPLE_RATE
        # envelope: quick attack (5ms), fast decay
        if t < 0.005:
            env = t / 0.005
        else:
            env = math.exp(-28.0 * (t - 0.005))
        s = math.sin(phase) * env * 0.7
        samples.append(s)
    return samples

def generate_success() -> list[float]:
    """Cheerful major chime arpeggio: C6 (1046.5Hz) -> E6 (1318.5Hz) -> G6 (1567.9Hz), 500ms"""
    duration = 0.55
    num_samples = int(duration * SAMPLE_RATE)
    notes = [
        (0.00, 1046.5, 0.4),  # C6
        (0.08, 1318.5, 0.45), # E6
        (0.16, 1567.9, 0.55), # G6
        (0.24, 2093.0, 0.4),  # C7
    ]
    samples = [0.0] * num_samples
    
    for start_t, freq, amp in notes:
        start_idx = int(start_t * SAMPLE_RATE)
        note_len = num_samples - start_idx
        phase = 0.0
        for i in range(note_len):
            t = i / SAMPLE_RATE
            phase += 2.0 * math.pi * freq / SAMPLE_RATE
            # Harmonics for bell-like quality
            fundamental = math.sin(phase)
            harmonic2 = math.sin(phase * 2.0) * 0.25
            harmonic3 = math.sin(phase * 3.0) * 0.1
            tone = fundamental + harmonic2 + harmonic3
            
            # Envelope: 4ms attack, smooth exponential decay
            if t < 0.004:
                env = t / 0.004
            else:
                env = math.exp(-8.0 * (t - 0.004))
            samples[start_idx + i] += tone * env * amp
    return samples

def generate_celebrate() -> list[float]:
    """Celebratory sparkle fanfare chord + sparkle chime, 850ms"""
    duration = 0.85
    num_samples = int(duration * SAMPLE_RATE)
    notes = [
        (0.00, 783.99, 0.35),  # G5
        (0.08, 1046.5, 0.40),  # C6
        (0.16, 1318.5, 0.45),  # E6
        (0.24, 1567.9, 0.50),  # G6
        (0.32, 2093.0, 0.55),  # C7
        (0.40, 2637.0, 0.35),  # E7 sparkle
    ]
    samples = [0.0] * num_samples
    
    for start_t, freq, amp in notes:
        start_idx = int(start_t * SAMPLE_RATE)
        note_len = num_samples - start_idx
        phase = 0.0
        for i in range(note_len):
            t = i / SAMPLE_RATE
            phase += 2.0 * math.pi * freq / SAMPLE_RATE
            # Subtle vibrato for sparkle
            vibrato = 1.0 + 0.01 * math.sin(2.0 * math.pi * 12.0 * t)
            tone = math.sin(phase * vibrato) + 0.2 * math.sin(phase * 2.0)
            if t < 0.005:
                env = t / 0.005
            else:
                env = math.exp(-5.5 * (t - 0.005))
            samples[start_idx + i] += tone * env * amp
    return samples

def generate_retry() -> list[float]:
    """Gentle, warm, non-punitive marimba two-step (F5 -> D5), 320ms"""
    duration = 0.32
    num_samples = int(duration * SAMPLE_RATE)
    notes = [
        (0.00, 698.46, 0.4), # F5
        (0.12, 587.33, 0.4), # D5
    ]
    samples = [0.0] * num_samples
    
    for start_t, freq, amp in notes:
        start_idx = int(start_t * SAMPLE_RATE)
        note_len = num_samples - start_idx
        phase = 0.0
        for i in range(note_len):
            t = i / SAMPLE_RATE
            phase += 2.0 * math.pi * freq / SAMPLE_RATE
            # Warm marimba body: mostly sine with gentle warmth
            tone = math.sin(phase) + 0.15 * math.sin(phase * 2.0)
            if t < 0.003:
                env = t / 0.003
            else:
                env = math.exp(-12.0 * (t - 0.003))
            samples[start_idx + i] += tone * env * amp
    return samples

def main():
    print("Synthesizing cute procedural SFX for Kids Language Companion...")
    save_wav(SFX_DIR / "pop.wav", generate_pop())
    save_wav(SFX_DIR / "success.wav", generate_success())
    save_wav(SFX_DIR / "celebrate.wav", generate_celebrate())
    save_wav(SFX_DIR / "retry.wav", generate_retry())
    print("All SFX generated successfully in:", SFX_DIR)

if __name__ == "__main__":
    main()
