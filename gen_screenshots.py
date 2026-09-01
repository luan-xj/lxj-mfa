#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成 LXJ-MFA 的应用界面示意截图（基于真实布局绘制的渲染图，非实机抓取）。"""
from PIL import Image, ImageDraw, ImageFont
import os

W, H = 1080, 2340
GREEN = (46, 158, 123)
GREEN_D = (36, 132, 102)
BG = (244, 245, 247)
SURFACE = (255, 255, 255)
TEXT = (30, 33, 38)
TEXT_2 = (122, 127, 134)
CHIP = (223, 242, 234)
CHIP_TXT = (26, 122, 92)
LINE = (232, 234, 237)

F_CJK = "C:/Windows/Fonts/msyh.ttc"
F_CJK_B = "C:/Windows/Fonts/msyhbd.ttc"
F_MONO = "C:/Windows/Fonts/arialbd.ttf"


def font(path, size, idx=0):
    try:
        return ImageFont.truetype(path, size, index=idx)
    except Exception:
        return ImageFont.load_default()


f_reg = font(F_CJK, 30)
f_bold = font(F_CJK_B, 30)
f_mono = font(F_MONO, 30)


def F(size, bold=False):
    if bold:
        return font(F_CJK_B, size)
    return font(F_CJK, size)


def FM(size):
    return font(F_MONO, size)


def shield(d, cx, cy, w, h, fill):
    """Draw a shield centered at (cx,cy)."""
    x0, y0 = cx - w / 2, cy - h / 2
    x1, y1 = cx + w / 2, cy + h / 2
    pts = [
        (x0, y0), (x1, y0),
        (x1, y0 + h * 0.45),
        (cx, y1),
        (x0, y0 + h * 0.45),
    ]
    d.polygon(pts, fill=fill)


def logo(d, cx, cy, size):
    """Draw the app logo: white rounded plate + green shield + white text."""
    plate = (255, 255, 255)
    r = size / 10
    d.rounded_rectangle([cx - size/2, cy - size/2, cx + size/2, cy + size/2],
                        radius=r, fill=plate)
    shield(d, cx, cy, size * 0.62, size * 0.72, GREEN)
    d.text((cx, cy - size*0.16), "LXJ", font=FM(int(size*0.20)), fill=plate,
           anchor="mm")
    d.text((cx, cy + size*0.20), "MFA", font=FM(int(size*0.20)), fill=plate,
           anchor="mm")


def status_bar(img, d, dark=True):
    col = TEXT if dark else (255, 255, 255)
    d.text((60, 30), "9:41", font=F(34, True), fill=col)
    # right side indicators (simple bars)
    d.rounded_rectangle([900, 36, 930, 52], radius=4, fill=col)
    d.rounded_rectangle([942, 30, 972, 58], radius=6, fill=col)
    d.rounded_rectangle([984, 24, 1010, 64], radius=8, outline=col, width=4)
    d.rounded_rectangle([988, 28, 1006, 60], radius=6, fill=col)


def toolbar(img, d, title, on_green=True):
    if on_green:
        d.rectangle([0, 90, W, 250], fill=GREEN)
        logo(d, 100, 170, 96)
        d.text((210, 132), title, font=F(46, True), fill=(255, 255, 255))
    else:
        d.rectangle([0, 90, W, 250], fill=SURFACE)
        d.line([0, 250, W, 250], fill=LINE, width=2)
        d.text((60, 132), title, font=F(46, True), fill=TEXT)


def search_box(d, y, hint):
    x, w, h = 60, W - 120, 96
    d.rounded_rectangle([x, y, x + w, y + h], radius=24, fill=SURFACE,
                        outline=GREEN, width=3)
    # magnifier
    cx, cy = x + 48, y + h / 2
    d.ellipse([cx - 18, cy - 18, cx + 18, cy + 18], outline=GREEN, width=5)
    d.line([cx + 13, cy + 13, cx + 30, cy + 30], fill=GREEN, width=5)
    d.text((x + 96, y + h / 2 - 22), hint, font=F(34), fill=TEXT_2)


def progress(d, x, y, w, ratio, color=GREEN):
    h = 10
    d.rounded_rectangle([x, y, x + w, y + h], radius=5, fill=(225, 228, 230))
    d.rounded_rectangle([x, y, x + w * ratio, y + h], radius=5, fill=color)


