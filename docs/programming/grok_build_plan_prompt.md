# Grok Build plan-mode prompt（在 myants 根目錄執行 `grok`，切到 plan mode，貼上下面整段）

```
你在 neojou/myants（v0.7.0；Kotlin 2.3.0 + kool 0.19.0，module koolApp，desktop JVM + js）。
任務：把世界畫面從 2D 平面重做成「真 3D」，視覺質感要超過 AntSim_V2 原版（截圖：docs/programming/3d_refs/references/），
模擬邏輯與操作功能完全不變。目標平台以 macOS desktop 為主，js 能編譯、能跑基本版即可。

先讀（以這些文件為準，與程式衝突時以 kool 0.19.0 原始碼為準）：
- docs/programming/3d_visual_overhaul_overview.md（目標、架構、P0–P6 分期與退路）
- docs/programming/rendering_pipeline_kool.md（已查證 API 與檔案位置、deferred/forward 分工、macOS 後端）
- docs/programming/camera_modes_and_ant_pov.md
- docs/programming/world_scene_composition.md
- docs/programming/assets_gltf_pipeline.md
- docs/programming/ui_hud_aesthetics.md
- docs/programming/perf_and_quality_presets.md
- docs/programming/3d_refs/feasibility_trial/（已編譯通過的 API 範例）
- 既有：antsim_ui_ux_ref.md、antsim_feature_inventory_ref.md、myants_antsim_deviations.md

現在只做計畫：不要改檔、不要 commit、不要 push。

硬性限制：
1. 不改 kool 0.19.0 / Kotlin 2.3.0 / Gradle 9.3.0 / jvmToolchain(25)；不用 kool main（0.20.0-SNAPSHOT）；不加 Compose/wasm。
2. sim 套件（com.neojou.myants.sim）不改行為；渲染不得呼叫 Rng；HeadlessInvariantsTest 黃金值不變。
3. 新程式放 com.neojou.myants.render3d 與 ui/；舊 2D 渲染保留成 Grafik「Renderer: Classic 2D」。
4. 正常模式：俯視（正交或窄 FOV），可平移、滾輪以游標為錨縮放、點擊聚焦，相機旋轉鎖定；只為未來「化身螞蟻」預留 CameraController 介面，不實作 POV。
5. bloom 只在 KoolSystem.features.computeShaders 為真且後端是 Vulkan/WebGPU 時開；半透明一律在 deferred output quad 之後用 forward 畫。
6. 素材只用 CC0/CC-BY（清單見 assets 文件），署名寫進 THIRD_PARTY_NOTICES.md；任何素材缺席都要有程序幾何退路，app 在沒有任何 .glb 時也能完整執行。
7. UI 版位、控制項、文字照 AntSim（英/德）；淡紫 #6750A4/#EADDFF 只用在互動狀態；圖示用 Material Symbols SVG，載入失敗退回 ASCII。
8. 文件中標「推測」的 API/行為，計畫要列出驗證步驟與退路。

一次交付全部（P0–P5），但每期結束都必須可編譯、可執行、可截圖；後期做不出來就停在前一期並在 deviations 記錄。每期 ./configure.sh version 0.8.0、0.9.0…

請輸出：
A. 檔案層級變更清單（新增/修改/刪除），對應到分期。
B. P0–P5 步驟，每期的驗收條件與退路。
C. 相機、實例化（含骨架動畫姿態桶）、費洛蒙 emissive、巢穴 OffscreenPass2d 的關鍵程式草稿（介面與資料流即可）。
D. 品質檔位表如何對映到程式（QualityPreset）。
E. 風險清單（macOS Vulkan/GL、compute、透明、效能）與偵測方式。
F. 驗證計畫（下列）。

驗收條件：
- ./gradlew compileKotlinDesktop compileKotlinJs desktopTest 全部通過（每期）。
- macOS：./gradlew :koolApp:runDesktop 開出 3D 世界；Medium、雙群 300 蟻 ≥ 60 fps（M 系列）；Ultra 1200 蟻 ≥ 30 fps。
- 畫面要素齊全：PBR 土壤、3D 草/石/花、發光食物 + bloom、四色發光費洛蒙、帶數字的時鐘石、3D 螞蟻有步行動畫、太陽陰影隨日夜移動、雨（雨絲 + 地面變濕）、右側 3D 巢穴剖面。
- 所有 AntSim 控制項可用：Speed、Grafik、Pause/Step/Zoom/Reset/Screenshot、Tool 5 種、Restart、Rivalenkolonie、Stats/Params/Analyse、Inspector、Kino-Kamera、鍵盤 Space/+/-。
- Grafik 切 Classic 2D 與 3D 不需重啟；品質檔位可切換。
- 截圖按鈕在 desktop 存 PNG（Texture.download + ImageIO），檔名 antsim_seed{seed}_t{age}.png；做不到就保留 log 並記在 deviations。
- 交付時附 4 張截圖到 docs/programming/3d_refs/shots/：預設白天、夜晚、下雨、放大到螞蟻尺度，並與 AntSim 截圖並排自評（ui_hud_aesthetics.md §6 清單）。
- README、myants_antsim_deviations.md、THIRD_PARTY_NOTICES.md 已更新。

計畫用繁體中文。
```
