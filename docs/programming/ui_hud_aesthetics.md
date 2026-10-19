# HUD / UI 美學設計（ui2）

依據：AntSim 原版截圖（`references/`）、`antsim_ui_ux_ref.md`（控制項完整清單、文字格式，**照舊有效**）、kool ui2（`K/modules/ui2/`）。
原則：**版位、控制項、文字照 AntSim**（功能對等，使用者一眼認得）；質感升級成「精緻、安靜、讓世界當主角」。淡紫是 myants 的品牌色，只用在**互動狀態**（選取、焦點、啟用、滑桿），不改 AntSim 的資料色（糖金、蛋白紅、費洛蒙色）。

## 1. 版面（landscape，對照 AntSim）

```
┌─ 世界 3D（左 74%）──────────────────────────────────────────────┬─ 巢穴（右 26%）──────────────┐
│ ┌HUD 卡片┐                                                       │            [Speed − 1x +]    │
│ │FPS 60                    │                                    │                   [🎨 Grafik]│
│ │Population 46 (23 vs 23)  │                                    │ [⏸][⏭][＋][－][⌖][📷][ⓘ]     │
│ │Protein 245 · Sugar 729   │                                    │ Tool [🔍][🍬][🥩][☠][🌧]       │
│ │Queen Age 0m 33s          │                                    │                              │
│ │v0.8.0                    │                                    │   巢穴 3D 剖面（Image）       │
│ └──────────────────────────┘                                    │                              │
│                       （時鐘石、螞蟻、費洛蒙…）                    │                              │
│   [Inspector 浮動卡，貼近被選物，帶指示線]                          │ [Restart] ☑ Rivalenkolonie   │
│                                                                 │ [📈 Stats][🎛 Params][⚙ Analyse]│
│                                                                 │      myants 0.8.0 · kool 0.19 │
└─────────────────────────────────────────────────────────────────┴──────────────────────────────┘
```
- 與 v0.7.0 差異：把左上那一大塊按鈕拆回 AntSim 的位置（右上工具列、右下動作列）；左上只留 HUD 資訊。
- 新增只有兩個：`ⓘ` About（myants 規定保留 About，放在 playback 列最後）、Grafik 面板裡的 3D 選項（§4）。
- 右欄背景：巢穴剖面貼圖滿版，工具列浮在其上（半透明卡片），與 AntSim 一樣。
- 4 px 分隔線改成 1 px `#2A2730` + 左側 12 px 內陰影（更像「玻璃蟻巢」邊框）。

## 2. 色票與樣式 token（`ui/Theme.kt`）

| token | 值 | 用途 |
|---|---|---|
| `surface` | `#120F17` α 0.62 | HUD/工具列卡片（比 AntSim `rgba(0,0,0,.4)` 稍暖帶紫） |
| `surfaceRaised` | `#1C1822` α 0.86 | 面板（Grafik/Params/Stats/Analyse/Inspector）|
| `outline` | `#EADDFF` α 0.10 | 卡片 1 px 邊 |
| `onSurface` / `onSurfaceVar` | `#EEEEEE` / `#AAAAAA` | 主文字 / 次文字（同 AntSim）|
| `primary` | **`#6750A4`** | 啟用中的工具按鈕底、開關開啟、滑桿已填段、焦點環 |
| `primaryContainer` | **`#EADDFF`** | 啟用按鈕的圖示色、hover 光暈（α 0.12）、選取光圈外環 |
| `onPrimary` | `#FFFFFF` | |
| 資料色（不改） | 糖 `#FFD700`、蛋白 `#CD5C5C`、工蟻 `#c4703a`、兵蟻 `#c01810`、圖表 `#dddddd/#6fdf6f/#df6f6f/#6fb0df` | 與 AntSim 一致 |
| 危險 | `#F2B8B5` on `#601410` | Restart 確認、Analyse severity high |

ui2 對應（已查證）：`Colors.darkColors(primary, primaryVariant, secondary, secondaryVariant, background, backgroundVariant, onPrimary, onSecondary, onBackground)` `K/modules/ui2/Colors.kt:93`；圓角背景 `RoundRectBackground(color, cornerRadius)`、邊框 `RoundRectBorder(color, radius, width)` `K/modules/ui2/UiRenderer.kt:32,95`；`Tooltip` `K/modules/ui2/Tooltip.kt:28`；`Image` + `tint()` `K/modules/ui2/Image.kt:34,47`；`Sizes.small/medium` `K/modules/ui2/Sizes.kt`。
建議 `Colors.darkColors(primary = #6750A4, primaryVariant = #4F378B, secondary = #EADDFF, secondaryVariant = #CCC2DC, background = #120F17, backgroundVariant = #1C1822, onPrimary = WHITE, onSecondary = #21005D, onBackground = #EEEEEE)`。

形狀與節奏：圓角 6 dp（按鈕）/ 10 dp（面板）；按鈕 28×28 dp（圖示 18 dp）；群組間距 6 dp、群組內 2 dp；HUD 字級 12 sp（AntSim 11 px，Retina 下 12 sp 更清晰），數字等寬（`Font.textDimensions(…, enforceSameWidthDigits)` 已存在，`K/util/MsdfFont.kt:37`）。
動態：hover 120 ms 淡入光暈、按下 0.96 縮放、面板開啟 160 ms（淡入 + 4 dp 上移）、tooltip 延遲 0.6 s（`TooltipState(delay)`）。不做彈跳、不做大動畫。

