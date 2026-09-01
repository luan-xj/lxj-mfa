from PIL import Image, ImageDraw
import base64

# Preview 1: launcher icon on Android-ish background
launcher = Image.open('app/src/main/res/mipmap-xxxhdpi/ic_launcher.png').resize((192,192), Image.LANCZOS)
bg = Image.new('RGB', (320,320), (245,245,245))
shadow = Image.new('RGBA', (200,200), (0,0,0,0))
d = ImageDraw.Draw(shadow)
d.rounded_rectangle((6,6,196,196), radius=42, fill=(0,0,0,40))
bg.paste(shadow, (60,64), shadow)
bg.paste(launcher, (64,64), launcher)
bg.save('preview_launcher.png')

# Preview 2: toolbar mockup
toolbar = Image.new('RGB', (720, 128), (46, 158, 123))
logo = Image.open('app/src/main/res/drawable-xhdpi/ic_app_logo.png')
toolbar.paste(logo, (16, (128-logo.height)//2), logo)
toolbar.save('preview_toolbar.png')

def b64(data):
    return base64.b64encode(data).decode()

html = f"""<html><head><meta charset="utf-8"><style>
body{{font-family:Arial,sans-serif;background:#f4fbf8;margin:0;padding:24px;color:#163a2f}}
h2{{color:#1f7a5e}} .row{{display:flex;gap:32px;align-items:flex-start;flex-wrap:wrap}}
.card{{background:#fff;border:1px solid #cfe9df;border-radius:12px;padding:16px;text-align:center;box-shadow:0 2px 8px rgba(0,0,0,.05)}}
.card img{{display:block}} .lab{{margin-top:8px;color:#5e7a70;font-size:14px}}
</style></head><body>
<h2>图标预览 v1.0.27（按截图标准：白底 + 绿盾 + 白字）</h2>
<div class="row">
<div class="card"><img width="220" src="data:image/png;base64,{b64(open('preview_launcher.png','rb').read())}"><div class="lab">启动图标</div></div>
<div class="card"><img width="360" src="data:image/png;base64,{b64(open('preview_toolbar.png','rb').read())}"><div class="lab">账号页左上角（仅图标，无文字）</div></div>
</div></body></html>"""
open('preview.html','w').write(html)
print('preview generated')
