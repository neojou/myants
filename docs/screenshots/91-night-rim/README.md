# #91 夜間月光 rim（同一套 RimShader；實測值(headless)）

現行規則見 [`docs/ART_DIRECTION.md`](../../ART_DIRECTION.md) §6.8 與 [`../MEASURE.md`](../MEASURE.md)。夜間**不套用**正午 D1／D3，也**不套用**正午輪廓合格帶；夜間合格帶由 Artist 在 #105 審查 §5 定案（本體帶），本頁照該帶判定；寫進 ART_DIRECTION／MEASURE.md 由 PM 在合併後處理。

## 公式與參數

與 #84 同一條（作法 A，deferred KSL 材質 emission）：

```
rim = rimColor(bright) × k(bright) × pow(1 − saturate(N·V), p(bright))
```

| | 夜（`isNight`，bright < 0.2） | 晨昏 | 正午（bright = 1） |
|---|---|---|---|
| `rimColor` | `moonColor` (0.62, 0.66, 0.78) | `lightColor`（太陽） | `sunColor` (1, 0.93, 0.82) |
| `k`（蟻、甲蟲） | `RIM_NIGHT` **0.45** | `RIM_DAY·t + RIM_NIGHT·(1−t)`，t 同 `ambientStrength` | `RIM_DAY` 0.10（#98 不變） |
| `k`（瓢蟲紅殼，含黑點與前胸） | 0.6 × `RIM_NIGHT` = 0.27 | 0.6 × `RIM_NIGHT` × (1−t) | **0**（正午瓢蟲不變） |
| `p` | `RIM_EXP_NIGHT` **2.5** | `RIM_EXP·t + RIM_EXP_NIGHT·(1−t)` | `RIM_EXP` 4.0（#98 不變） |

- 參數用 Artist 原案起點（#91 規格：`RIM_NIGHT` 0.30–0.60 起點 0.45、p 2.0–3.0 起點 2.5），沒有另外調。
- 寫成 `a·t + b·(1−t)`，t=1 時精確等於 `RIM_DAY`／`RIM_EXP`，正午 uniform 與 #98 位元相同。
- 線性峰值 0.45 × 0.78 = 0.35 < `BLOOM_THRESHOLD` 1.2（計算值）。
- 套用部位：工蟻／兵蟻頭、胸、腹；甲蟲身體與斑紋；瓢蟲殼與黑點（夜間限定 profile）。腳、觸角、顎、眼、蚜蟲等不套用。
- **Artist FAIL 修正（#105）**：修正前蟻觸角（`Head/AntennaL/{Scape,Funiculus}`）落在頭部批次、甲蟲觸角與大顎落在身體批次，實際有套 rim；夜間 N·V≈0 打到峰值，成為斷續亮點。現在蟻觸角在 `antenna` 批次（頭部顏色、`CHITIN_ROUGHNESS`、無 rim），甲蟲 `Antenna*`／`Mandible*` 在 `appendage` 批次（身體顏色與 roughness、無 rim）。`RIM_NIGHT`、p、`rimColor` 未改。
- 地面、環境光、`MOON_INTENSITY`、albedo、roughness 都沒改。

## 拍攝

Playwright + Chrome headless（swiftshader）、1600×900、seed=12345、quality=medium、`83-specimen-night/shot_lock.py`，一次拍一張（port 18911）。

- `pr_r1_*`、`pr_r2_*`：修正後 PR build（`c928041`），兩次獨立啟動。四張 r2 都和 r1 位元組相同；正午 r2 未另存。
- `k0_r1_*`：**同一個 build** 只把 `RIM_DAY`、`RIM_NIGHT` 設 0 的量測用 build（未提交）。正午 k0 當夜間幾何 MASK；它和 `84-rim/k0_r1_specimen*.png` 只差左上 FPS 區（甲蟲構圖位元組相同）。夜 k0 和修正前 main `2eaa03b` 的夜圖位元組相同。
- 正午「main」對照（mid、瓢蟲）：`84-rim/pr_r1_specimen*.png`（#98，main 正午圖）。

