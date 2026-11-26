# cmpfps.py — 兩張同構圖截圖逐像素比較（排除左上 FPS 區 y0–59、x0–199）。
# 另分兩區印：左側 3D 室外世界 x<1180，與右側面板／巢穴剖面 x≥1180（巢穴剖面直接用 antColor hex，#86 不動）。
# 用法：python3 cmpfps.py A.png B.png
import sys,numpy as np
from PIL import Image
a=np.asarray(Image.open(sys.argv[1]).convert('RGB')).astype(int);b=np.asarray(Image.open(sys.argv[2]).convert('RGB')).astype(int)
d=np.abs(a-b).max(2); d[0:60,0:200]=0
w=d[:, :1180]; p=d[:, 1180:]
print(sys.argv[1],'vs',sys.argv[2],'diffpx(excl FPS)=',int((d>0).sum()),'max',int(d.max()),'| world x<1180:',int((w>0).sum()),'| panel/nest x>=1180:',int((p>0).sum()))
