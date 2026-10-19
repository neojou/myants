# AntSim_V2 功能清單（給 myants 移植用）

來源：https://github.com/nilsgollub/AntSim_V2（clone commit `cefa083`，depth 1，2026-10-19 查）。授權：**MIT**（`LICENSE`，Copyright (c) 2026 nilsgollub）。移植後必須保留原版權聲明與授權全文（建議放 `docs/programming/THIRD_PARTY_NOTICES.md`，並在 About 面板加一行 "Based on AntSim_V2 by nilsgollub (MIT)"）。

標記：**已查證** = 讀過原始碼或實際跑過 live demo；**推測** = 由文件/命名推得，沒細讀實作。

## 1. 規模概觀（已查證）
- TypeScript 約 1.1 萬行（`src/`），Pixi.js v8 + Canvas-2D fallback，Vite。
- 最大檔案：`src/simulation/antStates.ts`（1260 行，14 個狀態 handler）、`src/main.ts`（1137 行，UI/輸入/迴圈）、`src/graphics/layers/entities.ts`（871）、`src/simulation/Ant.ts`（691）、`src/simulation/World.ts`（616）、`src/graphics/PixiBackdrop.ts`（595）、`src/graphics/layers/nest.ts`（570）、`src/graphics/layers/background.ts`（561）。
- 結論：這**不是**簡單的「螞蟻+費洛蒙」demo，而是含巢穴內部、蟻后/育幼、天敵、蚜蟲、天氣、日夜、雙蟻群戰爭的完整模擬。一次做到完全一致不實際，需分階段（見 `myants_migration_plan_ref.md`）。

## 2. 功能清單與優先級

