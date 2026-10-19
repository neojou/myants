# 素材與 glTF 管線

授權與網址於 2026-10-19（UTC+8）查證：Poly Haven 用官方 API（`api.polyhaven.com`），Sketchfab 用公開 API `https://api.sketchfab.com/v3/models/<uid>` 的 `license` 欄位。**原則**：只用 CC0 或 CC-BY（需署名）；不用 NC/ND；不複製 AntSim 的美術。每個檔案記在 `docs/programming/THIRD_PARTY_NOTICES.md`（作者、標題、網址、授權、修改內容）。

## 1. 來源清單

### 1.1 Poly Haven（CC0，`https://polyhaven.com/license` 已查證：可商用、免署名、可再散布）
| 類型 | slug（已用 API 確認存在） | 用途 |
|---|---|---|
| 土壤貼圖 | `forest_ground_04`、`brown_mud_leaves_01`、`pebble_ground_01`、`leaves_forest_ground`、`dry_decay_leaves`、`brown_mud_dry` | 地面 splat 三層 + 乾/濕變化 |
| 岩石貼圖 | `mossy_rock`、`rock_boulder_dry`、`lichen_rock` | 障礙物、時鐘石 |
| 模型 | `grass_medium_01`、`grass_medium_02`、`grass_bermuda_01`、`dandelion_01`、`celandine_01`、`flower_gazania`、`flower_ursinia`、`boulder_01`、`namaqualand_stones_01`、`moss_01`、`pine_roots`、`bark_debris_01`、`dry_branches_medium_01` | 草、花、石、苔、樹根（巢穴剖面）、枯枝 |
| HDRI | `kloofendal_48d_partly_cloudy_puresky`（正午）、`kiara_1_dawn`（清晨）、`qwantani_dusk_2_puresky`（黃昏）、`qwantani_night_puresky`（夜）、`overcast_soil_puresky`（陰雨）、`meadow_2` | 預烘 IBL 退路 / 陰雨天 |
- 下載：模型與貼圖都有 glTF（`api.polyhaven.com/files/<slug>` → `gltf` 有 1k/2k/4k/8k，已查證 `grass_medium_01`）。**用 1k**（2k 只給地面）。
- HDRI 直鏈格式：`https://dl.polyhaven.org/file/ph-assets/HDRIs/hdr/1k/<slug>_1k.hdr`（`meadow_2_1k.hdr` 已實際下載）。
- kool 的 HDRI 必須是 **RGBE 編碼 PNG**（`*.rgbe.png`）：用 `tools/hdr_to_rgbe_png.py in.hdr out.rgbe.png`（已用 `meadow_2_1k.hdr` 測過，輸出 1024×512，解碼後天空偏藍、地面偏綠，符合 kool `RgbeDecoder.kt:65-66` 的 `rgb·2^(a·255−128)` 公式）。
- 尺度：Poly Haven 是真實公尺尺度（草約 0.3 m）。照 1 unit ≈ 0.5 mm 換算，真實的草是 600 unit，約螞蟻的 50 倍，俯視時會把螞蟻整片蓋掉。所以**刻意風格化**：載入後以 bounding box 正規化到 30–120 unit（螞蟻的 3–10 倍）。化身螞蟻模式可以把相機附近的草放大。

### 1.2 ambientCG（CC0，`https://ambientcg.com/license` 已查證 "CC0 1.0 Universal"）
土壤/苔蘚/樹皮 PBR 貼圖替代來源。

### 1.3 Kenney（CC0，`https://kenney.nl/assets/nature-kit` 頁面標示 CC0，已查證）
Nature Kit：低多邊形石頭/植物，風格偏卡通。只當**低品質檔位或 LOD2** 用，避免和 Poly Haven 寫實風混搭在同一距離。

### 1.4 Quaternius（CC0，例：`https://quaternius.com/packs/stylizednaturemegakit.html` 標示 CC0、glTF）
沒有螞蟻（已搜尋）。可用 Stylized Nature MegaKit 的花草當低檔位替代；Ultimate Monsters / Animated Animal Pack 無適用昆蟲。

