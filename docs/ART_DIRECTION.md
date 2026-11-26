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
| 色彩與對比 | UI 只用 Theme token，程式裡不出現新的 `Color(...)` 寫死值；`primary` 系列只用於 hover、pressed、selected、focus、開關 on；文字按鈕與圖示按鈕同一套 hover／pressed／selected 規則，不使用 kool 預設青色與點擊擴散圈；文字對比度見 §4。 |
| 字體排版 | 用 kool 內建 Fira Sans MSDF，不改 `classloaderAssetPath`；字級只用 10／12／14／18sp；HUD 數字欄位固定寬（`size.hudDigit`），不隨數值跳動。 |
| 間距與層級 | 群組內 2dp、群組間 6dp、面板內距 8dp；層級只有兩層：`surface`（HUD）和 `surfaceRaised`（面板、tooltip），不疊第三層。 |
| 動效 | hover 120ms；按下縮到 0.96、120ms、不回彈；面板開關 160ms；tooltip 延遲 0.45s。沒有循環動畫，也沒有閃爍（時鐘石冒號屬於世界物件，不算 UI）。 |
| 光影與材質 | 世界物件的光影由 3D 管線負責（見 `3d_visual_overhaul_overview.md`）；蟲體可讀性見 §6.8，不以全域後處理取代可讀性層；UI 不吃 bloom、不受場景光照影響；發光只保留給世界裡本來就會發光的東西（例如屍體球），不用在 UI。 |
| 資訊密度 | HUD 一行一項資訊，不超過 6 行；次要資訊用 `onSurfaceVar`，不靠縮字級來降級；說明文字放 tooltip，不常駐。 |

## 3. 已定案 token（已核對 main 的 `Theme.kt`）
| Token | 值 | 來源 |
|---|---|---|
| `surface` | `#120F17` α0.72 | #3 → PR #16（從 α0.62 調高，原因是對比度） |
| `surfaceRaised` | `#1C1822` α0.86 | #3、PR #16 |
| `surfaceVariant` / `outline` | `#2A2730` / `#EADDFF` α0.10 | #3 |
| `outlineStrong` | `#EEEEEE` α0.40 | #87 |
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

**文字按鈕狀態**（#87；對比皆為 WCAG 公式的**計算值**，不四捨五入）

| 狀態 | 底 | 字 | 邊框 1dp |
|---|---|---|---|
| rest | `surfaceVariant` #2A2730 | `onSurface` #EEEEEE，**12.65** | `outlineStrong`（疊在底上 #78777C，對底 **3.29**） |
| hover | rest 再疊 `primaryHover`（= #413D49） | #EEEEEE，**9.12** | 同 rest |
| pressed | rest 再疊 `primaryPressed`（= #504B59），內容縮 0.96、120ms、不回彈 | #EEEEEE，**7.22** | 同 rest |
| selected（對應面板開著） | `primary` #6750A4 | `primaryContainer` #EADDFF，**5.00** | 1dp `primaryContainer` |
| selected + pressed | `primaryVariant` #4F378B | #EADDFF，**7.23** | 同上 |
| disabled | 無底 | `onSurfaceDisabled`（WCAG 豁免，3.11） | `outline`（#EADDFF α0.10） |
| danger（Restart）rest | `danger` #601410 | `onDanger` #F2B8B5，**7.66** | `onDanger` α0.55（= #B06E6B，對底 **3.30**） |
| danger hover／pressed | 疊 `primaryHover`／`primaryPressed`（#712C2D／#7C3C40） | **5.87**／**4.78** | 同 danger rest |

## 4. 無障礙
- **對比度**：文字對背景 ≥ 4.5:1。最差情況是半透明底疊在最亮的日景天空 `#C8D8E8` 上：`#EEEEEE` 為 7.93:1，`#BDBDBD` 為 4.90:1，選中圖示 `#EADDFF` 對 `primary` 為 5.0:1，外框對 HUD 底為 7.15:1（以上都是 WCAG 公式的**計算值**，`ThemeTest` 有守住）。disabled 狀態可以低於標準，這是 WCAG 豁免。
- **色弱**：用 Machado 2009 模擬 protan／deutan／tritan，CIEDE2000 ΔE < 12 視為分不開，這時一定要有非顏色提示（形狀、線型、標籤、文字名稱）。
  - 3D 室外鉗制後的 Copper 與 Gold（敵群），protan／deutan 分不開（ΔE 1.3／3.7，計算值）；非顏色提示為 Inspect 的所屬群列（#109）。
