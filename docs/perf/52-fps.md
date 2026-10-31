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
- 第二輪加 `lockQuality=1`（見下），**本次量測全程維持 Medium**：console 有 `[PERF] Quality locked: auto-downgrade is off for this run.`，`monitorFps` 不會降級。
- 每輪跑兩次。量測用的 http.server（port 8831）用完已關。

## 結果（僅供參考：無 GPU 軟體算圖，不作 60 fps 判定）

鎖定畫質（`lockQuality=1`，全程 Medium）— 以這組為準：

| 平台 | 次 | 取樣秒數 | 格數 | 平均 fps | 1% low | 中位格時間 | 最長格時間 |
|---|---|---|---|---|---|---|---|
| Web（Chrome 156 headless, SwiftShader） | 1 | 59.8 | 96 | 1.61 | 1.30 | 617 ms | 767 ms |
| Web（Chrome 156 headless, SwiftShader） | 2 | 59.8 | 100 | 1.67 | 1.36 | 600 ms | 733 ms |
| Desktop（JVM） | — | — | — | box 量不到 | — | — | — |

第一輪（未鎖定，可能中途被自動降到 Low）：1.68／1.70 fps，1% low 1.43／1.33。兩輪差異在雜訊內。
樣本不到 100 格，1% low 等於最慢的 1 格。

## Desktop 為什麼量不到

kool 0.19 的 `GlfwWindowSubsystem.onEarlyInit` 在 Linux 只要 GLFW 支援 Wayland 就強制 `GLFW_PLATFORM_WAYLAND`；box 沒有 Wayland compositor（apt 也找不到 weston），在 Xvfb 下 `glfwInit` 失敗（`Wayland: Failed to connect to display`）。所以桌面版要在 Neo 本機（macOS，有 GPU）量，步驟見下節。

## Neo 本機重現步驟（macOS，有 GPU）

`FrameLog`（`desktopMain/FrameLog.kt`）是量測工具，保留在程式裡、預設關閉：只有設了 `MYANTS_FRAME_LOG` 才會每格寫一行 `System.nanoTime()`。`MYANTS_LOCK_QUALITY=1`（web 用 `lockQuality=1`）停用自動降畫質，讓整段維持 Medium。量測前先關掉其他吃 GPU 的程式，視窗保持在前景、不要縮小。

### 0. 前置需求

- **JDK 25**，`JAVA_HOME` 要指向它（沒設時 gradlew 會停在「Please set the JAVA_HOME variable…」）：

  ```sh
  /usr/libexec/java_home -V          # 列出已裝的 JDK
  brew install openjdk@25            # 沒有 25 才需要；或從 https://adoptium.net 下載 Temurin 25 安裝
  # brew 版要讓 java_home 找得到：
  sudo ln -sfn "$(brew --prefix openjdk@25)/libexec/openjdk.jdk" /Library/Java/JavaVirtualMachines/openjdk-25.jdk
  export JAVA_HOME=$(/usr/libexec/java_home -v 25)
  java -version                      # 應該顯示 25
  ```

  以下每個新開的終端機都要先 `export JAVA_HOME=$(/usr/libexec/java_home -v 25)`。
- **Python 3** 與 **Google Chrome**（web 量測用）。

### 1. 取得程式

```sh
git clone https://github.com/neojou/myants.git && cd myants
git checkout feat/52-gltf-fallback-fps   # 合併後改用 main
```

### 2. Desktop（JVM）

```sh
rm -f /tmp/myants-frames.txt
MYANTS_FRAME_LOG=/tmp/myants-frames.txt MYANTS_LOCK_QUALITY=1 \
  ./gradlew :koolApp:runDesktop --args="quality=medium seed=12345"
# 視窗出現後不要操作，預設相機、自動運鏡開著，等 80 秒以上再關視窗
python3 docs/perf/frames_to_fps.py /tmp/myants-frames.txt 10 60
```

注意：`~/.myants/antsim.ui.v1.txt` 存有之前的設定（例如關掉自動運鏡）。要用預設值，先把它改名備份，量完再改回來。

### 3. Web（本機 Chrome，用 GPU）

```sh
./gradlew :koolApp:jsBrowserDistribution
(cd koolApp/build/dist/js/productionExecutable && python3 -m http.server 8831) &
python3 -m venv .venv && source .venv/bin/activate   # 用 venv，避開 PEP 668 的 externally-managed-environment
pip install playwright && python -m playwright install chromium
python docs/perf/fps_web.py 8831 gpu_chrome "&lockQuality=1" \
  "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
kill %1      # 關掉臨時 server
deactivate   # 離開 venv
```

腳本會用全新的瀏覽器設定開 `?backend=gl&seed=12345&quality=medium&lockQuality=1`（1600×900），暖機 10 秒、取 60 秒，印出平均 fps、1% low、UA 與 GPU 名稱（`info.gl`），逐格資料存在 `gpu_chrome.json`。tag 裡沒有 `swiftshader` 時不加 SwiftShader 旗標，會用真正的 GPU。想看 WebGPU，就把第三個參數裡的 `backend` 改掉，或在網址拿掉 `backend=gl`。

回報時請附：機型、macOS 版本、GPU、Chrome 版本，以及兩邊的平均 fps 與 1% low。

## 結論

Web 在 SwiftShader 下約 1.7 fps，遠低於 60，但瓶頸主要是沒有 GPU 的軟體算圖，這台機器測不出真實瓶頸。要等 Neo 在有 GPU 的 macOS 上量完 desktop 與 web，才判斷是否低於 60、要不要另開 issue。
