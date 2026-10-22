# Changelog

本檔記錄 myants 的重要變更。格式依 [Keep a Changelog](https://keepachangelog.com/zh-TW/1.1.0/)，版號採 [SemVer](https://semver.org/lang/zh-TW/) 0.x（見 `docs/adr/0003-versioning-and-release.md`）。

## [Unreleased]

### 新增
- Inspect 點到世界螞蟻、食物、昆蟲、巢內螞蟻或蟻后後，HUD 顯示類型與狀態，約每模擬秒重讀。物件離開原本的清單就清空。不寫回模擬。英文一行仍留給測試。地形與 3D 高亮尚未做（Refs #12）
- 巢穴裡沒被搬走的卵、幼蟲、蛹可以點選，命中半徑小於螞蟻。沒點到螞蟻、蟻后或幼體時，點在房室半徑內會顯示用途與半徑。幼蟲多一列飢餓，蛹多一列預定階級。圖集補上蛹，以及後面地形與跟隨會用的石、葉、危險、跟隨、追従（Refs #12）
- 介面語言加上日本語。工具列第一列最右是地球圖示（Material Symbols Rounded `language`，缺檔退回 `Aa`）。選單為 English、繁體中文、日本語、Deutsch，預設 English。寫入既有的 `language=`。Grafik 不再另有一排語言按鈕。切換不重建世界

### 變更
- 左側世界的食物與昆蟲在 Inspect 時也畫選取圈，圈比物件輪廓再大一點。世界螞蟻的圈大小不變。巢內螞蟻與蟻后這次不畫圈（Refs #12）
- 工具列擋住指標時，點擊不再送進巢穴檢查。暫停、繼續與速度加減不會清掉 Inspect 選取（Refs #12）
- 下雨從世界列搬到工具列，放在骷髏頭右邊，中間用 `gapGroup`。它仍是天氣開關
- 敵群核取方塊、Params 滑桿名稱、品質選項、分析建議與 Inspect 文字跟所選語言走。分析套用按鈕仍是 `path` 與數字。About 的 MIT 授權行維持英文。`3D` 與 `SSAO` 不翻譯
- 繁體中文與日本語使用 Noto Sans CJK 的 MSDF 圖集（只含介面用字，OFL）。英文、德文與 HUD 數字欄位仍用 Fira。語言選單的自稱在英文介面也用這份圖集。Stats 點陣字仍沒有中日文，線尾在繁中與日文時畫英文短名

## [0.16.0] - 2026-10-22

涵蓋 M3（UI/UX 與架構整理）。tag `v0.16.0` 已在 origin。

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
- 重構：工具列改訂閱暫停、速度、工具、下雨、群落數；HUD 的 inspect 在表面訂閱。About 不再訂閱全域 revision。側邊面板仍共用 revision，Grafik／Params／Stats／Analyse 才會跟著刷新（Refs #9）
- 變更：HUD 的 FPS、族群、食物、蟻后字串約每 0.25 秒寫一次；暫停單步立即寫。`revision.use()` 從 4 處降到 1 處（只剩側邊面板）。暫停、速度、下雨、切工具不再增加 `revision`，所以只改速度時 Stats 與 About 的重組觸發次數不變；改費洛蒙仍會加 1。`HudState.syncPublishes` 在 0.25 秒內多次 sync 只加 1。沒有量 fps（Closes #9）
- 重構：場景材質色收進 `render3d/WorldPalette`。2D 與 3D 原本不同的顏色各自保留，數值不變。蟻色預設改引用同一處。UI 色仍在 `MyantsTheme`（Refs #18）
- 變更：HUD、工具列、About、側邊面板改用 `radius.lg` 圓角與 `outline` 邊。Button／ComboBox 的圓角改跟選取外框一樣用 `radius.md`（6dp）。對比 6.63:1 是次文字疊在 surfaceRaised 上，7.44:1 是同一段文字疊在 surface 且 α 用 0.86；兩者都超過 4.5，色值不變（Refs #18）
- 變更：暫停圖示改為實心 `pause_fill.svg`（Material Symbols Rounded FILL 1，路徑填 `#FFFFFF`）。`pause.svg` 仍留在 assets。Grafik 蟻色改成色塊，名稱在色塊下方，tooltip 含名稱與色碼；色值不變。敵群寫出色號與名稱，不只靠顏色。德文是「Rivalenkolonie 2 · Gold #d9bf52」（Closes #18）
- 變更：圖示按鈕按下時內容縮到 0.96，放開回到 1，不彈跳。點擊範圍維持 28dp，版面不位移。停用的按鈕不縮（Refs #17）
- 變更：HUD 的 FPS、族群、食物、蟻后數字放在固定寬度欄位並靠右。字型仍是 Fira。位數變多時標籤不左右移動（Closes #17）
- 變更：Stats 四條線顏色不變，改為實線、虛線、點線、點劃線。標籤太近時會錯開（Closes #19）
- 新增：介面語言 English／繁體中文／Deutsch，預設 English。工具列、面板標題、OK、Reset 走字串表。Grafik 可切換，寫入設定檔 `language`。舊檔沒有這個鍵時維持 English。模擬句子、Rivalenkolonie 核取方塊、MIT 授權行不翻譯（Refs #24）
- 變更：Grafik 其餘標籤、HUD、About 的 Version、圖示 tooltip、蟻色名稱與敵群行改走字串表。Stats 圖下標籤跟著語言；英文糖線是 Sugar，德文仍是 Zucker。圖上的點陣字沒有中文，繁中時線尾改畫英文。Fira 仍沒有中文字（Closes #24）
- 變更：右上工具列分成播放、相機、工具、世界（雨／Restart）、面板五組。組與組用 `gapGroup`，組內用 `gapInGroup`。About 改走 `openPanel(Panel.ABOUT)`，會暫停電影相機（Refs #11）
- 變更：Restart 按鈕用 danger 色。按下先問，確認才重建世界；取消則世界不變。敵群勾選仍會立刻重開（Refs #11）
- 新增：快捷鍵空白鍵暫停、`.` 單步（僅暫停）、`+`／`-` 改速度、`1`–`4` 選工具，並寫進 tooltip。`=` 放大、`_` 縮小。`[`、`]` 原作沒有，不加。3D 與 Classic 2D 共用同一組（Closes #11）

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
