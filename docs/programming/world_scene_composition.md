# 世界場景構成（sim 2D → 3D）

sim 型別在 `koolApp/src/commonMain/kotlin/com/neojou/myants/sim/`（下稱 `S/`）。尺寸：世界 1400×840（`SimConfig.width/height`），巢穴 392×840，費洛蒙格 420×252（`myants_antsim_deviations.md`）。

## 1. 座標與尺度

| sim | render3d |
|---|---|
| `x` (0–1400, 右) | `X = x` |
| `y` (0–840, 下) | `Z = y`（相機從 +Y 往下看、畫面上方 = -Z；這樣 sim 的「下」就是畫面下方，不必翻轉） |
| — | `Y = groundHeight(x, y)`，振幅 ≤ 6 unit（極淺起伏，不影響俯視可讀性） |
| `angle`（rad，sim 慣例） | 繞 Y 軸 `-angle`（推測：依 sim 的 atan2(dy,dx) 與 Z 向下的關係，要以一隻往右走的螞蟻驗證方向） |
| `sizeVar` | 模型等比縮放 |

1 unit ≈ 0.5 mm（見 overview §2.4）。`groundHeight` 只用於渲染，由渲染端固定種子的 value noise 產生；障礙物/時鐘石周圍做局部抬升（石頭陷進土裡）。

## 2. 圖層與實體

| 實體 | sim 來源 | 3D 表現 | 材質/光 | 實例化 |
|---|---|---|---|---|
| 地面 | — | 一張 1400×840 網格（P0 平面；P2 細分 280×168 + 高度） | `KslPbrSplatShader` 或自訂 deferred shader：2–3 層 Poly Haven 土壤（`forest_ground_04`、`brown_mud_leaves_01`、`pebble_ground_01`）+ 渲染端 splat 噪聲；濕潤度 uniform | 否 |
| 費洛蒙 | `colony.outdoorField`（4 場）| 地面 shader 取樣費洛蒙貼圖（420×252，線性濾波）→ 加到 emission；顏色公式沿用 `R=√(protein+sugar+danger)`、`G=√sugar`、`B=√(home+danger)`、門檻 0.01、強度×Pheromon-Stärke | emissive → bloom | — |
| 草叢（sim） | `world.grass`（200 叢 `GrassTuft(x,y,size,angle)`，`S/World.kt:36,122`）| 每叢 5–12 片 3D 草葉（彎曲帶狀 mesh，高 = size×12–25 unit），部分帶穗（AntSim 的蒲草穗） | 雙面、alpha mask 或純幾何；風擺在頂點 shader（參考 `DEMO/physics/terrain/GrassShader.kt`）| 是 |
| 細草（渲染） | — | 渲染隨機散布 2k–20k 片短草（依品質），避開障礙物/時鐘石/巢口 | 同上，不投影 | 是 + LOD |
| 小石 | — | 3–5 種石頭 mesh，1k–5k 顆，隨機旋轉縮放、半埋 | PBR 灰褐 | 是 |
| 落葉/枯枝 | — | 平面彎曲葉片，數百 | albedo 貼圖 + alpha mask | 是 |
| 花 | — | 小花 50–200 朵（粉/黃/白，對應截圖） | 花瓣輕微 emissive（夜間微光，選配） | 是 |
| 障礙物 | `terrain.obstacles`（8 顆，`S/Terrain.kt:7-19`）| 大石頭（glTF boulder，按 `radius` 縮放），帶苔蘚（AntSim 截圖有綠色苔斑） | PBR + 苔蘚 splat | 否（少量） |
| 時鐘石 | `terrain.clockStone`（半徑 150，`SimConfig.clock.radius`）| 扁平鵝卵石（程序：壓扁的 superellipsoid + 噪聲位移），中央凹槽；數字 `HH:mm` 牆鐘 | 石：PBR；數字：emissive 米黃 `#E9C46A` 類（截圖約 `#E8C070`），七段 mesh（現有 `SEGMENTS` 表）或 `MeshBuilder.text(font){…}`（`K/scene/geometry/MeshBuilder.kt:878`，MSDF 需搭配字型 shader，推測） | 否 |
| 食物（糖） | `world.foods`（`FoodKind.SUGAR`，`amount/maxAmount`）| 發光蜜滴/糖粒堆：半透明感用不透明 + 強 emissive 模擬；尺寸 ∝ √(amount/max) | emissive 金色 `#FFD700`，每顆一盞小點光（品質 High+，最多 32 盞） | 是 |
| 食物（蛋白） | `FoodKind.PROTEIN` | 肉塊/死蟲（程序）| emissive 紅 `#CD5C5C` 較弱 | 是 |
| 屍體 | `FoodKind.CORPSE` / `graveyard` | 螞蟻模型翻倒、去色 | — | 是 |
| 螞蟻 | `colony.ants`（`location == WORLD`）| glTF 或程序螞蟻，姿態桶動畫；搬運物掛在顎前 | 蟻群 tint（`#c4703a` 可調）、兵蟻頭 `#c01810`、敵群另一色 | 是（姿態桶）|
| 昆蟲 | `world.insects`（`PREY, PREDATOR, APHID, SPIDER, BEETLE, LADYBUG`）| 各一種模型（CC0 掃描模型減面 or 程序），顏色沿用 AntSim（見 `antsim_ui_ux_ref.md` §2）| PBR | 是 |
| 粒子 | `world.particles`（`DEFAULT, BLOOD, DUST`）| 小 billboard / 小球；血滴落地留 decal 淡出 | — | 是 |
| 雨 | `world.raining` | 雨絲（拉長 billboard，速度 `env.rainSpeed`）、地面漣漪貼花、濕潤 | forward 加法 | 是 |
| 巢口 | `colony.entranceWorld` | 地面凹陷 + 暗色土丘環 | — | 否 |
| 選取 | Inspector 目標 | 地面光圈（淡紫 `#EADDFF` 外環 + `#6750A4` 內環，緩慢呼吸） | forward 加法 | 否 |

