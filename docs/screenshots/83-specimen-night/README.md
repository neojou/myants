# #83 specimen-night 與暫停鎖時鐘

`shot_lock.py PORT PREFIX SHOTS WAIT_MS`：開場暫停後 resume 時鐘、等 WAIT_MS 毫秒再截圖。同一 shot 用 0 ms 和 4000 ms 各拍一次，若畫面仍讀真實時間就會不同。

| 組 | 0 ms vs 4000 ms 平均差 (/255) | 差 >8 的像素 |
|---|---|---|
| main specimen（修前） | 0.905 | 18,135 |
| PR specimen | 0.008 | 0 |
| PR specimen-night | 0.010 | 0 |

正午 specimen（measure.py）：main 是 D1 4.71/4.70、D2 3.31/3.31；PR 兩次都是 D1=4.71、D2=3.31。

夜間（specimen-night、specimen-night&specimenCritter=beetle）只附圖，ΔE 與對比由 QE 量測，沒有門檻。

## PM 決定後（commit c0e30fc：一般暫停時動畫照常，只有 shot=specimen* 凍結）

重拍（`pr2_*`）：

| 組 | 0 ms vs 4000 ms 平均差 (/255) | 差 >8 的像素 |
|---|---|---|
| specimen | 0.016 | 117 |
| specimen-night | 0.018 | 115 |

差 >8 的像素都落在左上角 x 80–95、y 23–34 的 FPS 數字上，與場景無關。main 上同樣的測法差了 18,135 px，而且分布在整個畫面。第一次拍 night 的 0 ms 圖還沒載完（畫面全藍），已經重拍。

正午 specimen 兩次都是 D1=4.71、D2=3.31，沒有變。夜間參考值是 D1=1.29、D2=1.84，與先前相同。

## QE 退回後（commit e6af104、0ff729a）：排除 FPS 後差異像素 = 0

`compare.py A B` 會印出整張圖的差異像素數，以及排除左上 FPS 區塊（x 0–199、y 0–59）後的差異像素數。任何不為零的差都算進去。

| 對照 | 整張 | 排除 FPS |
|---|---|---|
| pr4 specimen 0 ms vs 4000 ms | 115 | **0** |
| pr4 specimen-night 0 ms vs 4000 ms | 106 | **0** |

整張圖的差全部落在 FPS 數字上。正午 specimen 兩次都是 D1=4.71、D2=3.31；夜間 D1=1.29、D2=1.84。

追查過程：
- e6af104 之後，同一次載入在 resume 後立刻拍一張，4 秒後再拍一張（`shot_same.py`，`pr3sameA_*_t0` vs `_t4000`），排除 FPS 後是 0 px。可見畫面已經不跟實際時間走。
- 但兩次各自載入（`pr3sameA_specimen_t0` vs `pr3sameB_specimen_t0`）仍差 29,613 px，最多 4/255，集中在畫面下半部（含苜蓿）。
- 根因：kool 0.19 的 `AmbientOcclusionPass` 用沒有給種子的 `shuffled()` 產生 4×4 SSAO 旋轉雜訊，每次載入的 AO 顆粒都不同。0ff729a 改用固定種子的 `AoNoise`。
- `pr4_w4_specimen_iconfail.png`：第一次拍的 4000 ms 圖裡，HUD 圖示沒有載到，按鈕顯示成文字 fallback（Aa、S、P…）。這是資產載入失敗，不是時間來源。重拍後就是上表的 pr4_w4_specimen。