- **AntSim 資料色不改**（糖 `#FFD700`、蛋白 `#CD5C5C`、蟻色預設、圖表四色）。分不開時加非顏色提示，不調色。
  - 蟻色的 hex 和名稱不改。3D 室外的 albedo 可以由 hex 換算、鉗制（§6.7 `CHITIN_Y_MAX`），或由 Artist 指定字面值（Copper、Black）（#86）。
  - 費洛蒙 home 色因 #87 改為 `#0088D9`（紋理位元組；3D 室外位元組當線性值乘 boost，巢穴與 Classic 2D 為顯示空間加法）；sugar、protein、danger 不變。理由：舊色在正午褐土上讀成紫／洋紅，與 danger 分不開。
- **選中狀態三重提示**：`primary` 底色、1dp 外框、實心版圖示（如果圖示沒有實心版，至少要有前兩種）。
- **圖示視覺重量一致**：同一列不要實心、空心混用到明顯一輕一重；播放控制一律用實心版。

## 5. 圖示
- 來源是 Material Symbols Rounded，參數 `FILL 0`、`wght 400`、`GRAD 0`、`opsz 20`；選中時改用 `_fill`（FILL 1）。SVG 只能有單一路徑、填色為 `#FFFFFF`，由 tint 上色。
- 顯示尺寸 18dp，放在 28dp 按鈕裡；載入時點陣化成 54px（3 倍）。
- 每個圖示都要有 EN/DE tooltip，只寫實際有綁定的快捷鍵；圖示載入失敗時退回 ASCII 文字。
- 新增圖示時，要同時更新 `docs/programming/THIRD_PARTY_NOTICES.md` 的檔名清單（Apache-2.0）。對照表在 #5。

## 6. 審查流程與清單
1. 讀 PR 描述、diff、留言和 CI；只看 HEAD sha，並寫在留言裡。
2. 逐項對照 §2–§5。若 PR 涉及室外蟲體或可讀性層，也要對照 §6.7–§6.8 與 `docs/screenshots/MEASURE.md`。若涉及費洛蒙軌跡，也要對照 §6.9。
3. 留言開頭標 `[Artist]`，分成「必改」（擋合併）和「建議」（可以另開 issue），每點都寫出要改哪裡、改成什麼值。
4. 每個數字都要標明是**計算值**還是**實測值**。
5. 用 headless Chrome 渲染 SVG 或 mock 只算**預覽**，不等於 kool 的實機畫面；留言中不能把預覽寫成截圖。
6. 結論寫 `[Artist] PASS` 或 `[Artist] FAIL`。PASS 時要註明還沒做的確認項目。
7. 桌面版日、夜實機畫面的最終驗收由 Neo 在 macOS 執行 `./gradlew :koolApp:runDesktop` 並截圖；Artist 看過截圖後才算完成視覺驗收。

## 6.7 室外蟻殼（正確法線下的 albedo，#94／#95）

原則：**不以錯誤法線把殼壓暗來換 D1**。甲殼 NORMAL 必須與外向 winding 一致；對比靠材質與光照，不靠 faceforward 或朝內法線。材質與光照的正午基準以 #95 為準，之後不再為了對比去改 albedo、roughness 或法線（#86 自選色亮度上限除外）。可讀性層與驗收門檻見 §6.8。

色票名稱必須和 2D、巢穴剖面、室外 3D 三處畫面看到的顏色相符。可讀性不足時用亮度鉗制處理，不用改名掩蓋（#86）。

