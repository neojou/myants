# AntSim_V2 UI/UX 參考

來源：`index.html`、`src/style.css`、`src/main.ts`（已查證）；截圖 `antsim_screenshots/antsim_default.png`、`antsim_screenshots/antsim_colonies2.png`（live demo，2026-10-19，headless Chrome 1600×900）。
**語言**：AntSim UI 是英文＋德文混合（如 "Grafik"、"Rivalenkolonie"、"Pheromon-Stärke"）。myants 規則：**照 AntSim 原文**（只用 Latin 字元，kool 預設字型可顯示；德文變音 ä/ö/ü 推測在預設 MSDF 字型內，需驗證）。不加中文 UI，避免 CJK 字型問題。

## 1. 版面（landscape）
```
┌───────────────────────────── 74vw ─────────────────────────────┬4px┬──── 26vw (#1a1a1a) ─────┐
│ ┌stats(左上，半透明黑)┐                                         │#333│        [Speed: − 1x +]   │
│ │FPS: 60              │                                        │    │              [🎨 Grafik] │
│ │Population: 32 (16 vs 16)                                     │    │  [⏸][⏭][➕][➖][⌖][📷]    │
│ │Protein: 278 | Sugar: 599                                     │    │ Tool:[🔍][🍬][🥩][☠][🌧] │
│ │Queen Age: 0m 0s     │                                        │    │                          │
│ │v0.1.0               │                                        │    │   巢穴剖面 (nestCanvas)  │
│ └─────────────────────┘      地表 world (gameCanvas+glCanvas)   │    │  入口通道 ═══(蟻后房)    │
│                                ┌──────────┐                     │    │                          │
│        石頭    草              │  16:52   │ 時鐘石              │    │   [inspector 浮動]        │
│                                └──────────┘                     │    │   [各面板 浮動]            │
│                                                                 │    │ [Restart] ☑Rivalenkolonie│
│                                                                 │    │[📈 Stats][🎛 Params][⚙ Analyse]│
└─────────────────────────────────────────────────────────────────┴────┴──────────────────────────┘
```
- 截圖中 stats 實際位於**地表左上**（已查證 `style.css:64`：`position:fixed; top:10px; left:10px; font-size:11px; color:#aaa; background:rgba(0,0,0,.4)`）。控制按鈕群在右上（右對齊），Restart/Tuner 在右下。
- Portrait：world 100vw×74dvh，巢穴在下方 26dvh（style.css）。myants desktop 預設 landscape，portrait 列為 P2。
- 世界畫布 `object-fit: contain`，保持長寬比，可能 letterbox。

## 2. 顏色（已查證）
| 用途 | 值 |
|---|---|
| body 背景 / 文字 | `#111` / `#eee`，sans-serif |
| 巢穴面板背景 / 分隔線 | `#1a1a1a` / 4px `#333` |
| 我方工蟻（預設，可調） | `#c4703a`（antColorPicker）|
| 兵蟻頭 | `#c01810` |
| 糖貨物 / 糖 | `#FFD700` |
| 蛋白質貨物 | `#CD5C5C`（推測對應）|
| 圖表 Pop/Zucker/Protein/Energie | `#dddddd` / `#6fdf6f` / `#df6f6f` / `#6fb0df` |
| 費洛蒙疊圖（`PixiBackdrop.ts:426`） | R=√(protein+sugar+danger)·255, G=√sugar·255, B=√(home+danger)·255，門檻 0.01，alpha=min(1,0.85·intensity)，再 blur 1.5px；疊加（additive）|
| 地表 | 深褐土壤＋草＋小石＋落葉（程序生成，`layers/background.ts`），截圖取樣約 `#3a3128`（推測）|
| 時鐘石 | 灰色漸層石 + 深色凹槽，數字米黃色 Courier New 68px |
| 敵人 | predator 紅黑漸層 `#ff5a2a→#c00000`；spider 黑；beetle `#5a0e0a/#c0241a`；aphid 綠 `#c4f23a→#8fc41e` |

