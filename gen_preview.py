import os
from PIL import Image, ImageDraw, ImageFont

RES = "app/src/main/res"
FONT = "/c/Windows/Fonts/arialbd.ttf"

def font(sz):
    return ImageFont.truetype(FONT, sz)

def squircle_mask(size):
    # approximate rounded mask
    m = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(m)
    r = int(size * 0.22)
    d.rounded_rectangle([0, 0, size, size], radius=r, fill=255)
    return m

# --- 1. Launcher adaptive icon composite (bg + fg) in a squircle ---
bg = Image.open(f"{RES}/drawable-anydpi-v26/ic_launcher_background.png").convert("RGBA")
fg = Image.open(f"{RES}/drawable-anydpi-v26/ic_launcher_foreground.png").convert("RGBA")
comp = Image.alpha_composite(bg, fg)
S = 512
comp_big = comp.resize((S, S), Image.LANCZOS)
m = squircle_mask(S)
icon = Image.new("RGBA", (S, S), (0, 0, 0, 0))
icon.paste(comp_big, (0, 0), m)

# place on light backdrop
canvas = Image.new("RGBA", (S + 80, S + 80), (244, 251, 248, 255))
canvas.paste(icon, (40, 40), icon)
canvas.convert("RGB").save("preview_launcher.png")
print("wrote preview_launcher.png")

# --- 2. Toolbar mock (green bar with logo at left) ---
bar_w, bar_h = 720, 120
bar = Image.new("RGBA", (bar_w, bar_h), (46, 158, 123, 255))
bd = ImageDraw.Draw(bar)
logo = Image.open(f"{RES}/drawable-xxhdpi/ic_app_logo.png").convert("RGBA").resize((84, 84), Image.LANCZOS)
bar.paste(logo, (24, (bar_h - 84) // 2), logo)
bd.text((130, bar_h // 2 - 14), "", font=font(20), fill=(255, 255, 255))
bar.convert("RGB").save("preview_toolbar.png")
print("wrote preview_toolbar.png")
