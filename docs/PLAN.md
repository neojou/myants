# myants

換手時只更新這份。設計長文在 `docs/programming/`，索引見 `docs/programming/README.md`。

## 目標

初步目標是復刻 [AntSim_V2](https://github.com/nilsgollub/AntSim_V2)（nilsgollub，MIT）。實作用 Kotlin Multiplatform 與 kool 0.19.0：desktop JVM 是主要目標，js 要能編譯、能跑基本版。模擬行為、操作與英／德 UI 文案跟來源；刻意差異只記在 `docs/programming/myants_antsim_deviations.md`。

左側世界視窗要做成高品質的 3D 環境，畫面力求最好。角色與物品以 glTF 2.0 設計。缺檔或載入失敗時仍要有程序幾何退路，沒有任何 `.glb` 也要能跑。

將來可能做第一人稱：玩家化身一隻螞蟻，直接進入左側 3D 場景，去搬糖、搬蛋白質或作戰。相機可以自由旋轉的只有這個模式。玩家意圖以後才交給模擬，相機與渲染不寫 sim。設計預留在 `docs/programming/camera_modes_and_ant_pov.md`（該文件的 P6）。`CameraMode.ANT_POV` 目前只留介面，`CameraDirector` 拒絕切過去。沒有寫進下面的 Next 之前，不要實作第一人稱，也不要新增會改模擬結果的玩家操作。

## Status

- 日期：2026-10-22
- Agent：Grok Build（單人模式）
- 版本：0.17.0
- SHA：848a16c7a700817d8440cc6a9bc5e70dfc5718d5（右側巢穴剖面：深土壤、房間切邊、巢內蟻分頭與身體）。本列是其後的文件 commit。v0.17.0 的 tag 與這次 M5 室外模型都還沒 push。
- 內容：M1、M2 在 v0.15.0。M3 在 v0.16.0（642f082）。0.17.0 涵蓋其後的介面語言，以及 M4 Inspect。M5 的左側室外這段在本機：工蟻與兵蟻（頭和胸分開、腰收細、肚子是一顆橢圓；兵蟻頭仍較大）、獵物、捕食者、蜘蛛、甲蟲（鍬形蟲剪影，頭近黑，鞘翅是深栗褐的整片盾）、瓢蟲、糖、蛋白、小石、落葉、時鐘石。時鐘石的數字平貼在石面，顯示本地時與分。冒號在時與分正中，每秒明暗。直畫是上下兩格，1 與 7 連成兩格。右側巢穴仍是程序繪製：深暖褐土壤、房間有切邊、巢內蟻分得開頭與身體。巢穴 glTF 沒做。Classic 2D、草與花仍是程序繪製。Inspect 不寫回模擬。模擬沒有為這隻鍬形蟲改數值。
- 下一步：#13 與 milestone M5 仍開。巢穴 glTF 沒做。Medium 300 蟻是否 ≥ 60 fps 這次沒重測。不 push、不關 issue、不改版號。
- 工作方式：`handoff.md` 第 0 節，以及 `docs/AI/How-To-Grok-build-one-AI-developing-approach.md`。

已完成、仍有效的結構：

- `AppSession` 是組合根，加上 `session/` 底下的 SimClock、CameraState、ToolController、ViewState、HudState、CinemaDirector、ShotPose、TunerOverrides、FpsCounter。
- `AntSimUi.kt` 只留組合。HUD、工具列、About、各面板在 `ui/` 與 `ui/panels/`。面板用 `session/Panel` enum。
- 指標事件走 `input/PointerRouter`。3D 地面拾取用 `TopDownCamera.screenToGround`（OrthoGround，不用 unProjectScreen）。

已落地：

- 左側 3D 俯視世界；Grafik 可切 Classic 2D，不必重啟。
- 室外工蟻 `worker_black.glb`、兵蟻 `soldier.glb`（頭和胸分開、腰收細、肚子是一顆橢圓，腿較細；顏色仍跟群，兵蟻頭仍是紅的）、蚜蟲，以及獵物、捕食者、蜘蛛、甲蟲（鍬形蟲剪影，鞘翅是深栗褐的整片盾）、瓢蟲。糖與蛋白、小石、落葉、時鐘石體也是 glTF。載入失敗退回程序網格。巢穴與 Classic 2D 仍是程序繪製。
- 時鐘石上的數字平貼在石面，是本地時與分。冒號在時與分正中，每秒明暗。直畫是上下兩格。Classic 2D 用同一組比例，冒號不閃。草、花、屍體、巢內幼體仍是程序幾何。
- 右側 3D 巢穴直接畫在 present 場景，不走會變黑的離屏貼圖。土壤是深暖褐，下半更深。房間有較亮的切邊和較暗的地板，通道較窄。巢內蟻分成腹、胸、頭，兵蟻頭是紅的。仍是程序繪製，不是 glTF。
- 第一人稱未做。

未達標（沿用 deviations，這次沒有重測）：Medium、雙群約 300 蟻是否 ≥ 60 fps；Ultra 1200 蟻是否 ≥ 30 fps；GL 3.3 context；費洛蒙貼圖上下與螞蟻朝向是否與移動一致（靜態圖看不出）。

## Now

M3 已在 origin 的 v0.16.0（642f082）。順序與範圍在 `handoff.md` 第 3 節。

| 順序 | Issue | 狀態 |
|---|---|---|
| 1 | #22 建置維護（不升級 Gradle） | 本機已做 |
| 2 | #39 `noteDrag` 測試 | 本機已做 |
| 3 | #36 Classic 2D 拆到 `classic2d/` | 本機已做 |
| 4 | #9 細粒度 state | 本機已做 |
| 5 | #18 色值與 token | 本機已做 |
| 6 | #17 按下縮放與數字欄寬 | 本機已做 |
| 7 | #19 Stats 線型 | 本機已做 |
| 8 | #24 English／繁體中文／Deutsch，預設 English | 本機已做 |
| 9 | #11 工具列分組、Restart 確認、快捷鍵 | 本機已做 |

## Next

（擁有者 Neo 於 2026-10-21 指定 M1–M5 roadmap，由 [PM] 建立；細節見 GitHub milestones 與 issues。）

| 順序 | Milestone | Issues | 完成條件 |
|---|---|---|---|
| 1 | M1 試跑流程 | #1（CI，PR #2）、#3 Theme token 規格、#4 Theme.kt 實作 | CI 綠；Theme token 落地；完整走過一次 issue→PR→QE/Artist PASS→Neo 合併 |
| 2 | M2 UI 元件圖示化 | #5 圖示規格、#6 圖示按鈕＋tooltip | `>|` 等文字按鈕換圖示，有 tooltip 與缺檔退路 |
| 3 | M3 UI/UX 與架構整理 | #7 拆 AppSession、#8 AntSimUi 拆檔、#9 細粒度 state、#10 PointerRouter、#11 工具列分組 | 行為不變、黃金值不變，檔案職責單一 |
| 4 | M4 Tool - Inspect | #12 Epic | M1–M3 大致完成才開始 |
| 5 | M5 glTF 單位模型美化 | #13 Epic | 有程序退路、效能達標 |

調整理由：
- 先 M1 驗證流程（CI + 角色審查），之後所有 PR 才有一致品質門檻。
- Theme token 先於圖示化，圖示狀態色才有來源。
- M3 架構整理放在 M4 之前：Inspect 需要乾淨的拾取（PointerRouter）與 AppSession 拆分，否則會繼續堆在 794 行的 AppSession。
- M5 主要是美術工作，可在 M3 之後與 M4 並行。
- 第一人稱仍不在 Next；Inspect 只讀 sim，不寫回。

### 流程

1. 一 issue 一分支一 PR：`feat/<issue#>-slug`（CI 用 `ci/`、文件用 `docs/`），PR 描述 `Closes #n`。
2. 完成條件：CI 綠 + issue/PR 上有 `[QE] PASS` 留言 + 畫面相關變更有 `[Artist] PASS` 留言。
3. 合併只由 Neo 執行；agent 不 push main、不 merge、不 force-push、不刪分支。
4. PM 留言、issue、PR 標題以 `[PM]` 開頭；其他角色用 `[CI]`/`[Coder]`/`[QE]`/`[Artist]`。
5. 決策紀錄見 `docs/adr/`。
6. 每個 PR 都要更新 `CHANGELOG.md` 的 `Unreleased` 區（附 PR 編號）。
7. 每個 milestone 的最後一個 PR 是 release PR：`./configure.sh version x.y.z`，標題 `chore(release): vX.Y.Z`。
8. Neo 合併 release PR 後，由 PM 在該 merge commit 建 tag `vX.Y.Z` 與 GitHub Release（內容取自 CHANGELOG；Neo 也可自己打）。版本規則見 ADR 0003（提議中）。

## 不要動

- `com.neojou.myants.sim` 的行為。`HeadlessInvariantsTest` 黃金值不變：種子 12345，1000 tick 為 pop 21、sugar 673、protein 238、queenEnergy 2201、extraChambers 2；2500 tick 為 pop 23、sugar 711、protein 204、queenEnergy 2242。
- kool 0.19.0、Kotlin、Gradle、JDK 的版本。不加 wasm、不加 Compose。
- 介面語言是設定：English／繁體中文／日本語／Deutsch，預設 English。舊檔沒有 `language` 時用 English。切換不重建世界。可見文字跟所選語言走；MIT 授權行保持英文。不要改回「只能英德混用」。
- 版本只透過 `./configure.sh version` 改。
- 第一人稱與任何會寫回 sim 的玩家操作，直到出現在 Next。

## Traps

每條的出處在 `docs/programming/myants_antsim_deviations.md`，除非另註。

- 費洛蒙寫回用 `(arr[i].toDouble() * decay).toFloat()`，對齊 JS Float32 存回。不要寫 `arr[i] *= decay.toFloat()`。
- 正交地面拾取用 `OrthoGround`（`TopDownCamera.screenToGround`）。kool 0.19 的 `unProjectScreen` 在這台正交相機上會算出 NaN，寫進 `camX`/`camY` 後延遲光照會整片黑。
- 3D 巢穴不要改回 `OffscreenPass2d` 顏色貼圖。MoltenVK 上該貼圖取樣是黑；現在直接畫在 present 場景右側 26%。Unlit 要 `LinearToSrgb`。
- `classloaderAssetPath` 留空。設成 `assets` 會讓內建 Fira 字型路徑壞掉，桌面 UI 沒有字。模型放 `koolApp/assets/`。
- Mac 預設 Vulkan。`--backend=gl` 不是預設。Bloom 只在 compute shaders 可用且後端是 Vulkan 或 WebGPU 時開。
- glTF 覆蓋室外工蟻、兵蟻、蚜蟲、其餘五種昆蟲、糖、蛋白、小石、落葉與時鐘石。缺檔要退回程序網格並寫 log，不能讓啟動失敗。
- 渲染不得呼叫 sim 的 `Rng`。裝飾用 `kotlin.random.Random`。

## Log

- 2026-10-20 Grok Build：建立本檔與 `docs/programming/README.md`，並把產品目標寫進 `AGENTS.md`。程式仍是 `2859ffe` / 0.14.0。Next 空白。
- 2026-10-22 Grok Build：改單人模式。接手點 `390d4b9`。工作方式寫進 `handoff.md` 與 `docs/AI/How-To-Grok-build-one-AI-developing-approach.md`。
- 2026-10-22 Grok Build：本機 0.17.0。M4 Inspect 的點選、狀態、選取圈與世界跟隨已落地。GitHub #12 與 milestone M4 仍開。CI、QE、Artist 還沒有。尚未 push。
- 2026-10-22 Grok Build：M5 左側室外模型在本機 `134a4b0`。時鐘數字平貼石面，頂端朝畫面上方。蟻后、卵、幼蟲、蛹沒做。#13 仍開。fps 沒重測。尚未 push。
- 2026-10-22 Grok Build：室外甲蟲 glTF 在本機 `4639420` 改為鍬形蟲剪影，頭近黑、鞘翅栗色。模擬沒改。#13 仍開。fps 沒重測。尚未 push。
- 2026-10-22 Grok Build：室外鍬形蟲鞘翅在本機 `cc3ac3b` 改成深栗褐的整片盾，尾端收圓。模擬沒改。#13 仍開。fps 沒重測。尚未 push。
- 2026-10-22 Grok Build：時鐘數字在本機 `10f4363` 把冒號放到時與分正中，1 與 7 的直畫改成兩格。模擬沒改。#13 仍開。fps 沒重測。尚未 push。
- 2026-10-22 Grok Build：室外工蟻與兵蟻在本機 `360080d` 收成火蟻的比例，腰更細、肚子是橢圓。顏色沒改。#13 仍開。fps 沒重測。尚未 push。
- 2026-10-22 Grok Build：右側巢穴剖面在本機 `848a16c` 改成深暖褐土壤、房間切邊、巢內蟻分腹胸頭。Classic 2D 與模擬沒改。#13 仍開。fps 沒重測。尚未 push。
