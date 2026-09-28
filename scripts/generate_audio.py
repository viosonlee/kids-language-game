#!/usr/bin/env python3
"""
AI Cartoon Audio Asset Generator for Kids Language Companion
Uses edge-tts to generate child-friendly, cute cartoon voice MP3 assets
for Japanese, Chinese, and English vocabulary items.

Author / Agent: Antigravity Agent
Date: 2026-09-28
"""

import asyncio
import json
import os
import sys
from pathlib import Path
import edge_tts

# Root directory of the repository
BASE_DIR = Path(__file__).resolve().parent.parent
ASSETS_DIR = BASE_DIR / "app" / "src" / "main" / "assets"
CONTENT_DIR = ASSETS_DIR / "content"
AUDIO_DIR = ASSETS_DIR / "audio"

CONFIGS = {
    "ja": {
        "voice": "ja-JP-NanamiNeural",
        "pitch": "+10Hz",
        "rate": "-10%",
        "json_path": CONTENT_DIR / "items_ja.json",
        "audio_subpath": "audio/ja",
        "audio_dir": AUDIO_DIR / "ja",
    },
    "zh": {
        "voice": "zh-CN-XiaoyiNeural",
        "pitch": "+8Hz",
        "rate": "-10%",
        "json_path": CONTENT_DIR / "items_zh.json",
        "audio_subpath": "audio/zh",
        "audio_dir": AUDIO_DIR / "zh",
    },
    "en": {
        "voice": "en-US-AnaNeural",
        "pitch": "+5Hz",
        "rate": "-10%",
        "json_path": CONTENT_DIR / "items_en.json",
        "audio_subpath": "audio/en",
        "audio_dir": AUDIO_DIR / "en",
    },
}

SEM = asyncio.Semaphore(4)

async def generate_single_audio(text: str, voice: str, pitch: str, rate: str, output_file: Path, max_retries: int = 3) -> bool:
    if output_file.exists() and output_file.stat().st_size > 0:
        return True

    for attempt in range(1, max_retries + 1):
        try:
            async with SEM:
                communicate = edge_tts.Communicate(text, voice, pitch=pitch, rate=rate)
                await communicate.save(str(output_file))
            if output_file.exists() and output_file.stat().st_size > 0:
                return True
        except Exception as e:
            if attempt == max_retries:
                print(f"  [ERROR] Failed to generate {output_file.name} for '{text}': {e}", file=sys.stderr)
                return False
            await asyncio.sleep(1.0 * attempt)
    return False

async def process_language(lang: str, cfg: dict):
    print(f"\n--- Processing Language: {lang.upper()} (Voice: {cfg['voice']}) ---")
    json_path = cfg["json_path"]
    audio_dir = cfg["audio_dir"]
    audio_subpath = cfg["audio_subpath"]
    audio_dir.mkdir(parents=True, exist_ok=True)

    with open(json_path, "r", encoding="utf-8") as f:
        items = json.load(f)

    tasks = []
    updated_items = []

    for item in items:
        item_id = item["id"]
        # In Japanese, use reading (hiragana) if present to avoid mispronounced kanji
        if lang == "ja":
            text_to_speak = item.get("reading") or item["text"]
        else:
            text_to_speak = item["text"]

        filename = f"{item_id}.mp3"
        out_path = audio_dir / filename
        rel_audio_path = f"{audio_subpath}/{filename}"

        # Update json item audio field
        item_copy = dict(item)
        item_copy["audio"] = rel_audio_path
        updated_items.append(item_copy)

        tasks.append((text_to_speak, out_path))

    # Run generation
    success_count = 0
    total_count = len(tasks)

    async def worker(idx: int, text: str, out_p: Path):
        ok = await generate_single_audio(
            text=text,
            voice=cfg["voice"],
            pitch=cfg["pitch"],
            rate=cfg["rate"],
            output_file=out_p,
        )
        return ok

    res_list = await asyncio.gather(*(worker(i, t, p) for i, (t, p) in enumerate(tasks)))
    success_count = sum(1 for r in res_list if r)

    print(f"Generated {success_count}/{total_count} audio files for {lang.upper()}.")

    # Save updated JSON
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(updated_items, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print(f"Updated metadata in {json_path.name}.")

async def main():
    print("==================================================")
    print("Starting AI Cartoon Audio Generation Pipeline")
    print("==================================================")
    for lang, cfg in CONFIGS.items():
        await process_language(lang, cfg)
    print("\n==================================================")
    print("All audio assets generated and metadata updated successfully!")
    print("==================================================")

if __name__ == "__main__":
    asyncio.run(main())
