# 相機模式與「化身螞蟻」預留設計

kool 路徑同 `rendering_pipeline_kool.md`（`K/` = kool-core commonMain）。

## 1. 需求

| 模式 | 投影 | 旋轉 | 操作 | 期 |
|---|---|---|---|---|
| **TopDown**（預設） | 正交，或窄 FOV（15–25°）透視 | **鎖定**（固定俯角、固定方位） | 左鍵拖曳平移、滾輪以游標為錨縮放、雙擊/中鍵點選聚焦、`+`/`-` 縮放、`⌖` 重設、鍵盤方向鍵平移（選配） | P0 |
| **Cinema**（Kino-Kamera，閒置 25 秒） | 同 TopDown 投影 | 仍鎖定方位（保持俯視語言），只平移+縮放跟隨 | 任何輸入中斷（沿用 `AppSession.updateCinema` 規則） | P1 |
| **Follow**（Inspector 選中螞蟻時可開「跟隨」） | 同 TopDown | 鎖定 | 相機平滑跟著目標，拖曳即解除 | P1 |
| **AntPov**（化身螞蟻，未來） | 透視 FOV 70–90°，近平面 0.05 unit | **自由**（只有這個模式能旋轉） | 第三人稱：右鍵拖曳環繞、滾輪拉遠近；第一人稱：滑鼠看、WASD 由玩家「建議」方向 | P6 |

俯角建議：正交時 `pitch = -90°`（正上方，最接近 AntSim），可在 Grafik 提供 `Neigung`（傾斜）滑桿 60°–90°，但**只改固定值，不給使用者拖曳旋轉**。傾斜能讓草、石頭、螞蟻的 3D 立體感出來；正上方最像原版。預設建議 **-70°**（推測：在美感與可讀性間最平衡，Mac 實機看過再定）。

## 2. 抽象

```kotlin
interface CameraController {
    val id: CameraMode               // TOP_DOWN, CINEMA, FOLLOW, ANT_POV
    val allowsRotation: Boolean      // 只有 ANT_POV = true
    fun attach(scene: Scene)         // 加入 InputStack listener、設定 scene.camera
    fun detach(scene: Scene)
    fun update(dt: Float, view: SimView) // SimView = sim 快照的唯讀介面
    fun focusOn(worldX: Float, worldZ: Float, zoom: Float? = null, animate: Boolean = true)
    fun screenToGround(px: Float, py: Float): Vec2f?  // 射線打到地面（y = groundHeight）
}
class CameraDirector(scene: Scene) { var active: CameraController; fun switchTo(mode, blendSec = 0.6f) }
```
- `CameraDirector` 負責兩模式間插值（位置、目標點、FOV/正交高度），切到 AntPov 時從正交平滑轉透視：先把正交換成等效窄 FOV 透視，再拉近、放寬 FOV。
- `SimView` 讓相機知道目標螞蟻位置（sim x,y → render X,Z），**相機不寫 sim**。AntPov 的「操作螞蟻」以後透過 `PlayerIntent`（目標方向/拿起/放下）交給 sim 的一個新狀態，不在相機層做。

## 3. TopDown 實作（P0）

方案 A（選定）：自寫 `TopDownCamera`，直接操作 `OrthographicCamera`：
- 狀態：`focus: Vec2f`（世界 XZ）、`zoom: Float`（1–6，同 AntSim；正交高度 = 840/zoom）、固定 `pitch`、`yaw=0`。
- 相機位置 = focus + 方向(pitch)·距離；`lookAt(focus)`；`OrthographicCamera.setCentered(height, near, far)`（`K/scene/Camera.kt:371`）。
- 世界只佔左 74%：把正交的 `left/right` 做非對稱偏移，讓 focus 落在左窗中心；或把右欄直接用不透明 UI 蓋住、focus 偏移 13% 寬。
- 滾輪縮放錨點：縮放前後讓游標下的地面點不動（AntSim `zoomAt`，myants `AppSession.zoomAt` 已有同公式，改成 3D 射線版）。
- 夾限：focus 夾在世界 AABB 內（zoom 小時置中，同 `AppSession.clampCamera`）。
- 平移拖曳門檻：移動 < 4 px 視為點擊（AntSim 規則）。
- 點擊：`camera.computePickRay(ray, ptr, viewport)`（`K/scene/Camera.kt:126`）→ 與地面平面求交 → 世界 (x, z) → `session.clickWorld(x, z)`（Inspect/放糖/放蛋白/敵人照舊）。

方案 B（備選，已編譯）：`OrbitInputTransform`（`K/scene/OrbitInputTransform.kt:45`）設定 `leftDragMethod = PAN`、`rightDragMethod = NONE`、`panMethod = yPlanePan()`（`:383`）、`zoomMethod = ZOOM_TRANSLATE`、`minHorizontalRot = maxHorizontalRot = pitch` 鎖死俯角、`translationBounds` 夾世界。優點是平滑（`smoothingDecay`）現成；缺點是與 AntSim 的 zoom 1–6 語意、點擊門檻、UI 吃事件要再對齊。AntPov 第三人稱可直接用它（打開 ROTATE）。

輸入路由：世界輸入放在 InputStack 中 UI 之下（現有作法：UI 面板 `onPointer` 吃掉自己範圍內事件；世界滑鼠處理在 UI 場景後的空場景）。3D 版沿用。

## 4. 「化身螞蟻」需要現在就預留的東西

| 項目 | 現在做 | 原因 |
|---|---|---|
| 世界單位 = sim px，Y 向上，地面高度函式 `groundHeight(x,z)` 共用 | P0 | POV 相機要貼地 |
| 草葉、石頭是真 3D 幾何（不是貼圖），高度合乎比例（草 30–120 unit、蟻 12 unit） | P2 | 從螞蟻眼睛看出去才有「森林感」 |
| 散布物分「地圖區塊」(chunk 128×128 unit) + `InstancedLodController` 距離 LOD | P2 | POV 時近處要高密度細草、遠處稀疏 |
| 相機周邊加密草（參考 `DEMO/physics/terrain/CamLocalGrass.kt`） | P2 骨架，P6 開 | POV 需要相機附近大量草，俯視不需要 |
| 螞蟻渲染分 LOD0（單隻高精度 skinned glTF，獨立動畫）與 LOD1（實例化姿態桶） | P1 | POV 的主角螞蟻要有自己的動畫與搬運物 |
| 搬運物掛點：螞蟻 model 的「下顎」節點或固定偏移 | P1 | 從 POV 看得到嘴上的糖/蛋白 |
| near plane / 陰影 CSM 參數依模式切換 | P0 介面 | 正交俯視用 SimpleShadowMap，POV 用 CSM |
| 巢穴入口在 3D 地表有真實洞口（凹陷 + 深色），巢穴剖面與地表座標可對應 | P4 | 未來 POV 走進巢穴的轉場 |
| 品質檔位分「俯視」與「POV」兩組草密度 | P5 | POV 需要的草量是俯視的 10× 以上 |

POV 互動草案（P6，不在本次交付）：選一隻工蟻 → 「Ameise werden」按鈕 → 相機 1 秒內俯衝到第三人稱；sim 中該蟻狀態改為 `PLAYER_CONTROLLED`（新狀態，需寫進 deviations），WASD 決定 `angle`，碰到食物自動拾取，沿 HOME 費洛蒙回巢；HUD 顯示「回巢方向」羅盤與搬運量。`Esc` 回俯視。sim 決定性：玩家輸入算外部事件，記錄在 replay 也行，但會破壞黃金對照 → 只在玩家模式中生效。
