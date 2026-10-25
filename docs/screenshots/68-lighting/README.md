# #68 戶外光照對照圖

左：origin/main（1038504），右：feat/68-outdoor-lighting。headless Chrome（SwiftShader WebGL2），`?backend=gl&seed=12345&quality=medium&shot={day,night,ant}`，1600×900 縮成並排。
兩邊截圖時刻不同，相機位置略有差異。headless WebGL 沒有 bloom（kool 的 bloom 只在 Vulkan/WebGPU），所以 bloom 參數無法用這組圖驗。

第二輪（數字 unlit）重拍，量測腳本 `measure.py`（石頭、數字範圍是手動框的；CIEDE2000、WCAG 相對亮度）。數值見 PR #74 說明。
