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


def draw_icon(size, poly, plate=True, ss=4):
    """Render the icon at `size` px with `ss`x supersampling."""
    S = size * ss
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    if plate:
        d.rounded_rectangle([0, 0, S - 1, S - 1], radius=int(CORNER_R * S), fill=PLATE)
    d.polygon([(x * S, y * S) for x, y in poly], fill=GREEN)
    for text, cx, cy, th, tw in TEXT_SPECS:
        draw_fitted_text(d, text, cx, cy, th, tw, S)
    return img.resize((size, size), Image.LANCZOS)


def main():
    poly = extract_shield_outline(SRC)
    print(f"shield outline points: {len(poly)}")

    # launcher: white rounded plate + shield + text
    MIPMAP = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for dens, size in MIPMAP.items():
        d = f"{BASE}/mipmap-{dens}"
        os.makedirs(d, exist_ok=True)
        draw_icon(size, poly).save(f"{d}/ic_launcher.png")

    # adaptive layers (108dp) - same image so it fills without a system white frame
    os.makedirs(f"{BASE}/drawable", exist_ok=True)
    draw_icon(108, poly).save(f"{BASE}/drawable/ic_launcher_background.png")
    draw_icon(108, poly).save(f"{BASE}/drawable/ic_launcher_foreground.png")

    # toolbar logo: same rounded plate + shield
    LOGO = {"mdpi": 36, "hdpi": 54, "xhdpi": 72, "xxhdpi": 96, "xxxhdpi": 144}
    for dens, size in LOGO.items():
        d = f"{BASE}/drawable-{dens}"
        os.makedirs(d, exist_ok=True)
        draw_icon(size, poly).save(f"{d}/ic_app_logo.png")

    print("AI-redrawn icon assets generated.")


if __name__ == "__main__":
    main()
