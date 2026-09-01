#!/usr/bin/env python3
"""AI redraw of the LXJ-MFA icon as a clean vector-style image.

Reference: user screenshot (white rounded-square plate + green shield + white LXJ/MFA).
Geometry (shield outline, text position/size) is measured from the reference,
then redrawn at high resolution and downsampled -> crisp at any density.
"""
from PIL import Image, ImageDraw, ImageFont
import numpy as np
import os

SRC = "C:/Users/luan-/.workbuddy/clipboard-images/clipboard-2026-09-01T17-30-05-960Z-e036fa0a.png"
BASE = "app/src/main/res"

GREEN = (46, 158, 123, 255)
WHITE = (255, 255, 255, 255)
PLATE = (255, 255, 255, 255)     # white rounded-square plate
CORNER_R = 0.20                  # plate corner radius, fraction of size
FONT_BOLD = "C:/Windows/Fonts/arialbd.ttf"

# Measured from the reference screenshot (fractions of the square canvas)
TEXT_SPECS = [
    ("LXJ", 0.496, 0.355, 0.094, 0.279),   # text, center_x, center_y, height, width
    ("MFA", 0.496, 0.557, 0.172, 0.615),
]


def extract_shield_outline(path):
    """Return shield outline as a polygon in normalized square-canvas coords."""
    a = np.array(Image.open(path).convert("RGBA"))
    h, w = a.shape[:2]
    r = a[:, :, 0].astype(int)
    g = a[:, :, 1].astype(int)
    b = a[:, :, 2].astype(int)
    green = (g > 100) & (g > r + 15) & (g > b + 15)

    base = max(h, w)
    ox, oy = (base - w) / 2.0, (base - h) / 2.0
    left, right = [], []
    for y in range(h):
        row = np.where(green[y])[0]
        if len(row) == 0:
            continue
        left.append(((row.min() + ox) / base, (y + oy) / base))
        right.append(((row.max() + ox) / base, (y + oy) / base))
    return left + right[::-1]


def draw_fitted_text(d, text, cx, cy, target_h, target_w, S):
    """Draw `text` centered at (cx, cy) with exact target height and width."""
    th_px, tw_px = target_h * S, target_w * S
    # initial font size so cap height ~= target height
    fs = max(4, int(th_px / 0.72))
    font = ImageFont.truetype(FONT_BOLD, fs)
    bb = d.textbbox((0, 0), text, font=font)
    measured_h = bb[3] - bb[1]
    if measured_h > 0:
        fs = max(4, int(fs * th_px / measured_h))
        font = ImageFont.truetype(FONT_BOLD, fs)
    # per-char advance widths
    adv = [font.getlength(ch) for ch in text]
    total = sum(adv)
    n = len(text)
    spacing = (tw_px - total) / (n - 1) if n > 1 else 0
    x = cx * S - tw_px / 2.0
    for i, ch in enumerate(text):
        d.text((x, cy * S), ch, font=font, fill=WHITE, anchor="lm")
        x += adv[i] + spacing


def scale_around_center(poly, specs, k):
    """Scale shield polygon + text specs about the canvas center by factor k."""
    sp = [(0.5 + (x - 0.5) * k, 0.5 + (y - 0.5) * k) for x, y in poly]
    ss = [(t, 0.5 + (cx - 0.5) * k, 0.5 + (cy - 0.5) * k, th * k, tw * k)
          for t, cx, cy, th, tw in specs]
    return sp, ss


def draw_icon(size, poly, specs=TEXT_SPECS, plate=True, ss=4):
    """Render the icon at `size` px with `ss`x supersampling."""
    S = size * ss
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    if plate:
        d.rounded_rectangle([0, 0, S - 1, S - 1], radius=int(CORNER_R * S), fill=PLATE)
    d.polygon([(x * S, y * S) for x, y in poly], fill=GREEN)
    for text, cx, cy, th, tw in specs:
        draw_fitted_text(d, text, cx, cy, th, tw, S)
    return img.resize((size, size), Image.LANCZOS)


# Adaptive-icon geometry:
# Layers are 108dp. The system masks to ~72dp, so content must live inside the
# central 66dp safe circle. To keep the shield at 78% of the *visible* icon
# (matching the reference), it must be 0.78 * 72 / 108 = 52% of the layer.
SAFE_SCALE = 0.63


def draw_background(size, ss=4):
    """Adaptive background: opaque white plate only (no shield)."""
    S = size * ss
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 0, S - 1, S - 1], radius=int(CORNER_R * S), fill=PLATE)
    return img.resize((size, size), Image.LANCZOS)


def draw_foreground(size, poly, ss=4):
    """Adaptive foreground: shield + text only, scaled into the safe zone."""
    sp, specs = scale_around_center(poly, TEXT_SPECS, SAFE_SCALE)
    return draw_icon(size, sp, specs, plate=False, ss=ss)


def main():
    poly = extract_shield_outline(SRC)
    print(f"shield outline points: {len(poly)}")

    # launcher: white rounded plate + shield + text
    MIPMAP = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for dens, size in MIPMAP.items():
        d = f"{BASE}/mipmap-{dens}"
        os.makedirs(d, exist_ok=True)
        draw_icon(size, poly).save(f"{d}/ic_launcher.png")

    # adaptive layers (108dp):
    #  background is a static white XML shape (drawable/ic_launcher_background.xml) -
    #  a plain white PNG gets optimized away by AAPT2, so it lives in XML on purpose.
    #  foreground is the shield only, scaled inside the safe zone.
    #  Generate at every density bucket so high-DPI devices don't upscale a single
    #  108px raster (which looked blurry on xxxhdpi). Geometry is identical; only
    #  pixel resolution increases. ss is bumped for the larger sizes to stay crisp.
    FG = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}
    for dens, px in FG.items():
        d = f"{BASE}/drawable-{dens}"
        os.makedirs(d, exist_ok=True)
        draw_foreground(px, poly, ss=4).save(f"{d}/ic_launcher_foreground.png")
    # Keep a no-density fallback too (mdpi-equivalent), harmless.
    os.makedirs(f"{BASE}/drawable", exist_ok=True)
    draw_foreground(108, poly).save(f"{BASE}/drawable/ic_launcher_foreground.png")

    # toolbar logo: same rounded plate + shield
    LOGO = {"mdpi": 36, "hdpi": 54, "xhdpi": 72, "xxhdpi": 96, "xxxhdpi": 144}
    for dens, size in LOGO.items():
        d = f"{BASE}/drawable-{dens}"
        os.makedirs(d, exist_ok=True)
        draw_icon(size, poly).save(f"{d}/ic_app_logo.png")

    print("AI-redrawn icon assets generated.")


if __name__ == "__main__":
    main()
