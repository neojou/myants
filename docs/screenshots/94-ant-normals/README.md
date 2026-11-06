# #94 蟻殼法線＋albedo（實測值(headless)）

基準：main `c410196`（錯誤法線、`antChitin` 0.07）。PR：正確朝外法線 + `antChitin` (0.036, 0.036, 0.039) 計算值＝實測採用值（未再微調）。

Playwright + Chrome headless（swiftshader）、1600×900、seed=12345、quality=medium、`83-specimen-night/shot_lock.py`。

| 指標 | 門檻 | main 基準 | PR r1 | PR r2 |
|---|---|---|---|---|
| D1 蟻/地 | ≥4.5 | 4.71 | **4.62** | **4.62** |
| D2 瓢蟲殼/黑點 | ≥3.3 | 3.31 | **3.31** | **3.31** |
| D3 甲蟲身體/地 | ≥3.25 | 3.28 | **3.28** | **3.28** |

附圖：`main_specimen*.png`、`n94_r{1,2}_specimen*.png`、蟻 3× crop 與前後對照。
