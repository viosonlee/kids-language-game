"""Verify a signed APK and prepare a GitHub Releases update manifest (stdlib only)."""

import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
from urllib.parse import quote, urlsplit

MAX_APK_BYTES = 512 * 1024 * 1024


def parse_badging(text):
    package = re.search(r"^package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", text, re.M)
    sdk = re.search(r"^sdkVersion:'(\d+)'", text, re.M)
    if not package or not sdk:
        raise ValueError("Unable to read APK application ID, version and minSdk using aapt")
    return {"packageName": package[1], "versionCode": int(package[2]),
            "versionName": package[3], "minSdk": int(sdk[1])}


def create_manifest(apk, identity, apk_url, notes=""):
    parsed = urlsplit(apk_url)
    if parsed.scheme != "https" or not parsed.hostname or parsed.username is not None or parsed.password is not None or parsed.fragment:
        raise ValueError("APK URL must be HTTPS without credentials or fragments")
    if len(apk_url) > 4096 or len(notes) > 8000:
        raise ValueError("URL or release notes too long")
    if identity["versionCode"] <= 0 or not 0 < len(identity["versionName"]) <= 64 or identity["minSdk"] <= 0:
        raise ValueError("Invalid APK version or minSdk")
    size = apk.stat().st_size
    if not 0 < size <= MAX_APK_BYTES:
        raise ValueError("APK must be between 1 byte and 512 MiB")
    with apk.open("rb") as source:
        digest = hashlib.file_digest(source, "sha256").hexdigest()
    return {"schemaVersion": 1, **identity, "apkUrl": apk_url,
            "bytes": size, "sha256": digest, "notes": notes}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--aapt", type=Path, required=True)
    parser.add_argument("--apksigner", type=Path, required=True)
    parser.add_argument("--repository", required=True, help="Public binary-only owner/repository")
    parser.add_argument("--tag", required=True)
    parser.add_argument("--variant", choices=("debug", "release"), required=True)
    parser.add_argument("--notes", type=Path)
    parser.add_argument("--output", type=Path, default=Path("build/update-release"))
    args = parser.parse_args()
    if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", args.repository):
        parser.error("Invalid GitHub repository")
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9.+_-]{0,63}", args.tag):
        parser.error("Invalid release tag")
    # Verify the actual binary; do not trust stale Gradle output-metadata.json.
    subprocess.run([str(args.apksigner.resolve()), "verify", str(args.apk.resolve())], check=True)
    badging = subprocess.run([str(args.aapt.resolve()), "dump", "badging", str(args.apk.resolve())],
                             check=True, capture_output=True, encoding="utf-8").stdout
    identity = parse_badging(badging)
    expected = "com.kidslanguage.companion" + (".debug" if args.variant == "debug" else "")
    if identity["packageName"] != expected:
        parser.error(f"Expected {expected}, got {identity['packageName']}")
    apk_name = f"app-{args.variant}.apk"
    url = f"https://github.com/{args.repository}/releases/download/{quote(args.tag, safe='')}/{apk_name}"
    notes = args.notes.read_text(encoding="utf-8") if args.notes else ""
    manifest = create_manifest(args.apk, identity, url, notes)
    args.output.mkdir(parents=True, exist_ok=True)
    destination = args.output / apk_name
    if args.apk.resolve() != destination.resolve():
        shutil.copyfile(args.apk, destination)
    path = args.output / f"update-{args.variant}.json"
    path.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Prepared signed {identity['packageName']} version {identity['versionCode']} ({manifest['bytes']} bytes)")
    print(path)


if __name__ == "__main__":
    main()