| 參數 | 字面值（線性） | 備註 |
|---|---|---|
| `WorldPalette.antChitin`（Black 色票，#86 起） | `(0.036, 0.036, 0.039)` | 計算值起點（由錯誤法線下 0.07、D1 3.53→目標 ≈4.71 反推 k≈0.52）；實測微調容許 0.035–0.038 |
| `WorldPalette.copperChitin` | `(0.165, 0.040, 0.002)` | Copper（預設）在 3D 室外用的真銅色，Y 0.0638；不經 ×0.52 與鉗制。正午 D1 3.82（實測值(headless)，#86 Artist 規格） |
| `WorldPalette.rivalChitin` | `(0.0857, 0.0644, 0.0104)` | 敵群（colony 1）＝ Gold `#d9bf52` 走自選色管線（×0.52，`CHITIN_Y_MAX` 鉗制）的結果，Y 0.065；不理會玩家 antColor；正午 D1 3.58（實測值(headless)，Gold 代理，#109） |
| `CHITIN_ALBEDO_SCALE` | `0.52` | 自選色 `outdoorChitin`：`toLinear()` 後再乘此係數，避免法線翻正後自選色再亮一截。鉗制見 `CHITIN_Y_MAX` |
| `CHITIN_Y_MAX` | `0.065` | 自選色 `toLinear×0.52` 後，線性亮度 Y＝0.2126R＋0.7152G＋0.0722B 超過此值就把 RGB 等比縮小，色相與 channel 比例不變；不設下限；Y 已 ≤0.065 的顏色照原樣。適用 Copper、Black 以外的色票與設定檔裡的任意 hex，以及敵群（#86、#109） |
| `CHITIN_ROUGHNESS` | `1.0` | 不動（#79） |
| `antEye`／瓢蟲／甲蟲／地面／光照 | — | #94／#95 未改 |

觀感：陰影核可暗，但體節受光明暗要留住。正午驗收數字見 §6.8。

## 6.8 角色可讀性（方案 C，2026-11-11 定案）

研究依據見 `docs/research/character-readability.md`（#96）。量法與腳本見 `docs/screenshots/MEASURE.md`。

原則：把「甲殼看起來的樣子」和「讓蟲找得到」分開。材質與光照維持 #95 的正午基準。可讀性另外做一層，只作用在蟲體輪廓。

可讀性層的作法如下。

- 優先用沒有側面基底的 fresnel／rim，而且所有後端行為一致。
- 若俯視小尺寸仍不夠，再試薄的深度描邊，或角色後方的分隔墊。
- 全域 tone mapping、LUT、bloom 不能當角色分離的主手段。
- 可讀性層關閉（k=0）時，正午畫面須與 #95 基準一致（像素或度量）。#95 正午 headless 兩輪實測值：D1 4.62、D2 3.31、D3 3.28。

驗收在正午 `shot=specimen` 進行，兩輪皆須達標。對比值用 WCAG 相對亮度，公式與 `docs/screenshots/79-chitin/measure.py`、`81-beetle/measure.py` 相同，不能四捨五入。

| 指標 | 門檻 | 用途 |
|---|---|---|
| D1 蟻身／地（本體填色） | ≥3.0 | 地板：本體不要和地面糊在一起。不再用 ≥4.5。這是 #79 當初的目標。 |
| D3 甲蟲本體／地（本體填色） | ≥3.0 | 同上。不再用 ≥3.25。 |
| D2 瓢蟲殼／斑點（內部識別） | ≥3.3 | 物件內部圖案，與 rim 分開驗。 |
| 輪廓對比 | 見下方合格帶；量法見 `docs/screenshots/MEASURE.md`。 | 用來驗 rim 或描邊。k=0 時增益接近 1.000，加強時單調上升；本體內部中位數維持 ≤1.01（期望 1.000）。 |
| 灰階檢查 | `shot=specimen` 去飽和後，在實際遊戲尺度下能立刻指出工蟻、兵蟻、甲蟲、瓢蟲。 | Artist 審查項，不必第一次就自動化。 |

正午輪廓合格帶（`shot=specimen`；#98 定案，#105 Artist 復審改量準）：量準是 `docs/screenshots/84-rim/contour.py --body` 的本體帶。遮罩先去掉 ≤4px 的腳、觸角、大顎，再取外緣 2px。每張單獨過、兩輪都要過。預設 2px 帶的數字只記錄，不判合格（原因見 MEASURE.md §4）。

