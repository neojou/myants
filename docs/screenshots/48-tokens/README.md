# #48 / #82 截圖與量測（shot=specimen）

`shot.py PORT TAG specimen [antColor]`：與 `shot_lock.py` 相同，第 4 個參數會在載入前寫入 localStorage `antsim.ui.v1` 的 antColor。量測：`python3 measure.py *.png`（ROI 與 #80 相同）。

| 檔案 | 說明 | D1 | D2 |
|---|---|---|---|
| pr_a_specimen.png | 本 PR，預設設定，第 1 次啟動 | 4.70 | 3.31 |
| pr_b_specimen.png | 本 PR，預設設定，第 2 次獨立啟動 | 4.70 | 3.31 |
| mainblue_specimen.png | main（05def22），antColor=#6fa8dc：glTF 蟻仍是黑色（#82 的錯誤） | 4.70 | 3.31 |
| blue_specimen.png | 本 PR，antColor=#6fa8dc：glTF 蟻變藍 (60,85,105) | 2.70 | 3.31 |

#80 的結果是 D1 4.70、D2 3.31，預設畫面的 ROI 數值完全相同。
