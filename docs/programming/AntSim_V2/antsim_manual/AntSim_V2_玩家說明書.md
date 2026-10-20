---
title: AntSim_V2 玩家說明書
aliases:
  - AntSim 玩家說明書
  - AntSim_V2
tags:
  - 遊戲/模擬
  - 螞蟻
  - 費洛蒙
  - agent-based
  - 說明書
type: evergreen
status: draft
created: 2026-10-20
updated: 2026-10-20
source:
  - https://github.com/nilsgollub/AntSim_V2
  - https://nilsgollub.github.io/AntSim_V2/
  - https://nilsgollub.github.io/AntSim_V2/?colonies=2
---

# AntSim_V2 玩家說明書

> [!abstract] 核心概念
> AntSim_V2 是一個「看」比「玩」重要的蟻群沙盒：你不直接指揮螞蟻，而是透過放食物、放掠食者、下雨、開戰，觀察費洛蒙路徑如何從混亂中自我組織成「螞蟻高速公路」。

![[screen-world.png|480]] ![[screen-nest.png|160]]

---

## 1. 這是什麼？

- 一個用 TypeScript 寫的 **agent-based（個體導向）螞蟻群落模擬**，以 Pixi.js（WebGL）繪製，沒有 WebGL 時自動改用 Canvas‑2D。
- 畫面分兩半：**左邊是地面世界**（覓食、戰鬥、天氣），**右邊是地下巢穴剖面**（蟻后、幼蟲、倉庫）。
- 所有隨機性都經過可設定種子的亂數產生器（mulberry32），**同一個 seed 會重現完全相同的一局**。
- 預設會有一個**敵對蟻群（Rivalenkolonie）** 在對面邊緣，兩群會打仗、搶資源、偷幼蟲。
- 線上試玩：<https://nilsgollub.github.io/AntSim_V2/>（雙蟻群：`?colonies=2`）。

---

## 2. 怎麼玩（新手快速上手）

