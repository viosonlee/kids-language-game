"""Host-only Vosk baseline on bundled reference audio, not microphone/child acceptance.
Codex, 2026-10-03. Optional dependency: vosk==0.3.45; requires FFmpeg on PATH.
"""
import argparse
import hashlib
import importlib.metadata
import json
from pathlib import Path
import platform
import subprocess
import time
import unicodedata

from setup_vosk_models import verify_archive, extract_model

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"


def reference_key(text):
    return "".join(c for c in unicodedata.normalize("NFKC", text).casefold() if c.isalnum())


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--languages", nargs="+", choices=["zh-CN", "en-US", "ja-JP"], default=["zh-CN", "en-US"])
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--cache", type=Path, default=ROOT / "build/asr-reference-models")
    args = parser.parse_args()
    if args.report.resolve().is_relative_to(ASSETS):
        parser.error("Report must not overwrite application assets")
    from vosk import Model, KaldiRecognizer, SetLogLevel
    SetLogLevel(-1)
    manifests = json.loads((ASSETS / "models/manifest.json").read_text(encoding="utf-8"))["models"]
    report = {"scope": "bundled synthetic reference audio; host decoder; no microphone; not application scoring",
              "vosk_version": importlib.metadata.version("vosk"), "platform": platform.platform(),
              "sample_rate": 16000, "chunk_ms": 100, "added_silence_seconds": 0,
              "ffmpeg": subprocess.check_output(["ffmpeg", "-version"], text=True).splitlines()[0], "languages": []}
    for language in dict.fromkeys(args.languages):
        entry = next(m for m in manifests if m["language"] == language)
        archive = ASSETS / "models" / entry["file"]
        verify_archive(archive, entry)
        model_path = extract_model(archive, args.cache, language, entry["sha256"])
        model = Model(str(model_path))
        items = json.loads((ASSETS / f"content/items_{language[:2]}.json").read_text(encoding="utf-8"))
        rows = []
        for item in items:
            audio_path = (ASSETS / item["audio"]).resolve()
            if not audio_path.is_relative_to((ASSETS / "audio").resolve()):
                raise ValueError("Audio path outside assets/audio")
            pcm = subprocess.check_output(["ffmpeg", "-nostdin", "-v", "error", "-i", str(audio_path),
                "-map", "0:a:0", "-ar", "16000", "-ac", "1", "-f", "s16le", "-"])
            decoder = KaldiRecognizer(model, 16000)
            endpoints = []
            started = time.perf_counter()
            for offset in range(0, len(pcm), 3200):
                if decoder.AcceptWaveform(pcm[offset:offset + 3200]):
                    text = json.loads(decoder.Result()).get("text", "").strip()
                    if text:
                        endpoints.append(text)
            final = json.loads(decoder.FinalResult()).get("text", "").strip()
            transcript = " ".join(endpoints + ([final] if final else []))
            candidates = [item["text"]] + item.get("aliases", [])
            rows.append({"id": item["id"], "target": item["text"], "aliases": item.get("aliases", []),
                "audio": item["audio"], "sha256": hashlib.sha256(audio_path.read_bytes()).hexdigest(),
                "audio_seconds": len(pcm) / 32000, "decode_seconds": time.perf_counter() - started,
                "endpoint_texts": endpoints, "final_text": final, "transcript": transcript,
                "empty": not transcript,
                "reference_text_match": bool(transcript) and any(reference_key(transcript) == reference_key(c) for c in candidates)})
            del decoder
        summary = {"language": language, "model_version": entry["version"], "model_sha256": entry["sha256"],
                   "count": len(rows), "empty_count": sum(r["empty"] for r in rows),
                   "reference_match_count": sum(r["reference_text_match"] for r in rows), "items": rows}
        report["languages"].append(summary)
        print(f"{language}: {len(rows)} references, {summary['empty_count']} empty, {summary['reference_match_count']} normalized text/alias matches", flush=True)
        del model
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