| # | 功能 | 原始碼 | 優先級 |
|---|---|---|---|
| F1 | 種子化 RNG（mulberry32），`?seed=` 重現 | `src/rng.ts`, `src/main.ts:19` | P0 |
| F2 | 世界（地表）+ 巢穴（地下）雙層，蟻在入口轉換 | `World.ts`, `Nest.ts`, `Ant.ts:433` | P0 |
| F3 | 4 種費洛蒙場 HOME/SUGAR/PROTEIN/DANGER（Float32 格、衰減、可分離模糊擴散） | `PheromoneGrid.ts` | P0 |
| F4 | 三感測器轉向（slime-mold 式） | `Ant.ts:239 senseAndSteer` | P0 |
| F5 | 螞蟻 FSM：FORAGING, RETURNING, NURSING, PATROLLING, FLEEING, ATTACKING, RAIDING, TRANSPORTING, HARVESTING, MILKING, HUNGRY, RESTING, DIGGING, IDLE | `Ant.ts:180-219`, `antStates.ts` | P0（前 4 個 + HUNGRY/IDLE）/ P1（其餘）|
| F6 | 食物：糖、蛋白質來源、品質影響費洛蒙強度；玩家可放置 | `Food.ts`, `World.placeFood` | P0 |
| F7 | 蟻群儲量：Sugar/Protein stockpile、upkeep、穀倉容量上限 | `Colony.ts`, `config.ts nest.*` | P0 |
| F8 | 蟻后產卵 → 卵 → 幼蟲 → 蛹 → 成蟻；營養決定兵蟻 | `Queen.ts`, `Brood.ts`, `config.brood` | P1 |
| F9 | 階級多型（工蟻/兵蟻 size 影響 HP/攻擊/速度/耗能） | `config.ant.poly` | P1 |
| F10 | 交哺（trophallaxis） | `config.ant.troph`, `Ant.ts` | P2 |
| F11 | 巢穴挖掘：房間角色 QUEEN/BROOD/STORAGE/CEMETERY，動態擴建 | `Nest.ts`, `config.nest` | P1 |
| F12 | 昆蟲：PREY, PREDATOR, SPIDER, BEETLE, LADYBUG, APHID（蚜蟲牧場） | `Insect.ts:7` | P1（PREY/PREDATOR）/ P2 |
| F13 | 防禦/集結/圍攻（mob rally, grapple 減速） | `config.combat`, `antStates.ts handleCombat` | P1 |
| F14 | 對手蟻群（第 2 群）、邊界戰鬥、突襲偷育幼/資源 | `Colony.ts`, `handleRaiding`, `config.combat.raid*` | P1 |
| F15 | 日夜循環（dayLength 6000 tick）影響戶外活動 | `World.ts:62,69,78` | P1 |
| F16 | 下雨：洗掉戶外費洛蒙、螞蟻躲雨、減速 | `World.update`, `config.environment` | P2 |
| F17 | 世界中央「時鐘石」顯示實際時間 HH:MM（也是障礙物） | `config.clock` | P1（截圖中很醒目）|
| F18 | 障礙物（12 顆石頭）、圓形碰撞與沿切線滑動 | `Terrain.ts` | P0 |
| F19 | 相機：拖曳平移、滾輪以游標為中心縮放（zoom 1.0–6.0）、按鈕/鍵盤 +/- | `Camera.ts`, `main.ts:311,471` | P0 |
| F20 | 播放：速度 0/0.5/1/2/4/8×、暫停（Space）、單步 | `main.ts:126,260` | P0 |
| F21 | 工具：檢視（點螞蟻/昆蟲看即時數據）、放糖、放蛋白質、生成天敵、雨開關 | `index.html #tool-controls` | P0（檢視/放食物）/ P1 |
| F22 | HUD：FPS、Population（W/S 或 A vs B）、Protein/Sugar、Queen Age、版本 | `main.ts:1075-1090` | P0 |
| F23 | Grafik 面板：品質、費洛蒙開關+強度、蟻色、日夜色調+強度、電影相機、WebGL、Bloom+強度 | `index.html #graphics-panel` | P1（部分選項在 kool 不適用）|
| F24 | Params 面板：23 個即時參數滑桿 + Reset | `main.ts:686-712` | P1 |
| F25 | Stats 面板：Pop/Zucker/Protein/Energie 歷史折線圖 280×130 | `main.ts:761` | P1 |
| F26 | Analyse：SimObserver 參數建議 | `SimObserver.ts` | P2 |
| F27 | 截圖存 PNG | `main.ts takeScreenshot` | P2 |
| F28 | 電影相機（閒置時自動漂移/切到熱點） | `main.ts` | P2 |
| F29 | 效能管理：5 段品質、自動降級（60 幀 <20FPS） | `PerformanceManager.ts` | P2 |
| F30 | localStorage 保存 UI 設定（key `antsim.ui.v1`），URL 參數優先 | `main.ts:32,316` | P2（desktop 可用檔案）|
| F31 | 蟻群滅亡後自動重開（screensaver） | `main.ts:1100` | P1 |
| F32 | 視覺：程序化腳步動畫、搬運圖示、戰鬥紅光、雨滴、螢火蟲、bloom | `layers/*.ts` | P2（簡化版）|
| F33 | Headless 測試、golden snapshot | `headless.ts`, `*.test.ts` | P1（移植為 commonTest）|
| F34 | Android（Capacitor） | `android/` | 不移植 |

## 3. URL 參數（已查證，README + `main.ts`）
- `seed=<n>`：固定種子；`colonies=1|2`（**預設 2**，`main.ts:37`：`Number.isFinite(n) ? n : 2`；README 寫 optional on by default）；`quality=ULTRA_LOW..ULTRA`；`fpslog=1`。
- kool desktop 無 URL：改用命令列參數 `--seed= --colonies= --quality=`；js 版讀 `window.location.search`（推測可行，需 `kotlinx.browser`）。

## 4. Live demo 觀察（已查證，2026-10-19 headless Chrome 截圖）
- `antsim_screenshots/antsim_default.png`、`antsim_screenshots/antsim_colonies2.png`（1600×900，headless，sim 只跑了極短時間）。
- Live 版右下角顯示 `Build: 2025-12-17 21:00 - RELEASE 2.0`，左上角 `v0.1.0`；live 部署可能比 repo HEAD 舊，**以 repo 原始碼為準**。
- 左 74%：地表（深褐土壤、草、石頭、落葉、中央時鐘石）；右 26%：巢穴剖面（深褐、根、入口通道、蟻后房），UI 疊在巢穴面板上。
