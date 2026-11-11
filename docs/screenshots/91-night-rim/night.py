# night.py — #91 夜間合格帶的附加項（Artist 審查 #105 §5；實作同 Artist /workspace/m105 的 thin.py、n4.py）
# 用法：python3 night.py MASK_NOON_K0.png NIGHT_K0.png [--beetle] IMG.png ...
#   MASK＝同構圖、同 build 的正午 k=0 圖（幾何）；亮度比夜 k=0 與 IMG。
# 本體 body＝遮罩 binary_opening 2 次（去掉 ≤4px 細結構）；本體帶＝body − body 侵蝕 2px；細結構 thin＝遮罩 − body。
# 印：
#   thin  細結構中位增益：contour 框（同 contour.py --body 的 thinGain）與寬框（蟻 x500–600,y418–472；甲蟲 x590–700,y415–470，含觸角前端／大顎）
#   zone  頭前區域內對地 ≥3:1 的像素數（區域內全部像素，不限遮罩；numpy 半開區間）：蟻 x571–582,y430–462；甲蟲 x590–618,y428–458；與夜 k0 比
#   N4    本體帶上增益 >1.2 的像素 RGB 中位數 B−R；本體帶最亮 15% 的 RGB 中位數 B−R（兩者都要 ≥ 0）
import sys, numpy as np
from PIL import Image
from scipy.ndimage import binary_opening, binary_erosion


def L(c):
    c = np.asarray(c, float) / 255.
    c = np.where(c <= .04045, c / 12.92, ((c + .055) / 1.055) ** 2.4)
    return c @ [.2126, .7152, .0722]


def ld(f):
    return np.asarray(Image.open(f).convert('RGB')).astype(float)


G = (slice(470, 500), slice(700, 740))
a = sys.argv[1:]
beetle = '--beetle' in a
a = [x for x in a if x != '--beetle']
box = (slice(422, 462), slice(615, 695)) if beetle else (slice(420, 470), slice(505, 600))
wide = (slice(415, 470), slice(590, 700)) if beetle else (slice(418, 472), slice(500, 600))
zone = (slice(428, 458), slice(590, 618)) if beetle else (slice(430, 462), slice(571, 582))
M, K = ld(a[0]), ld(a[1])
gm = np.median(L(M[G]))


def parts(b):
    mask = L(M[b]) < 0.5 * gm
    body = binary_opening(mask, iterations=2, border_value=0)
    return mask, body


def ge3(img):
    g = np.median(L(img[G]))
    return int((((L(img[zone]) + .05) / (g + .05)) >= 3).sum())


mask, body = parts(box)
band = body & ~binary_erosion(body, iterations=2, border_value=1)
wmask, wbody = parts(wide)
wthin = wmask & ~wbody
thin = mask & ~body
lk, lkw = L(K[box]), L(K[wide])
for f in a[2:]:
    P = ld(f)
    lp, lpw, rgb = L(P[box]), L(P[wide]), P[box]
    up = band & (lp > 1.2 * lk)
    u = np.median(rgb[up], 0) if up.sum() else np.full(3, np.nan)
    v = lp[band]
    t = np.median(rgb[band][v >= np.percentile(v, 85)], 0)
    print(f"{f}: {'beetle' if beetle else 'ant'} "
          f"thinGain(box)={np.median(lp[thin]) / np.median(lk[thin]):.3f} [n={thin.sum()}] "
          f"thinGain(wide)={np.median(lpw[wthin]) / np.median(lkw[wthin]):.3f} [n={wthin.sum()}] | "
          f"zone>=3:1 {ge3(P)} (k0 {ge3(K)}) | "
          f"N4 litBand[n={up.sum()}] RGB={u.round(1)} B-R={u[2] - u[0]:+.1f}; bandTop15 RGB={t.round(1)} B-R={t[2] - t[0]:+.1f}")
