#!/usr/bin/env python3
"""
Generate LXJ-MFA launcher icons from the user's reference screenshot style:
- White rounded-rectangle background
- Green shield centered
- White text "LXJ" (small, top) and "MFA" (large, bottom)
"""
from PIL import Image, ImageDraw, ImageFont
import os

GREEN = (46, 158, 123)      # #2E9E7B from screenshot shield body
WHITE = (255, 255, 255)
BG_WHITE = (255, 255, 255)

# Density multipliers for mipmap-* directories (baseline mdpi = 1x)
DENSITIES = {
    "mdpi": 1,
    "hdpi": 1.5,
    "xhdpi": 2,
    "xxhdpi": 3,
    "xxxhdpi": 4,
}

def make_shield_path(draw_ctx, cx, cy, w, h):
    """Create a shield polygon path: flat top, rounded-ish bottom."""
    # Points in clockwise order starting top-left
    # Use a classic shield shape
    top_w = w * 0.95
    bottom_w = w * 0.35
    shoulder_y = cy - h * 0.10
    bottom_y = cy + h * 0.48
    top_y = cy - h * 0.48
    pts = [
        (cx - top_w/2, top_y),           # top-left
        (cx + top_w/2, top_y),           # top-right
        (cx + top_w/2, shoulder_y),      # right shoulder down a bit
        (cx + bottom_w/2, bottom_y),     # right lower curve
        (cx, bottom_y + h * 0.08),       # bottom point
        (cx - bottom_w/2, bottom_y),     # left lower curve
        (cx - top_w/2, shoulder_y),      # left shoulder
    ]
    draw_ctx.polygon(pts, fill=GREEN)

def draw_shield_and_text(img, shield_scale=0.78, text_top="LXJ", text_bottom="MFA"):
    """Draw green shield + white text on a transparent or white image."""
    w, h = img.size
    draw = ImageDraw.Draw(img, 'RGBA')
    
    shield_w = w * shield_scale
    shield_h = h * shield_scale
    cx, cy = w / 2, h / 2
    
    make_shield_path(draw, cx, cy, shield_w, shield_h)
    
    # Fonts - use Arial Bold
    font_path_bold = "/c/Windows/Fonts/arialbd.ttf"
    font_path_regular = "/c/Windows/Fonts/arial.ttf"
    
    # Bottom text MFA - large
    fsize_bottom = int(shield_w * 0.38)
    font_bottom = ImageFont.truetype(font_path_bold, fsize_bottom)
    bbox = draw.textbbox((0, 0), text_bottom, font=font_bottom)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    x = (w - tw) / 2
    y = cy + shield_h * 0.02 - th / 2
    draw.text((x, y), text_bottom, font=font_bottom, fill=WHITE)
    
    # Top text LXJ - small
    fsize_top = int(shield_w * 0.20)
    font_top = ImageFont.truetype(font_path_bold, fsize_top)
    bbox = draw.textbbox((0, 0), text_top, font=font_top)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    x = (w - tw) / 2
    y = cy - shield_h * 0.24 - th / 2
    draw.text((x, y), text_top, font=font_top, fill=WHITE)
    
    return img

def rounded_rect(size, radius, fill):
    img = Image.new('RGBA', size, (0,0,0,0))
    draw = ImageDraw.Draw(img)
    draw.rounded_rectangle((0, 0, size[0]-1, size[1]-1), radius=radius, fill=fill)
    return img

def generate():
    base = "app/src/main/res"
    
    # --- Adaptive icon layers (108x108 dp) ---
    bg_108 = Image.new('RGBA', (108, 108), BG_WHITE + (255,))
    bg_108.save(f"{base}/drawable/ic_launcher_background.png")
    
    fg_108 = Image.new('RGBA', (108, 108), (0,0,0,0))
    draw_shield_and_text(fg_108, shield_scale=0.74)
    fg_108.save(f"{base}/drawable/ic_launcher_foreground.png")
    
    # --- Mipmap PNGs (legacy fallback) ---
    # Standard sizes: mdpi 48, hdpi 72, xhdpi 96, xxhdpi 144, xxxhdpi 192
    MIPMAP_SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for dens, size in MIPMAP_SIZES.items():
        # Round-corner white background, radius ~22% of size like iOS style
        radius = int(size * 0.22)
        img = rounded_rect((size, size), radius, BG_WHITE + (255,))
        draw_shield_and_text(img, shield_scale=0.70)
        out_dir = f"{base}/mipmap-{dens}"
        os.makedirs(out_dir, exist_ok=True)
        img.save(f"{out_dir}/ic_launcher.png")
    
    # --- Toolbar logo (transparent background, various densities) ---
    LOGO_SIZES = {"mdpi": 36, "hdpi": 54, "xhdpi": 72, "xxhdpi": 96, "xxxhdpi": 144}
    for dens, size in LOGO_SIZES.items():
        img = Image.new('RGBA', (size, size), (0,0,0,0))
        draw_shield_and_text(img, shield_scale=0.80)
        out_dir = f"{base}/drawable-{dens}"
        os.makedirs(out_dir, exist_ok=True)
        img.save(f"{out_dir}/ic_app_logo.png")
    
    print("Icon assets generated.")

if __name__ == "__main__":
    generate()