| 對象 | 本體帶 edgeGain | 內部中位數 | 內部平均 |
|---|---|---|---|
| 工蟻（兵蟻暫同；specimen 尚無兵蟻構圖） | 1.15–1.35 | ≤1.01 | ≤1.01 |
| 甲蟲 | 1.07–1.16 | ≤1.01 | ≤1.03 |

內部中位數：期望值 1.000。超過時，變動像素須只在本體邊緣往內 ≤3 px，而且每像素 ≤1 個 8-bit 級（#110：Copper 1.005 是量化造成）。

甲蟲剪影較圓，同樣 k 下邊帶增益較低，所以分兩條帶；rim 參數仍共用。#105 正午實測值(headless)：蟻本體帶 1.237、甲蟲 1.105；內部中位數皆 1.000，內部平均 1.001／1.018。不要為了把預設 2px 帶拉回舊下限而調高 `RIM_DAY`。

自選蟻色（#86）：每個色票都要過正午 D1 ≥3.0、hot% ≤2%，兩輪各自判。預設色票（Copper）另外要過正午與夜間的輪廓合格帶，k0 圖用同 build 的銅色圖。Black：3D 室外（x<1180，排除 FPS 區）與 main `b405ae4` 預設 specimen 逐像素相同；面板／巢穴剖面（x≥1180）與 main 同色票（`#222222`）的 specimen 逐像素相同（#110 更正）。

仍保留：瓢蟲觀感與 shell／gnd 不變；hot%≤2%；mid 不高於同 build、同色票 k0 的 1.10 倍（色票 albedo 沒變的 PR，也可對照 main 同色票；色票本身亮度由 CHITIN_Y_MAX 0.065、D1、hot% 管）；兩輪一致。桌面實機畫面仍等 Neo 補拍（見 §6）。

敵蟻（#109）：3D 室外 `rivalChitin` 走 Gold `#d9bf52` 自選色管線（見 §6.7）。驗收用含敵蟻的可重現構圖（例如 `specimenRival=1`），或 colony 0 換色票的對照圖；兩輪各自判。

- 正午可讀性（R3）：敵蟻正午 D1 ≥3.0、hot% ≤2%。期望約 3.58／0.0（實測值(headless)，Gold 代理）。D2 3.31、瓢蟲 shell/gnd 1.62 不變（實測值(headless)，與 #86 相同）。
- 正午分辨（R4）：量敵蟻本體中位色的 CIEDE2000。對 Copper ≥10（期望 14.8，實測值(headless)），色相差 ≥20°（期望 36°）；對 Black、Sage、Blue 各 ≥10（期望 12.0／15.3／22.6，實測值(headless)）；對甲蟲本體 ≥6（期望 8.0，實測值(headless)），另外靠體型分辨；對地面 ≥25（期望 32.9，實測值(headless)）。玩家選 Gold 時 ΔE 0，接受（兩群在 3D 同色；非顏色提示見 §4）。
- 夜間可讀性（R5）：用同一構圖與同 build 的 k0（`RIM_DAY`、`RIM_NIGHT` 設 0 的量測 build）。夜間合格帶同工蟻（本體帶 1.75–2.55，內部中位 ≤1.10、平均 ≤1.20，core/gnd ≤1.5）；細結構 ≤1.05，頭前 ≥3:1 的像素 ≤k0；N4 照本節 #110 條文（抬亮像素的 Δ(B−R) 中位 ≥0、加上的線性光 B≥R，本體帶最亮 15% 的絕對 B−R ≥0）；N5 地面不變。夜間只驗可讀性，對 Copper 的夜間 ΔE 只記錄。

夜間（`shot=specimen-night`）不要套用正午的 D1／D3 門檻，也不要套用上面的正午輪廓合格帶。本體填色可以低於 3。夜間用同一套可讀性層，強度與色溫可以不同，以輪廓對比和灰階「找得到」為驗收。

夜間合格帶（#91，#105 Artist 定案）：量準同樣是 `contour.py --body` 的本體帶，遮罩用同一 build 的正午 k=0 圖（`--mask`）。每張單獨過、兩輪都要過，不平均。

| 對象 | 本體帶 edgeGain | 內部中位數 | 內部平均 | core/gnd |
|---|---|---|---|---|
| 工蟻（兵蟻暫同） | 1.75–2.55 | ≤1.10 | ≤1.20 | ≤1.5 |
| 甲蟲 | 2.10–3.25 | ≤1.20 | ≤1.75 | ≤1.5 |