def fab(d, cx, cy):
    r = 78
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=GREEN, outline=(255, 255, 255), width=4)
    d.line([cx, cy - 38, cx, cy + 38], fill=(255, 255, 255), width=10)
    d.line([cx - 38, cy, cx + 38, cy], fill=(255, 255, 255), width=10)


def account_card(d, x, y, w, issuer, label, code, meta, edit_del=True, tag=None):
    h = 250
    d.rounded_rectangle([x, y, x + w, y + h], radius=28, fill=SURFACE,
                        outline=CHIP, width=2)
    iy = y + 34
    d.text((x + 40, iy), issuer, font=F(40, True), fill=TEXT)
    if tag:
        tw = d.textlength(tag, font=F(26, True)) + 32
        d.rounded_rectangle([x + 40 + d.textlength(issuer, font=F(40, True)) + 24, iy + 4,
                             x + 40 + d.textlength(issuer, font=F(40, True)) + 24 + tw, iy + 50],
                            radius=16, fill=CHIP)
        d.text((x + 40 + d.textlength(issuer, font=F(40, True)) + 40, iy + 9), tag,
               font=F(26, True), fill=CHIP_TXT)
    d.text((x + 40, iy + 56), label, font=F(30), fill=TEXT_2)
    d.text((x + 40, iy + 104), code, font=FM(72), fill=TEXT)
    d.text((x + 40, iy + 196), meta, font=F(28), fill=TEXT_2)
    progress(d, x + 40, y + h - 40, w - 80, 0.62)
    if edit_del:
        # two small round buttons top-right
        bx = x + w - 56
        d.ellipse([bx - 26, iy - 6, bx + 26, iy + 52], fill=(244, 246, 245))
        d.line([bx - 12, iy + 22, bx + 12, iy + 22], fill=TEXT_2, width=5)
        bx2 = bx - 70
        d.ellipse([bx2 - 26, iy - 6, bx2 + 26, iy + 52], fill=(244, 246, 245))
        d.rectangle([bx2 - 12, iy + 14, bx2 + 12, iy + 40], fill=TEXT_2)


def round_field(d, x, y, w, h, hint, filled=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=22, fill=SURFACE,
                        outline=LINE, width=3)
    if filled:
        d.text((x + 34, y + h / 2 - 22), filled, font=F(34), fill=TEXT)
    else:
        d.text((x + 34, y + h / 2 - 22), hint, font=F(34), fill=TEXT_2)


def out_btn(d, x, y, w, h, text, fillc=GREEN, txtc=(255, 255, 255)):
    d.rounded_rectangle([x, y, x + w, y + h], radius=20, fill=fillc)
    tw = d.textlength(text, font=F(36, True))
    d.text((x + w / 2 - tw / 2, y + h / 2 - 24), text, font=F(36, True), fill=txtc)


def text_btn(d, x, y, w, h, text, txtc=GREEN):
    d.rounded_rectangle([x, y, x + w, y + h], radius=20, outline=txtc, width=3)
    tw = d.textlength(text, font=F(36, True))
    d.text((x + w / 2 - tw / 2, y + h / 2 - 24), text, font=F(36, True), fill=txtc)


# ---------------- 1. 主界面 ----------------
def gen_home():
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    status_bar(img, d, dark=True)
    toolbar(img, d, "LXJ-MFA", on_green=True)
    search_box(d, 300, "搜索账号 / 发行方…")
    cards = [
        ("GitHub", "user@github.com", "428 913", "TOTP · SHA1 · 30s"),
        ("Google", "alice@gmail.com", "739 204", "TOTP · SHA256 · 30s"),
        ("微信", "lxj_vx", "551 802", "TOTP · SHA1 · 60s"),
        ("公司内网", "zhangsan", "016 477", "HOTP · SHA1", "工作"),
    ]
    y = 460
    for c in cards:
        tag = c[4] if len(c) > 4 else None
        account_card(d, 50, y, W - 100, c[0], c[1], c[2], c[3], tag=tag)
        y += 280
    fab(d, W - 130, H - 160)
    return img


