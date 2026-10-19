# 任務：myants 改用 kool 引擎，跑出 Hello World（plan mode）

先讀這三份文件，再提出計畫。這一輪只出計畫，不改程式。

- `docs/programming/kool_setup_ref.md`：版本、Gradle 設定、macOS 注意事項
- `docs/programming/kool_hello_world_ref.md`：Hello World 程式、import、jvm/js launcher
- `docs/programming/kool_study_migration_plan_ref.md`：檔案層級變更、要新增的程式碼、驗收條件、風險

文件裡標「已驗證」的設定已經在 scratch 複本 build 並執行過，請直接採用，不要另外發明版本組合。

## 目標

`./gradlew :koolApp:runDesktop` 在 macOS（Apple Silicon）開出 kool 視窗，畫出 kool README 的 Hello World：會自轉的彩色立方體加一盞方向光，可以用滑鼠繞著看。JS 版也要能 build。

## 已定案的決策

1. 整個模組改成純 kool app：Compose 與 wasmJs 全部拿掉。原因是 kool 自己管視窗、macOS 上 AWT 會被設成 headless，而且 kool 0.19.0 沒有 wasm 版。
2. `composeApp` 用 `git mv` 改名為 `koolApp`；`wasmJsMain` 改成 `jsMain`。
3. 版本：kool `de.fabmax.kool:kool-core:0.19.0`（只要 mavenCentral）。Kotlin 2.3.0、Gradle 9.3.0、`jvmToolchain(25)` 都不變。
4. desktop 用 `jvm("desktop") { binaries { executable { ... } } }`，macOS 加 `-XstartOnFirstThread`。js 用 `browser`，`outputFileName = "koolApp.js"`，`index.html` 放 `<canvas id="glCanvas">`。
5. README 的場景放在 commonMain 的 `KoolApplication.addHelloCubeScene()`。desktop 與 js 各寫一個很薄的 `main`。
6. 保留 About 與版號：用 kool ui2 在左上角放「About」按鈕，點了打開兩行面板（`AppVersion.APP_NAME` / `Version ${AppVersion.NAME}`）。視窗標題用 `APP_NAME NAME`。刪除 AWT `setAboutHandler`。
7. `AppVersion.kt` 的兩個 configure 標記維持各正好一處。`configure.sh` 的 `SOURCE_ROOTS` 與 `INDEX_HTML` 改成 koolApp 的路徑。

## 計畫要包含

- 逐檔變更清單：新增、修改、刪除、改名。依照 migration plan §4 與 §5。
- 更新 `docs/programming/How-To-Change_Project_Name.md` 與 `How-To-Change_Version_number.md` 的路徑與 macOS 說明。
- 最後執行 `./configure.sh version 0.2.0`。
- 驗證步驟：
  - `./gradlew :koolApp:compileKotlinDesktop :koolApp:compileKotlinJs`
  - `./gradlew :koolApp:runDesktop`（請 Neo 目視確認立方體會轉、About 能開也能關）
  - `./gradlew :koolApp:jsBrowserDistribution`，再用 `python3 -m http.server` 開 `koolApp/build/dist/js/productionExecutable/`
  - 跑 `./configure.sh version` 與 `./configure.sh proj_name` 確認沒壞
- 風險處理：如果 macOS 上 Vulkan 失敗後當掉，在 `KoolConfigJvm` 加 `renderBackend = RenderBackendGl`。

## 限制

- 不要用 kool main（0.20.0-SNAPSHOT），也不要加 snapshot repository。
- 不要自己列 lwjgl natives，kool-core 已經帶齊，包括 macos-arm64。
- 不要保留任何 Compose 相依。
- 不要 push，也不要改 git 歷史。commit 前先給 Neo 看 diff。
- 回覆用繁體中文。