夜間另外必過：
- 細結構（遮罩減本體）中位增益 ≤1.05。
- 頭前細結構區（不含頭殼）對地 ≥3:1 的像素不得多於 k0。區域：甲蟲 x590–616、y428–457（含兩端；numpy [428:458, 590:617]）；蟻 x571–581、y430–461（含兩端；numpy [430:462, 571:582]）。頭殼前緣的邊光屬本體帶，由本體帶 edgeGain 管。
- N4（rim 色溫）：本體帶上被抬亮（增益 >1.2）的像素，rim 加上的光必須偏冷：同像素「IMG − 同 build 夜 k0」的逐像素 Δ(B−R)（sRGB 8-bit）中位 ≥0，且加上的線性光總和 B ≥ R。本體帶最亮 15% 的中位，仍要求絕對 B−R ≥0。理由：自選暖色殼（例如 Copper）本身 R>B，被抬亮的像素絕對 B−R 不代表 rim 的色溫（#110 裁決）。
- N5 地面 RGB 每個 channel ±2；N6 ≥1.35；夜 D2 ≥1.8；瓢蟲殼與黑點觀感可辨；rim 線性峰值低於 bloom 門檻 1.2。

hot% 夜間不適用。D1／D3 夜間只記參考。觀感：暗身加冷色細邊，不整隻發亮，沒有 bloom，沒有正面亮斑，沒有懸空亮點或斷續閃點。#105 夜間實測值(headless)：蟻本體帶 2.163、內部 1.024／1.100；甲蟲 2.672、1.115／1.575。

UI 文字對比仍見 §4（≥4.5），不和蟲體共用這張表。

## 6.9 費洛蒙可讀性（#87 Artist 規格）

**費洛蒙 home 色（P5）量法與門檻。**
1. 對照圖：同 build、同 seed、同模擬秒數的兩張圖，一張正常，一張把費洛蒙關成零光（headless 用 localStorage `pheromoneIntensity=0.0001`，不要用 `pheromones=false`，後者會讓 3D 土壤變黑）。
2. home 像素：正常圖減對照圖，B 增量 > R 增量＋8，B 增量 >0，G 增量 ≥0，且 ΔE2000 ≥5。排除糖（黃）、danger（洋紅）、蟻影（變暗）。
3. 取「ΔE 大於等於 home 像素 ΔE 中位數」的上半群，量下列兩項的**中位數**。
4. **可見度**：上半群對對照圖同位置地面的 ΔE2000，正午 ≥15、夜間 ≥30。
5. **色相**：用位移向量（對照圖到正常圖的 Lab Δa*、Δb*）的色相，範圍 190–270°（藍青；排除 ≥290° 的紫／洋紅）。**不用像素本身的 Lab 色相**：褐土加藍光後彩度低於約 10，色相無定義。夜間像素本身色相 190–250° 另作參考。
6. 全 home 像素（含低濃度光暈）的 ΔE 只記錄，不作門檻。
7. 正午的 home 軌跡在褐土上讀成冷灰白，夜間讀成青藍，這是加法混色的結果，接受；桌機實拍不得出現光暈（P7）。