# ---------------- 2. 添加账号 ----------------
def gen_add():
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    status_bar(img, d, dark=True)
    toolbar(img, d, "LXJ-MFA", on_green=True)
    # faded list behind
    search_box(d, 300, "搜索账号 / 发行方…")
    for i, c in enumerate([("GitHub", "user@github.com", "428 913"),
                           ("Google", "alice@gmail.com", "739 204"),
                           ("微信", "lxj_vx", "551 802")]):
        account_card(d, 50, 460 + i * 280, W - 100, c[0], c[1], c[2], "TOTP · SHA1 · 30s")
    # dim overlay
    d.rectangle([0, 0, W, H], fill=(20, 22, 25, 120) if False else (20, 22, 25))
    img = img.convert("RGBA")
    ov = Image.new("RGBA", (W, H), (15, 18, 22, 160))
    img = Image.alpha_composite(img, ov).convert("RGB")
    d = ImageDraw.Draw(img)
    # dialog
    dx, dy, dw, dh = 70, 360, W - 140, 1340
    d.rounded_rectangle([dx, dy, dx + dw, dy + dh], radius=40, fill=SURFACE)
    d.text((dx + 60, dy + 56), "添加账号", font=F(48, True), fill=TEXT)
    y = dy + 150
    round_field(d, dx + 50, y, dw - 100, 110, "发行方", filled="GitHub")
    y += 140
    round_field(d, dx + 50, y, dw - 100, 110, "账号", filled="alice@example.com")
    y += 140
    round_field(d, dx + 50, y, dw - 100, 110, "标签（可选）")
    y += 140
    # two columns type / algorithm
    hw = (dw - 100 - 30) / 2
    round_field(d, dx + 50, y, hw, 110, "类型", filled="TOTP")
    round_field(d, dx + 50 + hw + 30, y, hw, 110, "算法", filled="SHA1")
    y += 140
    round_field(d, dx + 50, y, dw - 100, 110, "密钥", filled="JBSWY 3DPEH P3DNP")
    y += 160
    out_btn(d, dx + 50, y, dw - 100, 100, "扫描二维码", fillc=(255, 255, 255), txtc=GREEN)
    d.rounded_rectangle([dx + 50, y, dx + 50 + dw - 100, y + 100], radius=20, outline=GREEN, width=3)
    y += 160
    bw = 220
    text_btn(d, dx + dw - 50 - 2*bw - 20, y, bw, 100, "取消", txtc=TEXT_2)
    out_btn(d, dx + dw - 50 - bw, y, bw, 100, "保存", fillc=GREEN)
    return img


# ---------------- 3. 数据与备份 ----------------
def gen_data():
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    status_bar(img, d, dark=True)
    # green header banner
    d.rectangle([0, 90, W, 360], fill=GREEN)
    logo(d, 120, 200, 110)
    d.text((210, 150), "你的数据，只属于你", font=F(50, True), fill=(255, 255, 255))
    d.text((212, 224), "本地加密 · 备份自主 · 无广告无追踪", font=F(32), fill=(225, 245, 237))
    cards = [
        ("本地加密存储", "主密码经 PBKDF2 + Android Keystore 保护，凭据存于 EncryptedSharedPreferences，密钥永不离开你的设备。"),
        ("Git 加密备份", "备份加密后同步到你自己的 Git 仓库，不经过任何第三方服务器，多设备自在同步。"),
        ("启动即锁", "支持指纹 / 面容 / 密码锁定，打开应用即需验证，他人无法偷看你的口令。"),
        ("无广告 · 无追踪", "纯本地小工具：不弹广告、不收集数据、不后台上报，绿色轻量、打开即用。"),
    ]
    y = 420
    for title, body in cards:
        ch = 280
        d.rounded_rectangle([50, y, W - 50, y + ch], radius=28, fill=SURFACE,
                            outline=CHIP, width=2)
        # small green checkmark bullet
        bx, by = 92, y + 55
        d.polygon([(bx, by+12), (bx+12, by+24), (bx+36, by), (bx+12, by+24)],
                  fill=GREEN)
        d.text((140, y + 40), title, font=F(40, True), fill=GREEN_D)
        # wrap body
        words = body
        lines, cur = [], ""
        maxw = W - 200
        for ch2 in words:
            if d.textlength(cur + ch2, font=F(30)) > maxw:
                lines.append(cur)
                cur = ch2
            else:
                cur += ch2
        lines.append(cur)
        ty = y + 110
        for ln in lines[:3]:
            d.text((90, ty), ln, font=F(30), fill=TEXT_2)
            ty += 46
        y += ch + 30
    return img


os.makedirs("screenshots", exist_ok=True)
gen_home().save("screenshots/home.png")
gen_add().save("screenshots/add.png")
gen_data().save("screenshots/data.png")
print("screenshots generated: home.png, add.png, data.png")
