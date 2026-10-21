# Changelog

本檔記錄 myants 的重要變更。格式依 [Keep a Changelog](https://keepachangelog.com/zh-TW/1.1.0/)，版號採 [SemVer](https://semver.org/lang/zh-TW/) 0.x（見 `docs/adr/0003-versioning-and-release.md`）。

## [Unreleased]

### 新增
- 文件：Grok Build 單人開發方式，寫入 `handoff.md` 第 0 節與 `docs/AI/How-To-Grok-build-one-AI-developing-approach.md`。session 只在本機 commit，不 push
- Dependabot 每週檢查 github-actions 與 gradle。ignore 同時包含 `gradle` 與 `gradle-wrapper`，並排除 Kotlin（`org.jetbrains.kotlin*`）與 kool（`de.fabmax.kool*`）。不升級 Gradle（仍 9.3.0）（#22）

### 變更
- 編譯加上 `-Xexpect-actual-classes`，CI 的 Gradle 指令加上 `--warning-mode all`（#22）。Kotlin Gradle Plugin 自帶的 `url.parse()`／`punycode` 與 archives configuration 棄用警告不靠升級消除，只記錄
- 重構：`AppSession` 的時序（暫停、速度、60 Hz 累加器）拆到 `session/SimClock`，相機拆到 `session/CameraState`；行為不變（#7 第 1 步）
- 重構：工具、放置與 Inspect 判定拆到 `session/ToolController`；行為不變（#7 第 2 步）
- 重構：面板與偏好設定（含設定檔格式）拆到 `session/ViewState`，HUD 字串與圖表歷史拆到 `session/HudState`，電影鏡頭與跟隨拆到 `session/CinemaDirector`，另拆出 `ShotPose`、`TunerOverrides`、`FpsCounter`；`AppSession` 降到 296 行，行為不變（#7 第 3 步，Closes #7）
- 重構：`AntSimUi.kt` 拆成 `ui/HudPanel`、`Toolbar`、`AboutDialog` 與 `ui/panels/*`（共用 `PanelFrame`）；面板改用 `Panel` enum，移除 `openPanel("...")` 字串；行為不變（#37，Closes #8）
- 重構：指標輸入改由 `input/PointerRouter` 單一入口處理（左側世界 2D/3D 座標換算與工具分派、右側巢穴 Inspect），`CameraState` 新增 `worldToScreen`；行為不變，附單元測試（#10）
- 重構：Classic 2D 場景與巢穴 overlay 從 `AntSimScene.kt` 搬到 `classic2d/ClassicScene.kt`。指標輸入留在 `addAntSimInput`。行為不變（#36）
- 重構：2D shader、費洛蒙貼圖與圓形貼圖抽到 `classic2d/ClassicShaders.kt`。行為不變（Closes #36）

### 修正
- 移除 `World.kt` 中被 Kotlin 內建成員遮蔽、實際不會被呼叫的 `Double.inc`／`Double.dec` 擴充函式，消除編譯警告；模擬行為不變（#31）
- 測試：3D 拖曳經 `sessionTarget` 會呼叫 `noteDrag`（清掉 `userFollow`）；2D 拖曳不會（#39）

## [0.15.0] - 2026-10-21

涵蓋 M1（試跑流程）與 M2（HUD 圖示）。

### 新增
- CI 工作流程：JDK 25、Gradle 快取、產出 JS 成品（#2）
- M1–M5 roadmap 與角色流程：`docs/PLAN.md`、ADR 0001（#14）
- `Theme.kt` 設計 token，並收斂 `AntSimUi` 的樣式（#16，Closes #4）
- ADR 0002：HUD 圖示實作方式（#21）
- HUD Speed／Zoom／Tool 三列改為 Material Symbols 圖示按鈕，附 EN/DE tooltip（#23，Closes #6）

### 變更
- GitHub Actions 升級到 Node 24 版本（#20）
