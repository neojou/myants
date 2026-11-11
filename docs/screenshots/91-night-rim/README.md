# #91 夜間月光 rim（同一套 RimShader；實測值(headless)）

現行規則見 [`docs/ART_DIRECTION.md`](../../ART_DIRECTION.md) §6.8 與 [`../MEASURE.md`](../MEASURE.md)。夜間**不套用**正午 D1／D3，也**不套用**正午輪廓合格帶；夜間合格帶由 Artist 依本頁實測另定。本頁的夜間數字只是實測紀錄，不是 PASS／FAIL。

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
- 地面、環境光、`MOON_INTENSITY`、albedo、roughness 都沒改。

## 拍攝

Playwright + Chrome headless（swiftshader）、1600×900、seed=12345、quality=medium、`83-specimen-night/shot_lock.py`，一次拍一張。
k=0（夜間 rim 關閉）對照：main `2eaa03b` build（`main_r1_*`；`17f2c0d` 只改文件）。PR：`pr_r1_*`、`pr_r2_*`，兩次獨立啟動。

## 白天回歸（正午 `shot=specimen`）

| 指標 | 門檻 | main | PR r1 | PR r2 |
|---|---|---|---|---|
| D1 蟻/地 | ≥3.0 | 4.51 | 4.51 | 4.51 |
| D2 瓢蟲殼/斑點 | ≥3.3 | 3.31 | 3.31 | 3.31 |
| D3 甲蟲本體/地 | ≥3.0 | 3.22 | 3.22 | 3.22 |
| 蟻 edgeGain／內部中位／內部平均（2px，對 `84-rim/k0_r1_*`） | 1.15–1.35／1.000／≤1.01 | 1.235／1.000／1.001 | 1.235／1.000／1.001 | 1.235／1.000／1.001 |
| 甲蟲 edgeGain／內部中位／內部平均 | 1.10–1.22／1.000／≤1.03 | 1.148／1.000／1.018 | 1.148／1.000／1.018 | 1.148／1.000／1.018 |
| hot%（蟻／甲蟲） | ≤2% | 0.0／0.0 | 0.0／0.0 | 0.0／0.0 |
| mid（蟻／甲蟲） | ≤main×1.10 | 0.0214／0.0419 | 同 | 同 |
| 瓢蟲 shell/gnd | 不變 | 1.62 | 1.62 | 1.62 |

PR 的正午兩張（`specimen`、`specimen_beetle`）r1、r2 都和 main 位元組相同，也和 `84-rim/pr_r1_*`（#98）位元組相同，所以只存 `pr_r1_specimen*.png`。

## 夜間（`shot=specimen-night`，只記錄，合格帶待 Artist）

### 輪廓對比（`84-rim/contour.py --mask`，2px）

夜地面 L≈0.012，`contour.py` 預設的「k=0 圖 L < 0.5×地面」在夜圖上選不到蟲（遮罩為空，edgePx=0）。所以夜間加 `--mask`：遮罩與輪廓帶的幾何借同構圖的正午 k=0 圖（`84-rim/k0_r1_specimen*.png`），亮度仍是夜 k=0 與夜 PR 相比。構圖凍結（#83），剪影像素位置相同。不加 `--mask` 時腳本行為不變（正午數字重跑一致）。

| | edgeGain | interiorGain（中位） | edgeMeanGain | interiorMeanGain | changedInEdge% |
|---|---|---|---|---|---|
| 夜 k=0（main 對自己） | 1.000 | 1.000 | 1.000 | 1.000 | 0.0 |
| 蟻 PR r1 | 1.697 | 1.061 | 2.456 | 1.070 | 90.9 |
| 蟻 PR r2 | 1.697 | 1.061 | 2.456 | 1.070 | 90.9 |
| 甲蟲 PR r1 | 2.320 | 1.115 | 4.271 | 1.574 | 22.0 |
| 甲蟲 PR r2 | 2.320 | 1.115 | 4.271 | 1.574 | 22.0 |

注意：p=2.5 比白天的 4.0 寬，夜間內部中位數也被抬亮（蟻 1.061、甲蟲 1.115）。正午的「內部中位 1.000」不套用於夜間；這組數字留給 Artist 定帶時參考。

### 灰階（`84-rim/gray.py`）

`main_r1_specimen-night_gray*.png`（k=0）、`pr_r1_specimen-night_gray*.png`、`pr_r1_specimen-night_beetle_gray*.png`。「找得到」由 Artist 判定。

### 其他保留項（`rim.py`，本目錄；參考 #91 Artist 舊表 N4–N6）

| 項目 | main k=0 | PR r1 | PR r2 |
|---|---|---|---|
| N5 夜地面框 RGB 中位數（蟻構圖／甲蟲構圖） | [30 28 27]／[30 28 27] | [30 28 27]／[30 28 27] | 同 r1 |
| N6 夜地面 B/R ÷ 日地面 B/R（#74，≥1.35） | 0.900／0.661 = 1.36 | 1.36 | 1.36 |
| N4 遮罩內最亮 15% RGB 中位數（蟻／甲蟲） | [44 42.5 42.5]／[42 32 27] | [100 98 99]／[74 70 73] | 同 r1 |
| 蟻 rim/gnd、core/gnd | 1.20、1.07 | 2.80、1.07 | 同 r1 |
| 甲蟲 rim/gnd、core/gnd | 1.05、1.03 | 1.81、1.03 | 同 r1 |
| 瓢蟲 shell/gnd、shellrim/gnd | 1.51、2.02 | 1.58、2.85 | 同 r1 |
| 夜 D1／D2／D3（`81-beetle/measure.py`，只參考） | 1.30／1.84／1.02 | 1.29／1.94／1.03 | 同 r1 |

- N4：rim 讓最亮 15% 從暖灰變成接近中性灰（B−R 由 −1.5／−15 變 −1／−1）；舊表「B ≥ R」差 1 階未成立。是否要求，由 Artist 定帶時決定。
- 本體 core/gnd 不變（蟻 1.07、甲蟲 1.03）：本體仍是暗剪影，亮的是邊。

**風險（PM 決策 #91）**：夜地面 B/R 實測 1.36，門檻 1.35（#74），餘裕極薄；之後任何動到夜間地面／環境光／月光的改動都要重測 N5／N6。

兩輪一致：`specimen-night` r1/r2 位元組相同；`specimen-night_beetle` r1/r2 0 差異像素。

## 圖

- 全圖：`main_r1_specimen-night*.png`（k=0）、`pr_r1_specimen-night*.png`、`pr_r2_specimen-night*.png`、`pr_r1_specimen.png`／`pr_r1_specimen_beetle.png`（正午；main 正午圖與 `84-rim/pr_r1_*` 位元組相同，不重複存）
- 3× crop：`*_specimen-night_crop3x.png`（x 480–720、y 370–500，蟻與瓢蟲）、`*_specimen-night_beetle_crop3x.png`（x 600–710、y 410–475）；上下對照 `main_vs_pr_night_ant.png`（上 k=0、下 PR r1）
- 重跑全部數字：`bash docs/screenshots/91-night-rim/verify.sh`（只用已提交的圖；正午 main 對照＝`84-rim/pr_r1_specimen*.png`，與 main 位元組相同）
