# kool 0.19.0 渲染/UI 對應設計

kool 原始碼根：`/workspace/kool-0.19.0/kool-core/src/commonMain/kotlin/de/fabmax/kool/`（下稱 `K/`）。標記：**已查證**＝在 0.19.0 原始碼看到 API；**推測**＝用法未實際編譯過。

## 1. 架構
```
koolApp/src/commonMain/kotlin/com/neojou/myants/
  sim/            純 Kotlin，無 kool 相依（可 headless 測）
    Rng.kt  SimConfig.kt  PheromoneGrid.kt  World.kt  Colony.kt  Ant.kt  AntStates.kt
    Queen.kt  Brood.kt  Nest.kt  Terrain.kt  Food.kt  Insect.kt  SpatialGrid.kt  SimObserver.kt
  render/         kool 場景
    WorldView.kt (地表)  NestView.kt (巢穴)  PheromoneOverlay.kt  AntInstances.kt  CameraController.kt
  ui/             kool ui2
    HudPanel.kt  ControlsPanel.kt  GraphicsPanel.kt  ParamsPanel.kt  StatsGraphPanel.kt  InspectorPanel.kt  TunerPanel.kt
  AppUi.kt / AppVersion.kt（保留 About）
koolApp/src/commonTest/kotlin/...  RngTest, PheromoneGridTest, HeadlessTest（golden）
```
- sim 用 **SoA 不強制**：AntSim 是 OOP，為了逐行對照移植與 RNG 呼叫順序一致，**先用 class Ant（同欄位）**；效能目標 300–600 蟻 OOP 足夠。費洛蒙格用 `FloatArray`（已查證可對應 Float32Array 語意：存 float、運算 double→float 轉換要逐步一致）。
- 注意 JS 中 `Float32Array[i] *= decay` 是 double 乘法後 round 到 float32；Kotlin `arr[i] = (arr[i].toDouble() * decay).toFloat()` 才一致（`arr[i] *= decay.toFloat()` 會不同！）。

## 2. 推薦渲染方式（選定）：**正交相機俯視 3D 場景 + 實例化 quad + 動態貼圖**
理由：AntSim 為 2D，但 kool ui2 沒有通用 Canvas 繪圖 API（已查證：ui2 只有 `Box/Image/Text/...`，無 `Canvas`/drawRect 類 API；可用 `UiRenderer`/自訂 `UiNode` 畫 primitive，但大量動態精靈不適合）。用場景圖 + 實例化是 kool 的強項。

| 需求 | kool API | 位置 | 狀態 |
|---|---|---|---|
| 正交相機 | `OrthographicCamera`（left/right/top/bottom、`setCentered`、`isKeepAspectRatio`、`isClipToViewport`）| `K/scene/Camera.kt:359` | 已查證 |
| 平移/縮放 | 自寫：改 ortho 的 left/right/top/bottom（世界座標 = AntSim 像素，y 向下 → 相機翻轉或在 sim→render 時 y 取負）| | 推測 |
| 實例化（蟻、昆蟲、食物）| `MeshInstanceList(layout: Struct, initialSize)` + `addInstances(n){ buf -> }`；`Mesh(VertexLayouts..., instances = list)` | `K/scene/MeshInstanceList.kt:37,77,83`；範例 `kool-demo/.../bees/CpuBees.kt:21,53,122` | 已查證 |
| 實例 layout | 自訂 `object AntInst : Struct("AntInst", MemoryLayout.TightlyPacked){ val modelMat = mat4("instattr_model_mat"); val color = float4("attrib_instance_color")}`；或 `InstanceLayouts.ModelMat` | `K/scene/InstanceLayouts.kt:6-11` | 已查證（自訂 color 屬性名需和 shader 對應：推測用 `KslUnlitShader { color { instanceColor() } }`，需驗證）|
| 著色器 | `KslUnlitShader`（2D 不需光照）| `K/modules/ksl/` | 已查證存在，instanced 設定推測 |
| 動態貼圖（費洛蒙、地表）| `Texture2d(format, mipMapping, samplerSettings)` + `uploadLazy(BufferedImageData2d(Uint8Buffer, w, h, TexFormat.RGBA))` 每 N 幀 | `K/pipeline/Texture.kt:57,117`、`K/pipeline/ImageData.kt:141`、`K/util/Buffer.kt:5` | 已查證 API；每幀重傳的效能推測 OK（420×252×4 ≈ 420KB）|
| 貼圖取樣 | `MipMapping.Off` + `SamplerSettings().clamped()`（線性濾波讓費洛蒙平滑，近似原版 blur 1.5px）| `K/pipeline/` | 推測 |
| 透明/加法混合 | pipeline `blendMode`（BlendMode.BLEND_ADDITIVE 推測名）| | 推測，需查 |
| 文字（時鐘石）| ui2 `Text` 疊加，或 `TextMesh`/`MsdfFont` 世界空間 | `K/util/MsdfFont.kt` | 推測 |
| 指標 | `PointerInput.pointerState.primaryPointer`：`isLeftButtonDown/Pressed/Released`、`isDrag`、`dragMovement`、`deltaScroll`、`pos` | `K/input/PointerInput.kt:13`、`K/input/Pointer.kt:30-92`、`K/input/PointerState.kt:37` | 已查證 |
| 指標與 UI 衝突 | 使用 `InputStack`（ui2 會消耗事件）；世界輸入放 InputStack 較低層 | `K/input/InputStack.kt:114-146` | 已查證存在，用法推測 |
| 鍵盤 | `KeyboardInput.addKeyListener(keyCode, name, filter){}`，Space/+/- | `K/input/KeyboardInput.kt:17` | 已查證存在 |
| 多場景 | `ctx.scenes += scene`；world 場景、nest 場景（各自 viewport）、UI 場景 | `K/KoolContext.kt:55`；現有 `HelloCubeScene.kt` | 已查證 |
| 分割視窗 74/26 | 兩個 Scene 各設 `mainRenderPass.viewport`（或單一場景兩個 OrthographicCamera）| | 推測，需查 viewport API |