## 3. 圖示
- 原版用 emoji；kool 的 Fira Sans MSDF 沒有 emoji（myants deviations 已記錄）。改用 **Material Symbols Rounded SVG**（Apache-2.0）：`pause/play_arrow`、`skip_next`、`zoom_in`、`zoom_out`、`center_focus_strong`、`photo_camera`、`info`、`search`、`water_drop`(糖)、`set_meal` 或 `nutrition`(蛋白)、`skull` 或 `pest_control`(敵人)、`rainy`、`palette`(Grafik)、`monitoring`(Stats)、`tune`(Params)、`analytics`(Analyse)、`restart_alt`。
- 載入：`Assets.loadTexture2d("icons/pause.svg", resolveSize = Vec2i(48, 48))`（簽名已查證），`Image { modifier.image(tex).tint(color) }`。
- 每個圖示按鈕都要 Tooltip，文字照 AntSim 的 title（例如 "Pause/Resume (Space)"、"Single Step"、"Zoom in"）。
- 退路：SVG 載入失敗 → 現行 ASCII 標籤。

## 4. 面板內容（照 AntSim，加 3D 區段）
- **Grafik**：原有 Performance-Modus、☑ Pheromon-Spuren、Pheromon-Stärke、Ameisen-Farbe、☑ Tag/Nacht-Tönung、Tag/Nacht-Stärke、☑ Kino-Kamera、☑ Bloom / Glühen、Bloom-Stärke；「WebGL-Renderer」改為 **Renderer: 3D / Classic 2D**。新增分隔標題 "3D" 下：Qualität（Ultra Low…Ultra，與 Performance-Modus 連動或分開，見 `perf_and_quality_presets.md`）、Schatten（Aus/Niedrig/Mittel/Hoch）、Umgebungsverdeckung (SSAO) ☑、Wetter-Effekte ☑、Gras-Dichte 滑桿、Kamera-Neigung 60–90°。不支援的選項（無 compute 時的 Bloom）顯示但鎖定，附 tooltip 說明原因（沿用 v0.7.0 作法）。
- **Inspector**：浮動卡，位置跟著被選物的螢幕投影（夾在視窗內），左側 3 px 色條＝該物種/蟻群色；內容與 AntSim 相同（state/energy/health/age/cargo）；新增「Folgen」（跟隨）按鈕，P6 再加「Ameise werden」。
- **Stats（Verlauf）**：280×130 → 放大到 320×150，線寬 1.5 dp、背景格線 α 0.06，legend 用資料色。
- **Params / Analyse**：沿用；滑桿已填段用 `primary`。

## 5. 文字與 CJK
- 預設語言：**AntSim 原文（英/德）**，維持功能與截圖對照（舊規則仍有效）。
- 預留 zh-TW（選配）：所有 UI 字串集中到 `ui/Strings.kt`（`L10n` 也存在 `K/util/L10n.kt`，可評估）。
- kool 預設字型 Fira Sans 沒有 CJK。作法（已編譯 API）：desktop 工具程式呼叫 `MsdfFontGenerator.writeMsdfFont(inputTtfPath, outputMsdfPath, fontName, GeneratorGlyphSet.FromString(全部中文字串), atlasDim = 1024 或 2048)`（`KD/util/MsdfFontGenerator.kt:84`，`GeneratorGlyphSet.FromString` `:431`）→ 產生 `noto-sans-tc-ui.json/.png` 放 resources；執行期兩個平台都用 `MsdfFont("fonts/noto-sans-tc-ui")`（`K/util/MsdfFont.kt:155`）載入，`Text { modifier.font(cjkFont) }`。
  - 只產生「用到的字」（幾百字），atlas 1024² 足夠；別產生整個 CJK（數萬字）。
  - 風險：生成器註解說不處理自交字形（`MsdfFontGenerator.kt:33-35`）；Noto Sans TC 官方是 OTF(CFF)，stb_truetype 對 CFF 支援度推測可用但要試。失敗就改用 Google Fonts 的 TTF 版或 `msdf-atlas-gen`（`github.com/Chlumsky/msdf-atlas-gen`）。
  - 混排：中文字型的拉丁字母較寬；HUD 數字仍用 Fira（Tabular）。

## 6. 「美學大師」檢查清單（交付時逐項截圖自評）
1. 世界佔視覺 85% 以上權重；UI 卡片不搶色（低彩度、半透明）。
2. 資訊層級：HUD 數字 > 標籤；啟用狀態只用一種強調色（淡紫）。
3. 對齊：右上工具列右對齊、按鈕等寬、群組間距一致；文字基線對齊。
4. 發光物（食物、費洛蒙、時鐘數字）是畫面上唯一高亮區；UI 本身不發光。
5. 夜間：UI 不變亮，世界變暗，發光物更顯眼（對比 AntSim 的氣氛）。
6. Retina：所有 UI 以 dp/sp 定義；字不糊（MSDF）。
7. 互動可發現性：每個圖示有 tooltip；鍵盤快捷鍵寫在 tooltip。
