"""Measure bundled item speech; optionally apply verified constant gain (Codex, 2026-10-03)."""
import argparse
from concurrent.futures import ThreadPoolExecutor
import hashlib
import json
import math
from pathlib import Path
import re
import subprocess
import tempfile

TARGET_LUFS = -20.0
TOLERANCE_LU = 1.0
MAX_TRUE_PEAK = -1.5
ROOT = Path(__file__).resolve().parents[1]


def run(*args):
    return subprocess.run(args, capture_output=True, text=True, encoding="utf-8",
                          errors="replace", check=True, timeout=60)


def item_audio_files(root):
    root = root.resolve()
    paths = set()
    catalogs = sorted((root / "content").glob("items_*.json"))
    if not catalogs:
        raise ValueError("No item catalogs found")
    for catalog in catalogs:
        for item in json.loads(catalog.read_text(encoding="utf-8")):
            path = (root / item["audio"]).resolve()
            if not path.is_relative_to(root / "audio") or path.suffix.lower() != ".mp3" or not path.is_file():
                raise ValueError(f"Invalid or missing item audio: {item['audio']}")
            paths.add(path)
    if not paths:
        raise ValueError("No item audio found")
    return sorted(paths)


def parse_loudness(stderr):
    match = re.search(r'\{\s*"input_i".*?\}', stderr, re.S)
    if match is None:
        raise ValueError("FFmpeg did not report loudness")
    values = json.loads(match.group())
    result = {"lufs": float(values["input_i"]), "true_peak_dbtp": float(values["input_tp"])}
    if not all(math.isfinite(x) for x in result.values()):
        raise ValueError("Silent or unmeasurable speech")
    return result


def measure(path):
    probe = json.loads(run("ffprobe", "-v", "error", "-show_streams", "-show_format",
                           "-of", "json", str(path)).stdout)
    streams = probe["streams"]
    if len(streams) != 1 or streams[0]["codec_type"] != "audio":
        raise ValueError(f"Expected a single audio stream: {path}")
    stream = streams[0]
    result = parse_loudness(run("ffmpeg", "-nostdin", "-hide_banner", "-i", str(path),
        "-af", "loudnorm=I=-20:TP=-1.5:LRA=7:print_format=json", "-f", "null", "-").stderr)
    result.update(duration_seconds=float(probe["format"]["duration"]),
                  sample_rate=int(stream["sample_rate"]), channels=int(stream["channels"]),
                  sha256=hashlib.sha256(path.read_bytes()).hexdigest())
    if not math.isfinite(result["duration_seconds"]) or result["duration_seconds"] <= 0:
        raise ValueError(f"Invalid audio duration: {path}")
    return result


def compliant(values):
    return abs(values["lufs"] - TARGET_LUFS) <= TOLERANCE_LU and values["true_peak_dbtp"] <= MAX_TRUE_PEAK


def gain_for(values):
    gain = TARGET_LUFS - values["lufs"]
    # Keep additional encoder headroom; don't silently substitute limiting or time compression.
    if values["true_peak_dbtp"] + gain > MAX_TRUE_PEAK - 0.5:
        raise ValueError("Constant gain would exceed peak headroom; manual audio review required")
    return gain


def normalize(path, before):
    if compliant(before):
        return before
    gain = gain_for(before)
    with tempfile.TemporaryDirectory(prefix="audio-quality-", dir=path.parent) as temp:
        output = Path(temp) / "normalized.mp3"
        run("ffmpeg", "-nostdin", "-hide_banner", "-loglevel", "error", "-i", str(path),
            "-map", "0:a:0", "-af", f"volume={gain:.4f}dB", "-ar", str(before["sample_rate"]),
            "-ac", str(before["channels"]), "-codec:a", "libmp3lame", "-b:a", "96k", str(output))
        after = measure(output)
        if (not compliant(after) or abs(after["duration_seconds"] - before["duration_seconds"]) > 0.1
                or after["sample_rate"] != before["sample_rate"] or after["channels"] != before["channels"]):
            raise ValueError(f"Encoded output failed audio checks: {path.name}")
        output.replace(path)
        return after


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--assets-root", type=Path, default=ROOT / "app/src/main/assets")
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--normalize", action="store_true", help="Replace only individually verified outputs")
    args = parser.parse_args()
    root = args.assets_root.resolve()
    files = item_audio_files(root)
    if args.report.resolve().is_relative_to(root):
        parser.error("Report must be outside the assets directory")

    def inspect(path):
        row = {"path": path.relative_to(root).as_posix()}
        try:
            row["before"] = measure(path)
            row["after"] = normalize(path, row["before"]) if args.normalize else row["before"]
            row["passed"] = compliant(row["after"])
        except (ValueError, KeyError, OSError, subprocess.SubprocessError) as error:
            row.update(passed=False, error=str(error))
        return row

    with ThreadPoolExecutor(max_workers=4) as pool:
        rows = list(pool.map(inspect, files))
    passed = all(row["passed"] for row in rows)
    report = {"target_lufs": TARGET_LUFS, "tolerance_lu": TOLERANCE_LU,
              "max_true_peak_dbtp": MAX_TRUE_PEAK, "normalized": args.normalize,
              "ffmpeg": run("ffmpeg", "-version").stdout.splitlines()[0],
              "passed": passed, "files": rows}
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2, allow_nan=False) + "\n", encoding="utf-8")
    print(f"{sum(row['passed'] for row in rows)}/{len(rows)} assets passed; report: {args.report}")
    return 0 if passed else 1


if __name__ == "__main__":
    raise SystemExit(main())