## 白天回歸（正午 `shot=specimen`）

| 指標 | 門檻 | main（#98） | 修正後 r1 | 修正後 r2 |
|---|---|---|---|---|
| D1 蟻/地 | ≥3.0 | 4.51 | 4.56 | 4.56 |
| D2 瓢蟲殼/斑點 | ≥3.3 | 3.31 | 3.31 | 3.31 |
| D3 甲蟲本體/地 | ≥3.0 | 3.22 | 3.22 | 3.22 |
| 蟻 edgeGain／內部中位／內部平均（2px，對同 build k0） | 1.15–1.35／1.000／≤1.01 | 1.235／1.000／1.001 | **1.132**／1.000／1.001 | **1.132**／1.000／1.001 |
| 蟻本體帶 edgeGain（`--body`，只參考） | — | 1.237 | 1.237 | 1.237 |
| 甲蟲 edgeGain／內部中位／內部平均 | 1.10–1.22／1.000／≤1.03 | 1.148／1.000／1.018 | 1.135／1.000／1.018 | 1.135／1.000／1.018 |
| hot%（蟻／甲蟲） | ≤2% | 0.0／0.0 | 0.0／0.0 | 0.0／0.0 |
| mid（蟻／甲蟲） | ≤main×1.10 | 0.0214／0.0419 | 0.0214／0.0419 | 同 |
| 瓢蟲 shell/gnd | 不變 | 1.62 | 1.62 | 1.62 |

**偏差（待 PM／Artist 裁決）**：工蟻 2px edgeGain 1.132，低於正午帶下限 1.15。原因是觸角不再套白天 rim：2px 帶有一部分是觸角像素，修正前它們被 rim 抬亮，現在增益是 1.000，把中位數拉低。本體帶（`--body`，去掉觸角、腳等細結構）是 1.237，和修正前 1.237 相同，頭胸腹的白天邊光沒變。我沒有調 `RIM_DAY`，也沒有改門檻。D1 由 4.51 升到 4.56（推測是觸角像素變暗）。

## 夜間（`shot=specimen-night`，Artist §5 合格帶，每張單獨判定）

量法：`84-rim/contour.py --mask k0_r1_specimen*.png --body`（幾何用同 build 正午 k0；亮度比夜 k0 與夜 PR）。`--body`：遮罩先 `binary_opening(iterations=2)` 去掉 ≤4px 細結構，再取外緣 2px；內部＝本體侵蝕 2px（同 Artist `nband.py`）。不加 `--body` 時輸出和修改前逐字相同（正午、夜間各圖重跑比對）。

### 本體帶（正式）

| | 門檻 | 修正前（36e99ba） | 修正後 r1 | 修正後 r2 |
|---|---|---|---|---|
| 工蟻 edgeGain | 1.85–2.70 | 2.275 | 2.163 | 2.163 |
| 工蟻 內部中位／平均 | ≤1.10／≤1.20 | 1.024／1.100 | 1.024／1.100 | 1.024／1.100 |
| 工蟻 core/gnd（`rim.py`） | ≤1.5 | 1.07 | 1.07 | 1.07 |
| 甲蟲 edgeGain | 2.40–3.85 | 3.146 | 2.672 | 2.672 |
| 甲蟲 內部中位／平均 | ≤1.20／≤1.75 | 1.115／1.575 | 1.115／1.575 | 1.115／1.575 |
| 甲蟲 core/gnd | ≤1.5 | 1.03 | 1.03 | 1.03 |

本體帶 edgeGain 比修正前低（蟻 2.275→2.163、甲蟲 3.146→2.672），推測是 opening 後的本體仍含觸角根部與大顎基部的幾個像素，修正前它們是 rim 峰值。仍在合格帶內。

2px 預設帶（紀錄，不判）：工蟻 1.697／1.061／1.070；甲蟲 1.987／1.115／1.574（r1、r2 相同）。

### 細結構不得套 rim（`night.py`）