> [!tip] 五分鐘上手
> 1. 打開 [線上 Demo](https://nilsgollub.github.io/AntSim_V2/?colonies=2)，什麼都別按，先看 1–2 分鐘：螞蟻從巢口散開，**發光的路徑**慢慢浮現。
> 2. 用滑鼠**拖曳**平移、**滾輪**縮放（往游標處縮放）；觸控螢幕：單指平移、雙指捏合縮放、點一下等於點擊。
> 3. 按 🍬 然後在地圖上點一下 → 放一堆糖。看螞蟻怎麼發現它、綠色路徑怎麼長出來。
> 4. 按 🔍（預設工具）點任一隻螞蟻或昆蟲，看牠的狀態、能量、血量。
> 5. 按 ☠ 在蟻群附近放一隻掠食者，看螞蟻**拉警報 → 圍成一圈 → 集體衝上去**。
> 6. 按 🌧 下雨，看路徑被沖掉、螞蟻躲回巢裡；雨停後再看路徑重新長出來。
> 7. 想要純粹的單一蟻群：取消勾選「Rivalenkolonie（敵對蟻群）」。

這遊戲**沒有勝負條件**（推測：程式中找不到勝利/失敗判定，只有蟻群可能自然滅亡）。蟻后會老、會餓死，被拋棄的蟻群真的會死光——那就按 **Restart**。

---

## 3. 介面總覽

### 3.1 HUD 讀數（右側巢穴面板上方）

| 讀數 | 意義（依 `src/main.ts` 驗證） |
|---|---|
| `FPS: n` | 每秒影格數，每秒更新一次 |
| `Population: N (a vs b)` | 雙蟻群模式：總數，括號內為「我方 vs 敵方」各自數量（戰況比分） |
| `Population: N (W:x S:y)` | 單蟻群模式：括號內為工蟻（Worker）與兵蟻（Soldier）數 |
| `Protein: x \| Sugar: y` | 我方巢內**蛋白質**與**糖**庫存（取整數） |
| `Queen Age: Xm Ys` | 蟻后年齡，以模擬時間計（60 tick ≈ 1 秒） |
| 小字 Build 資訊 | 版本；若有自訂參數會加上 `· custom params` |

### 3.2 上方控制列

| 按鈕 | 功能 | 備註 |
|---|---|---|
| `Speed: − 1x +` | 模擬速度，依序 **0 / 0.5 / 1 / 2 / 4 / 8×** | 0× 等於靜止 |
| 🎨 Grafik（圖形） | 開啟圖形設定面板 | 見 §3.5 |
| ⏸ / ▶ | 暫停 / 繼續（**空白鍵**） | 暫停時畫面仍繪製，可平移、縮放、檢視 |
| ⏭ | 單步（僅暫停時有效，前進 1 tick） | |
| ➕ / ➖ | 放大 / 縮小（×1.25，鍵盤 `+`/`=`、`-`/`_`） | |
| ⌖ | 重設鏡頭 | |
| 📷 | 把世界畫面存成 PNG 下載 | |

### 3.3 工具列

| 按鈕 | 工具 | 點擊地圖時 |
|---|---|---|
| 🔍 Inspect | 檢視（預設） | 點最近的**地面**螞蟻或昆蟲 → 顯示檢視面板，並在目標周圍畫黃色脈動圈 |
| 🍬 Place Sugar | 放糖 | `placeFood(x, y, 'SUGAR')` |
| 🥩 Place Protein | 放蛋白質 | `placeFood(x, y, 'PROTEIN')` |
| ☠ Spawn Enemy | 生成敵人 | 固定生成一隻 **PREDATOR（掠食者）** |
| 🌧 Regen an/aus（下雨 開/關） | 切換下雨 | 不需點地圖；開啟時雨會持續 `rainMinDuration×2` = 800 tick，再按一次立即停雨 |

放食物/敵人時游標會變十字準星。

### 3.4 下方控制列

| 控制 | 功能 |
|---|---|
| **Restart** | 用新種子重建整個世界（保留你的螞蟻顏色設定） |
| ☑ **Rivalenkolonie（敵對蟻群）** | 開/關第二個蟻群；**切換會立刻 Restart**。App 預設為開（2 群） |
| 📈 Stats（統計） | 歷史曲線：Pop（族群）、Zucker（糖）、Protein（蛋白質）、Energie（能量 %）；剛開局顯示「sammelt Daten…（收集資料中…）」 |
| 🎛 Params（參數） | 即時參數滑桿（見 §7），附 ↺ Reset（重設回預設並重新載入） |
| ⚙ Analyse（分析） | 參數調校顧問：觀察一段時間後給出分級建議，每條可按「Übernehmen（套用）」或「Alle übernehmen（全部套用）」 |

### 3.5 🎨 Grafik 圖形面板

| 德文標籤 | 中文 | 說明 |
|---|---|---|
| Performance-Modus | 效能模式 | Ultra Low / Low / Medium（預設）/ High / Ultra |
| Pheromon-Spuren | 費洛蒙軌跡 | 顯示/隱藏發光路徑覆蓋層 |
| Pheromon-Stärke | 費洛蒙強度 | 0–1.5，調整路徑亮度 |
| Ameisen-Farbe (eigene Kolonie) | 螞蟻顏色（我方） | 預設 `#c4703a` |
| Tag/Nacht-Tönung | 日夜色調 | 開/關 |
| Tag/Nacht-Stärke | 日夜強度 | 0–1，預設 0.5 |
| Kino-Kamera (Screensaver) | 電影鏡頭（螢幕保護） | 閒置時自動輪播 WIDE 全景 / ENTRANCE 巢口 / EVENT 事件（襲擊、戰鬥、擠蜜）/ FOLLOW 跟拍單隻螞蟻；操作時暫停 |
| WebGL-Renderer (Standard) | WebGL 繪製器（預設） | 不支援時顯示「WebGL nicht verfügbar（WebGL 無法使用）」 |
| Bloom / Glühen (nur WebGL) | 泛光（僅 WebGL） | |
| Bloom-Stärke | 泛光強度 | 0–1.5 |

所有 UI 選擇存在 `localStorage`（`antsim.ui.v1`），網址參數優先於存檔。

### 3.6 鍵盤與滑鼠

| 輸入 | 動作 |
|---|---|
| `Space` | 暫停/繼續（焦點在輸入框時無效） |
| `+` / `=` | 放大 |
| `-` / `_` | 縮小 |
| 左鍵拖曳（>5px） | 平移 |
| 左鍵點擊 | 使用目前工具 |
| 滾輪 | 往游標處縮放 |
| 單指 / 雙指 / 點擊 | 平移 / 捏合縮放 / 點擊 |

> [!note] 程式碼中只找到以上三組鍵盤快捷鍵，沒有工具切換的快捷鍵。

### 3.7 網址參數

| 參數 | 範例 | 效果 |
|---|---|---|
| `seed` | `?seed=12345` | 重現特定一局（否則以時間為種子） |
| `colonies` | `?colonies=1` / `?colonies=2` | 1 = 單蟻群，2 = 敵對蟻群 + 戰爭（限 1–2） |
| `quality` | `?quality=LOW` | 鎖定畫質 `ULTRA_LOW`…`ULTRA`（覆蓋存檔與自動降級起點） |
| `fpslog` | `?fpslog=1` | 記錄 FPS（開發用，推測） |

例：`index.html?colonies=2&quality=LOW`（樹莓派 kiosk 螢幕保護使用的網址）。

---

## 4. 畫質等級

| 等級 | 螞蟻上限 | 粒子上限 | 動態陰影 | 解析度 | 腳部動畫 | 草叢 |
|---|---|---|---|---|---|---|
| ULTRA_LOW | 80 | 20 | ✗ | 0.4× | ✗ | ✗（2D 模式不畫草） |
| LOW | 120 | 40 | ✗ | 0.4× | ✗ | ✓ |
| MEDIUM（預設） | 300 | 100 | ✗ | 0.6× | ✗ | ✓ |
| HIGH | 600 | 200 | ✗ | 0.85× | ✓ | ✓ |
| ULTRA | 1200 | 2000 | ✓ | 1.0× | ✓ | ✓（草會擺動）+ 光束、暗角 |

- **自動降級**：連續 60 幀低於 20 FPS 就降一級（冷卻 10 秒），最低只降到 **LOW**。
- **畫質不影響模擬**：費洛蒙網格解析度、擴散、更新頻率都在 `CONFIG` 裡固定，所以任何畫質下蟻群行為完全相同（有測試保證）。

---

## 5. 螞蟻生態與機制

### 5.1 階級與多型性
| 階級 | 體型 `sizeVar` | 特性 |
|---|---|---|
| 工蟻 Worker | 0.80–1.05 | 小、快、便宜 |
| 兵蟻 Soldier | 1.25–1.50 | 大、血多、咬得重，但慢、維持成本高；頭部偏紅 |

幼蟲吃得好（蛋白質庫存 > `soldierProteinLevel` 150）才會長成兵蟻，上限約 20%。**你放越多蛋白質 → 兵蟻越多**。

### 5.2 生命週期
蟻后產卵（每 `queenLayInterval` 180 tick 一顆）→ 卵 1000 → 幼蟲 2000 → 蛹 1500 tick → 成蟻，共約 4500 tick（≈75 秒）。平衡族群約 **211 隻**（壽命 38000 ÷ 產卵間隔 180）。

### 5.3 交哺（Trophallaxis）
覓食蟻把糖存在「社會胃」裡，回巢後嘴對嘴分給餓的同伴，護理蟻不用每次都跑去倉庫。

### 5.4 狀態
| 狀態 | 說明 |
|---|---|
| FORAGING 覓食 | 找糖或蛋白質，跟路徑或隨機遊走 |
| RETURNING 返巢 | 帶貨回家，沿 toHome 路徑 |
| NURSING 護理 | 巢內照顧幼蟲/蟻后 |
| PATROLLING 巡邏 | 兵蟻在巢口附近巡邏 |
| ATTACKING 攻擊 | 戰鬥中 |
| FLEEING 逃跑 | 恐慌，邊跑邊留警報費洛蒙 |
| IDLE 閒置 | 休息 |
| （另有 RAIDING 襲擊、MILKING 擠蜜露等，見電影鏡頭程式碼） | |

### 5.5 導航（類黏菌演算法）
每隻螞蟻有左/中/右三個感測器，在前方 `antSensorDist` 處聞費洛蒙：中間最濃就直走，哪邊濃就轉哪邊，都很淡就亂走，再加一點抖動。

### 5.6 四種費洛蒙
| 網格 | 誰留下 | 動態 |
|---|---|---|
| toHome（回家） | 外出覓食的螞蟻 | 衰減 + 擴散 |
| toSugar（糖） | 帶糖回家的螞蟻 | 衰減（食物路徑較銳利、持久） |
| toProtein（蛋白質） | 帶蛋白質回家的螞蟻 | 同上 |
| toDanger（危險/警報） | 逃跑的螞蟻 | 只衰減、不擴散、消退快 → 警報只在局部 |

延伸：[[費洛蒙]]、[[蟻群演算法]]。

### 5.7 戰鬥、警報與防禦
- 敵人血量/每次咬傷：掠食者 42/6、蜘蛛 32/7、甲蟲 88/12。
- 兵蟻聞到超過門檻的 DANGER，會**順著警報梯度**衝向源頭。
- 工蟻只有在附近同伴夠多、數量優勢夠大時才圍攻，否則逃跑並擴散警報；兵蟻不管多少都會打。
- 每有一隻螞蟻貼在敵人身上，敵人就變慢一點（有上限）。
- 昆蟲死亡會留下屍體（蛋白質）；螞蟻屍體則由「送葬蟻」搬到巢內墓室。
- 雙蟻群時：強勢的一方會組織**襲擊隊**，攻向敵巢偷**幼蟲**（當自己的養）或**資源**。

### 5.8 巢穴邏輯
巢是節點與隧道的圖，隨族群成長向外挖成**分枝樹狀**。挖掘順序：育幼室 → 倉庫 → 墓室 → 之後育幼室/倉庫交替。

| 房間 | 功能 |
|---|---|
| QUEEN 蟻后室 | 創巢房間，蟻后在此產卵 |
| BROOD 育幼室 | 護理蟻把卵/幼蟲搬到最近的育幼室 |
| STORAGE 倉庫 | 決定庫存上限（基本 + 每間倉庫加成） |
| CEMETERY 墓室 | 螞蟻屍體放這裡慢慢腐爛 |

### 5.9 環境
- **日夜**：一天 6000 tick（約 1.5 分鐘）。夜裡地面螞蟻的速度與嗅覺降到最低 50%（`nightActivityMin`）；巢內不受影響。
- **下雨**：機率約一天一次，持續 400–1000 tick。地面費洛蒙每幀 ×0.97 **被沖掉**；覓食蟻躲回巢；地面螞蟻速度 ×0.7。巢內費洛蒙不受影響。

### 5.10 經濟平衡（BALANCE.md 重點）
- 糖消耗約 0.8/tick（P=200），每趟糖 14 → 全群約每秒需 3.4 趟送貨，**糖剛好夠**。
- **蛋白質才是生死關鍵**：幼蟲每 tick 吃 0.0006；遇敵襲中斷狩獵太久 → 幼蟲餓死 → 族群老化崩潰（「死亡螺旋」）。
- 長期存活率約 50–60%，主要由亂數決定，不是調參能解決的。

---

## 6. 左側世界畫面：除了螞蟻還畫了什麼

依 `Renderer.renderWorld()` 的繪製順序（後畫的蓋在上面）：

| # | 圖層 | 內容 |
|---|---|---|
| 0 | **地面背景**（靜態烘焙一次） | 暖色土壤質地、深淺土斑與顆粒、細小**乾裂紋**、**卵石**（含接觸陰影和高光，比螞蟻大很多）、**落葉**（有葉脈與葉柄）、分岔**枯枝**、黃色花心的**野花** |
| 1 | **費洛蒙發光層** | 四種氣味合成 RGB，疊加混色（lighter）+ 輕微模糊（HIGH/ULTRA 2px、MEDIUM 1px），強度由 Pheromon-Stärke 控制 |
| 2 | **時鐘石** | 一塊水平石板，刻槽裡顯示**你電腦的即時 24 小時 LED 時間**（琥珀色發光，HH:MM）；它同時是**障礙物**（半徑 150），螞蟻會繞行 |
| 3 | **岩石** | 帶陰影，部分岩石長**青苔** |
| 4 | **動態陰影**（僅 ULTRA） | 螞蟻與昆蟲的方向性陰影，隨太陽由左向右移動改變方向長度，夜裡消失 |
| 5 | **食物** | SUGAR：金黃色「蜜滴」帶高光；PROTEIN：肉塊；CORPSE：昆蟲屍體——死螞蟻（深灰、腳蜷曲）、**甲蟲屍體**（深石板灰）、**瓢蟲屍體**等，翻倒朝上（LOW 畫質用簡化圖形） |
| 6 | **巢口** | 地面上的巢穴入口標記 |
| 7 | **草叢** | 直立草叢與穗（ULTRA_LOW 不畫；ULTRA 會擺動） |
| 8 | **昆蟲** | 見下表 |
| 9 | **螞蟻** | 各蟻群、攜帶物圖示（糖晶、肉塊、卵、屍體） |
| 10 | **粒子** | 塵土、紅色「血」粒子、戰鬥火花 |
| 11 | **選取圈** | 🔍 選中目標的黃色脈動圓圈 |
| — | *以下為螢幕空間（不隨鏡頭移動）* | |
| 12 | **日夜色調** | 黎明/白天/黃昏/夜晚環境色，日月光暈（所有畫質） |
| 13 | **螢火蟲** | 夜裡約 18 隻飄動的光點 |
| 14 | **雨** | 陰天變暗 + 多層斜雨絲 + 落地漣漪 |
| 15 | **光束 + 暗角**（僅 ULTRA） | God rays 緩慢移動的光柱、邊緣輕微變暗 |

> [!info] WebGL 模式
> 在 WebGL（預設）下，背景、費洛蒙、岩石、草、食物、昆蟲、螞蟻由 Pixi 圖層繪製（材質從同一套 2D 美術烘焙），2D 畫布只負責時鐘石、選取圈與螢幕空間效果。外觀相同。

### 6.1 費洛蒙顏色怎麼看

程式：`R = √(protein + sugar + danger)`、`G = √sugar`、`B = √(home + danger)`。

| 主要氣味 | 結果顏色 | 意思 |
|---|---|---|
| 只有 toHome | **藍** | 回家的路／巢口周圍扇形 |
| 只有 toSugar | **黃**（R+G） | 糖的路徑 |
| 只有 toProtein | **紅** | 獵物/蛋白質的路徑 |
| 只有 toDanger | **洋紅/紫**（R+B） | 警報、戰場 |
| toSugar + toHome | 偏**白**（R+G+B） | 繁忙的糖高速公路（雙向） |
| toProtein + toHome | **紫粉** | 蛋白質路線 |

> [!note] ARCHITECTURE 文件寫 SUGAR 是「綠」，但實際渲染公式讓糖同時加到 R 和 G，所以看起來是**黃色**。

### 6.2 昆蟲圖鑑（依 `Insect.ts` 行為）

| 類型 | 檢視面板名稱 | 血量/速度 | 行為 | 對你的意義 |
|---|---|---|---|---|
| PREY | Beute-Insekt（獵物昆蟲） | 20 / 1.0 | 60px 內有我方螞蟻就以 3.5 衝刺逃跑（比追獵的螞蟻 3.75 慢一點） | **蛋白質主要來源** |
| PREDATOR | Räuber（掠食者） | 42 / 1.3 | 主動追獵我方螞蟻（偵測約 122px），每 30 tick 咬 6 | ☠ 工具放的就是牠；要靠群體圍攻 |
| SPIDER | Spinne（蜘蛛） | 32 / 1.8 | 最快、偵測最遠（約 141px），咬 7 | 最危險，需要大群 |
| BEETLE | Käfer（甲蟲） | 88 / 0.6 | 很慢，只在很近（約 55px）才追咬，咬 12 | 皮厚耐打 |
| LADYBUG | Marienkäfer（瓢蟲） | 80 / 1.0 | 不攻擊螞蟻；**獵捕蚜蟲**（每 60 tick 咬 10） | 會吃掉你的「蜜露牧場」 |
| APHID | Blattlaus（蚜蟲） | 10 / 0.15 | 幾乎不動，聚成一群 | 螞蟻**擠蜜露**得糖，並留下糖路徑——牧場成為覓食樞紐 |

> [!note] 掠食者類只追獵**第 0 蟻群（我方）**的螞蟻；獵物也只躲我方螞蟻。

---

## 7. 右側巢穴面板

依 `nest.ts` 的 `renderNest()`：

1. **土壤底色 + 深度漸層**（越深越暗），有地層色帶與顆粒。
2. **隧道**：深色走廊連接各節點；**房間**：較亮的有機形狀，含陰影光暈、土牆邊緣、依功能著色的**光暈**、地面小碎石。
3. **巢口光**：入口處透進來的光。
4. **樹根網路**：暖棕色根系最後畫上，覆蓋在隧道與房間上方（純裝飾）。
5. **食物堆**：糖堆與蛋白質堆，把總庫存**平均分到每一間倉庫**畫出來——堆越大代表存量越多。
6. **墓室屍體**：我方的螞蟻屍體。
7. **蟻后**：與螞蟻同風格的大型蟻后，顏色跟隨蟻群；死亡則不畫。
8. **幼體**：卵（珍珠白水滴）、幼蟲（C 形，隨年齡變大）、蛹（琥珀色外殼）。
9. **巢內螞蟻**與巢內粒子（挖土塵）。

> [!tip] 巢穴裡**挖出新房間**就是蟻群在成長的訊號；倉庫堆縮小、幼蟲變少則是危險前兆。

---

## 8. 🎛 Params 滑桿一覽

| 滑桿 | 效果 |
|---|---|
| antSpeed | 螞蟻速度 |
| antEnergyDecay | 能量消耗 |
| queenLayInterval (Pop ↑) | 產卵間隔（調小 → 族群變大） |
| ant.lifespan (Pop ↑) | 壽命（調大 → 族群變大） |
| eggCost | 產卵成本 |
| sugarValue / proteinValue | 每趟糖/蛋白質價值 |
| sugarEnergyValue | 糖換能量比 |
| colonyUpkeep / broodProteinUpkeep | 蟻群/幼蟲維持成本 |
| pheromone.diffusionRate / decay / foodDecay (Straßen=道路) | 費洛蒙擴散、衰減、食物路徑衰減 |
| antSensorDist (Spur-Reichweite=路徑感測距離) | 嗅覺距離 |
| ant.dispersalRadius (Streuung=擴散) / dispersalStrength | 探索者向外分散 |
| ant.memoryBias (Site Fidelity=地點忠誠) | 記得上次的食物點 |
| ant.proteinForagerShare | 蛋白質覓食者比例 |
| brood.soldierFoodThreshold (Soldaten ↑=weniger：數值越高兵蟻越少) / soldierProteinLevel | 兵蟻生成門檻 |
| sugarSourceCount | 糖源數量 |
| predatorSpawnRate | 掠食者生成率 |

---

## 9. 技巧與觀察

> [!tip] 看路徑成形
> 開局先別動，從 1× 切到 4× 看藍色扇形從巢口長出，接著黃色/紅色細線連到食物，最後變成又亮又白的雙向「高速公路」。

> [!tip] 雨會重置一切
> 按 🌧：路徑每幀衰減 3%，幾秒內幾乎消失，地面螞蟻撤回巢。雨停後是觀察「重新探索」的最佳時機。

> [!tip] 放食物做實驗
> 在遠處放糖，再在近處放一堆，看蟻群是否轉移路線（memoryBias 讓牠們有些「念舊」）。放多一點 🥩 → 過一陣子兵蟻（大頭紅色）變多。

> [!warning] 放掠食者要節制
> 一隻 Räuber 通常會被圍殺，但會先帶走幾隻螞蟻；中斷狩獵太久可能引發蛋白質死亡螺旋。連放好幾隻可能讓蟻群滅亡。

> [!example] 戰爭模式
> `?colonies=2`：看 HUD 的 `a vs b` 比分；開啟電影鏡頭會自動切到 RAIDING 襲擊現場。強勢方會跨地圖去偷對方的幼蟲。

- 夜裡（畫面變暗、螢火蟲出現）螞蟻明顯變慢、收工；黎明又活躍起來。
- 找到蚜蟲群附近的糖路徑＝「牧場」；瓢蟲出現時牧場會縮小。
- 用 `?seed=` 記下有趣的一局，之後可完整重現。
- 想當螢幕保護：開「Kino-Kamera」，Speed 1×，放著就好。

---

## 10. 相關筆記
- [[AntSim_V2]]
- [[myants]]
- [[蟻群演算法]]
- [[費洛蒙]]

## 參考來源
- Repo：<https://github.com/nilsgollub/AntSim_V2>
- [README.md](https://github.com/nilsgollub/AntSim_V2/blob/main/README.md)
- [ARCHITECTURE_AND_MECHANICS.md](https://github.com/nilsgollub/AntSim_V2/blob/main/ARCHITECTURE_AND_MECHANICS.md)
- [BALANCE.md](https://github.com/nilsgollub/AntSim_V2/blob/main/BALANCE.md)
- [ROADMAP.md](https://github.com/nilsgollub/AntSim_V2/blob/main/ROADMAP.md)
- [GRAPHICS_UPDATE_SUMMARY.md](https://github.com/nilsgollub/AntSim_V2/blob/main/GRAPHICS_UPDATE_SUMMARY.md)
- [index.html](https://github.com/nilsgollub/AntSim_V2/blob/main/index.html)
- [src/main.ts](https://github.com/nilsgollub/AntSim_V2/blob/main/src/main.ts)
- [src/config.ts](https://github.com/nilsgollub/AntSim_V2/blob/main/src/config.ts)
- [src/PerformanceManager.ts](https://github.com/nilsgollub/AntSim_V2/blob/main/src/PerformanceManager.ts)
- [src/graphics/Renderer.ts](https://github.com/nilsgollub/AntSim_V2/blob/main/src/graphics/Renderer.ts)
- [src/graphics/layers/](https://github.com/nilsgollub/AntSim_V2/tree/main/src/graphics/layers)（background / entities / atmosphere / nest）
- [src/simulation/Insect.ts](https://github.com/nilsgollub/AntSim_V2/blob/main/src/simulation/Insect.ts)
- [src/simulation/Nest.ts](https://github.com/nilsgollub/AntSim_V2/blob/main/src/simulation/Nest.ts)
- 線上 Demo：<https://nilsgollub.github.io/AntSim_V2/> · <https://nilsgollub.github.io/AntSim_V2/?colonies=2>
