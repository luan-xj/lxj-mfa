#!/usr/bin/env python3
"""Use the user's screenshot icon directly for all launcher/logo assets."""
from PIL import Image
import os

SRC = "assets/icon_source.png"
BASE = "app/src/main/res"

def ensure_dir(d):
    os.makedirs(d, exist_ok=True)

def save_scaled(src, size, out_path):
    """Scale source image to fit inside size x size box, then center on a size x size RGBA canvas."""
    src_im = Image.open(src).convert("RGBA")
    # Make a square-ish icon by fitting to a square canvas (maintain aspect ratio)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    src_im.thumbnail((size, size), Image.LANCZOS)
    x = (size - src_im.width) // 2
    y = (size - src_im.height) // 2
    canvas.paste(src_im, (x, y), src_im)
    canvas.save(out_path)

def main():
    # Adaptive icon layers: 108x108 dp; use the same image for both bg and fg
    # so the exact screenshot is always shown regardless of device shape crop.
    ensure_dir(f"{BASE}/drawable")
    save_scaled(SRC, 108, f"{BASE}/drawable/ic_launcher_background.png")
    save_scaled(SRC, 108, f"{BASE}/drawable/ic_launcher_foreground.png")

    # Mipmap legacy icons
    MIPMAP = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for dens, size in MIPMAP.items():
        d = f"{BASE}/mipmap-{dens}"
        ensure_dir(d)
        save_scaled(SRC, size, f"{d}/ic_launcher.png")

    # Toolbar logo: transparent background, fit inside square
    LOGO = {"mdpi": 36, "hdpi": 54, "xhdpi": 72, "xxhdpi": 96, "xxxhdpi": 144}
    for dens, size in LOGO.items():
        d = f"{BASE}/drawable-{dens}"
        ensure_dir(d)
        save_scaled(SRC, size, f"{d}/ic_app_logo.png")

    print("Icon assets generated from user screenshot.")

if __name__ == "__main__":
    main()
