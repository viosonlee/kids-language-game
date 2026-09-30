"""Validate prebuilt distribution inputs before any GitHub Release is created."""

import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import traceback


def signer_digests(output):
    # New build-tools may repeat the same signer for different Android SDK ranges.
    return {value.lower() for value in re.findall(
        r"certificate SHA-256 digest:\s*([a-fA-F0-9]{64})(?=\s|$)", output)}


def validate(root, repository, build_tools):
    root, build_tools = Path(root), Path(build_tools)
    manifest = json.loads((root / "artifacts/update-debug.json").read_text(encoding="utf-8"))
    apk = root / "artifacts/app-debug.apk"
    assert manifest["schemaVersion"] == 1
    assert manifest["packageName"] == "com.kidslanguage.companion.debug"
    assert isinstance(manifest["versionCode"], int) and 0 < manifest["versionCode"] <= 2100000000
    assert re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9.+-]{0,63}", manifest["versionName"])
    tag = f"v{manifest['versionName']}-{manifest['versionCode']}"
    expected_url = f"https://github.com/{repository}/releases/download/{tag}/app-debug.apk"
    assert manifest["apkUrl"] == expected_url, "APK URL must match this repository and tag"
    assert apk.stat().st_size == manifest["bytes"] and 0 < manifest["bytes"] <= 512 * 1024 * 1024
    with apk.open("rb") as stream:
        assert hashlib.file_digest(stream, "sha256").hexdigest() == manifest["sha256"], "APK hash mismatch"
    suffix = ".bat" if os.name == "nt" else ""
    signature = subprocess.run([str(build_tools / f"apksigner{suffix}"), "verify", "--print-certs", str(apk)],
                               check=True, capture_output=True, text=True).stdout
    actual = signer_digests(signature)
    expected = (root / "signer-sha256.txt").read_text().strip()
    assert re.fullmatch(r"[a-f0-9]{64}", expected) and actual == {expected}, (
        "APK signer mismatch; expected=" + expected + "; actual=" + str(sorted(actual)) + "\n" + signature[:6000])
    suffix = ".exe" if os.name == "nt" else ""
    badging = subprocess.run([str(build_tools / f"aapt{suffix}"), "dump", "badging", str(apk)],
                             check=True, capture_output=True, encoding="utf-8").stdout
    identity = re.search(r"^package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", badging, re.M)
    sdk = re.search(r"^sdkVersion:'(\d+)'", badging, re.M)
    assert identity and sdk, "Unable to read APK identity"
    assert (identity[1], int(identity[2]), identity[3], int(sdk[1])) == (
        manifest["packageName"], manifest["versionCode"], manifest["versionName"], manifest["minSdk"])
    notes = manifest.get("notes", "")
    assert len(notes) <= 8000
    (root / "release-notes.txt").write_text(notes, encoding="utf-8")
    return manifest, tag


if __name__ == "__main__":
    try:
        manifest, tag = validate(Path.cwd(), os.environ["GITHUB_REPOSITORY"], os.environ["BUILD_TOOLS"])
        with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
            output.write(f"tag={tag}\nversion={manifest['versionName']}\ncode={manifest['versionCode']}\n")
        print(f"Verified {tag}: {manifest['bytes']} bytes; package, hash and signing certificate match")
    except Exception as error:
        detail = traceback.format_exc()
        if isinstance(error, subprocess.CalledProcessError):
            detail += "\n" + (error.stdout or "") + "\n" + (error.stderr or "")
        detail = detail.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
        print(f"::error title=APK verification failed::{detail}")
        raise
