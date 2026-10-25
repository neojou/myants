# #68 戶外光照對照圖

左：origin/main（1038504），右：feat/68-outdoor-lighting。headless Chrome（SwiftShader WebGL2），`?backend=gl&seed=12345&quality=medium&shot={day,night,ant}`，1600×900 縮成並排。
兩邊截圖時刻不同，相機位置略有差異。headless WebGL 沒有 bloom（kool 的 bloom 只在 Vulkan/WebGPU），所以 bloom 參數無法用這組圖驗。

第二輪（數字 unlit）重拍，量測腳本 `measure.py`（石頭、數字範圍是手動框的；CIEDE2000、WCAG 相對亮度）。數值見 PR #74 說明。

第三輪（冷色夜間環境光）：`shot_lock.py` 用 Playwright 假時鐘鎖在 2026-10-26 12:00（UTC+8），模擬 10 秒後截圖，main 與 PR 的時刻與相機一致；先開一頁暖機，避免第一頁空白。量測 `measure.py`。ant 視角這輪沒拍（D 項移到 #76）。
