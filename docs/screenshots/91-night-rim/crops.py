# crops.py — #91 修正版的局部放大圖（最近鄰放大，不平滑）。
# 用法：python3 crops.py DIR（DIR 內有 k0_r1_*、pr_r1_*）；輸出寫到本目錄。
#   3×：蟻＋瓢蟲 x480–720,y370–500；甲蟲 x600–710,y410–475
#   6×：蟻頭（含觸角） x555–600,y422–470；甲蟲頭前（觸角／大顎） x582–630,y418–466
#   *_k0_vs_pr_6x.png：上 k=0、中 修正前 PR（36e99ba，DIR/old_r1_*）、下 修正後 PR r1
import sys, os
from PIL import Image
src = sys.argv[1]
out = os.path.dirname(os.path.abspath(__file__))
CROPS = {
    'specimen-night_crop3x': ('specimen-night', (480, 370, 720, 500), 3),
    'specimen-night_beetle_crop3x': ('specimen-night_beetle', (600, 410, 710, 475), 3),
    'specimen-night_anthead_crop6x': ('specimen-night', (555, 422, 600, 470), 6),
    'specimen-night_beetlehead_crop6x': ('specimen-night_beetle', (582, 418, 630, 466), 6),
    'specimen_anthead_crop6x': ('specimen', (555, 422, 600, 470), 6),
    'specimen_beetlehead_crop6x': ('specimen_beetle', (582, 418, 630, 466), 6),
}
for tag in ('k0_r1', 'pr_r1'):
    for name, (shot, box, s) in CROPS.items():
        im = Image.open(f'{src}/{tag}_{shot}.png').convert('RGB').crop(box)
        im.resize((im.width * s, im.height * s), Image.NEAREST).save(f'{out}/{tag}_{name}.png')
for name in ('specimen-night_anthead_crop6x', 'specimen-night_beetlehead_crop6x'):
    shot, box, s = CROPS[name]
    old = Image.open(f'{src}/old_r1_{shot}.png').convert('RGB').crop(box)
    rows = [Image.open(f'{out}/k0_r1_{name}.png'), old.resize((old.width * s, old.height * s), Image.NEAREST),
            Image.open(f'{out}/pr_r1_{name}.png')]
    a = rows[0]
    c = Image.new('RGB', (a.width, a.height * 3 + 12), (255, 255, 255))
    for i, im in enumerate(rows):
        c.paste(im, (0, i * (a.height + 6)))
    c.save(f'{out}/{name.replace("_crop6x", "")}_k0_vs_pr_6x.png')
