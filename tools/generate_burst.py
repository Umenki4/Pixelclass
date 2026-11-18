"""Slices tools/reference/purple_explosion_sheet.png (4x4 frames, checkerboard baked into the picture) into 16 transparent
particle frames  assets/pixelclasses/textures/particle/burst_00..15.png  + particles/burst.json.   Needs: pillow numpy scipy"""
import json, os
import numpy as np
from PIL import Image
from scipy import ndimage
HERE = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(HERE, "..", "src", "main", "resources", "assets", "pixelclasses")
TEX = os.path.join(A, "textures", "particle"); PART = os.path.join(A, "particles"); os.makedirs(TEX, exist_ok=True); os.makedirs(PART, exist_ok=True)
sheet = np.asarray(Image.open(os.path.join(HERE, "reference", "purple_explosion_sheet.png")).convert("RGB")).astype(np.float32)
H, W, _ = sheet.shape; cw, ch = W // 4, H // 4
frames = []
for i in range(16):
    r, c = divmod(i, 4)
    cell = sheet[r * ch:(r + 1) * ch, c * cw:(c + 1) * cw]
    mx, mn = cell.max(axis=2), cell.min(axis=2)
    neutral = ((mx - mn) < 22) & (mn > 150)                       # white / grey checkerboard squares
    lab, n = ndimage.label(neutral)
    border = (set(lab[0, :]) | set(lab[-1, :]) | set(lab[:, 0]) | set(lab[:, -1])) - {0}
    bg = np.isin(lab, list(border))                               # neutral pixels touching the frame edge = background
    holes = neutral & ~bg                                         # enclosed white = the bright core of the burst
    alpha = np.clip((255 - cell[..., 1]) / (255 - 60), 0, 1)      # purple over white -> how opaque is it
    alpha[bg] = 0
    alpha = np.where(holes, 1.0, alpha); alpha[alpha < 0.06] = 0
    rgb = np.clip((cell - (1 - alpha[..., None]) * 255) / np.maximum(alpha[..., None], 0.05), 0, 255); rgb[holes] = 255
    im = Image.fromarray(np.dstack([rgb, alpha * 255]).astype(np.uint8), "RGBA")
    side = max(cw, ch); canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0)); canvas.alpha_composite(im, ((side - cw) // 2, (side - ch) // 2))
    out = canvas.resize((128, 128), Image.LANCZOS); out.save(os.path.join(TEX, f"burst_{i:02d}.png")); frames.append(out)
json.dump({"textures": [f"pixelclasses:burst_{i:02d}" for i in range(16)]}, open(os.path.join(PART, "burst.json"), "w"), indent=2)
sheet_out = Image.new("RGBA", (4 * 130, 4 * 130), (14, 10, 26, 255))
for i, f in enumerate(frames): sheet_out.alpha_composite(f, ((i % 4) * 130 + 1, (i // 4) * 130 + 1))
sheet_out.save("/tmp/pc_burst.png"); print("burst frames: 16")
