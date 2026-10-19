# kool 0.19.0 渲染管線設計（myants 3D）

原始碼根：`/workspace/kool-0.19.0`（tag v0.19.0，`a8f0788`）。下稱
`K/` = `kool-core/src/commonMain/kotlin/de/fabmax/kool/`，
`KD/` = `kool-core/src/desktopMain/kotlin/de/fabmax/kool/`，
`DEMO/` = `kool-demo/src/commonMain/kotlin/de/fabmax/kool/demo/`。
**已查證**＝看過原始碼；**已編譯**＝寫在 `feasibility_trial/Trial3d.kt`，在 myants 複本以 `compileKotlinDesktop compileKotlinJs` 通過（2026-10-19 UTC+8，未在 GPU 上執行）；**推測**＝待 Mac 實機確認。

> 注意：kool-demo 的程式碼**不在** `kool-core` 發佈的 artifact 裡。要用 demo 的草、風、天空（`DEMO/physics/terrain/*`）得複製進 myants 並保留 Apache-2.0 版權聲明（kool 是 Apache-2.0，`LICENSE`）。記在 `THIRD_PARTY_NOTICES.md`。

## 1. 功能查證表

| 功能 | API / 位置 | 狀態 |
|---|---|---|
| Deferred pipeline | `DeferredPipeline(scene, DeferredPipelineConfig)`：`K/pipeline/deferred/DeferredPipeline.kt:16`；config `:290-319`（`isWithAmbientOcclusion`、`isWithScreenSpaceReflections`、`isWithBloom`、`isWithVignette`、`isWithChromaticAberration`、`maxGlobalLights=4`、`useShadowMaps()`、`useImageBasedLighting()`、`outputDepthTest`）；`sceneContent`、`lightingPassContent`、`createDefaultOutputQuad()` `:180`；`renderResolution` `:18`；執行期開關 `isAoEnabled/isSsrEnabled/isBloomEnabled`、`bloomStrength/Radius/Threshold` `:63-102` | 已查證、已編譯 |
| G-buffer 含 emissive | `MaterialPass.FORMATS_DEFERRED_EMISSIVE`（position/flags、normal/rough、albedo/metal、emissive/AO）`K/pipeline/deferred/MaterialPass.kt:86-92` | 已查證 |
| Deferred PBR shader | `deferredKslPbrShader { color{}; emission{}; roughness(); metallic(); normalMapping{}; ao{}; vertices{ instancedModelMatrix() } }` `K/pipeline/deferred/DeferredPbrShader.kt:15` | 已編譯 |
| Forward PBR | `KslPbrShader { …; lighting { addShadowMap() }; enableImageBasedLighting(env); alphaMode = AlphaMode.Blend }` `K/modules/ksl/KslPbrShader.kt:97` | 已編譯 |
| Splat 地形 shader | `KslPbrSplatShader` `K/modules/ksl/KslPbrSplatShader.kt:25`（demo：`DEMO/physics/terrain/Terrain.kt:27` splat map） | 已查證，用法推測 |
| glTF 2.0 載入 | `Assets.loadGltfModel(path, GltfLoadConfig)` `K/modules/gltf/GltfAssetManagerExtensions.kt:32`；`loadGltfFileAsync` `:35`；`GltfLoadConfig`（`instanceLayout`、`loadAnimations`、`applySkins`、`applyMorphTargets`、`mergeMeshesByMaterial`、`materialConfig`）`K/modules/gltf/GltfConfig.kt:11-36`；`GltfMaterialConfig(shadowMaps, scrSpcAmbientOcclusionMap, environmentMap, isDeferredShading, fixedNumberOfJoints)` | 已編譯 |
| 骨架/動畫 | `Model.animations`、`enableAnimation(i)`、`setAnimationWeight`、`applyAnimation(dt)` `K/scene/Model.kt:13-32`；`AnimationController` 狀態機 `K/scene/animation/AnimationController.kt:5`；`Skin` `K/scene/animation/Skin.kt:7` | 已查證（`applyAnimation` 已編譯）|
| glTF + 實例化 | `GltfLoadConfig(instanceLayout=…)` → 每個 mesh 帶 `MeshInstanceList`，shader 自動 `instancedModelMatrix()` `K/modules/gltf/GltfFile.kt:601,640-643` | 已查證、已編譯 |
| 半透明自動轉 forward | `if (alphaMode is AlphaMode.Blend) { isDeferred = false }` `GltfFile.kt:675-678` | 已查證 |
| IBL（HDRI） | `Assets.hdriEnvironment(path)` `K/AssetExtensions.kt:256`；`EnvironmentMap.fromHdriTexture` `K/pipeline/ibl/EnvironmentMap.kt:38`（**輸入是 RGBE 編碼 PNG**，demo 檔名 `*.rgbe.png`，`DEMO/AoDemo.kt:32`）；`fromSingleColor`/`fromGradientColor` `:23,30` | 已編譯 |
| 動態天空 + IBL | `SkyCubeIblSystem.simpleSkyCubeIblSystem(scene)` `K/pipeline/ibl/SkyCubePass.kt:243`；`skyPass.elevation/azimuth`、`skyPass.syncLights += sunLight`、`environmentMap`、`isAutoUpdateIblMaps`、`updateIblMaps()` `:198-240` | 已編譯 |
| Skybox | `Skybox.cube(envMap.reflectionMap, lod)` `K/scene/Skybox.kt:22` | 已查證 |
| 陰影 | `SimpleShadowMap(scene, light, mapSize)`（`shadowBounds: BoundingBoxF?` `:52` 可鎖住整個世界）`K/util/ShadowMap.kt:21`；`CascadedShadowMap(scene, light, maxRange, numCascades=3, mapSizes)` `:200`；方向光陰影相機為正交，**支援場景用正交相機**（`computeFrustumPlane` 在 `OrthographicCamera` 有實作 `K/scene/Camera.kt:411`）；點光源陰影**不支援**（`DeferredPipeline.kt:279-281` 印警告） | 已編譯 |
| SSAO | `AoPipeline.createForward(scene)` / `createDeferred(pipeline)` `K/pipeline/ao/AoPipeline.kt:183,189` | 已編譯 |
| Bloom | `BloomPass(inputTexture) : ComputePass` `K/pipeline/BloomPass.kt:14`（**需要 compute shader**）；deferred 內建 `isWithBloom`；forward 範例 `DEMO/helloworld/HelloBloom.kt:61` | 已查證；deferred 版已編譯 |
| SSR | `isWithScreenSpaceReflections`、`ReflectionPass` `K/pipeline/deferred/ReflectionPass.kt:19`、範例 `DEMO/ReflectionDemo.kt:89-99` | 已查證（雨後水窪可用，預設關） |
| Vignette / 色差 | `isWithVignette`、`isWithChromaticAberration` | 已查證 |
| 實例化 | `MeshInstanceList(layout, n)`、`InstanceLayouts.ModelMat / ModelMatColor` `K/scene/InstanceLayouts.kt`、`addMesh(…, instances=…)`；myants 2D 版已在用 | 已編譯 |
| 實例 LOD | `InstancedLodController<T: Struct>` `K/util/InstancedLodController.kt:12`（`addLod(mesh, maxDistance, maxInstances)`）| 已查證 |
| 高度圖 | `Heightmap(FloatArray, rows, cols).getHeightLinear(x,y)` `K/util/Heightmap.kt:11,31` | 已查證 |
| 草 / 風 / 天空 | demo：`DEMO/physics/terrain/Grass.kt`、`CamLocalGrass.kt`（實例化相機周邊草）、`GrassShader.kt`、`Wind.kt`、`Sky.kt`（timeOfDay、太陽/月亮、星空） | 已查證（demo，需複製）|
| 毛皮 / 苔蘚 | `DEMO/ShellShadingDemo.kt:21`（shell shading） | 已查證（可做苔蘚、蟻體細毛，選配）|
| 粒子 | **kool-core 沒有粒子系統**（`K/` 內 grep "particle" 無結果）。`DEMO/helloworld/HelloComputeParticles.kt` 是 compute 範例 | 已查證 → 雨/塵/血用 CPU + 實例化 quad 自寫 |
| Path tracer | 只有 demo `DEMO/pathtracing/PathTracingDemo.kt:28`（compute 光線追蹤示範），**不是引擎功能** | 已查證 |
| 渲染到貼圖 | `OffscreenPass2d(drawNode, AttachmentConfig{…}, initialSize)` `K/pipeline/OffscreenPass2d.kt:13`；範例 `DEMO/helloworld/HelloRenderToTexture.kt:31-55` | 已查證 |
| 截圖 | `Texture2d.download(): BufferedImageData2d` `K/pipeline/Texture.kt:124`；`FrameCopy` `K/pipeline/FrameCopy.kt:16`。desktop 用 `javax.imageio.ImageIO` 寫 PNG（AWT headless 下 ImageIO 仍可用，推測） | API 已查證，流程推測 |
| 指標射線 | `camera.computePickRay(ray, ptr, viewport)` `K/scene/Camera.kt:126` | 已查證 |
| 後端能力 | `KoolSystem.features.computeShaders`（`DEMO/Demos.kt:31`） | 已查證 |

