"""Builds the custom particle sprites of Pixel Classes (assassin VFX).
 - slash_a..d and swirl are CUT from tools/reference/assassin_concept.png (brightness -> transparency, so the black background vanishes)
 - sparkle_a/b, shard_a/b, glow are drawn here as pixel art
Run:  python3 tools/generate_fx.py   (needs pillow numpy)"""
import os, json
import numpy as np
from scipy import ndimage
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "src", "main", "resources", "assets", "pixelclasses")
TEX = os.path.join(ASSETS, "textures", "particle"); PART = os.path.join(ASSETS, "particles")
os.makedirs(TEX, exist_ok=True); os.makedirs(PART, exist_ok=True)
src = Image.open(os.path.join(HERE, "reference", "assassin_concept.png")).convert("RGB")

def cut(box, size=128, floor=28, span=120, gamma=0.85, boost=1.15):
    """Crop a region of the concept image and turn brightness into alpha (glow on black -> transparent sprite)."""
    a = np.asarray(src.crop(box), dtype=np.float32)
    mx = a.max(axis=2)
    alpha = np.clip((mx - floor) / span, 0, 1) ** gamma
    alpha[alpha < 0.04] = 0
    # keep only the main piece(s): drop small stray sparkles that were next to it in the concept image
    labels, n = ndimage.label(alpha > 0.15, structure=np.ones((3, 3)))
    if n > 1:
        sizes = ndimage.sum(np.ones_like(alpha), labels, range(1, n + 1))
        keep = np.isin(labels, [i + 1 for i, v in enumerate(sizes) if v >= 0.25 * sizes.max()])
        keep = ndimage.binary_dilation(keep, iterations=4)
        alpha = alpha * keep
    rgb = np.clip(a / np.maximum(alpha[..., None], 0.08) * 0.92 * boost, 0, 255)
    rgba = np.dstack([rgb, alpha * 255]).astype(np.uint8)
    im = Image.fromarray(rgba, "RGBA")
    bbox = im.getbbox()
    if bbox: im = im.crop(bbox)
    scale = (size - 6) / max(im.size)
    im = im.resize((max(1, int(im.width * scale)), max(1, int(im.height * scale))), Image.LANCZOS)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    canvas.alpha_composite(im, ((size - im.width) // 2, (size - im.height) // 2))
    return canvas

# coordinates in the 1672x941 concept image (crop panel started at x=985,y=600, shown at 2x)
def o(x0, y0, x1, y1): return (985 + x0 // 2, 600 + y0 // 2, 985 + x1 // 2, 600 + y1 // 2)
cut(o(15, 30, 420, 215)).save(f"{TEX}/slash_a.png")      # big top-left crescent
cut(o(320, 225, 560, 385)).save(f"{TEX}/slash_b.png")    # diagonal blade streak
cut(o(25, 250, 290, 390)).save(f"{TEX}/slash_c.png")     # long thin crescent
cut(o(590, 250, 790, 380), size=96).save(f"{TEX}/slash_d.png")   # small streak
cut(o(665, 415, 1005, 600), size=128).save(f"{TEX}/swirl.png")   # swirling ring

# ---------------- hand-drawn pixel art
LAV = (190, 150, 255, 255); VIO = (140, 90, 255, 255); DEEP = (90, 50, 200, 255); WHITE = (255, 255, 255, 255)
def sparkle(size, arm, thick, core=True):
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0)); d = ImageDraw.Draw(im); c = size // 2
    for t in range(arm, 0, -1):               # tapered 4-point star
        w = max(0, thick - (t * thick) // arm)
        col = LAV if t > arm // 2 else WHITE
        d.rectangle([c - w, c - t, c + w - (1 if w else 0), c - t], fill=VIO if t > arm * 0.6 else col)
        d.rectangle([c - w, c + t - 1, c + w - (1 if w else 0), c + t - 1], fill=VIO if t > arm * 0.6 else col)
        d.rectangle([c - t, c - w, c - t, c + w - (1 if w else 0)], fill=VIO if t > arm * 0.6 else col)
        d.rectangle([c + t - 1, c - w, c + t - 1, c + w - (1 if w else 0)], fill=VIO if t > arm * 0.6 else col)
    if core: d.rectangle([c - 1, c - 1, c, c], fill=WHITE); d.rectangle([c - 2, c - 1, c + 1, c], fill=LAV)
    return im
sparkle(16, 7, 1).save(f"{TEX}/sparkle_a.png")
sparkle(32, 15, 1).filter(ImageFilter.GaussianBlur(0.4)).save(f"{TEX}/sparkle_b.png")
def shard(size, w, h):
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0)); d = ImageDraw.Draw(im); c = size // 2
    d.polygon([(c, c - h), (c + w, c), (c, c + h), (c - w, c)], fill=VIO)
    d.polygon([(c, c - h + 1), (c + w - 2, c), (c, c), (c - w + 2, c)], fill=LAV)
    d.point((c - 1, c - 2), fill=WHITE)
    return im
shard(16, 5, 7).save(f"{TEX}/shard_a.png"); shard(16, 3, 7).save(f"{TEX}/shard_b.png")
def glow(size):
    y, x = np.mgrid[0:size, 0:size]; r = np.hypot(x - (size - 1) / 2, y - (size - 1) / 2) / (size / 2)
    a = np.clip(1 - r, 0, 1) ** 1.6
    rgba = np.zeros((size, size, 4), np.uint8); rgba[..., 0] = 200; rgba[..., 1] = 160; rgba[..., 2] = 255; rgba[..., 3] = (a * 255).astype(np.uint8)
    return Image.fromarray(rgba, "RGBA")
glow(16).save(f"{TEX}/glow.png")

sets = {"slash": ["slash_a", "slash_b", "slash_c", "slash_d"], "swirl": ["swirl"], "sparkle": ["sparkle_a", "sparkle_b"], "shard": ["shard_a", "shard_b"], "glow": ["glow"]}
for name, tex in sets.items():
    json.dump({"textures": ["pixelclasses:" + t for t in tex]}, open(f"{PART}/{name}.json", "w"), indent=2)

# preview on a dark background (not shipped)
names = [t for v in sets.values() for t in v]
sheet = Image.new("RGBA", (len(names) * 136, 136), (12, 8, 24, 255))
for i, n in enumerate(names):
    t = Image.open(f"{TEX}/{n}.png"); t = t.resize((128, 128), Image.NEAREST) if t.width < 64 else t
    sheet.alpha_composite(t, (i * 136 + 4, 4))
sheet.save("/tmp/pc_fx.png"); print("fx sprites:", ", ".join(names))
