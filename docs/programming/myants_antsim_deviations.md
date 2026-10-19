# myants 與 AntSim_V2 的差異

對照來源是 [AntSim_V2](https://github.com/nilsgollub/AntSim_V2) commit `cefa0830dc527c758f48a785c4395a1f5b945816`。模擬在 `com.neojou.myants.sim`，不引用 kool。渲染與 UI 在 `AntSimScene.kt`、`AntSimUi.kt`、`AppSession.kt`。

## 與來源一致、容易被誤改的行為

- 世界尺寸鎖定 headless 無 DOM 的算法：1400×840，巢穴 392×840，費洛蒙格 `ceil(1400×0.3)=420` × `ceil(840×0.3)=252`。
- 障礙物數量寫死 8。`SimConfig.obstacleCount` 的 12 與來源 `CONFIG.obstacleCount` 一樣沒有被地形產生器讀取。
- 草叢在 `World` 初始化時用模擬用的 `Rng` 產生。來源註解若寫成「只給渲染」已過時。
- 巢穴高大於寬時，colony 0 入口在右側，colony 1 在左側。
- 預設族群數是 2。`SimConfig.colonyCount` 預設仍是 1，由 app 覆寫。
- `World.update` 的順序跟來源相同：時間、天氣、費洛蒙、蟻后、空間格、幼體、粒子、突襲、螞蟻、昆蟲、觀察、食物、墓地、糖、維護與挖掘。
- 重新開始不重設種子，與來源 `restartWorld` 相同。
- 電影鏡頭閒置 25 秒（來源 `pausedUntil = now + 25000`）。滅團後 480 幀（約 8 秒，且未暫停）才自動重開。
- 電影鏡頭用 `kotlin.random.Random(1)`，不推進 `Rng`。
- colony 0 的螞蟻在能量耗盡且生命歸零時，`Ant.update` 會先從 `world.ants` 移除自己，`updateAnts` 接著仍按索引移除。這與來源的雙重 `splice` 相同。Kotlin 多一道 `i < size` 檢查，避免最後一隻時丟出索引例外；JavaScript 的 `splice` 在這個位置是空操作。
- `World.kt` 裡 `Double.inc` / `Double.dec` 的擴充會被標準函式庫成員遮住。`Queen.layTimer--` 走的是成員運算。

## 刻意不同的地方

- 模擬時脈固定 60 Hz，與繪製幀率分開，每幀最多補 64 個 tick。來源是每個繪製幀跑 `floor(simSpeed)` 次，小數部分再用 `Math.random()` 決定要不要多一步。這裡的 0.5× 是每兩個 60 Hz 步進跑一次，結果可重現。
- 畫面是單一正交場景。左側 74% 是可平移縮放的世界，4px `#333` 分隔線，右側 26% 是固定的 colony 0 巢穴。kool 的 `ClearColorFill` 會清整張 framebuffer，而且這版沒有呼叫 `gl.scissor`，所以不用兩張會互相清掉的世界場景。UI 場景用預設 `ClearColorLoad`。
- 世界以 letterbox 放進左窗。縮放 1–6，滾輪以游標為錨點。
- 品質只改 `maxAnts` 與繪製成本（80 / 120 / 300 / 600 / 1200）。費洛蒙解析度留在 `SimConfig`，切品質不呼叫 `rebuildPheromoneGrids`。來源會重蓋格，因為品質會改 `resolutionScale`。
- 費洛蒙寫回使用 `arr[i] = (arr[i].toDouble() * decay).toFloat()`，對齊 JavaScript `Float32Array` 的存回捨入。
- 費洛蒙疊加是 `BLEND_ADDITIVE`（`ONE, ONE`），alpha 不會被採用。顏色先乘上 `min(1, 0.85 * strength)`。通道是 `R=√(protein+sugar+danger)`、`G=√sugar`、`B=√(home+danger)`，門檻 0.01。
- kool 的 GL 載入器會用影像位元組的雜湊當貼圖 id。全零的緩衝區不論尺寸都是同一個 id，室外與巢穴會共用一張 GPU 貼圖，後者接著被釋放。動態貼圖改用每個圖層兩個固定 id 輪流上傳。
- 世界的滑鼠處理放在 UI 場景之後的空場景。面板用 `onPointer` 吃掉自己範圍內的指標，避免按鈕點擊同時作用在世界上。
- 日夜是左窗一張半透明黑四邊形（`BLEND_MULTIPLY_ALPHA`），不是真正的乘法混合。
- 時鐘是系統牆鐘 `HH:MM` 的七段顯示，畫在世界座標，會跟著平移縮放。來源 `drawClockStone` 也用 `new Date()`。
- 沒有 PNG 編碼器。截圖按鈕只寫 log：`PNG encoder is not available in this build`。
- 設定與調參覆寫共用一個儲存。Desktop 是 `~/.myants/antsim.ui.v1.txt`，JS 是 `localStorage` key `antsim.ui.v1`。覆寫行是 `ov:path=value`。來源把覆寫放在另一個 `antsim.overrides.v1`。`applyTunerAction` 只改記憶體中的 `SimConfig`；存檔由 `AppSession.save()` 負責。
- 啟動參數 `--seed=`、`--colonies=`、`--quality=`（JS 用網址 query）覆蓋已存的族群數與品質。
- 按鈕用 ASCII（`||`、`>|`、`?`、`S`、`P`、`E`）。Fira MSDF 沒有 emoji，德文變音符號有。WebGL 與 Bloom 核取方塊顯示但鎖定。滑桿沒有 step，量化做在 `onChange`。
- HUD 的 Protein / Sugar 用 `floor`，與來源 `Math.floor` 相同。
- `HelloCubeScene.kt` 與 `AppUi.kt` 已移除。About 在 `AntSimUi.kt`，含 `Based on AntSim_V2 by nilsgollub (MIT)`。

## 黃金對照

`runHeadless(12345, n)` 與 cefa083 的 headless 指標一致：

| ticks | population | sugar | protein | queenEnergy | extraChambers | brood |
| --- | --- | --- | --- | --- | --- | --- |
| 1000 | 21 | 673 | 238 | 2201 | 2 | 23 |
| 2500 | 23 | 711 | 204 | 2242 | 2 | 29 |

`HeadlessInvariantsTest.selfCheckAgainstTypescriptGolden` 印出這兩行，並斷言同一種子重跑相等。數字若以後分叉，以這份文件記錄差值，並維持 Kotlin 自身可重現，不為了對齊去改模擬。
