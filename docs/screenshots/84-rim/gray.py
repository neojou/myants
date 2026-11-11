# gray.py — #84 灰階檢查用圖（MEASURE.md §5）：把 specimen 截圖去飽和（WCAG 相對亮度→sRGB 灰），
# 原尺寸輸出供「實際遊戲尺度」審查，另輸出 3× 局部（x480–740、y370–500）。Artist 審查項，不自動判定。
# 用法：python3 gray.py IMG.png ...   → IMG_gray.png、IMG_gray_crop3x.png
import sys
import numpy as np
from PIL import Image
from contour import lum

for f in sys.argv[1:]:
    y = lum(np.asarray(Image.open(f).convert('RGB')))
    s = np.where(y <= 0.0031308, y * 12.92, 1.055 * y ** (1 / 2.4) - 0.055)
    im = Image.fromarray((np.clip(s, 0, 1) * 255 + 0.5).astype(np.uint8), 'L')
    stem = f[:-4]
    im.save(f'{stem}_gray.png')
    c = im.crop((480, 370, 740, 500))
    c.resize((c.width * 3, c.height * 3), Image.NEAREST).save(f'{stem}_gray_crop3x.png')
