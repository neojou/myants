# 角色可讀性量測說明

現行門檻以 [`docs/ART_DIRECTION.md`](../ART_DIRECTION.md) §6.8 為準。本檔說明怎麼量、用哪支腳本、拍什麼構圖。

各次 PR 資料夾（例如 `79-chitin/`、`94-ant-normals/`）裡的「門檻」欄若與本檔不同，那些表是當時的紀錄。審查新的對比或可讀性變更時，用本檔與 ART_DIRECTION 的現行規則。

## 1. 拍攝條件

- 構圖：正午用 `shot=specimen`，夜間用 `shot=specimen-night`。要把甲蟲擺進同一構圖時，加 `specimenCritter=beetle`。
- 環境：Playwright 加 Chrome headless（swiftshader）、1600×900、seed=12345、quality=medium。
- 鎖時鐘：用 `docs/screenshots/83-specimen-night/shot_lock.py`，避免畫面跟著真實時間走。
- 正午驗收要拍兩輪，兩輪皆須達標。
- 桌面實機畫面仍等 Neo 補拍。headless 數字是**實測值(headless)**，不能寫成實機截圖。

## 2. 對比公式

對比值用 WCAG 相對亮度，與現有 `measure.py` 相同：`(L1 + 0.05) / (L2 + 0.05)`。

`L` 是 sRGB 轉線性後，以 `0.2126R + 0.7152G + 0.0722B` 得到的相對亮度。較亮的一邊當分子。門檻不能四捨五入：2.999 不算達到 3.0。

在報告裡標明每個數字是**計算值**還是**實測值**。

## 3. 本體填色與內部圖案（D1、D2、D3）

這三項的定義沒有改，改的是它們在驗收裡扮演的角色。D1 和 D3 是本體填色的地板，不再用來卡 rim 或描邊。D2 是瓢蟲殼與斑點的內部識別，和 rim 分開驗。

| 指標 | 定義 | 腳本與框 | 正午門檻 |
|---|---|---|---|
| D1 | specimen 固定框裡，蟻身最暗 10% 像素的中位數，對同一框裡較亮（≥第 60 百分位）的地面中位數。 | `79-chitin/measure.py` 或 `81-beetle/measure.py`（不加 `--beetle`）。蟻 ROI：x 505–600、y 420–470。 | ≥3.0（不再用 ≥4.5） |
| D2 | 瓢蟲紅殼中位數，對非紅像素中最暗 10%（斑點）的中位數。這是物件內部對比，不是對地面。 | 同上。瓢蟲 ROI：x 590–680、y 395–480。紅色判定：`R > 1.8G` 且 `R > 120`。 | ≥3.3 |
| D3 | 甲蟲固定框裡 L<0.15 的本體像素中位數，對另一個地面框的中位數。 | `81-beetle/measure.py --beetle`。本體框：x 615–695、y 422–462。地面框：x 700–740、y 470–500。 | ≥3.0（不再用 ≥3.25） |

用法：

```bash
python3 docs/screenshots/79-chitin/measure.py specimen.png
python3 docs/screenshots/81-beetle/measure.py --beetle specimen_beetle.png
```

## 4. 輪廓對比（用來驗 rim 或描邊）

對照同一構圖、同一 seed 的兩張圖：k=0（可讀性層關閉，且須對得上 #95 基準）與開啟可讀性層的圖。

- 只量固定幾何輪廓帶，不要用會把本體算進去的框去驗 rim。#84 試做時用的是 2px 邊帶。
- **邊帶增益**：輪廓帶內相對亮度中位數，相對 k=0 同一帶的比值。k=0 時增益應接近 1.000；加強時應單調上升。
- **本體內部**：輪廓帶以內的中位數增益須維持 1.000。若內部也被抬亮，代表公式帶了側面基底，或作用範圍過寬，不算通過。
- 數字目標由 Artist 依實測校準，本檔不先寫死（例如不先訂 1.20）。#84 在含側面基底的試做裡，邊帶增益約 1.066–1.092，內部中位數 1.000；無側面基底的合格帶，等重開後的實測再寫進 ART_DIRECTION。