### 1.5 Sketchfab 螞蟻（CC-BY 4.0，需署名；下載需登入 Sketchfab，由 Neo 手動下載 glTF）
| uid | 標題 / 作者 | 授權（API） | 面數 | 動畫數 | 建議用途 |
|---|---|---|---|---|---|
| `ae6dcdb4aa4a48ceaf71874c3fa693b7` | Rigged Ant / tmarsland（2017） | CC Attribution | 3,616 | 1 | **LOD1 實例化主力**（無貼圖，正好用 tint 上色） |
| `df93e4c92ff74dfc81ae9a713baeb9bc` | Worker Ant Animation / Komojo（2020） | CC Attribution | 10,430 | 2 | LOD0 / 工蟻 |
| `b48893d316cc4b2f98ec1d1e37027e6a` | Game-Ready Worker ant Model / LilCick（2024） | CC Attribution | 47,872 | 16 | 化身螞蟻主角（LOD0，單隻）|
| `b04b636a35e14b38bfb05bd21931549d` | Game-Ready Queen ant Model / LilCick（2024） | CC Attribution | 47,872 | 13 | 蟻后（巢穴）|
| `88785783d841441cae6f083c34241300` | Ant walkcycle / matrixrex（2017） | CC Attribution | 428 | 1 | LOD2（極遠/Ultra Low）|
| `413c013b434f43648743298445a0a3a9` | Ant Worker / spuma（2024） | CC Attribution | 10,444 | 9 | **先查出處**：描述寫 "A free model of a 5$"，可能是付費素材的免費版，確認作者即原創者再用 |

**不要用**：kenchoo「Walking Ant」「Ant」（CC-BY-NC）；tunosemi 的蟻類（CC-BY-NC-ND）；MAXDESIGN「ANT」、ap-school「ant」、tech.service.allwe「ant」——三者都標 CC-BY 但面數都正好 9,768，與 kenchoo 的 NC「Ant」相同，疑似轉傳，授權不可信。
署名格式（`THIRD_PARTY_NOTICES.md` 與 About）：`"Rigged Ant" by tmarsland (https://sketchfab.com/3d-models/rigged-ant-ae6dcdb4aa4a48ceaf71874c3fa693b7), CC BY 4.0, modified (rescaled, re-rigged material)`。

### 1.6 昆蟲（CC0 掃描模型，Sketchfab；高面數需減面）
`b14201002b5e4a22957819204be935d0`「Tytthaspis sedecimpunctata」（瓢蟲，disc3d，CC0，151k 面）、`77a30e2a1d7d4fc98a30b549ce10e185`「Joro Spider」（ffishAsia-and-floraZia，CC0，90k 面）、`157900941156422fbd9dcdfc38a3be19`「Musca domestica」（disc3d，CC0）、`05d5d69295644b9eadd4cd8a7ac5adca`「European stag beetle」（CC0，345k 面）。都沒有動畫 → 減面到 2–5k（Blender Decimate，或 kool `modules/mesh/simplification/MeshSimplifier.kt` 執行期簡化，推測太慢，建議離線），走路用程序腳擺動或只滑行 + 輕微上下。蚜蟲找不到 → 程序（綠色小卵形 + 6 腳）。

### 1.7 字型與圖示
- Noto Sans TC（SIL OFL 1.1，`github.com/notofonts/noto-cjk` 的 `Sans/LICENSE` 已查證）。
- Material Symbols（Apache-2.0，`github.com/google/material-design-icons` LICENSE 已查證）：SVG，kool desktop 用 jsvg、JS 用瀏覽器解 SVG（`KD/platform/ImageDecoder.kt:119-122`、`jsMain/.../Assets.js.kt:106-107`），可直接 `Assets.loadTexture2d("icons/pause.svg", resolveSize = Vec2i(48,48))`（簽名已查證 `K/AssetExtensions.kt:213-219`；SVG 實際渲染效果推測）。

## 2. 目錄與載入

