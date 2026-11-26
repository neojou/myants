# crops.py — #86 觀感圖（最近鄰放大，不平滑）。在本目錄執行：python3 crops.py
#   cmp_copper_beetle_3x.png：銅蟻與甲蟲並排（pr_r1_copper_specimen_beetle，x480–700,y370–470）3×，下排為灰階（WCAG 亮度→sRGB 灰）
#   swatches_before_after_3x.png：每列一個色票，左 main b405ae4（修改前）、右 PR r1（修改後）；蟻＋瓢蟲 x480–720,y370–500 3×
#   night_copper_3x.png：夜間 k0（rim 關）／PR r1，蟻＋瓢蟲 3×
import os, numpy as np
from PIL import Image
os.chdir(os.path.dirname(os.path.abspath(__file__)))
def lum(a):
    c = a / 255.; c = np.where(c <= .04045, c / 12.92, ((c + .055) / 1.055) ** 2.4); return c @ [.2126, .7152, .0722]
def gray(im):
    l = lum(np.asarray(im.convert('RGB')).astype(float)); s = np.where(l <= .0031308, 12.92 * l, 1.055 * l ** (1 / 2.4) - .055)
    g = (np.clip(s, 0, 1) * 255 + .5).astype(np.uint8); return Image.fromarray(np.stack([g] * 3, -1))
def crop3(f, box): im = Image.open(f).convert('RGB').crop(box); return im.resize((im.width * 3, im.height * 3), Image.NEAREST)
def stack(rows):
    w = sum(i.width for i in rows[0]); h = sum(r[0].height for r in rows); out = Image.new('RGB', (w, h)); y = 0
    for r in rows:
        x = 0
        for i in r: out.paste(i, (x, y)); x += i.width
        y += r[0].height
    return out
b = crop3('pr_r1_copper_specimen_beetle.png', (480, 370, 700, 470)); stack([[b], [gray(b)]]).save('cmp_copper_beetle_3x.png')
A = (480, 370, 720, 500)
stack([[crop3(f'main_{s}_specimen.png', A), crop3(f'pr_r1_{s}_specimen.png', A)] for s in ['copper', 'black', 'gold', 'sage', 'blue']]).save('swatches_before_after_3x.png')
stack([[crop3('k0_r1_copper_specimen-night.png', A), crop3('pr_r1_copper_specimen-night.png', A)]]).save('night_copper_3x.png')
print('ok')