### 2.1 圖層（由下而上，對照 PixiBackdrop）
1. 地表底圖：啟動時用 sim 無關的 `kotlin.random.Random` 程序生成土壤＋草＋小石到一張 Texture2d（render-only randomness，與原版相同原則）；一個 quad。
2. 費洛蒙疊圖：`PheromoneOverlay` 每 3 幀把 4 場轉 RGBA（同 §UI 公式 √），上傳；alpha = 0.85×強度，加法混合。
3. 食物/障礙物/時鐘石：實例化圓 quad（圓形 alpha 由 shader 或貼圖）。
4. 螞蟻：一個 ant 精靈貼圖（程序繪製 32×32 RGBA）+ 實例 model matrix（位置、角度、sizeVar 縮放）+ instance color（蟻群 tint、兵蟻）；腳動畫 P2（可用 2–3 幀 atlas）。
5. 昆蟲、粒子、搬運圖示：同上各一組 instance list。
6. 日夜色調：全螢幕 quad 乘色（P2）。
- 巢穴視圖：同手法，獨立相機、不受世界相機影響。

### 2.2 替代方案（未選）
- ui2 自訂 UiNode 畫 primitive：適合 HUD/圖表，不適合上千精靈。
- 真 3D（透視、蟻模型）：破壞一致性，只做為 **可選 flourish**，預設關閉（例如 Grafik 面板加 "3D Tilt" toggle，把正交相機換成略傾斜的透視相機；sim 不變）。

## 3. UI（ui2）對應
| AntSim | kool ui2 | 位置 |
|---|---|---|
| 按鈕 | `Button("text"){ modifier.onClick{} }` | `modules/ui2/Button.kt` |
| 勾選 | `Checkbox(state)` / `Switch` | `modules/ui2/Toggle.kt:111,141` |
| 滑桿 | `Slider(value, min, max)` + `onChange` | `modules/ui2/Slider.kt:53` |
| 下拉選單（品質）| `ComboBox` | `modules/ui2/ComboBox.kt:82` |
| 顏色選擇（蟻色）| `ColorChooserH`/`ColorWheel` | `modules/ui2/` |
| 浮動面板 | `Panel` / `Window` + `TitleBar` + `CloseButton` | `modules/ui2/` |
| 長清單（23 滑桿）| `ScrollArea` + `Column` | |
| 折線圖 280×130 | 方案 A：`Image` 顯示一張每秒重畫的 Texture2d（CPU 畫線到 RGBA buffer）；方案 B：自訂 `UiNode` 用 `getUiPrimitives()`/`localRect` 畫線段（推測）。建議 A | |
| 狀態列 | `Text` 11px `#aaa` 背景 `rgba(0,0,0,.4)` | |
- 狀態管理：`mutableStateOf` / `remember`（ui2 標準）。sim 每 tick 不觸發 UI 重組；HUD 每 250ms 更新 state。
- 字型：維持 AntSim 原文英/德文，kool 預設字型（Latin）。若之後要中文，需另載 CJK MSDF 字型（見 kool_study 文件）。

## 4. 效能目標
- 預設：雙群、蟻口上限 300（MEDIUM），macOS M 系列 60 fps，js（Chrome）≥ 45 fps @1600×900。
- 壓力：1200 蟻（ULTRA cap）desktop ≥ 30 fps。
- 每幀 alloc 0（重用 instance buffer、Uint8Buffer）。js 端 `FloatArray` 對應 Float32Array（Kotlin/JS 已查證行為：FloatArray→Float32Array），double 運算一致。

## 5. 決定性
- sim 不得使用 `kotlin.random.Random`、`System.nanoTime` 等；只用 `Rng`。
- 0.5× 速度原版用 Math.random → myants 改成累加器（每 2 幀一 tick），文件標註偏離。
