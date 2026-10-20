# myants

換手時只更新這份。設計長文在 `docs/programming/`，索引見 `docs/programming/README.md`。

## 目標

初步目標是復刻 [AntSim_V2](https://github.com/nilsgollub/AntSim_V2)（nilsgollub，MIT）。實作用 Kotlin Multiplatform 與 kool 0.19.0：desktop JVM 是主要目標，js 要能編譯、能跑基本版。模擬行為、操作與英／德 UI 文案跟來源；刻意差異只記在 `docs/programming/myants_antsim_deviations.md`。

左側世界視窗要做成高品質的 3D 環境，畫面力求最好。角色與物品以 glTF 2.0 設計。缺檔或載入失敗時仍要有程序幾何退路，沒有任何 `.glb` 也要能跑。

將來可能做第一人稱：玩家化身一隻螞蟻，直接進入左側 3D 場景，去搬糖、搬蛋白質或作戰。相機可以自由旋轉的只有這個模式。玩家意圖以後才交給模擬，相機與渲染不寫 sim。設計預留在 `docs/programming/camera_modes_and_ant_pov.md`（該文件的 P6）。`CameraMode.ANT_POV` 目前只留介面，`CameraDirector` 拒絕切過去。沒有寫進下面的 Next 之前，不要實作第一人稱，也不要新增會改模擬結果的玩家操作。

## Status

- 日期：2026-10-20
- Agent：Grok Build（只補交接文件；下一個執行者尚未指定）
- 版本：0.14.0（`gradle.properties` 的 `app.version` 與 `AppVersion.NAME`）
- Branch：`main`，與 `origin/main` 同一程式 commit
- SHA：`2859ffe`（3D 場景無法平移／縮放的修復）。本檔與 `AGENTS.md`、`docs/programming/README.md` 是這次未 commit 的文件。
- 工作樹：程式與 `2859ffe` 相同。未 commit 的是交接文件。

已落地：

- 左側 3D 俯視世界；Grafik 可切 Classic 2D，不必重啟。
- 室外工蟻 `koolApp/assets/models/ants/worker_black.glb`、蚜蟲 `koolApp/assets/models/insects/aphid_green.glb`。載入失敗退回程序網格。巢穴與 Classic 2D 仍是程序繪製。
- 石頭、落葉、食物、時鐘數字等仍是程序幾何，還沒換成 glTF。
- 右側 3D 巢穴直接畫在 present 場景，不走會變黑的離屏貼圖。
- 第一人稱未做。

未達標（沿用 deviations，這次沒有重測）：Medium、雙群約 300 蟻是否 ≥ 60 fps；Ultra 1200 蟻是否 ≥ 30 fps；GL 3.3 context；費洛蒙貼圖上下與螞蟻朝向是否與移動一致（靜態圖看不出）。

## Now

沒有進行中的功能。這次只建立交接文件。

## Next

（空。擁有者指定下一件之前不要開工。寫進來時每項一行，並寫完成條件。）

## 不要動

- `com.neojou.myants.sim` 的行為。`HeadlessInvariantsTest` 黃金值不變：種子 12345，1000 tick 為 pop 21、sugar 673、protein 238、queenEnergy 2201、extraChambers 2；2500 tick 為 pop 23、sugar 711、protein 204、queenEnergy 2242。
- kool 0.19.0、Kotlin、Gradle、JDK 的版本。不加 wasm、不加 Compose。
- UI 文字維持 AntSim 的英文／德文。
- 版本只透過 `./configure.sh version` 改。
- 第一人稱與任何會寫回 sim 的玩家操作，直到出現在 Next。

## Traps

每條的出處在 `docs/programming/myants_antsim_deviations.md`，除非另註。

- 費洛蒙寫回用 `(arr[i].toDouble() * decay).toFloat()`，對齊 JS Float32 存回。不要寫 `arr[i] *= decay.toFloat()`。
- 正交地面拾取用 `OrthoGround`（`TopDownCamera.screenToGround`）。kool 0.19 的 `unProjectScreen` 在這台正交相機上會算出 NaN，寫進 `camX`/`camY` 後延遲光照會整片黑。
- 3D 巢穴不要改回 `OffscreenPass2d` 顏色貼圖。MoltenVK 上該貼圖取樣是黑；現在直接畫在 present 場景右側 26%。Unlit 要 `LinearToSrgb`。
- `classloaderAssetPath` 留空。設成 `assets` 會讓內建 Fira 字型路徑壞掉，桌面 UI 沒有字。模型放 `koolApp/assets/`。
- Mac 預設 Vulkan。`--backend=gl` 不是預設。Bloom 只在 compute shaders 可用且後端是 Vulkan 或 WebGPU 時開。
- glTF 只覆蓋室外工蟻與蚜蟲。缺檔要退回程序網格並寫 log，不能讓啟動失敗。
- 渲染不得呼叫 sim 的 `Rng`。裝飾用 `kotlin.random.Random`。

## Log

- 2026-10-20 Grok Build：建立本檔與 `docs/programming/README.md`，並把產品目標寫進 `AGENTS.md`。程式仍是 `2859ffe` / 0.14.0。Next 空白。
