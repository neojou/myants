# #52 Medium 300 蟻 fps 量測

量測日期：2026-10-30（Asia/Taipei）。commit：分支 `feat/52-gltf-fallback-fps`（基於 origin/main 5dceff1）。

## 環境（限制先講清楚）

- 機器：雲端 box，8 vCPU Intel Xeon、15 GB RAM，**沒有 GPU**。
- 瀏覽器：Google Chrome 156.0.8078.47，headless（UA 為 HeadlessChrome/156），WebGL 走 **SwiftShader 軟體算圖**：
  `ANGLE (Google, Vulkan 1.3.0 (SwiftShader Device (Subzero)), SwiftShader driver)`。
  旗標 `--use-angle=swiftshader --enable-unsafe-swiftshader --ignore-gpu-blocklist --disable-webgpu`，網址加 `?backend=gl`（SwiftShader 沒有 WebGPU）。
- 這個數字只代表「CPU 軟體算圖」的下限，**不能當成真實機器（有 GPU）的 fps**。

## 方法

- 設定：`?backend=gl&seed=12345&quality=medium`，視窗 1600×900，預設相機、沒有 shot 模式、自動運鏡為預設值（開），全新瀏覽器設定（localStorage 為空）。Medium = maxAnts 300。
- 用 `requestAnimationFrame` 記每一格的時間戳（`docs/perf/fps_web.py`）。載入後暖機 10 秒，清空紀錄，再取 60 秒。
- 平均 fps = 格數 ÷ 取樣時間；1% low = 最慢 1% 格的平均格時間換算成 fps（樣本不到 100 格時取最慢 1 格）。
- 跑兩次。量測用的 http.server（port 8831）用完已關。

## 結果

| 平台 | 次 | 取樣秒數 | 格數 | 平均 fps | 1% low | 中位格時間 | 最長格時間 |
|---|---|---|---|---|---|---|---|
| Web（Chrome 156 headless, SwiftShader） | 1 | 59.1 | 99 | 1.68 | 1.43 | 583 ms | 700 ms |
| Web（Chrome 156 headless, SwiftShader） | 2 | 59.6 | 101 | 1.70 | 1.33 | 600 ms | 750 ms |
| Desktop（JVM） | — | — | — | 量不到 | 量不到 | — | — |

前後半段平均：第 1 次 1.66／1.69，第 2 次 1.79／1.61，沒有明顯的階梯變化。

注意：`PerformanceManager.monitorFps` 在 fps < 20 連續超過 60 格時會自動降一級畫質。以 1.7 fps 來說，60 格約 35 秒，可能在取樣期間從 Medium 降到 Low；console 沒有對應的 log 可以確認，所以上表不保證整段都是 Medium。

## Desktop 為什麼量不到

kool 0.19 的 `GlfwWindowSubsystem.onEarlyInit` 在 Linux 只要 GLFW 支援 Wayland 就強制 `GLFW_PLATFORM_WAYLAND`；box 沒有 Wayland compositor（apt 也找不到 weston），在 Xvfb 下 `glfwInit` 失敗（`Wayland: Failed to connect to display`）。所以桌面版要在有螢幕的機器上量：

```
MYANTS_FRAME_LOG=/tmp/frames.txt ./gradlew :koolApp:runDesktop --args="quality=medium seed=12345"
```

跑超過 70 秒後關掉，丟掉前 10 秒，取接著的 60 秒算平均 fps 與 1% low（每行是一格的 `System.nanoTime()`）。

## 結論

Web 在 SwiftShader 下約 1.7 fps，遠低於 60，但瓶頸主要是沒有 GPU 的軟體算圖，這台機器測不出真實瓶頸。需要在有 GPU 的機器（例如 Neo 的 macOS）量 desktop 與 web 才能判斷是否真的低於 60。
