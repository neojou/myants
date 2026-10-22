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
- Classic 2D 仍是單一正交場景。左側 74% 是可平移縮放的世界，4px `#333` 分隔線，右側 26% 是固定的 colony 0 巢穴。3D 模式另開一個場景，用 `Scene.isVisible` 切換，不重啟行程。隱藏的場景不會被 collect，所以模擬 tick 只在當下可見的世界場景跑一次。
- 世界以 letterbox 放進左窗。縮放 1–6，滾輪以游標為錨點。
- 品質只改 `maxAnts` 與繪製成本（80 / 120 / 300 / 600 / 1200）。費洛蒙解析度留在 `SimConfig`，切品質不呼叫 `rebuildPheromoneGrids`。來源會重蓋格，因為品質會改 `resolutionScale`。
- 費洛蒙寫回使用 `arr[i] = (arr[i].toDouble() * decay).toFloat()`，對齊 JavaScript `Float32Array` 的存回捨入。
- 費洛蒙疊加是 `BLEND_ADDITIVE`（`ONE, ONE`），alpha 不會被採用。顏色先乘上 `min(1, 0.85 * strength)`。通道是 `R=√(protein+sugar+danger)`、`G=√sugar`、`B=√(home+danger)`，門檻 0.01。
- kool 的 GL 載入器會用影像位元組的雜湊當貼圖 id。全零的緩衝區不論尺寸都是同一個 id，室外與巢穴會共用一張 GPU 貼圖，後者接著被釋放。動態貼圖改用每個圖層兩個固定 id 輪流上傳。
- 世界的滑鼠處理放在 UI 場景之後的空場景。面板用 `onPointer` 吃掉自己範圍內的指標，避免按鈕點擊同時作用在世界上。
- Classic 2D 的日夜仍是左窗一張半透明黑四邊形（`BLEND_MULTIPLY_ALPHA`）。3D 世界改由方向光與天空亮度表現，不疊那張黑片。
- 時鐘是系統牆鐘 `HH:MM` 的七段顯示，畫在世界座標，會跟著平移縮放。來源 `drawClockStone` 也用 `new Date()`。左側 3D 的七段平貼在時鐘石頂面，俯視才讀得到；冒號在本地偶數秒亮、奇數秒暗；時與分跟本地時間改。Classic 2D 的冒號不閃，數字仍是直立的。
- Desktop 截圖把畫面複製進一張持久的 `OffscreenPass2d`（`shot-grab`），再用 `Texture2d.download` 與 `ImageIO` 寫 PNG，檔名 `antsim_seed{seed}_t{age}.png`。`--shot-out=` 可改路徑。Pass 層級的 single-shot 會在同一個 `update` 裡被清掉。畫面複製貼圖沒有 `TRANSFER_SRC`，不能直接 `download`。讀回必須排在 `BackendScope`：`FrontendScope` 碰上 `asyncSceneUpdate` 時，`mapReadbacks` 會在對應完成前清掉佇列，`await` 永不返回。kool 會開 AWT headless，這台機器上 `ImageIO` 仍寫得出 PNG。HUD 文字是正的，寫檔不翻 Y。JS 仍只寫 log：`PNG encoder is not available in this build`。
- `classloaderAssetPath` 留空。設成 `assets` 會讓內建 Fira 字型路徑變成 `assets/fonts/...`，桌面 UI 沒有字。選用的模型放在 `koolApp/assets/`。
- 設定與調參覆寫共用一個儲存。Desktop 是 `~/.myants/antsim.ui.v1.txt`，JS 是 `localStorage` key `antsim.ui.v1`。覆寫行是 `ov:path=value`。來源把覆寫放在另一個 `antsim.overrides.v1`。`applyTunerAction` 只改記憶體中的 `SimConfig`；存檔由 `AppSession.save()` 負責。
- 啟動參數 `--seed=`、`--colonies=`、`--quality=`（JS 用網址 query）覆蓋已存的族群數與品質。
- 按鈕用 ASCII（`||`、`>|`、`?`、`S`、`P`、`E`）。Fira MSDF 沒有 emoji，德文變音符號有。WebGL 與 Bloom 核取方塊顯示但鎖定。滑桿沒有 step，量化做在 `onChange`。
- 介面語言可選 English、繁體中文、日本語、Deutsch，預設 English。入口是工具列右上角的地球圖示；Grafik 不再另有一排語言按鈕。舊設定檔的 `EN`／`ZH_TW`／`DE` 仍有效，沒有 `language` 或不認得的值用 English。切換語言不重建世界。選了哪一種，工具列、HUD、Grafik、Params、分析建議、Inspect、Restart 對話框與敵群核取方塊就整面用那一種。品質選項顯示語系名稱，存檔仍是 `ULTRA_LOW` 這類 enum 名。分析的套用按鈕仍是 `path: 1.0 → 0.7`。`3D`、`SSAO`、色碼、版號與快捷鍵字元每種語言都一樣。`Based on AntSim_V2 by nilsgollub (MIT)` 仍是英文。英文與德文用 Fira。繁中與日文用 Noto Sans CJK 的 MSDF 圖集（只含介面用字）；HUD 數字欄位仍用 Fira。Stats 圖上的點陣字沒有中日文，繁中和日文時線尾改畫英文短名，圖下文字仍是字串表原文。下雨在骷髏頭右邊，仍是天氣開關。
- Inspect 點到世界螞蟻、食物、昆蟲、巢內螞蟻、蟻后、沒被搬走的卵／幼蟲／蛹、房室、石頭（含中央時鐘石）、模擬草地、裝飾落葉，或空白地面後，HUD 改顯示類型與狀態列，約每模擬秒重讀該物件，不寫回 sim。物件離開原本的清單就清空。空白地面顯示該點各群室外費洛蒙；再點一次空白地面取消選取，點在地圖外清空。幼體的命中半徑是 12，小於螞蟻的 30；蟻后仍是 40，而且先於幼體。房室只在前面都沒點中、且點在房間半徑內時出現。地形在螞蟻、食物、昆蟲都沒點中之後才測：石頭用自身半徑，草地與落葉用 8，再來才是費洛蒙。英文一行仍在，只是不再畫出來。左側世界的螞蟻、食物、昆蟲、石頭、草地、落葉與費洛蒙用同一枚地面圈。食物與昆蟲的圈比物件輪廓再大一點；世界螞蟻的圈大小不變。石頭圈是半徑的 1.5 倍，草地是大小的 2 倍，落葉是固定小圈，費洛蒙也是固定小圈。巢內螞蟻、蟻后、幼體與房室在右側巢畫同色的畫面圈，不畫到左側世界。Classic 2D 不畫圈。設計稿的雙環呼吸高亮還沒做。有世界座標的選取可從 HUD 按跟隨，相機每幀以 0.12 的比例移向該點；巢內選取沒有這個按鈕，也不移動左側相機。物件離開、世界螞蟻死亡、再點空白、點到地圖外或拖曳都會停止跟隨。工具列、HUD 與面板擋住指標時，該下點擊不送進世界或巢穴，所以暫停與速度按鈕不會清掉選取。
- 快捷鍵：空白鍵暫停、`.` 單步、`+`／`-` 改速度、`1`–`4` 選工具。AntSim 的 `+`／`-` 是縮放；#11 改成速度。`=` 仍放大、`_` 仍縮小。`[`、`]` 原作沒有，評估後不加。介面沒有文字欄，所以這些字元不會打進輸入框。
- HUD 的 Protein / Sugar 用 `floor`，與來源 `Math.floor` 相同。左上角 HUD 不印版號。版號在遊戲內 About、視窗標題，以及 macOS 系統 About。
- `HelloCubeScene.kt`、`AppUi.kt` 與 `KoolStudyApp.kt` 已移除。入口是 `myantsApp()`。About 在 `AntSimUi.kt`，含 `Based on AntSim_V2 by nilsgollub (MIT)`。
- 3D 世界在 `com.neojou.myants.render3d`。模擬套件不呼叫渲染，渲染不呼叫 `Rng`。裝飾用的高度、散布、雨與電影鏡頭用 `kotlin.random.Random`。
- 正常鏡頭是俯視正交，俯角預設 -70°（-90° 到 -60°），平移與滾輪縮放鎖在游標打到的地面點，不旋轉。`CameraMode.ANT_POV` 只留介面，導演拒絕切換。
- Bloom 只在 `KoolSystem.features.computeShaders` 為真且後端類名含 Vk 或 WebGpu 時開啟。Desktop GL 即使回報 computeShaders 也不開。雨與選取環畫在 deferred output quad 之後。
- 品質檔位的模擬上限沒變。`QualityPreset` 只改渲染。前 5 秒平均低於 40 fps 時只加 `renderTierBias`，不呼叫 `setQuality`。手動選品質會把 bias 歸零。陰影貼圖大小在第一個 3D 影圖建立時固定為 2048；Ultra 的 4096 與 zoom≥4 的 CSM 這版沒有依檔位重建。
- 左側出洞後的 3D 工蟻讀 `models/ants/worker_black.glb`，兵蟻讀 `models/ants/soldier.glb`（自製 glTF 2.0，頭朝 +X）。兵蟻的頭是紅色，身體跟群色。任一檔載入失敗，該階級退回方塊蟻。右側巢穴與 Classic 2D 不變。步行仍用姿態桶，支點來自模型的胸節。頭與身體分成兩份實例，所以頭可以另外上色。
- 左側獵物、捕食者、蜘蛛、甲蟲、瓢蟲讀 `models/insects/` 下對應的自製 glb。蜘蛛八腳。瓢蟲的圓背法線朝外，3D 用飽和紅，俯視被正午打亮後仍是猩紅，頭和斑點是黑的。Classic 2D 仍用調色盤的紅。甲蟲的室外模型是鍬形蟲剪影，頭近黑，鞘翅是深栗褐的整片盾。缺檔退回方塊。蚜蟲仍是 `aphid_green.glb`。Classic 2D 不變。
- 地面糖堆、蛋白堆，以及嘴上搬運的糖和蛋白，讀 `models/food/sugar.glb` 與 `protein.glb`。屍體仍是球。缺檔退回球。
- 小石與障礙石共用 `models/nature/pebble.glb`，落葉是 `leaf.glb`，時鐘石體是 `clock_stone.glb`。數字仍是七段方塊。缺檔退回方塊或圓球。左側 3D 的草是貼地的三葉草叢，約五叢有兩叢在同一點開白花；缺檔退回方柱和彩球。Classic 2D 的草仍是綠色橢圓。Medium 300 蟻是否 ≥ 60 fps 這次沒有重測。
- 3D 巢穴畫在 present 場景，用跟 Classic 2D 相同的螢幕正交相機（近裁剪面必須是負的，否則 z=0 的四邊形會被裁掉）。kool 0.19 的 `AttachmentConfig.singleColorDefaultDepth(format, clearColor)` 不使用傳入的清除色，一律清成黑；在 MoltenVK 上把 `OffscreenPass2d` 的顏色貼圖貼回畫面時，取樣結果也是黑，剖面幾何沒有出現在那張貼圖裡。因此不走離屏貼圖，直接在右側 26% 畫土塊、隧道節點與蟻后／幼體／巢內蟻。Unlit 顏色要設 `LinearToSrgb`，否則會以線性值寫進交換鏈，土色會暗成接近黑。`nest3dActive` 為假時才回到 2D 巢穴疊層。巢穴點擊只在 3D 巢穴有效時做 inspect。
- 這台 Mac（Apple M4 Pro、MoltenVK api 1.2.296）已開過 Vulkan 視窗，BloomPass 有建立，`ImageIO` 有寫出 PNG。尚未量到的項目：Medium、雙群約 300 蟻是否 ≥ 60 fps，以及 Ultra 1200 蟻是否 ≥ 30 fps。截圖當下族群 34，HUD 約 43–48 fps，不能當成達標。GL 3.3 context 沒測。費洛蒙貼圖的上下方向，以及螞蟻朝向是否與移動方向一致，靜態圖看不出來。對照見 `docs/programming/3d_refs/shots/REVIEW.md`。

## 黃金對照

`runHeadless(12345, n)` 與 cefa083 的 headless 指標一致：

| ticks | population | sugar | protein | queenEnergy | extraChambers | brood |
| --- | --- | --- | --- | --- | --- | --- |
| 1000 | 21 | 673 | 238 | 2201 | 2 | 23 |
| 2500 | 23 | 711 | 204 | 2242 | 2 | 29 |

`HeadlessInvariantsTest.selfCheckAgainstTypescriptGolden` 印出這兩行，並斷言同一種子重跑相等。數字若以後分叉，以這份文件記錄差值，並維持 Kotlin 自身可重現，不為了對齊去改模擬。