| | 門檻 | 修正前 | 修正後 r1 | 修正後 r2 |
|---|---|---|---|---|
| 工蟻 細結構中位增益（contour 框／寬框） | ≤1.05 | 1.000／1.000 | 1.000／1.000 | 1.000／1.000 |
| 甲蟲 細結構中位增益（contour 框／寬框） | ≤1.05 | 1.152／1.113 | 1.000／1.000 | 1.000／1.000 |
| 蟻頭前 x571–582,y430–462 對地 ≥3:1 像素（k0） | ≤k0 | 3（1） | 1（1） | 1（1） |
| 甲蟲頭前 x590–618,y428–458 對地 ≥3:1 像素（k0；#105 合併後區域改為 x590–616，修正後為 0） | ≤k0 | 28（0） | **1**（0） | **1**（0） |

**偏差（字面不過，待裁決）**：甲蟲頭前區仍有 1 個 ≥3:1 像素 (617,441)，RGB [125 127 136]（4.25:1），和修正前同一像素同值。它在正午遮罩的本體裡（opening 後仍在，屬本體帶），位置是頭殼前緣，落在區域右邊界 x=617。這個 build 的觸角與大顎批次沒有 rim，能被抬亮的只剩有 rim 的頭殼，所以它是頭殼邊光，不是細結構。觸角與大顎的 27 個碎點已全部消失（見 6× 對照圖）。我沒有改門檻或參數。

### 其他保留項

| 項目 | 門檻 | k0 | 修正後 r1 | 修正後 r2 |
|---|---|---|---|---|
| N4 本體帶增益 >1.2 像素 RGB 中位 B−R（蟻／甲蟲） | ≥0 | — | +6.0／+2.0 | 同 r1 |
| N4 本體帶最亮 15% B−R（蟻／甲蟲） | ≥0 | +1.0／−15.0 | +11.5／+2.0 | 同 r1 |
| N5 夜地面 RGB（兩構圖） | 每 channel ±2 | [30 28 27] | [30 28 27] | [30 28 27] |
| N6 夜／日地面 B/R | ≥1.35 | 0.900／0.661 = 1.36 | 1.36 | 1.36 |
| 夜 D2 | ≥1.8 | 1.84 | 1.94 | 1.94 |
| 瓢蟲 shell/gnd、shellrim/gnd | 可辨 | 1.51、2.02 | 1.58、2.85 | 同 r1 |
| rim 線性峰值 | <1.2 | — | 0.45×0.78 = 0.35（計算值） | 同 |
| 夜 D1（只參考） | — | 1.30 | 1.29 | 1.29 |

**風險（PM 決策 #91）**：N6 實測 1.36，門檻 1.35，餘裕極薄；之後動到夜間地面／環境光／月光都要重測 N5／N6。

## 圖

- 全圖：`k0_r1_specimen*.png`（同 build k=0；正午兩張是 MASK）、`pr_r1_specimen*.png`、`pr_r2_specimen-night*.png`
- **6× 對照**（上 k=0、中 修正前 36e99ba、下 修正後）：`specimen-night_beetlehead_k0_vs_pr_6x.png`（甲蟲頭前 x582–630,y418–466）、`specimen-night_anthead_k0_vs_pr_6x.png`（蟻頭 x555–600,y422–470）
- 6× 單張：`{k0,pr}_r1_specimen{,-night}_{anthead,beetlehead}_crop6x.png`
- 3× crop：`*_specimen-night_crop3x.png`（x 480–720、y 370–500）、`*_specimen-night_beetle_crop3x.png`（x 600–710、y 410–475）
- 灰階（`84-rim/gray.py`）：`{k0,pr}_r1_specimen-night_gray*.png`、`pr_r1_specimen-night_beetle_gray*.png`
- 重產 crop：`python3 crops.py DIR`（DIR 需有 `k0_r1_*`、`pr_r1_*` 與修正前 `old_r1_*`）
- 重跑全部數字：`bash docs/screenshots/91-night-rim/verify.sh`（只用已提交的圖）