實測值(headless，#116 HEAD `300e0fb`)：正午上半群 ΔE 20.2、位移色相 251°；夜間上半群 ΔE 33.1、位移色相 237°。

夜間最大強度 bloom 亮度 ≤1.4；與五個蟻色票的 ΔE ≥10；公式保持「無 home 像素不變」。bloom ≤1.4 是計算值（夜間最大強度 0.37R+0.41G+0.22B × boost × 0.85），因為 headless 不啟用 bloom；桌面實機要等 Neo 拍日景、夜景各一張，確認沒有光暈。

deutan 下 home 對 danger 的 ΔE 為 5–10，接受現況（與舊色相當）；danger 色不動，以 Inspect 點地面時各費洛蒙通道的文字名稱與數值當非顏色提示。

## 7. 參考資料
- 原作截圖：`docs/programming/antsim_screenshots/`
- 3D 參考與比較圖：`docs/programming/3d_refs/references/`（說明見該目錄的 README）
- 現況截圖與自評：`docs/programming/3d_refs/shots/REVIEW.md`、`docs/screenshots/`
- 角色可讀性量測：`docs/screenshots/MEASURE.md`；研究：`docs/research/character-readability.md`（#96）
- 圖示資產：`koolApp/assets/icons/`

## 8. 待決事項與修訂紀錄
**待決**
- 日、夜實機截圖的視覺驗收（#3、#5、PR #16、PR #23）還沒做，等 Neo 補拍。
- 是否加薄描邊仍待評估。
- 兵蟻沒有 specimen 構圖，合格帶暫用工蟻的（#103）。
- Stats 圖表用線型區分（#19）。
- 次要文字、tooltip 快捷鍵改用 `onSurfaceVar` 顯示（#18）。
- `search` 圖示沒有實心差異，是否換成其他圖示還沒決定。

**修訂紀錄**
| 日期 | 變更 |
|---|---|
| 2026-11-26 | #109 Artist 規格：敵蟻 3D 室外 `rivalChitin` 改為 Gold `#d9bf52` 走自選色管線的字面值 (0.0857, 0.0644, 0.0104)，Y 0.065。寫入 §4 色弱加註、§6.7 表（含 `CHITIN_Y_MAX` 適用敵群）、§6.8 敵蟻驗收 R3–R5。 |
| 2026-11-26 | #116 Artist 裁決：P5 改用位移向量色相與上半群 ΔE；headless 用 `pheromoneIntensity=0.0001` 當對照 |
| 2026-11-25 | #87 Artist 規格：文字按鈕用 MyantsTheme（新 token `outlineStrong` `#EEEEEE` α0.40）；費洛蒙 home 改為 `#0088D9`。寫入 §2、§3 文字按鈕狀態表、§4 加註與 §6.9。 |
| 2026-11-25 | #110 Artist 裁決：§6.8 mid 條改為不高於同 build、同色票 k0 的 1.10 倍（色票 albedo 沒變的 PR 也可對照 main 同色票）。色票本身亮度由 CHITIN_Y_MAX 0.065、D1、hot% 管。#110 Copper 對 k0 1.011 倍通過，對 main 1.26 倍接受。 |
| 2026-11-12 | #110 Artist 裁決：正午本體帶內部中位數改為 ≤1.01（期望 1.000，附量化條件）；夜間 N4 改看 rim 加上的光 Δ(B−R) 中位 ≥0 且加光 B ≥ R，最亮 15% 仍要絕對 B−R ≥0；#86 Black 驗收改為室外與巢穴分區比對。 |
| 2026-11-12 | #86 Artist 規格：Copper 在 3D 室外改用真銅色 `copperChitin` (0.165, 0.040, 0.002)，名稱、hex、2D、設定檔不動；Black 改用 `antChitin`；自選色加 `CHITIN_Y_MAX` 0.065 等比鉗制。§4 補註、§6.7 表與原則、§6.8 自選色驗收。 |
| 2026-11-12 | #105 合併後依 Artist 復審改寫 §6.8：正午量準改為 `contour.py --body` 本體帶（工蟻 1.15–1.35、甲蟲 1.07–1.16），預設 2px 只記錄；寫入夜間合格帶（工蟻 1.75–2.55、甲蟲 2.10–3.25）與夜間必過項；甲蟲頭前區改為 x590–616、不含頭殼。 |
| 2026-11-11 | #98 合併後寫入正午輪廓合格帶：工蟻 edgeGain 1.15–1.35、甲蟲 1.10–1.22；內部中位數 1.000；內部平均蟻 ≤1.01、甲蟲 ≤1.03。量法見 `docs/screenshots/MEASURE.md`。夜間門檻仍待 #91。 |
| 2026-11-11 | 方案 C 定案：可讀性層與材質分離；正午 D1／D3 放寬為 ≥3.0，D2 維持 ≥3.3；rim 改用輪廓對比與灰階檢查。見 §6.8 與 `docs/screenshots/MEASURE.md`（研究 #96）。 |
| 2026-10-21 | 初版：整理 #3、#5、PR #16、PR #23 的定案。 |
