# myants：以 Kotlin + kool 重製 AntSim_V2 的移植計畫

目前的程式入口是 `myantsApp()`（`MyantsApp.kt`）。下面記的是 0.1.0 移植當下，不是現在的畫面。

## 0. 現況（已查證，2026-10-19，commit `b71f65d` "Version 0.1.0 - kool hello world"）
- kool 0.19.0、Kotlin 2.3.0、Gradle 9.3.0、`jvmToolchain(25)`；module `koolApp`；targets `jvm("desktop")` + `js`。
- 內容：`HelloCubeScene.kt`（旋轉方塊）、`AppUi.kt`（左上 About 按鈕 + About 面板：APP_NAME、Version、OK）、`AppVersion.kt`、`KoolStudyApp.kt`（`koolStudyApp()` 入口，名稱沿用 kool_study）、`MyLog`/`SystemSettings`/`MyTopMenuItem`、desktop `MacOsApplicationName.kt` + `AboutPanelCallback.java`、`configure.sh`（改名/版本）。
- **版本不一致**：commit 訊息寫 0.1.0，但 `AppVersion.NAME = "0.0.1"`、`gradle.properties app.version=0.0.1`。
- 在 Linux 測試機 `./gradlew compileKotlinDesktop compileKotlinJs` **成功**（JDK 25，需設 JAVA_HOME）。

## 1. 原則
- 不改 kool/Kotlin/Gradle/JDK 版本與 build 配方（見 `kool_setup_ref.md`）。
- 每個 phase 結束都必須：`./gradlew compileKotlinDesktop compileKotlinJs desktopTest` 綠燈、desktop 可跑、js `jsBrowserDevelopmentRun` 可跑。
- 保留 About（加上 "Based on AntSim_V2 by nilsgollub (MIT)"）。版本號用 `./configure.sh version` 改。
- 新增 `docs/programming/THIRD_PARTY_NOTICES.md`（AntSim MIT 全文）與 README 致謝。
- UI 文字照 AntSim 原文（英/德）。

## 2. 一次到位目標 + 退回階段
**One-shot 目標**：Phase 1–4 全做完 = 雙群 AntSim 主要玩法在 kool 上可玩，UI 對齊。若一次做不完，依序退回到最後一個綠燈 phase 交付。

### Phase 0 — 腳手架（版本 0.1.0 → 0.2.0）
- 刪除 HelloCubeScene（或保留在 debug flag 後），建立 `sim/ render/ ui/` 套件；`Rng.kt` + commonTest（對照 node 產生的 mulberry32 序列，seed 1/12345/0）。
- 視窗 1600×900，74/26 分割兩個視圖；空的 HUD。
- 驗收：測試綠、About 仍可開、版本 0.2.0 顯示。

### Phase 1 — 核心 sim + 地表渲染（MVP，0.3.0）
- 移植 config（SimConfig data/object，含全部預設值）、PheromoneGrid（含 float 精度規則）、Terrain（12 石＋時鐘石）、Food、World（單群）、Ant（FORAGING/RETURNING/HARVESTING/HUNGRY/IDLE/NURSING 簡化）、SpatialGrid。
- 渲染：正交相機、地表貼圖、費洛蒙疊圖、蟻實例、食物、石頭、時鐘石 HH:MM。
- 控制：Speed −/+、Pause（Space）、Step、Zoom +/−/reset、拖曳平移、滾輪 zoom-to-cursor（1.0–6.0）、Tool：Inspect/Sugar/Protein。
- HUD：FPS/Population/Protein|Sugar/Queen Age/version。
- 驗收：螞蟻自發形成糖路線（截圖可見綠色/黃色光帶）；PheromoneGridTest 數值對照 TS 測試（`PheromoneGrid.test.ts` 案例逐一移植）；同 seed 兩次 run 1000 tick 結果相同。

### Phase 2 — 巢穴與生命週期（0.4.0）
- Nest（房間圖、角色、挖掘）、Queen、Brood（卵/幼蟲/蛹、兵蟻營養）、多型、交哺、NURSING/TRANSPORTING/DIGGING/RESTING、巢穴視圖渲染、墓地。
- 驗收：headless 20000 tick 蟻群存活、人口在區間（照 `headless.soak.test.ts` 的不變量）；巢穴視圖顯示蟻后/育幼/存糧。

