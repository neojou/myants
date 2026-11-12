# #86 3D 自選蟻色：真銅色 Copper、Black 黑殼、自選色亮度上限

規格：Artist #86 留言 6820849554；派工：PM 6820892223。ART_DIRECTION 條文在 #108（不在本 PR）。

## 改了什麼
- `WorldPalette.copperChitin = (0.165, 0.040, 0.002)`（線性，Y 0.0638）；`outdoorChitin(Copper #c4703a)` 直接回傳它。
- `outdoorChitin(Black #222222)` 回傳 `antChitin` (0.036, 0.036, 0.039)。
- 其他色票與設定檔任意 hex：`toLinear() × CHITIN_ALBEDO_SCALE`(0.52)；線性 Y > `CHITIN_Y_MAX` 0.065 時 RGB 等比縮小；alpha 保留；不設下限。
- 不動：Copper 的 hex／名稱／2D／巢穴剖面／設定檔、`CHITIN_ALBEDO_SCALE`、roughness、`RIM_*`、rimColor。

## 量法
Playwright + Chrome headless（swiftshader），1600×900、seed 12345、quality medium、shot_lock（假時鐘 10 秒後 resume；`shot86.py` 同 Artist `/workspace/s86`）。色票用 init script 預寫 `localStorage antsim.ui.v1 = antColor=<hex>`；Copper 不寫（新預設）。一次一張。

| 檔名 | 來源 |
|---|---|
| `pr_r{1,2}_<色票>_*.png` | PR build（實作 commit `0842d88`；其後只加測試與文件），兩次獨立啟動 |
| `k0_r1_copper_*.png` | 同一 commit，只把 `RIM_DAY`、`RIM_NIGHT` 設 0 的量測用 build（未提交，用完還原） |
| `main_<色票>_specimen.png`、`main_copper_specimen{_beetle,-night}.png` | 修改前：main `b405ae4` 程式（Copper＝`/workspace/m105r2` a581228 圖，與 `91-night-rim/pr_r1_specimen.png` 排 FPS 後 0 px 差；其他色票＝Artist 用 b405ae4 CI js-distribution 拍的圖），各 1 輪 |

重跑全部數字：`./verify.sh`（只讀已提交的圖）。觀感圖：`python3 crops.py`；灰階：`84-rim/gray.py`。

## 結果（實測值(headless)，`verify.sh` 輸出；r2 每張都與 r1 位元組相同）

### 正午 specimen（每張單獨判：D1 ≥3.0、hot% ≤2%）
| 色票 | 修改前 D1／hot% | 修改後 r1 D1／hot% | r2 | D2 | 瓢蟲 shell/gnd |
|---|---|---|---|---|---|
| Copper（預設） | 4.56／0.0（黑殼） | **3.84**／0.0 | 3.84／0.0 | 3.31 | 1.62 |
| Black | 5.50／0.0 | **4.56**／0.0 | 4.56／0.0 | 3.31 | 1.62 |
| Gold | 1.80／13.9 | **3.58**／0.0 | 3.58／0.0 | 3.31 | 1.62 |
| Sage | 1.74／0.2 | **3.62**／0.0 | 3.62／0.0 | 3.31 | 1.62 |
| Blue | 2.04／0.0 | **3.62**／0.0 | 3.62／0.0 | 3.31 | 1.62 |

甲蟲構圖 D3：Copper r1／r2、Black／Gold／Sage／Blue r1 全部 3.22（與 main 相同）。

### Black 對 main 預設（黑殼）逐像素
排除 FPS 區後 113 px 不同（r1、r2、甲蟲構圖都一樣），**全部在右側巢穴剖面**（x1387–1397、y440–454）：巢穴剖面直接用 antColor hex（#222222 vs #c4703a），規格指定不動。左側 3D 室外世界（x<1180）0 px 不同。

### Copper 新預設：正午 `contour.py --body`（同 build k0）
| 項目 | 門檻 | r1 | r2 |
|---|---|---|---|
| 本體帶 edgeGain | 1.15–1.35 | 1.219 | 1.219 |
| 內部中位 | 1.000 | **1.005（不過）** | **1.005（不過）** |
| 內部平均 | ≤1.01 | 1.001 | 1.001 |
| 2px 帶（紀錄） | — | 1.096／1.000／1.001 | 同 |

### Copper 夜間（`--mask` 同 build 正午 k0；亮度比同 build 夜 k0）
| 項目 | 門檻 | r1 | r2 |
|---|---|---|---|
| 本體帶 edgeGain | 1.75–2.55 | 2.031 | 2.031 |
| 內部中位／平均 | ≤1.10／≤1.20 | 1.064／1.086 | 同 |
| core/gnd | ≤1.5 | 1.04 | 1.04 |
| 細結構 thinGain | ≤1.05 | 1.009（寬框 1.011） | 同 |
| 頭前 ≥3:1 像素 | ≤k0（1） | 1 | 1 |
| N4 增益 >1.2 像素 B−R | ≥0 | **−10.0（n=130，不過）** | **−10.0** |
| N4 本體帶最亮 15% B−R | ≥0 | +2.0 | +2.0 |
| N5 地面 | 不變 | [30 28 27] | 同 |
| 夜 D2（參考） | — | 1.94 | 1.94 |

### 觀感圖
- `cmp_copper_beetle_3x.png`：銅蟻與甲蟲並排 3×，下排灰階。銅蟻是紅銅色；甲蟲鞘翅也是鏽紅，但體長、寬鞘翅、大顎與蟻的三節細身一眼可分。
- `swatches_before_after_3x.png`：每列一個色票（Copper、Black、Gold、Sage、Blue），左修改前、右修改後。
- `night_copper_3x.png`：夜間 k0／PR。
- `pr_r1_copper_specimen{,_beetle}_gray{,_crop3x}.png`：灰階，工蟻、甲蟲、瓢蟲都找得到。
