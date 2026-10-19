# kool 0.19.0 3D API 可行性試作

- 位置：複製 myants v0.7.0 到 `/workspace/myants_3d_trial`（scratch，不是 repo），加入 `Trial3d.kt`（commonMain）與 `Trial3dDesktop.kt`（desktopMain）。
- 指令：`./gradlew --offline compileKotlinDesktop compileKotlinJs`（JDK 25.0.4、Kotlin 2.3.0、kool 0.19.0）→ **BUILD SUCCESSFUL**（2026-10-19 約 20:00 UTC+8）。
- **只驗證編譯**，沒有在 GPU 上執行（box 沒有 GPU / 顯示器）；`models/ant.glb` 等素材不存在，載入會回 failure 並被 `getOrNull()` 吃掉。
- 試作沒有接到 `myantsApp()`，不會影響現有 app。

編譯通過的 API：
| 類別 | API |
|---|---|
| Deferred | `DeferredPipeline`、`DeferredPipelineConfig{ isWithAmbientOcclusion, isWithBloom, isWithVignette, maxGlobalLights, useImageBasedLighting, useShadowMaps }`、`bloomStrength/Threshold/Radius`、`isAoEnabled`、`sceneContent`、`createDefaultOutputQuad()`、`lightingPassShader.ambientMap` |
| Shader | `deferredKslPbrShader{ vertices{instancedModelMatrix()}; color{vertexColor()/instanceColor()}; emission{instanceColor(); constColor(…, Multiply)}; roughness() }`、`KslPbrShader{ alphaMode = AlphaMode.Blend; emission{}; lighting{ addShadowMap(csm) }; enableImageBasedLighting(env) }` |
| 光/影 | `Light.Directional().setup(dir).setColor(c, intensity)`、`CascadedShadowMap(scene, light, maxRange, numCascades)`、`SimpleShadowMap(scene, light, mapSize).shadowBounds = BoundingBoxF(…)` |
| 天空/IBL | `SkyCubeIblSystem.simpleSkyCubeIblSystem(scene)`、`skyPass.elevation/azimuth/syncLights`、`setupOffscreenPasses()`、`environmentMap`、`AssetLoader.hdriEnvironment(path)` |
| AO | `AoPipeline.createForward(scene)` |
| glTF | `GltfLoadConfig(materialConfig = GltfMaterialConfig(shadowMaps, environmentMap, isDeferredShading, fixedNumberOfJoints), instanceLayout, loadAnimations, applySkins)`、`Assets.loadGltfModel()`、`loadGltfFileAsync()`、`Model.enableAnimation()`、`applyAnimation(dt)` |
| 相機 | `OrthographicCamera`、`PerspectiveCamera.fovY`、`orbitCamera{ leftDragMethod=PAN; panMethod=yPlanePan(); zoomMethod=ZOOM_TRANSLATE; min/maxHorizontalRot 鎖定; translationBounds }` |
| 實例化 | `MeshInstanceList(InstanceLayouts.ModelMatColor, n)` + `addMesh(VertexLayouts.PositionNormal, instances=…)` |
| 協程 | `FrontendScope.launch {}`（`launchOnMainThread` 已 deprecated） |
| desktop | `KoolConfigJvm(renderBackend = RenderBackendGl / RenderBackendVk)`、`MsdfFontGenerator.writeMsdfFont(…, GeneratorGlyphSet.FromString("…"))` |

第一次編譯錯誤（已修正，供參考）：`BoundingBoxD/F` 在 `de.fabmax.kool.math.spatial`；`loadGltfFileAsync` 要另外 import；`PbrSceneShader.ambientMaps` 是唯讀 List（用 `ambientMap = env.irradianceMap`）。
