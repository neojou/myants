# myants 3D 視覺重製總覽

適用：neojou/myants **v0.7.0**（commit `1adea36`，2026-10-19 19:31 UTC+8）。kool **0.19.0**、Kotlin 2.3.0、Gradle 9.3.0、`jvmToolchain(25)`，desktop（JVM）+ js。
標記：**已查證**＝在 `/workspace/kool-0.19.0`（tag v0.19.0，commit `a8f0788`）原始碼看到，或在 scratch 複本編譯過；**推測**＝沒有實際跑過，要在 Mac 上確認。

本目錄的文件：`*.md` 放到 repo `docs/programming/`；`references/`、`feasibility_trial/`、`tools/` 放到 `docs/programming/3d_refs/` 底下（prompt 用這個路徑）。

| 檔案 | 內容 |
|---|---|
| `3d_visual_overhaul_overview.md` | 本檔：目標、差距、架構、分期與退路 |
| `rendering_pipeline_kool.md` | kool 0.19.0 渲染功能查證表、deferred/forward 分工、陰影、IBL、天空、AO、bloom、macOS 後端 |
| `camera_modes_and_ant_pov.md` | 相機控制器抽象、俯視鎖定旋轉、聚焦、未來「化身螞蟻」 |
| `world_scene_composition.md` | sim→3D 對應、尺度、地形、散布物、時鐘石、食物、費洛蒙、巢穴、天氣 |
| `assets_gltf_pipeline.md` | 素材來源與授權（已查證網址）、轉檔、程序生成退路、骨架動畫與實例化 |
| `ui_hud_aesthetics.md` | HUD 版面、色票（含淡紫 #6750A4/#EADDFF）、圖示、CJK 字型 |
| `perf_and_quality_presets.md` | 效能預算、Grafik 品質檔位、量測方法 |
| `grok_build_plan_prompt.md` | Grok Build plan-mode 精簡 prompt |
| `feasibility_trial/` | 對 kool 0.19.0 編譯通過的試作碼（desktop + js 都編過） |
| `references/` | 參考截圖 |
| `tools/hdr_to_rgbe_png.py` | Poly Haven `.hdr` → kool `*.rgbe.png` 轉檔（已實測） |

舊文件的關係：`kool_rendering_mapping_ref.md`（2D 正交 + quad）與 `antsim_ui_ux_ref.md` 的「不加中文 UI」規則，被本套文件**取代**的只有渲染方式與字型兩點；模擬、操作、文字內容仍以 `antsim_*_ref.md` 與 `myants_antsim_deviations.md` 為準。

## 1. 現況與差距（看兩張截圖）

| 項目 | AntSim_V2（參考） | myants v0.7.0 | 3D 版目標 |
|---|---|---|---|
| 地面 | 深褐土壤、細碎紋理、小石、落葉 | 平面深綠 `GROUND` 色 | PBR 土壤貼圖（Poly Haven CC0）+ 高度/法線 + splat 混合 + 濕潤度 |
| 草 | 細長草叢、蒲草穗 | 綠色橢圓 | 實例化 3D 草葉（`world.grass` 的 200 叢位置 + 渲染專用細草）、風擺 |
| 石頭 | 灰色坑洞石、苔蘚 | 灰色圓 | glTF 石頭（實例化、隨機旋轉/縮放）+ 程序退路 |
| 花 | 小粉/黃花 | 無 | 實例化小花（Poly Haven 花模型或程序） |
| 食物 | 發光金色球 | 黃色橢圓 | 發光（emissive）糖滴/蛋白塊 + bloom + 小點光 |
| 費洛蒙 | 黃/紅/紫/藍發光帶，加法混合 | 未見明顯 | 地面貼花（decal）+ emissive 加法層 + bloom，顏色公式沿用 |
| 時鐘石 | 3D 感石頭 + 凹槽 + 米色數字 19:29 | 七段（截圖未見） | glTF/程序石頭 + 內嵌 emissive 七段或 MSDF 數字 |
| 巢穴 | 右側地下剖面、根、房間、入口隧道 | 兩個圓 | 3D 剖面（OffscreenPass2d 渲染到右欄 Image），土壤切面、樹根、房間凹洞 |
| HUD | 左上半透明黑小字；右上精簡圖示列；右下 Restart/Rivalenkolonie/Stats/Params/Analyse | 左上一大塊按鈕 | 還原 AntSim 版位；圖示按鈕 + tooltip；淡紫只用在選取/焦點/滑桿 |
| 雨 | 雨區 | 藍色色塊 | 雨絲粒子（實例化）+ 地面濕潤（粗糙度降、顏色變深）+ 漣漪 |
| 日夜 | 色調 + 陰影方向 | 黑色半透明四邊形 | 太陽方向、色溫、`SkyCubeIblSystem` 動態 IBL、夜間螢光物更明顯 |

## 2. 架構原則（硬性）

