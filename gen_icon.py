import os
from PIL import Image, ImageDraw, ImageFont

RES = "app/src/main/res"
GREEN_BG = (46, 158, 123)      # #2E9E7B  primary
GREEN_SHIELD = (52, 176, 138)  # lighter shield
GREEN_STROKE = (31, 122, 94)   # darker outline
WHITE = (255, 255, 255)

FONT = "/c/Windows/Fonts/arialbd.ttf"

def font(sz):
    return ImageFont.truetype(FONT, sz)

def shield_points(cx, cy, w, h):
    # crest/shield polygon
    hw = w / 2.0
    top = cy - h / 2.0
    bottom = cy + h / 2.0
    mid = cy + h * 0.18
    return [
        (cx - hw, top),
        (cx + hw, top),
        (cx + hw, mid),
        (cx, bottom),
        (cx - hw, mid),
    ]

def draw_logo(size, transparent_bg, opaque_flat=False):
    """Return a square RGBA image of given size.
    transparent_bg: if True, background is transparent (foreground / toolbar logo)
                    if False, background is opaque green (adaptive bg / flat mipmap)
    """
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0) if transparent_bg else (*GREEN_BG, 255))
    d = ImageDraw.Draw(img)
    cx = size / 2.0
    cy = size / 2.0
    w = size * 0.62
    h = size * 0.78
    pts = shield_points(cx, cy, w, h)
    if transparent_bg:
        # shield drawn on transparent -> for toolbar / foreground
        d.polygon(pts, fill=GREEN_SHIELD + (255,), outline=GREEN_STROKE + (255,), width=max(2, int(size * 0.025)))
    else:
        # flat mipmap: shield slightly lighter on opaque green
        d.polygon(pts, fill=GREEN_SHIELD + (255,), outline=GREEN_STROKE + (255,), width=max(2, int(size * 0.025)))

    # text
    lxj = "LXJ"
    mfa = "MFA"
    # LXJ upper (small), MFA lower (large)
    f_lxj = font(int(size * 0.17))
    f_mfa = font(int(size * 0.30))

    def centered(text, fnt, y):
        bb = d.textbbox((0, 0), text, font=fnt)
        tw = bb[2] - bb[0]
        th = bb[3] - bb[1]
        x = cx - tw / 2.0
        d.text((x, y - th / 2.0), text, font=fnt, fill=WHITE)

    centered(lxj, f_lxj, cy - h * 0.16)
    centered(mfa, f_mfa, cy + h * 0.16)
    return img

def save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    print("wrote", path, img.size)

# --- Adaptive icon (API 26+) ---
# background: OPAQUE green -> never blank
save(draw_logo(108, transparent_bg=False, opaque_flat=True),
     f"{RES}/drawable-anydpi-v26/ic_launcher_background.png")
# foreground: transparent shield + text (within safe zone, will be masked)
save(draw_logo(108, transparent_bg=True),
     f"{RES}/drawable-anydpi-v26/ic_launcher_foreground.png")

# --- Flat mipmap fallback (API <26), opaque to avoid ColorOS white frame ---
densities = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
for dn, sz in densities.items():
    save(draw_logo(sz, transparent_bg=False, opaque_flat=True),
         f"{RES}/mipmap-{dn}/ic_launcher.png")

# --- Toolbar logo (transparent shield, used in account page top-left) ---
logo_densities = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
for dn, sz in logo_densities.items():
    save(draw_logo(sz, transparent_bg=True),
         f"{RES}/drawable-{dn}/ic_app_logo.png")