## 2. 選定架構：deferred 為主 + forward 疊加

```
Scene "world3d"
 ├─ DeferredPipeline(cfg: AO, bloom(若 compute), IBL=SkyCubeIblSystem.environmentMap, shadows=[CSM 或 SimpleShadowMap])
 │   └─ sceneContent: 地面、石、草、花、落葉、時鐘石、螞蟻、昆蟲、食物（不透明 / alpha mask）
 ├─ output quad（寫回深度）
 └─ forward 疊加（在 output quad 之後加入，依序）：
     費洛蒙貼花（加法）→ 雨絲（加法/alpha）→ 選取光圈 → 除錯線
Scene "ui"（ui2）: HUD、工具列、面板、巢穴 Image（顯示 NestRenderer 的 OffscreenPass2d.colorTexture）
```
理由：
- 數百螞蟻 + 數千草葉/石頭 + 食物發光點光 → deferred 對多光源便宜，`DeferredPointLights` 可替每個食物放一盞小點光（`K/pipeline/deferred/DeferredPointLights.kt`）。
- emissive 進 G-buffer，deferred bloom 直接吃 lighting pass 輸出（`DeferredPipeline.kt:150-156`）。
- forward 疊加物件要自己決定是否接受 bloom：deferred 的 bloom 只處理 lighting pass 的顏色，**output quad 之後才畫的 forward 物件不會被 bloom**（推測，依 `bloom.dependsOn(pass.lightingPass)` 判斷）。所以「要發光的半透明物件」（費洛蒙）改法如下：
  1. 首選：費洛蒙改為**不透明貼花寫進 G-buffer 的 emissive**：地面 shader 多取樣一張費洛蒙貼圖，把顏色加到 `emission`（地面本來就在 deferred）。不用透明，bloom 自然生效。
  2. 次選：費洛蒙另做一層 forward 加法 mesh，並調高 emissive 本身亮度（不靠 bloom）。