### Phase 3 — 天敵、戰爭、環境（0.5.0）
- Insect 全類型、combat/mob/grapple/FLEEING/ATTACKING、PATROLLING/RAIDING、第二群（`--colonies=2` / js `?colonies=2`，預設 2）、Rivalenkolonie 勾選、Restart、Enemy 工具、日夜、下雨（🌧）、滅亡自動重開。
- 驗收：雙群 30 分鐘 headless 不崩潰；突襲事件有發生（計數 > 0）。

### Phase 4 — 面板與一致性細節（0.6.0）
- Grafik 面板（Performance-Modus、Pheromon-Spuren/Stärke、Ameisen-Farbe、Tag/Nacht、Kino-Kamera；WebGL/Bloom 兩項：顯示但 disabled 或省略並註明）、Live-Parameter 23 滑桿 + Reset、Verlauf 折線圖、Inspector、圖示 atlas、設定持久化（desktop 檔案／js localStorage）、URL/CLI 參數 seed/colonies/quality、截圖。
- 驗收：parity checklist（§4）全勾。

### Phase 5（選配）— Analyse 顧問、電影相機、bloom 近似、3D tilt flourish（預設關）、golden snapshot 與 TS 版逐位元對照。

## 3. 測試策略
- commonTest：Rng 序列、PheromoneGrid、Brood、SpatialGrid、headless 不變量。
- 選配 bit-exact：在 /workspace 用 node 跑 TS `runHeadless(seed, ticks)` 輸出 metrics，與 Kotlin 對照（需要 World 尺寸固定 1000×600 aspect 的無 DOM 模式）。

## 4. Parity checklist
- [ ] 74/26 版面、分隔線 #333、巢穴底 #1a1a1a
- [ ] HUD 四行格式完全一致 + 版本
- [ ] Speed 6 檔、Pause/Space、Step、Zoom 按鈕與 +/- 鍵、Reset、拖曳、滾輪 zoom-to-cursor、zoom 1–6
- [ ] Tool：Inspect/Sugar/Protein/Enemy/Rain
- [ ] Restart、Rivalenkolonie、預設雙群
- [ ] Grafik/Params/Stats/(Analyse) 面板與預設值、範圍、step
- [ ] 費洛蒙顏色公式、強度滑桿
- [ ] 蟻色 #c4703a、兵蟻紅頭、搬運圖示（糖金/蛋白紅）
- [ ] 時鐘石 HH:MM
- [ ] 日夜 6000 tick、雨
- [ ] seed 重現
- [ ] 全部 config 預設值一致（逐項對照 `antsim_simulation_model_ref.md` §7）

## 5. 風險
| 風險 | 影響 | 對策 |
|---|---|---|
| 規模大（~1.1 萬行 TS，antStates 1260 行）| 一次做不完 | 分 phase、每 phase 綠燈可交付 |
| bit-exact 決定性難（float32 舍入、rand 順序、陣列迭代/刪除順序）| golden 對不上 | 只要求 Kotlin 自身可重現；bit-exact 列 Phase 5 |
| kool instanced shader 屬性名/混合模式 API 未驗證 | 渲染卡關 | 參考 `kool-demo/.../bees/CpuBees.kt`；先做無實例版（每蟻一個 Mesh，<300 蟻可接受）退路 |
| 視窗分割 viewport API 未驗證 | 版面 | 退路：單一場景，巢穴畫在世界座標右側區塊、用第二相機或固定 UI Image |
| Emoji 不能顯示 | 按鈕外觀 | ASCII → 圖示 atlas |
| 德文變音字元缺字 | 文字缺 | 驗證；缺字就用 ae/oe/ue |
| ui2 無 Canvas → 折線圖 | Stats | CPU 畫到貼圖 + Image |
| 原版每渲染幀 1 tick vs 我們固定 60Hz | 速度感不同 | 文件註明 |
| 世界尺寸依視窗 → 固定 1400×840 | 長寬比不同時 letterbox | 接受 |
| js 效能（Kotlin/JS 物件導向迴圈）| fps | maxAnts cap、費洛蒙每 3 幀上傳 |
| macOS 未實機驗證 | 可能 natives/首執行緒問題 | 沿用 kool_study 已驗證配方（`-XstartOnFirstThread` 等見 kool_setup_ref.md）|
| 授權 | 法律 | MIT 聲明保留於 THIRD_PARTY_NOTICES + About |
