#!/usr/bin/env python3
"""
Vosk Offline ASR Model Automation Setup Script
Downloads official lightweight Kaldi/Vosk speech models for Japanese, Chinese, and English,
and deploys them via ADB directly to connected Android devices or emulators.

Target on device:
/sdcard/Android/data/com.kidslanguage.companion/files/models/
  ├── vosk-model-small-ja
  ├── vosk-model-small-cn
  └── vosk-model-small-en-us

Author / Agent: Antigravity Agent
Date: 2026-09-28
"""

import argparse
import os
import shutil
import subprocess
import sys
import urllib.request
import zipfile
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent
CACHE_DIR = BASE_DIR / "models"

MODELS = {
    "ja": {
        "url": "https://alphacephei.com/vosk/models/vosk-model-small-ja-0.22.zip",
        "zip_name": "vosk-model-small-ja-0.22.zip",
        "extracted_name": "vosk-model-small-ja-0.22",
        "target_dir_name": "vosk-model-small-ja",
    },
    "cn": {
        "url": "https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip",
        "zip_name": "vosk-model-small-cn-0.22.zip",
        "extracted_name": "vosk-model-small-cn-0.22",
        "target_dir_name": "vosk-model-small-cn",
    },
    "en": {
        "url": "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip",
        "zip_name": "vosk-model-small-en-us-0.15.zip",
        "extracted_name": "vosk-model-small-en-us-0.15",
        "target_dir_name": "vosk-model-small-en-us",
    },
}

DEVICE_TARGET_DIR = "/sdcard/Android/data/com.kidslanguage.companion/files/models"

def download_file(url: str, dest: Path):
    dest.parent.mkdir(parents=True, exist_ok=True)
    if dest.exists() and dest.stat().st_size > 1024 * 1024:
        print(f"  [Cached] {dest.name} already exists ({dest.stat().st_size / 1024 / 1024:.1f} MB). Skipping download.")
        return

    print(f"  [Downloading] {url} -> {dest.name} ...")
    def report_hook(block_num, block_size, total_size):
        if total_size > 0:
            percent = (block_num * block_size / total_size) * 100
            print(f"\r    Progress: {percent:.1f}% ({block_num * block_size / 1024 / 1024:.1f}MB)", end="", flush=True)

    urllib.request.urlretrieve(url, str(dest), reporthook=report_hook)
    print("\n  [Done] Download completed.")

def extract_zip(zip_path: Path, extract_to: Path, extracted_name: str, target_name: str) -> Path:
    extract_to.mkdir(parents=True, exist_ok=True)
    final_target = extract_to / target_name
    if final_target.exists() and any(final_target.iterdir()):
        print(f"  [Ready] Model directory {final_target.name} already extracted.")
        return final_target

    print(f"  [Extracting] {zip_path.name} ...")
    with zipfile.ZipFile(str(zip_path), 'r') as zip_ref:
        zip_ref.extractall(str(extract_to))

    raw_extracted = extract_to / extracted_name
    if raw_extracted.exists() and raw_extracted != final_target:
        if final_target.exists():
            shutil.rmtree(str(final_target))
        raw_extracted.rename(final_target)
    print(f"  [Extracted] Ready at: {final_target}")
    return final_target

def check_adb_devices() -> list[str]:
    try:
        res = subprocess.run(["adb", "devices"], capture_output=True, text=True, check=True)
        devices = []
        for line in res.stdout.strip().splitlines()[1:]:
            parts = line.split()
            if len(parts) >= 2 and parts[1] == "device":
                devices.append(parts[0])
        return devices
    except Exception as e:
        print(f"[Warning] Failed to run adb command: {e}")
        return []

def push_to_device(local_model_dir: Path, device_id: str = None):
    target_name = local_model_dir.name
    cmd_base = ["adb"]
    if device_id:
        cmd_base += ["-s", device_id]

    print(f"  [ADB Push] Ensuring remote directory {DEVICE_TARGET_DIR} exists on device...")
    subprocess.run(cmd_base + ["shell", "mkdir", "-p", DEVICE_TARGET_DIR], check=False)

    dest_path = f"{DEVICE_TARGET_DIR}/{target_name}"
    print(f"  [ADB Push] Pushing {local_model_dir.name} -> {dest_path} ...")
    res = subprocess.run(cmd_base + ["push", str(local_model_dir), DEVICE_TARGET_DIR], check=True)
    if res.returncode == 0:
        print(f"  [ADB Push OK] Successfully pushed {target_name} to device.")
    else:
        print(f"  [ADB Push ERROR] Failed to push {target_name} to device.", file=sys.stderr)

def main():
    parser = argparse.ArgumentParser(description="Vosk Offline Model Setup & ADB Deployer")
    parser.add_argument("--lang", choices=["all", "ja", "cn", "en"], default="all", help="Language to set up")
    parser.add_argument("--download-only", action="store_true", help="Only download & unpack locally without pushing")
    parser.add_argument("--push-only", action="store_true", help="Only push without downloading")
    parser.add_argument("--device", help="Specific ADB device serial ID")
    args = parser.parse_args()

    langs_to_process = ["ja", "cn", "en"] if args.lang == "all" else [args.lang]

    print("==================================================")
    print("Vosk Offline ASR Model Setup Automation")
    print(f"Languages: {', '.join(langs_to_process)}")
    print(f"Local Storage: {CACHE_DIR}")
    print("==================================================")

    ready_model_dirs = []

    for lang in langs_to_process:
        cfg = MODELS[lang]
        zip_dest = CACHE_DIR / cfg["zip_name"]
        print(f"\n--- Processing Language: {lang.upper()} ---")

        if not args.push_only:
            try:
                download_file(cfg["url"], zip_dest)
            except Exception as e:
                print(f"  [Download Error] {e}", file=sys.stderr)
                continue

        if zip_dest.exists() or (CACHE_DIR / cfg["target_dir_name"]).exists():
            model_dir = extract_zip(zip_dest, CACHE_DIR, cfg["extracted_name"], cfg["target_dir_name"])
            ready_model_dirs.append(model_dir)

    if args.download_only:
        print("\n[Done] Models downloaded and extracted locally.")
        return

    # ADB Deploy
    print("\n--- Deploying to Android Device ---")
    devices = check_adb_devices()
    if not devices:
        print("  [Notice] No ADB device detected.")
        print(f"  To deploy models manually later, connect your Android tablet and run:")
        for m in ready_model_dirs:
            print(f"    adb push \"{m}\" {DEVICE_TARGET_DIR}")
        return

    target_device = args.device or devices[0]
    print(f"  Detected devices: {devices}")
    print(f"  Target device: {target_device}")

    for m in ready_model_dirs:
        push_to_device(m, target_device)

    print("\n==================================================")
    print("All selected Vosk models deployed successfully!")
    print("==================================================")

if __name__ == "__main__":
    main()
