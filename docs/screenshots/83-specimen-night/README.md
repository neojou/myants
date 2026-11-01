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
