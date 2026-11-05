# 用法：python3 compare.py A.png B.png
# 比較兩張截圖的任何像素差（只要不為 0 就算），分成整張圖和排除 FPS 區塊兩種。
# 排除區塊：左上 FPS 數字 x 0–199、y 0–59（1600x900 截圖，數字本身落在 x 80–95、y 23–34）。
import sys
import numpy as np
from PIL import Image
FPS_BOX = (slice(0, 60), slice(0, 200))  # (y, x)
a = np.asarray(Image.open(sys.argv[1]).convert("RGB"), int)
b = np.asarray(Image.open(sys.argv[2]).convert("RGB"), int)
d = np.abs(a - b).max(2)
whole = int((d > 0).sum())
d[FPS_BOX] = 0
rest = int((d > 0).sum())
print(f"{sys.argv[1]} vs {sys.argv[2]}: whole={whole} px, excluding FPS box={rest} px, max={int(d.max())}/255")