隨機規則：凡是 sim 沒有的裝飾（細草、小石、花、落葉、雲、螢火蟲）都用 `kotlin.random.Random(seed xor 常數)`，**絕不呼叫 `Rng`**；重新開始（Restart）時用新 sim 種子重新散布，使畫面與世界一致。

## 3. 地下巢穴視圖

AntSim：右側 26% 的地下剖面（深褐、樹根、房間、入口隧道、蟻后房）。myants 3D 版：

- **方案 A（選定）：剖面 3D**。`NestRenderer` 建一個獨立 `OffscreenPass2d`（大小 = 右欄像素 × 品質比例），內含：
  - 背景：土壤剖面（垂直平面，PBR 土壤 + 分層顏色；深處更暗）。
  - 隧道與房間：依 `nest.nodes`（`NestNode(x,y,radius,type)`）與 `nest.chambers`（`Chamber(x,y,radius,type)`，`S/Nest.kt:14-35`）用 extrude 的「凹洞」mesh（圓角、內壁較亮、邊緣 AO），深度 30 unit，像玻璃蟻巢的切面。
  - 樹根：渲染端 L-system 曲線管（截圖右側的細長根），固定種子。
  - 內容：螞蟻（巢內 `location == NEST`）、蟻后、卵/幼蟲/蛹（`BroodStage`）、儲藏（糖粒堆發光）、墓地。
  - 光：一盞暖色點光在入口、各房間小點光（蟻后房最亮）；forward `KslPbrShader`，不用 deferred（小畫面，省資源）。
  - 相機：正交、正面，固定，無互動（同 AntSim）；點擊可 Inspect（射線 → 巢內座標）。
  - 顯示：ui2 `Image(pass.colorTexture)` 填滿右欄。
- **方案 B（退路）**：維持現有 2D 巢穴繪製，只換配色與質感（漸層、根、房間陰影）。

## 4. 光源預算

| 類型 | 數量 | 備註 |
|---|---|---|
| 太陽/月 | 1（投影） | `maxGlobalLights = 2`（太陽 + 備用） |
| 食物點光 | 0 / 16 / 32 / 64（依品質） | `deferred.dynamicPointLights.addPointLight { … }`（`K/pipeline/deferred/DeferredPointLights.kt:75`），半徑 40–80 unit |
| 選取光 | 1 | 選配 |
| 巢穴點光 | 3–8 | 只在 Nest pass |