- 試作已編譯的設定（`feasibility_trial/Trial3d.kt`）：
  ```kotlin
  val sky = SkyCubeIblSystem.simpleSkyCubeIblSystem(this).apply {
      skyPass.elevation = 45f; skyPass.azimuth = 120f; skyPass.syncLights += sun; setupOffscreenPasses()
  }
  val csm = CascadedShadowMap(this, sun, maxRange = 200f, numCascades = 3)
  val defCfg = DeferredPipelineConfig().apply {
      isWithAmbientOcclusion = true; isWithBloom = KoolSystem.features.computeShaders
      isWithVignette = true; maxGlobalLights = 2
      useImageBasedLighting(sky.environmentMap); useShadowMaps(listOf(csm))
  }
  val deferred = DeferredPipeline(this, defCfg)
  deferred.sceneContent.apply { /* meshes with deferredKslPbrShader { … } */ }
  addNode(deferred.createDefaultOutputQuad())
  ```

## 3. 光照、日夜、天氣

- **太陽**：一盞 `Light.Directional`（主光，投影）＋ 夜間把同一盞轉成月光（冷色、低強度），參考 `DEMO/physics/terrain/Sky.kt:167-190` 的 `updateLight`。
- **時間來源**：沿用 sim 的 `world.timeOfDay`（0–1，`dayLength=6000` tick ≈ 100 秒，`World.kt:74-75,255`），與 AntSim 一致。時鐘石數字仍顯示**牆鐘**（AntSim `drawClockStone` 用 `new Date()`）。Grafik 的「Tag/Nacht-Tönung」關掉時鎖定正午光；「Tag/Nacht-Stärke」縮放日夜色差。
- **太陽角度**：`elevation = sin(π·dayProgress)·70°`，`azimuth` 從東到西（AntSim 陰影由左往右移）。夜間 elevation < 0 → 換月光。
- **IBL**：`SkyCubeIblSystem` 的 `skyPass.elevation/azimuth` 隨太陽更新；`isAutoUpdateIblMaps=false`，每 0.5–2 秒（依品質）呼叫一次 `updateIblMaps()`，避免每幀重算 irradiance/reflection（推測：每幀重算在 Mac 上太貴）。退路：預烘 4 張 RGBE HDRI（清晨/正午/黃昏/夜，Poly Haven CC0），在 lighting shader 的 `ambientMaps`（`PbrSceneShader.kt:46-47`，兩張 + 權重）間混合；這正是 `Sky.kt` 的作法。
- **雲**：渲染專用。一張可平鋪的噪聲貼圖在地面 shader 裡當「雲影」乘到太陽光（UV 隨風移動）；陰天降低太陽強度、提高環境光。sim 沒有雲狀態 → 用 `world.raining` 和一個渲染端 `cloudiness`（下雨前 30 秒漸增）平滑過渡。
- **雨**（`world.raining`）：雨絲實例 2k–8k（依品質）、地面 `wetness` 0→1（粗糙度 0.9→0.35、albedo ×0.6、SSR 可選開），雨停後慢慢乾。
- **夜間**：食物/費洛蒙 emissive 在夜間權重提高（更像 AntSim 的發光感）；可加螢火蟲粒子（純裝飾，渲染隨機）。

