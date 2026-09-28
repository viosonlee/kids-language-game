#!/usr/bin/env python3
"""Verify, prepare and optionally deploy the models declared by the application.
Author / Agent: Codex; Date: 2026-09-28.
Normal APK installation already bundles all models and does not need this tool.
"""
import argparse
import hashlib
import json
import re
import shutil
import stat
import subprocess
import sys
import tempfile
import urllib.request
import uuid
import zipfile
from pathlib import Path, PurePosixPath

BASE_DIR = Path(__file__).resolve().parent.parent
ASSETS = BASE_DIR / "app/src/main/assets/models"
CACHE_DIR = BASE_DIR / "models"
LANGUAGES = {"ja": "ja-JP", "cn": "zh-CN", "zh": "zh-CN", "en": "en-US"}
REQUIRED = ("am/final.mdl", "conf/mfcc.conf", "conf/model.conf", "graph/HCLr.fst",
            "graph/Gr.fst", "graph/phones/word_boundary.int")


def sha256(path):
    with path.open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def verify_archive(path, model):
    if not path.is_file() or path.stat().st_size != model["bytes"] or sha256(path) != model["sha256"]:
        raise ValueError(f"Invalid model archive: {path}")


def valid_model(path):
    return all((path / name).is_file() and (path / name).stat().st_size > 0 for name in REQUIRED)


def get_archive(model):
    bundled = ASSETS / model["file"]
    if bundled.exists():
        verify_archive(bundled, model)
        return bundled
    CACHE_DIR.mkdir(parents=True, exist_ok=True)
    cached = CACHE_DIR / model["file"]
    if cached.exists():
        try:
            verify_archive(cached, model)
            return cached
        except ValueError:
            pass
    with tempfile.TemporaryDirectory(prefix="download-", dir=CACHE_DIR) as temp:
        partial = Path(temp) / "archive.zip"
        with urllib.request.urlopen(model["source"], timeout=60) as source, partial.open("wb") as out:
            shutil.copyfileobj(source, out)
        verify_archive(partial, model)
        partial.replace(cached)
    return cached


def extract_model(archive, parent, name, expected_hash):
    parent = parent.resolve()
    parent.mkdir(parents=True, exist_ok=True)
    target = parent / name
    if target.resolve().parent != parent or target.is_symlink():
        raise ValueError("Model target must remain inside cache")
    marker = target / ".source-sha256"
    if valid_model(target) and marker.is_file() and marker.read_text().strip() == expected_hash:
        return target
    with tempfile.TemporaryDirectory(prefix="extract-", dir=parent) as temp:
        staging = Path(temp)
        roots = set()
        with zipfile.ZipFile(archive) as zipped:
            for info in zipped.infolist():
                path = PurePosixPath(info.filename)
                if (path.is_absolute() or ".." in path.parts or "\\" in info.filename or
                        ":" in info.filename or stat.S_ISLNK(info.external_attr >> 16) or not path.parts):
                    raise ValueError(f"Unsafe archive member: {info.filename}")
                roots.add(path.parts[0])
                destination = staging.joinpath(*path.parts)
                if not destination.resolve().is_relative_to(staging.resolve()):
                    raise ValueError("Archive escaped staging directory")
                if info.is_dir():
                    destination.mkdir(parents=True, exist_ok=True)
                else:
                    destination.parent.mkdir(parents=True, exist_ok=True)
                    with zipped.open(info) as source, destination.open("wb") as out:
                        shutil.copyfileobj(source, out)
        if len(roots) != 1:
            raise ValueError("Expected a single model root")
        prepared = staging / roots.pop()
        if not valid_model(prepared):
            raise ValueError("Incomplete model: required files missing")
        (prepared / ".source-sha256").write_text(expected_hash, encoding="utf-8")
        backup = parent / f"{name}.previous-{uuid.uuid4().hex}"
        had_previous = target.exists()
        if had_previous:
            target.rename(backup)
        try:
            prepared.rename(target)
        except Exception:
            if had_previous:
                backup.rename(target)
            raise
        if had_previous:
            print(f"Previous directory retained at {backup}")
    return target


def select_device(devices, requested=None):
    if requested:
        if requested not in devices:
            raise ValueError("Requested device is not online/authorized")
        return requested
    if len(devices) != 1:
        raise ValueError("Connect one authorized device or select it with --device")
    return devices[0]


def deploy(local, adb, device, package):
    base = [adb, "-s", device]
    installed = subprocess.run(base + ["shell", "pm", "path", package], check=True, capture_output=True, text=True)
    if not installed.stdout.strip().startswith("package:"):
        raise ValueError(f"Application is not installed: {package}")
    root = f"/sdcard/Android/data/{package}/files/models"
    target = f"{root}/{local.name}"
    staging = f"{root}/{local.name}.staging-{uuid.uuid4().hex}"
    backup = f"{target}.previous-{uuid.uuid4().hex}"
    subprocess.run(base + ["shell", "mkdir", "-p", staging], check=True)
    subprocess.run(base + ["push", local.as_posix() + "/.", staging], check=True)
    for name in REQUIRED:
        result = subprocess.run(base + ["shell", "sha256sum", f"{staging}/{name}"],
                                check=True, capture_output=True, text=True)
        if result.stdout.split()[0] != sha256(local / name):
            raise ValueError(f"Device checksum mismatch: {name}; staging retained at {staging}")
    exists = subprocess.run(base + ["shell", "test", "-d", target]).returncode == 0
    if exists:
        subprocess.run(base + ["shell", "mv", target, backup], check=True)
    try:
        subprocess.run(base + ["shell", "mv", staging, target], check=True)
    except Exception:
        if exists:
            subprocess.run(base + ["shell", "mv", backup, target], check=True)
        raise
    print(f"Installed {local.name}; reopen the app to load it. Previous directory: {backup if exists else 'none'}")


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--lang", choices=["all", *LANGUAGES], default="all")
    modes = parser.add_mutually_exclusive_group()
    modes.add_argument("--download-only", action="store_true")
    modes.add_argument("--push-only", action="store_true")
    parser.add_argument("--device")
    parser.add_argument("--adb", default="adb")
    parser.add_argument("--package", default="com.kidslanguage.companion.debug")
    args = parser.parse_args(argv)
    if not re.fullmatch(r"[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z][A-Za-z0-9_]*)+", args.package):
        parser.error("Invalid Android package name")
    try:
        models = json.loads((ASSETS / "manifest.json").read_text(encoding="utf-8"))["models"]
        selected = models if args.lang == "all" else [m for m in models if m["language"] == LANGUAGES[args.lang]]
        if not selected:
            raise ValueError("No matching model in manifest")
        prepared = []
        for model in selected:
            name = Path(model["file"]).stem
            target = CACHE_DIR / name
            if args.push_only:
                marker = target / ".source-sha256"
                if not valid_model(target) or not marker.is_file() or marker.read_text().strip() != model["sha256"]:
                    raise ValueError(f"Prepare verified model first: {name}")
            else:
                target = extract_model(get_archive(model), CACHE_DIR, name, model["sha256"])
            prepared.append(target)
        if not args.download_only:
            result = subprocess.run([args.adb, "devices"], check=True, capture_output=True, text=True)
            devices = [parts[0] for line in result.stdout.splitlines()[1:]
                       if len(parts := line.split()) == 2 and parts[1] == "device"]
            device = select_device(devices, args.device)
            for model in prepared:
                deploy(model, args.adb, device, args.package)
        print(f"Successfully {'prepared' if args.download_only else 'deployed'} {len(prepared)} model(s).")
        return 0
    except Exception as error:
        print(f"Model setup failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
