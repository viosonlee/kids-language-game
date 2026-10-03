"""Bundle the existing practice pictograms as offline Noto PNGs (requires Pillow).

Run from the repository root. No OpenAI API is used. Source revision is pinned;
network/source failures stop before changing the content files.
"""
import hashlib
import io
import json
from pathlib import Path
import urllib.request
from PIL import Image

REVISION = "22e564626297b4df0a40570ad81d6c05cc7c38bd"
BASE = f"https://raw.githubusercontent.com/googlefonts/noto-emoji/{REVISION}/"
ASSETS = Path(__file__).resolve().parents[1] / "app/src/main/assets"
DEST = ASSETS / "images/practice"
MANIFEST = DEST / "manifest.json"
GROUPS = {"🚶🚶": ["🚶", "🚶"], "🏃💨": ["🏃", "💨"], "🏃‍♂️🏃‍♀️": ["🏃‍♂️", "🏃‍♀️"]}


def name(emoji):
    return "emoji_u" + "_".join(f"{ord(c):04x}" for c in emoji if c != "\ufe0f") + ".png"


def download(path):
    with urllib.request.urlopen(BASE + path, timeout=30) as response:
        return response.read()


def main():
    previous = json.loads(MANIFEST.read_text(encoding="utf-8")) if MANIFEST.exists() else {"icons": []}
    reverse = {x["asset"]: x["symbol"] for x in previous["icons"]}
    documents = {p: json.loads(p.read_text(encoding="utf-8-sig")) for p in sorted((ASSETS / "content").glob("items_*.json"))}
    symbols = set()
    for items in documents.values():
        for item in items:
            symbols.update(reverse.get(s, s) for s in [item["image"], *item["distractors"]])
    if any("/" in s or not s for s in symbols):
        raise ValueError("Only known inline pictograms or this manifest's asset paths can be bundled")

    source_files = {}
    rendered = {}
    records = []
    for symbol in sorted(symbols):
        parts = GROUPS.get(symbol, [symbol])
        inputs = []
        sources = []
        for part in parts:
            source_path = "png/512/" + name(part)
            if source_path not in source_files:
                source_files[source_path] = download(source_path)
            raw = source_files[source_path]
            with Image.open(io.BytesIO(raw)) as decoded:
                decoded.load()
                if decoded.format != "PNG" or decoded.size != (512, 512):
                    raise ValueError(f"Unexpected source image: {source_path}")
                inputs.append(decoded.convert("RGBA"))
            sources.append({"path": source_path, "sha256": hashlib.sha256(raw).hexdigest()})
        if len(inputs) == 1:
            output = source_files[sources[0]["path"]]
        else:
            canvas = Image.new("RGBA", (512, 512))
            side = 512 // len(inputs)
            for index, img in enumerate(inputs):
                resized = img.resize((side, side), Image.Resampling.LANCZOS)
                canvas.alpha_composite(resized, (index * side, (512 - side) // 2))
            buffer = io.BytesIO()
            canvas.save(buffer, format="PNG")
            output = buffer.getvalue()
        asset = "images/practice/" + name(symbol)
        rendered[asset] = output
        records.append({"symbol": symbol, "asset": asset, "sources": sources,
                        "modified": len(parts) > 1, "sha256": hashlib.sha256(output).hexdigest()})

    # Preserve upstream notices and the complete redistribution license in the APK.
    notice = download("svg/LICENSE")
    upstream_readme = download("README.md")
    with urllib.request.urlopen("https://www.apache.org/licenses/LICENSE-2.0.txt", timeout=30) as response:
        license_text = response.read()
    if b"Apache License" not in license_text or b"Version 2.0" not in license_text:
        raise ValueError("Unexpected license response")
    mapping = {x["symbol"]: x["asset"] for x in records}
    for items in documents.values():
        for item in items:
            item["image"] = mapping[reverse.get(item["image"], item["image"])]
            item["distractors"] = [mapping[reverse.get(s, s)] for s in item["distractors"]]
    DEST.mkdir(parents=True, exist_ok=True)
    for asset, raw in rendered.items():
        (ASSETS / asset).write_bytes(raw)
    (DEST / "NOTICE.txt").write_bytes(notice + f"\nSource: {BASE}\nThree multi-symbol compositions resized and combined by Codex, 2026-10-03.\n".encode())
    (DEST / "LICENSE.txt").write_bytes(license_text)
    (DEST / "UPSTREAM_README.md").write_bytes(upstream_readme)
    MANIFEST.write_text(json.dumps({"author": "Codex", "date": "2026-10-03", "upstreamRevision": REVISION,
                                   "license": "Apache-2.0", "icons": records}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    for path, items in documents.items():
        path.write_text(json.dumps(items, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Bundled {len(records)} pictograms from {len(source_files)} source images, {sum(map(len, rendered.values()))} bytes")


if __name__ == "__main__":
    main()