```
koolApp/src/commonMain/resources/assets/
  models/ants/worker_lod0.glb  worker_lod1.glb  queen.glb  ...
  models/insects/ladybug.glb  spider.glb  beetle.glb  fly.glb
  models/nature/grass_a.glb  flower_a.glb  pebble_a.glb  boulder_a.glb  roots_a.glb
  textures/ground/forest_ground_04_{diff,nor_gl,arm,disp}_1k.jpg|png
  hdri/noon_1k.rgbe.png  dawn_1k.rgbe.png  dusk_1k.rgbe.png  night_1k.rgbe.png
  icons/*.svg
  fonts/noto-sans-tc-ui.{json,png}
```
- desktop 維持 `classloaderAssetPath` 預設 `""`。實機驗證：設成 `"assets"` 時，kool 內建字型路徑變成 `assets/fonts/fira-sans-regular.png`，classpath 與 `./assets` 都找不到，UI 沒有字。選用的 `.glb` 放檔案系統 `koolApp/assets/`（`NativeAssetLoader` 的預設 base）。JS 的 resources 仍由 webpack 放到輸出根目錄。
- 一律用 **.glb**（單檔、內嵌貼圖）。轉檔用 Blender 4.x：套用縮放、+Y up、移除不需要的動畫、貼圖縮到 1k、輸出 glTF Binary。
- `AssetCatalog` 用 `FrontendScope.launch { Assets.loadGltfModel(...) }` 背景載入；載入前先顯示程序幾何，載好再替換（不阻塞第一幀）。載入失敗只寫 log，保留程序版。

## 3. GltfLoadConfig 建議值（已編譯）
```kotlin
GltfLoadConfig(
    materialConfig = GltfMaterialConfig(
        shadowMaps = shadowMaps, environmentMap = env,
        isDeferredShading = true, fixedNumberOfJoints = 32,   // 所有螞蟻模型同一 shader 變體
    ),
    instanceLayout = InstanceLayouts.ModelMat,   // 要 tint 時改自訂 Struct（ModelMat + color）
    loadAnimations = true, applySkins = true, mergeMeshesByMaterial = true,
)
```
tint：glTF 材質固定色，要每隻蟻群色時用 `pbrBlock = { prim -> color { instanceColor(MyInstLayout.color) } }`（`GltfConfig.kt:25` 的 `pbrBlock` 參數；用法推測）。

## 4. 程序生成退路（無素材也能交付）
`ProceduralMeshes.kt`（MeshBuilder；`K/scene/geometry/MeshBuilder.kt`）：
- **螞蟻**：頭/胸/腹三個橢球（`icoSphere` 再非等比縮放）＋ 6 腳 × 3 段圓柱 ＋ 2 觸角（彎曲管）＋ 顎。腳是**獨立實例**：每隻螞蟻 1 個身體實例 + 6 個腳段實例，CPU 依步態相位算腳的矩陣（三腳架步態：L1/R2/L3 與 R1/L2/R3 交替）。600 蟻 × 18 腳段 ≈ 1 萬實例，CPU 每幀算得完（推測）。
- **石頭**：icoSphere(steps=2) + 3D 噪聲位移 + 壓扁；5 個變體。
- **草葉**：彎曲三角帶（5–7 段），寬度漸細；頂點色由根部深綠到尖端黃綠。
- **花**：5–6 片橢圓花瓣 + 中心球 + 莖。
- **時鐘石**：superellipsoid（指數 2.5）壓扁成 1:0.3，凹槽是內縮 0.8 的負形（用兩層 mesh + 深色內層近似）。
- **糖滴**：icoSphere + emissive；**蛋白**：不規則塊。

## 5. 骨架動畫 × 實例化 = 姿態桶
事實：kool 的 skinned mesh 的關節矩陣屬於 mesh（`Mesh(geometry, instances, morphWeights, skin)`，`GltfFile.kt:601-602`），所有實例共用一組 → 同一 mesh 的所有螞蟻踏步完全同步（推測，依結構判斷）。
作法：
1. 載入同一模型 **K 次**（K = 4/6/8 依品質），每份各自 `enableAnimation(walk)`，時間相位錯開 `i/K` 個週期。
2. 每隻螞蟻依 `(id * 黃金比例) mod 1` 決定桶；**速度**決定動畫播放率 → 改成「每個桶有固定播放率」，螞蟻按速度分配到「慢/中/快」× 相位的桶（例如 3×3=9 桶）。
3. 靜止/工作狀態（挖掘、餵食、戰鬥）用另一組動畫桶（若模型有該動畫），沒有就用 walk 停在固定幀。
4. 化身螞蟻主角：獨立一份不實例化的模型，自己的 `AnimationController`（`K/scene/animation/AnimationController.kt:24` `state(...)` 狀態機 + `blendTransition`）。
退路：模型無動畫或骨架出錯 → 用 §4 的程序螞蟻（腳獨立實例，動作最自然且不需骨架）。**建議 P1 先做程序螞蟻**，glTF 螞蟻是加分項。
