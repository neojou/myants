# #76 固定構圖截圖（`shot=specimen`）

拍法：`python3 docs/screenshots/68-lighting/shot_lock.py PORT TAG specimen`（Chrome + SwiftShader，鎖時鐘 2026-10-26 12:00 +08:00，網址參數 `backend=gl&seed=12345&quality=medium&shot=specimen`，1600×900）。

- `r1_specimen.png`、`r2_specimen.png`：兩次獨立啟動的瀏覽器各拍一張。
- `specimen_crop.png`：中央的工蟻（左）與瓢蟲（右）放大。

## 重現性（r1 對 r2，實測）
- 世界區域像素中位數差 (0, 0, 0)；全圖平均差 0.30/255。
- 差異大於 8/255 的像素佔 0.8%，全部在三葉草邊緣：草的擺動用的是 `Time.gameTime`，模擬暫停時它還在走。
- 瓢蟲紅殼像素數兩次都是 1414，蟲體區域沒有差異。

## 初步量測（r1，粗略的 ROI，最後以 QE 量測為準）
- D2 瓢蟲殼：中位數 (207, 49, 46)，H 1.1°、S 0.78，符合 350–15°、≥0.6。黑點 (55, 44, 42) 對殼的對比 2.66:1，**低於 3:1**。
- D1 工蟻：蟻身渲染成淺灰 (164, 163, 161)，對附近地面 (131, 138, 97) 的對比約 1.44:1，**低於 3:1**。`WorldPalette.worker` 是 #c4703a，畫面上卻是淺灰，原因還沒查。