1. **sim 不動**：`com.neojou.myants.sim` 仍是純 Kotlin、2D、60 Hz 固定 tick、只用 `Rng`。黃金對照（`HeadlessInvariantsTest`）必須照舊通過。渲染不得呼叫 `Rng`；渲染用的隨機只用 `kotlin.random.Random(固定種子)`。
2. **sim → render 單向**：每幀從 sim 讀快照（位置、角度、狀態），寫進實例緩衝區。sim 的 (x, y) 映射到 3D 的 (X, Z)，Y 向上，高度由地形高度函式 `groundHeight(x, z)` 提供（渲染專用，不回饋 sim）。
3. **render 模組分層**（新套件 `com.neojou.myants.render3d`）：
   ```
   render3d/
     WorldRenderer.kt        // 建 DeferredPipeline、燈光、陰影、IBL，持有各 layer
     camera/CameraController.kt  TopDownCamera.kt  AntPovCamera.kt(預留)  CinemaCamera.kt
     env/SkyAndSun.kt  Weather.kt(雨、雲影、濕潤)  DayNight.kt
     terrain/Ground.kt  Scatter.kt(石/草/花/落葉)  ClockStone.kt  Obstacles.kt
     actors/AntRenderer.kt(實例化+姿態桶)  InsectRenderer.kt  FoodRenderer.kt  ParticlesRenderer.kt
     phero/PheromoneDecal.kt  // 動態貼圖 → 地面貼花 + emissive
     nest/NestRenderer.kt     // OffscreenPass2d 地下剖面
     assets/AssetCatalog.kt  ProceduralMeshes.kt
     quality/QualityPreset.kt
   ui/ (ui2)  Hud.kt  TopToolbar.kt  BottomBar.kt  panels/…  Theme.kt  Icons.kt  Fonts.kt
   ```
   現有 `AntSimScene.kt`（2D）保留為 `LegacyFlatRenderer`，Grafik 面板可切回（也是 GPU 不支援時的最後退路）。
4. **世界單位**：1 render unit = 1 sim px（sim 世界 1400×840）。對照真實尺寸約 0.5 mm/unit：工蟻 12 unit ≈ 6 mm，時鐘石半徑 150 unit ≈ 7.5 cm。草葉高 30–120 unit（比螞蟻高 3–10 倍），這是「化身螞蟻」視角能成立的關鍵。
5. **兩個場景**：3D 世界場景（全視窗；相機投影偏移讓世界落在左側 74%）＋ UI 場景（ui2）。巢穴用 OffscreenPass2d 畫成貼圖，再用 ui2 `Image` 顯示在右側 26% 面板。

## 3. 分期（一次交付，但每期都能單獨編譯、執行、截圖；後期失敗就停在前一期）

| 期 | 內容 | 退路 |
|---|---|---|
| P0 | `render3d` 骨架、`CameraController` + 俯視鎖定、deferred pipeline + 太陽 + CSM、地面平面 PBR、Grafik「Renderer: 3D / Classic 2D」切換 | deferred 建不起來 → forward `KslPbrShader` + `SimpleShadowMap`；3D 全掛 → Classic 2D |
| P1 | 螞蟻/昆蟲/食物 3D（程序幾何先行，glTF 可替換）、實例化、腳步動畫（姿態桶）、點擊選取/聚焦 | glTF 載不到 → 程序螞蟻；骨架動畫有問題 → 剛體分段腳 CPU 擺動 |
| P2 | 地形細節：splat 土壤、散布石/草/花/落葉（實例化 + LOD）、時鐘石、障礙物 | 貼圖缺 → 程序噪聲貼圖 |
| P3 | 費洛蒙貼花 + emissive、食物發光、bloom、AO、日夜 + 動態 IBL | bloom 需 compute（Vulkan OK；GL/WebGL2 關閉）；動態 IBL 太慢 → 2–4 張預烘 HDRI 混合 |
| P4 | 天氣：雨絲、濕潤、漣漪、雲影；巢穴 3D 剖面 | 剖面太貴 → 巢穴維持 2D 但換新美術 |
| P5 | HUD 美化（圖示、淡紫焦點色、tooltip、CJK 字型）、品質檔位、截圖 PNG | 圖示載入失敗 → ASCII 標籤（現狀） |
| P6（預留） | 化身螞蟻：`AntPovCamera`、跟隨/第一人稱、自由旋轉、單隻高精度蟻 | 只做第三人稱跟隨 |

版本號：每期 `./configure.sh version 0.8.0`、`0.9.0` …；全部完成可定 `1.0.0`。

## 4. 主要風險（詳見各文件）

- **macOS 後端**：desktop 預設 `RenderBackendVk`（MoltenVK）。kool 的 GL 後端在 GLFW 建窗時沒有要求 core profile（`GlfwWindowSubsystem.kt:51-53` 只呼叫 `glfwDefaultWindowHints()`），而 `GlImpl.checkApiVersion()` 要求 ≥ 3.3（`GlImpl.kt:379`）。macOS 沒要 core profile 時只給 2.1 → GL 退路在 Mac 上**很可能直接失敗**（推測，要在 Mac 驗）。所以 Mac 上 Vulkan 是唯一實際路徑，GL 只當 Linux/Windows 退路。
- **bloom 是 compute pass**（`BloomPass : ComputePass`，`pipeline/BloomPass.kt:14`）。WebGL2 沒有 compute（`jsMain/.../gl/RenderBackendGlImpl.kt:47 computeShaders=false`）；macOS GL 4.1 也沒有（GL 後端把 computeShaders 寫死 true，`desktopMain/.../RenderBackendGlImpl.kt:62`，實際跑會出錯）。一律用 `KoolSystem.features.computeShaders` 判斷，false 就關 bloom、改用 emissive 加亮。
- **deferred 不能畫半透明**：glTF 的 `AlphaMode.Blend` 會被 kool 自動改成 forward（`GltfFile.kt:675-678`）。費洛蒙、雨、玻璃狀糖滴要在 output quad 之後用 forward shader 畫；output quad 會寫深度（`DeferredOutputShader.kt:123`），所以 forward 物件能和 deferred 物件正確遮擋（推測，試作已編過）。
- **骨架動畫 + 實例化**：同一個 skinned mesh 的所有實例共用一組關節矩陣（`Mesh(… skin = meshSkin, instances = …)`，`GltfFile.kt:601-602`）→ 所有螞蟻同步踏步。用「姿態桶」解（見 `assets_gltf_pipeline.md` §5）。
