# contour.py — #84 輪廓對比（MEASURE.md §4）：同構圖、同 seed 的 k=0 圖與開啟可讀性層的圖逐框比較。
# 用法：python3 contour.py K0.png [--beetle] [--r N] [--mask MASK.png] IMG.png ...
#   --mask（#91 夜間用）：遮罩與輪廓帶改由 MASK.png（同構圖的正午 k=0 圖）決定，亮度仍比 K0 與 IMG。
#   夜間地面太暗（L≈0.012），「L < 0.5×地面」在夜圖上選不到蟲體像素（遮罩為空），所以夜間要借正午 k=0 的幾何。
#   構圖凍結（#83：specimen 與 specimen-night 相機、蟲位、姿勢相同），兩張的剪影像素位置一致。不加 --mask 時行為不變。
#   specimen 圖量蟻（框 x505–600、y420–470）；--beetle 圖量甲蟲（框 x615–695、y422–462）。
# 遮罩：k=0 圖固定框內 L < 0.5 × k=0 地面中位 L（地面框 x700–740、y470–500）。只由 k=0 圖決定，兩張圖用同一組像素。
# 輪廓帶 edge = 遮罩 − 遮罩侵蝕 r px（預設 r=2；框邊視為遮罩內，不算輪廓）；本體內部 interior = 侵蝕後的遮罩。
# L = WCAG 相對亮度（sRGB→線性，0.2126R+0.7152G+0.0722B），與 79-chitin/measure.py 相同。
# edgeGain     = median(L_IMG[edge])     / median(L_K0[edge])      — k=0 時 ≈1.000，加強時應單調上升
# interiorGain = median(L_IMG[interior]) / median(L_K0[interior])  — 須維持 1.000（>1 代表內部被抬亮）
# 另印 mean 增益，以及 |ΔL|>0.002 的像素落在輪廓帶的比例（changedInEdge%），供 Artist 校準參考。
import sys
import numpy as np
from PIL import Image
from scipy.ndimage import binary_erosion

ANT = (slice(420, 470), slice(505, 600))
BEETLE = (slice(422, 462), slice(615, 695))
GROUND = (slice(470, 500), slice(700, 740))


def lum(c):
    c = np.asarray(c, float) / 255.0
    c = np.where(c <= 0.04045, c / 12.92, ((c + 0.055) / 1.055) ** 2.4)
    return c @ [0.2126, 0.7152, 0.0722]


def load(f):
    return np.asarray(Image.open(f).convert('RGB')).astype(float)


def bands(k0, box, r, ref=None):
    lb = lum(k0[box])
    ref = k0 if ref is None else ref
    mask = lum(ref[box]) < 0.5 * np.median(lum(ref[GROUND]))
    inner = binary_erosion(mask, iterations=r, border_value=1)
    return lb, mask & ~inner, inner


def gains(k0, img, box, r, ref=None):
    lb, edge, inner = bands(k0, box, r, ref)
    li = lum(img[box])
    changed = np.abs(li - lb) > 0.002
    return dict(
        edgePx=int(edge.sum()), innerPx=int(inner.sum()),
        edgeGain=np.median(li[edge]) / np.median(lb[edge]),
        interiorGain=np.median(li[inner]) / np.median(lb[inner]),
        edgeMeanGain=li[edge].mean() / lb[edge].mean(),
        interiorMeanGain=li[inner].mean() / lb[inner].mean(),
        changedInEdge=(changed & edge).sum() / max(changed.sum(), 1) * 100,
    )


def main(argv):
    beetle = '--beetle' in argv
    argv = [a for a in argv if a != '--beetle']
    r = 2
    if '--r' in argv:
        i = argv.index('--r')
        r = int(argv[i + 1])
        del argv[i:i + 2]
    ref = None
    if '--mask' in argv:
        i = argv.index('--mask')
        ref = load(argv[i + 1])
        del argv[i:i + 2]
    k0 = load(argv[0])
    box = BEETLE if beetle else ANT
    for f in argv[1:]:
        g = gains(k0, load(f), box, r, ref)
        print(f"{f}: {'beetle' if beetle else 'ant'} r={r} edgePx={g['edgePx']} innerPx={g['innerPx']} "
              f"edgeGain={g['edgeGain']:.3f} interiorGain={g['interiorGain']:.3f} "
              f"edgeMeanGain={g['edgeMeanGain']:.3f} interiorMeanGain={g['interiorMeanGain']:.3f} "
              f"changedInEdge%={g['changedInEdge']:.1f}")


if __name__ == '__main__':
    main(sys.argv[1:])
