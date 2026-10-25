# myants 美術方向（Artist 審查前必讀）

這份是 Artist 審查的唯一依據。細節規格以下列文件為準，這裡只寫判斷原則和已定案的數值：
`docs/programming/ui_hud_aesthetics.md`（HUD 版面與色票）、`3d_visual_overhaul_overview.md`（3D 目標）、`world_scene_composition.md`（場景構圖）、`antsim_ui_ux_ref.md`（上游 UI）。
這份與程式衝突時，以 `koolApp/src/commonMain/kotlin/com/neojou/myants/ui/Theme.kt` 為準，並回頭修正這份文件。

## 1. 核心理念
- 版位照 AntSim_V2：左邊是世界，右邊是巢穴；HUD 在左上，工具列在右上，面板放在左下。不另行發明版面。
- 質感要精緻、安靜。畫面的主角是螞蟻和地面，UI 只負責標示狀態，不搶戲。
- 世界佔視覺權重 ≥85%：HUD、工具列和收合狀態下的面板，合計面積 ≤15%。
- UI 低彩度、半透明、不發光：不加 glow、外陰影、漸層底。紫色只用在互動狀態。
- 夜間 UI 不變亮：日、夜、雨三種場景用同一組 token，UI 不隨場景調亮度。

## 2. 可檢查的原則
| 面向 | 原則（違反任一條就是必改） |
|---|---|
| 構圖 | 世界視窗不被 UI 切斷；HUD 離螢幕邊 12dp；面板可以收合；同一時間最多開一個側面板。 |
| 色彩與對比 | UI 只用 Theme token，程式裡不出現新的 `Color(...)` 寫死值；`primary` 系列只用於 hover、pressed、selected、focus、開關 on；文字對比度見 §4。 |
| 字體排版 | 用 kool 內建 Fira Sans MSDF，不改 `classloaderAssetPath`；字級只用 10／12／14／18sp；HUD 數字欄位固定寬（`size.hudDigit`），不隨數值跳動。 |
| 間距與層級 | 群組內 2dp、群組間 6dp、面板內距 8dp；層級只有兩層：`surface`（HUD）和 `surfaceRaised`（面板、tooltip），不疊第三層。 |
| 動效 | hover 120ms；按下縮到 0.96、120ms、不回彈；面板開關 160ms；tooltip 延遲 0.45s。沒有循環動畫，也沒有閃爍（時鐘石冒號屬於世界物件，不算 UI）。 |
| 光影與材質 | 世界物件的光影由 3D 管線負責（見 `3d_visual_overhaul_overview.md`）；UI 不吃 bloom、不受場景光照影響；發光只保留給世界裡本來就會發光的東西（例如屍體球），不用在 UI。 |
| 資訊密度 | HUD 一行一項資訊，不超過 6 行；次要資訊用 `onSurfaceVar`，不靠縮字級來降級；說明文字放 tooltip，不常駐。 |

## 3. 已定案 token（已核對 main 的 `Theme.kt`）
| Token | 值 | 來源 |
|---|---|---|
| `surface` | `#120F17` α0.72 | #3 → PR #16（從 α0.62 調高，原因是對比度） |
| `surfaceRaised` | `#1C1822` α0.86 | #3、PR #16 |
| `surfaceVariant` / `outline` | `#2A2730` / `#EADDFF` α0.10 | #3 |
| `onSurface` / `onSurfaceVar` | `#EEEEEE` / `#BDBDBD` | #3 → PR #16（從 #AAAAAA 改） |
| `onSurfaceDisabled` | `#EEEEEE` α0.38 | #3 |
| `primary` / `primaryVariant` / `primaryContainer` / `onPrimary` | `#6750A4` / `#4F378B` / `#EADDFF` / `#FFFFFF` | #3 |
| `primaryHover` / `primaryPressed` | `#EADDFF` α0.12 / α0.20 | #3、#5 |
| 按下時的底色 | 一般按鈕用 `primaryPressed`；選中的按鈕被按下時用 `primaryVariant` | #5、PR #23 |
| 選中狀態外框 | 1dp `primaryContainer` #EADDFF（`stroke.selected`） | PR #16 |
| `danger` / `onDanger` / `success` | `#601410` / `#F2B8B5` / `#6FDF6F` | #3 |
| 圓角 | sm 4dp、md 6dp（按鈕，kool 控制項也套用）、lg 10dp（面板） | #3、PR #16 |
| 間距 | s1–s6 = 4/8/12/16/20/24dp；組內 2dp、組間 6dp；面板內距 8dp；螢幕邊距 12dp | #3、PR #16 |
| 尺寸 | 按鈕 28dp、圖示 18dp、HUD 數字欄位 11dp | #5、PR #23 |
| 字級 | 10 / 12 / 14 / 18sp | #3 |
| 動效 | 按下縮到 0.96、120ms；tooltip 延遲 0.45s（原規格 0.6s，PM 改成 0.45s） | #5、PR #23 |

