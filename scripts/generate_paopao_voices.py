#!/usr/bin/env python3
"""
Custom Voice Generator for Pao Pao (泡泡专属互动卡通语音生成管线)
Generates high-quality cartoon voice clips addressing Pao Pao by name
for greetings, praises, gentle encouragement, and final celebration.

Author / Agent: Antigravity Agent
Date: 2026-09-28
"""

import asyncio
import argparse
import sys
import tempfile
from pathlib import Path
import edge_tts
from feedback_audio import SHORT_FEEDBACK_FILES, prepare_short_voice

# Ensure utf-8 stdout on Windows
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except Exception:
        pass

BASE_DIR = Path(__file__).resolve().parent.parent
OUTPUT_DIR = BASE_DIR / "app" / "src" / "main" / "assets" / "audio" / "paopao"

PAOPAO_CLIPS = [
    {
        "name": "greeting",
        "text": "泡泡你好呀！我是兔兔 Momo，快来跟我一起玩吧！",
        "voice": "zh-CN-XiaoyiNeural",
        "pitch": "+8Hz",
        "rate": "-5%",
        "file": "paopao_greeting.mp3"
    },
    {
        "name": "praise_1",
        "text": "真棒！",
        "voice": "zh-CN-XiaoyiNeural",
        "pitch": "+8Hz",
        "rate": "+0%",
        "file": "paopao_praise_1.mp3"
    },
    {
        "name": "praise_2",
        "text": "太棒啦！",
        "voice": "zh-CN-XiaoyiNeural",
        "pitch": "+8Hz",
        "rate": "+0%",
        "file": "paopao_praise_2.mp3"
    },
    {
        "name": "praise_effort",
        "text": "好样的！",
        "voice": "zh-CN-XiaoyiNeural",
        "pitch": "+8Hz",
        "rate": "+0%",
        "file": "paopao_praise_effort.mp3"
    },
    {
        "name": "try_again",
        "text": "不要着急，泡泡，跟着兔子再听一次吧！",
        "voice": "zh-CN-XiaoyiNeural",
        "pitch": "+6Hz",
        "rate": "-5%",
        "file": "paopao_try_again.mp3"
    },
    {
        "name": "guide_repeat",
        "text": "泡泡，按住大麦克风，大声说出来吧！",
        "voice": "zh-CN-XiaoyiNeural",
        "pitch": "+8Hz",
        "rate": "-5%",
        "file": "paopao_guide_repeat.mp3"
    },
    {
        "name": "celebrate",
        "text": "哇！泡泡今天收集了好多大星星，太厉害啦！",
        "voice": "zh-CN-XiaoyiNeural",
        "pitch": "+10Hz",
        "rate": "-5%",
        "file": "paopao_celebrate.mp3"
    }
]

async def generate_clip(clip_info):
    output_path = OUTPUT_DIR / clip_info["file"]
    print(f"Generating [{clip_info['name']}]: {clip_info['text']} -> {clip_info['file']}")
    communicate = edge_tts.Communicate(
        text=clip_info["text"],
        voice=clip_info["voice"],
        pitch=clip_info["pitch"],
        rate=clip_info["rate"]
    )
    if clip_info["file"] in SHORT_FEEDBACK_FILES:
        with tempfile.TemporaryDirectory(prefix="tts-", dir=OUTPUT_DIR) as temp:
            source = Path(temp) / "source.mp3"
            await asyncio.wait_for(communicate.save(str(source)), timeout=45)
            prepare_short_voice(source, output_path)
    else:
        await communicate.save(str(output_path))
    print(f" Saved: {output_path} ({output_path.stat().st_size} bytes)")

async def main(short_only=False):
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    for clip in PAOPAO_CLIPS:
        if short_only and clip["file"] not in SHORT_FEEDBACK_FILES:
            continue
        await generate_clip(clip)
    print("\nAll Pao Pao voice clips generated successfully!")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--short-only", action="store_true")
    asyncio.run(main(parser.parse_args().short_only))