## 4. 陰影選擇

- 俯視正交相機看整個 1400×840 世界時，**單張 `SimpleShadowMap` + `shadowBounds = 世界 AABB`** 比 CSM 穩定（解析度均勻、不閃）。試作已編譯：`SimpleShadowMap(this, sun, mapSize = 2048).apply { shadowBounds = BoundingBoxF(...) }`。
- 放大到螞蟻尺度或化身螞蟻時改用 `CascadedShadowMap`（`maxRange` 依相機距離設定、`setMapRanges()`）。
- 品質檔：Off / 1024 / 2048 / 4096（或 CSM 3×2048）。草葉預設**不投影**，只接受陰影（`isCastingShadow=false`），Ultra 才投影。

## 5. macOS / 後端注意事項

| 項目 | 事實 | 狀態 |
|---|---|---|
| desktop 預設後端 | `KoolConfigJvm.renderBackend = RenderBackendVk` `KD/KoolConfigJvm.kt:38` | 已查證 |
| MoltenVK | lwjgl-vulkan 的 natives 只在 macOS 帶（`kool-core/build.gradle.kts:26-28`）；`VK_KHR_portability_subset` 處理 `KD/pipeline/backend/vk/PhysicalDevice.kt:52`、`VkSetup.kt:30` | 已查證 |
| Vulkan compute | `computeShaders = device.computeQueue == device.graphicsQueue` `KD/pipeline/backend/vk/RenderBackendVk.kt:76` | 已查證；Apple GPU 推測為 true |
| GL 退路 | `KoolConfigJvm(renderBackend = RenderBackendGl)` 可編譯（`feasibility_trial/Trial3dDesktop.kt`）。但 macOS 預設 context 2.1、kool 要 ≥ 3.3（`GlImpl.kt:379`），且沒設 core-profile hint → **在 Mac 上大概率啟動失敗**；Linux/Windows 可用 | 已編譯；Mac 行為推測 |
| GL compute | GL 後端把 `computeShaders = true` 寫死（`RenderBackendGlImpl.kt:62`），實際需 GL 4.3（`GlImpl.kt:306`）→ 不能只看 features，GL 後端一律關 bloom | 已查證 |
| `-XstartOnFirstThread` | myants `koolApp/build.gradle.kts` 在 macOS 已加 | 已查證 |
| AWT headless | kool 在 macOS 設 `java.awt.headless=true`（`KD/platform/glfw/GlfwWindowSubsystem.kt:73-78`）→ 不能用 AWT 視窗/對話框；`BufferedImage`、`ImageIO`、字型光柵化可用（推測）| 已查證 / 推測 |
| JS | 預設 `RenderBackendWebGpu`，`useWebGlFallback` 時退 WebGL2（`jsMain/.../platform/JsContext.kt:49-51`）；WebGL2 無 compute → 無 bloom | 已查證 |

啟動旗標建議（desktop）：`--renderer=3d|2d`、`--backend=vk|gl`、`--quality=…`、`--no-bloom`。Vulkan 初始化例外時寫 log 並建議 `--backend=gl`；Mac 上 GL 也失敗則自動以 Classic 2D… 但 2D 也用同一後端，所以**真正的保底是在 Vulkan 上只開 forward 最簡管線**（無 deferred/bloom/AO）。

## 6. 已知陷阱（myants 已踩過，3D 版延續）

- GL 載入器用影像位元組雜湊當貼圖 id：全零緩衝區會共用貼圖（`myants_antsim_deviations.md`）。費洛蒙動態貼圖沿用「每層兩個固定 id 輪流上傳」。
- `ClearColorFill` 會清整個 framebuffer；多場景時只有最底層場景用 Fill，其餘 `ClearColorLoad`。巢穴改用 OffscreenPass2d 後，這問題消失。
- `launchOnMainThread` 在 0.19.0 已 deprecated，改用 `FrontendScope.launch { }`（`K/util/KoolDispatchers.kt:35`，試作已編譯）。