## 4. 無障礙
- **對比度**：文字對背景 ≥ 4.5:1。最差情況是半透明底疊在最亮的日景天空 `#C8D8E8` 上：`#EEEEEE` 為 7.93:1，`#BDBDBD` 為 4.90:1，選中圖示 `#EADDFF` 對 `primary` 為 5.0:1，外框對 HUD 底為 7.15:1（以上都是 WCAG 公式的**計算值**，`ThemeTest` 有守住）。disabled 狀態可以低於標準，這是 WCAG 豁免。
- **色弱**：用 Machado 2009 模擬 protan／deutan／tritan，CIEDE2000 ΔE < 12 視為分不開，這時一定要有非顏色提示（形狀、線型、標籤、文字名稱）。
- **AntSim 資料色不改**（糖 `#FFD700`、蛋白 `#CD5C5C`、蟻色預設、圖表四色）。分不開時加非顏色提示，不調色。
- **選中狀態三重提示**：`primary` 底色、1dp 外框、實心版圖示（如果圖示沒有實心版，至少要有前兩種）。
- **圖示視覺重量一致**：同一列不要實心、空心混用到明顯一輕一重；播放控制一律用實心版。

## 5. 圖示
- 來源是 Material Symbols Rounded，參數 `FILL 0`、`wght 400`、`GRAD 0`、`opsz 20`；選中時改用 `_fill`（FILL 1）。SVG 只能有單一路徑、填色為 `#FFFFFF`，由 tint 上色。
- 顯示尺寸 18dp，放在 28dp 按鈕裡；載入時點陣化成 54px（3 倍）。
- 每個圖示都要有 EN/DE tooltip，只寫實際有綁定的快捷鍵；圖示載入失敗時退回 ASCII 文字。
- 新增圖示時，要同時更新 `docs/programming/THIRD_PARTY_NOTICES.md` 的檔名清單（Apache-2.0）。對照表在 #5。

## 6. 審查流程與清單
1. 讀 PR 描述、diff、留言和 CI；只看 HEAD sha，並寫在留言裡。
2. 逐項對照 §2–§5。
3. 留言開頭標 `[Artist]`，分成「必改」（擋合併）和「建議」（可以另開 issue），每點都寫出要改哪裡、改成什麼值。
4. 每個數字都要標明是**計算值**還是**實測值**。
5. 用 headless Chrome 渲染 SVG 或 mock 只算**預覽**，不等於 kool 的實機畫面；留言中不能把預覽寫成截圖。
6. 結論寫 `[Artist] PASS` 或 `[Artist] FAIL`。PASS 時要註明還沒做的確認項目。
7. 桌面版日、夜實機畫面的最終驗收由 Neo 在 macOS 執行 `./gradlew :koolApp:runDesktop` 並截圖；Artist 看過截圖後才算完成視覺驗收。

## 7. 參考資料
- 原作截圖：`docs/programming/antsim_screenshots/`
- 3D 參考與比較圖：`docs/programming/3d_refs/references/`（說明見該目錄的 README）
- 現況截圖與自評：`docs/programming/3d_refs/shots/REVIEW.md`、`docs/screenshots/`
- 圖示資產：`koolApp/assets/icons/`

## 8. 待決事項與修訂紀錄
**待決**
- 日、夜實機截圖的視覺驗收（#3、#5、PR #16、PR #23）還沒做，等 Neo 補拍。
- Stats 圖表用線型區分（#19）。
- 次要文字、tooltip 快捷鍵改用 `onSurfaceVar` 顯示（#18）。
- `search` 圖示沒有實心差異，是否換成其他圖示還沒決定。

**修訂紀錄**
| 日期 | 變更 |
|---|---|
| 2026-10-21 | 初版：整理 #3、#5、PR #16、PR #23 的定案。 |