腳本：`84-rim/contour.py`（#84 補上）。遮罩只由 k=0 圖決定：固定框內 L < 0.5 × k=0 地面中位 L（蟻框 x 505–600、y 420–470；甲蟲框 x 615–695、y 422–462；地面框 x 700–740、y 470–500）。輪廓帶＝遮罩減去侵蝕 2px 後的遮罩（框邊不算輪廓），本體內部＝侵蝕後的遮罩。

```bash
python3 docs/screenshots/84-rim/contour.py k0_specimen.png pr_specimen.png
python3 docs/screenshots/84-rim/contour.py k0_specimen_beetle.png --beetle pr_specimen_beetle.png
```

輸出 `edgeGain`（輪廓帶中位數增益）、`interiorGain`（內部中位數增益，須為 1.000），另附 mean 增益與變動像素落在輪廓帶的比例，供 Artist 校準。灰階檢查用圖可用 `84-rim/gray.py` 產生（見下一節）。

## 5. 灰階檢查

把正午（以及夜間，見下一節）的 specimen 截圖去飽和（`python3 docs/screenshots/84-rim/gray.py IMG.png` 會輸出原尺寸灰階圖與 3× 局部），在實際遊戲尺度下看，應能立刻指出工蟻、兵蟻、甲蟲、瓢蟲。這是 Artist 審查項，不必第一次就自動化。審查留言裡註明看的是哪一張圖、哪一個 HEAD sha。

## 6. 夜間

夜間用 `shot=specimen-night`（甲蟲加 `specimenCritter=beetle`）。

- **不要套用正午的 D1／D3 門檻。** 本體填色可以低於 3。把 D1、D2、D3 記成參考值即可。
- 夜間驗收看同一套可讀性層的輪廓對比，以及灰階「找得到」。強度與色溫可以和白天不同。
- 夜間的數字門檻尚未定，由 Artist 用 specimen-night 實測後再寫進 ART_DIRECTION。
- 既有參考（#83／#90，實測值(headless)）：夜間 D1 1.29、D2 1.84；甲蟲鞘翅／前胸對地約 1.04／1.05。

## 7. 回歸：k=0 須對上 #95

任何可讀性層在關閉（k=0）時，正午畫面須與 #95 基準一致。這是為了避免「為了可讀又把材質弄壞」。

#95 正午 headless 兩輪實測值：D1 4.62、D2 3.31、D3 3.28。圖在 `docs/screenshots/94-ant-normals/`（`n94_r1_specimen*.png`、`n94_r2_specimen*.png`）。

優先用度量對上這組數字。若要比像素，沿用 `83-specimen-night/compare.py` 的作法，排除左上 FPS 區塊（x 0–199、y 0–59）後再比。

## 8. 仍保留的輔助條件

這些條件沒有隨方案 C 取消：

- 瓢蟲觀感與 shell／gnd 不變。
- hot%≤2%。
- mid 不高於對照 main 的 1.10 倍（沿用 #84 量測裡 G3／G6 的慣例）。
- 正午兩輪數字一致。
- 花色 ΔE 走自己的標準（見 `75-clover/`）。UI 文字對比仍見 ART_DIRECTION §4，不和蟲體共用這份說明。

## 9. 歷史資料夾

| 資料夾 | 內容 |
|---|---|
| `76-specimen/`、`79-chitin/` | #79 開工前的淡灰蟻身，以及 roughness／斑點修正後的 D1／D2。當時目標是 ≥3:1，後來 Artist 以 D2 ≥3.3 退回過中間版。 |
| `81-beetle/` | 甲蟲 D3 腳本與截圖。 |
| `83-specimen-night/` | 夜間構圖、鎖時鐘，以及夜間 D1／D2 參考值。 |
| `94-ant-normals/` | #94／#95 法線與 albedo 基準圖。當時驗收曾用 D1≥4.5、D3≥3.25；那是歷史門檻，不是現行規則。 |
