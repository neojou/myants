# #84 共用白天 rim（方案 C，無側面基底；實測值(headless)）

現行門檻見 [`docs/ART_DIRECTION.md`](../../ART_DIRECTION.md) §6.8 與 [`../MEASURE.md`](../MEASURE.md)。

## 公式

deferred KSL 材質 emission 加上：

```
rim = rimColor × k × pow(1 − saturate(N·V), p)
```

- 無 `side(N, L)` 基底（舊版 `0.35 + 0.65 × (1 − N·L)` 已拿掉），正面（N·V=1）為 0。
- 白天 `rimColor` = `sunColor` (1, 0.93, 0.82)，`k` = `RIM_DAY` 0.10，`p` = `RIM_EXP` 4.0；夜間 `RIM_NIGHT` 0（#91）。
- 套用：工蟻／兵蟻頭、胸、腹與甲蟲身體；腳、觸角、顎、眼與瓢蟲不套用（`RimParts`）。

## 拍攝

Playwright + Chrome headless（swiftshader）、1600×900、seed=12345、quality=medium、`83-specimen-night/shot_lock.py`。
k=0 對照：main `d0ec0d5` build（`k0_r1_*`，與 #95 基準 6c7f112 圖位元組相同；r2 與 r1 相同，未另存）。PR：`pr_r1_*`、`pr_r2_*`（兩次獨立啟動）。

## 本體填色與保留條件

| 指標 | 門檻 | k=0 | PR r1 | PR r2 |
|---|---|---|---|---|
| D1 蟻/地 | ≥3.0（地板） | 4.62 | 4.51 | 4.51 |
| D2 瓢蟲殼/斑點 | ≥3.3 | 3.31 | 3.31 | 3.31 |
| D3 甲蟲身體/地 | ≥3.0（地板） | 3.28 | 3.22 | 3.22 |
| 蟻 mid（`rim.py`） | ≤k0×1.10＝0.0227 | 0.0206 | 0.0214 | 0.0214 |
| 甲蟲 mid | ≤k0×1.10＝0.0452 | 0.0411 | 0.0419 | 0.0419 |
| hot%（蟻／甲蟲） | ≤2% | 0.0／0.0 | 0.0／0.0 | 0.0／0.0 |
| 瓢蟲 shell/gnd、殼像素最大差 | 不變 | 1.62 | 1.62、0 | 1.62、0 |

兩輪一致：specimen r1/r2 位元組相同；甲蟲 r1/r2 排除 FPS 區塊（x 0–199、y 0–59）後 0 差異像素。

## 輪廓對比（`contour.py`，2px 邊帶，對同輪 k=0）

| | k=0 | k=0.05 | **k=0.10（PR）** | k=0.15 |
|---|---|---|---|---|
| 蟻 edgeGain（中位） | 1.000 | 1.096 | **1.235** | 1.343 |
| 蟻 interiorGain（中位） | 1.000 | 1.000 | **1.000** | 1.000 |
| 甲蟲 edgeGain（中位） | 1.000 | 1.087 | **1.148** | 1.218 |
| 甲蟲 interiorGain（中位） | 1.000 | 1.000 | **1.000** | 1.000 |
| 甲蟲 interior mean | 1.000 | 1.009 | 1.018 | 1.027 |

k=0.05／0.15 只拍 r1，用來確認單調上升（只改 `RIM_DAY` 的掃描 build）。PR r1、r2 數值相同。
框：蟻 x 505–600、y 420–470；甲蟲 x 615–695、y 422–462；地面 x 700–740、y 470–500；遮罩為 k=0 圖框內 L < 0.5×地面中位 L。

## 圖

- 全圖：`k0_r1_specimen*.png`、`pr_r{1,2}_specimen*.png`
- 3× crop：`*_ant_crop3x.png`（x 480–720、y 370–500）、`*_beetle_crop3x.png`（x 600–710、y 410–475）；上下對照 `k0_vs_pr_ant.png`、`k0_vs_pr_beetle.png`（上 k=0、下 PR r1）
- 灰階（`gray.py`）：`pr_r1_specimen_gray.png`、`pr_r1_specimen_beetle_gray.png`、`k0_r1_specimen_gray.png` 與 3× 局部。灰階「找得到」由 Artist 判定；specimen 構圖只有工蟻（無兵蟻）。
- `rim.py` 是先前含側面基底試做時的 core/rim/S 量法（G1／G4 已不當判準），保留給 mid／hot%。