## 3. 控制項完整清單（`index.html`）
### 右上 controls-top
- `Speed: [−] 1x [+]`：檔位 0/0.5/1/2/4/8（顯示 `0x`..`8x`）。
- `🎨 Grafik`：開關 Graphics 面板。
- playback：`⏸/▶` Pause/Resume（Space）、`⏭` Single Step（只在暫停時）、`➕` Zoom in ×1.25（`+`/`=`）、`➖` Zoom out（`-`/`_`）、`⌖` Reset Camera、`📷` Screenshot PNG（檔名 `antsim_seed{seed}_t{age}.png`）。
- Tool（單選，active 高亮）：`🔍` Inspect（預設）、`🍬` Place Sugar、`🥩` Place Protein、`☠` Spawn Enemy、`🌧` 雨開關（toggle）。
### Inspector（點選後每幀更新）
- 內容：state / energy / health / age / cargo（ant），昆蟲類似；`✕` 關閉。
### 右下 controls-bottom
- `Restart`（新種子重建）、`☑ Rivalenkolonie`（切換 1/2 群，重建世界）。
- `📈 Stats`、`🎛 Params`、`⚙ Analyse`。
### 面板（浮動，標題列 + `✕`）
- **Grafik**：Performance-Modus（Ultra Low/Low/Medium/High/Ultra，預設 Medium）、☑ Pheromon-Spuren、Pheromon-Stärke 0–1.5 step .05 預設 1、Ameisen-Farbe (eigene Kolonie) `#c4703a`、☑ Tag/Nacht-Tönung、Tag/Nacht-Stärke 0–1 預設 .5、☑ Kino-Kamera (Screensaver)、☑ WebGL-Renderer (Standard)、☑ Bloom / Glühen (nur WebGL)、Bloom-Stärke 0–1.5 預設 .7、提示文字。
- **Live-Parameter**：23 個滑桿（清單見 `antsim_simulation_model_ref.md` §7），`↺ Reset`、`✕`。
- **Verlauf**：280×130 折線圖，4 條線 + legend。
- **Parameter-Tuner**：Analyse 結果（severity 標籤 + CONFIG key 建議），`↺ Reset`、`✕`。
### 滑鼠/觸控
- 拖曳平移（有移動門檻區分 click/drag）；滾輪 zoom-to-cursor；範圍 zoom 1.0–6.0（不能小於 1，因為會露出黑邊）。
- 觸控：單指平移、雙指縮放、tap=click（kool desktop 不需要；js 版 P2）。
- 點擊行為依 Tool：Inspect → 以 SpatialGrid 找最近螞蟻/昆蟲；Sugar/Protein → `World.placeFood`；Enemy → `World.spawnEnemyAt`。
### 鍵盤
Space 暫停、`+`/`=` 放大、`-`/`_` 縮小。

## 4. HUD 文字格式（`main.ts:1075`）
- `FPS: N`（每秒更新）
- 單群：`Population: N (W:a S:b)`；雙群：`Population: N (a vs b)`
- `Protein: P | Sugar: S`（floor）
- `Queen Age: Mm Ss`（age/60 秒）
- `v0.1.0` → myants 顯示 `AppVersion.NAME`。

## 5. 互動細節
- 暫停時渲染照跑（可平移/縮放/檢視）。
- 蟻群滅亡（queen.dead 或總數 0）後經 EXTINCT_RESTART_FRAMES 自動重開。
- 閒置一段時間 → 電影相機（任何 wheel/mousedown/touch/keydown 會打斷，`main.ts:1051`）。
- 設定存 localStorage `antsim.ui.v1`。

## 6. Emoji 按鈕在 kool 的處理
AntSim 按鈕大量用 emoji（⏸⏭➕➖⌖📷🔍🍬🥩☠🌧🎨📈🎛⚙）。kool 預設 MSDF 字型（`util/MsdfFont.kt:142 DEFAULT_FONT_DATA`）**不含 emoji**（推測，極可能）。對策（依序）：
1. 小型 PNG 圖示 atlas（自繪 16×16，放 `koolApp/src/commonMain/resources/icons/`）用 ui2 `Image`（`modules/ui2/Image.kt:47`）。
2. 或 ASCII 替代：`||`/`>`、`>|`、`+`、`-`、`o`、`Cam`、`?`、`S`、`P`、`X`、`Rain`。Phase 1 先用 ASCII，Phase 3 換圖示。
