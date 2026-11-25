# #87 文字按鈕 selected 狀態與費洛蒙回家色 #0088D9

規格：Artist 留言 https://github.com/neojou/myants/issues/87#issuecomment-7163993223（B1–B6、P1–P8）；範圍：PM 留言 https://github.com/neojou/myants/issues/87#issuecomment-7164018110。ART_DIRECTION 條文在 PR #114（本 PR 不改 ART_DIRECTION）。Checkbox／Slider 是 #115，本張不做。

## 改了什麼
- 新 `TextButton`／純函式 `textButtonVisual`，狀態色依 ART_DIRECTION §3 文字按鈕表；新 token `outlineStrong`（`#EEEEEE` α0.40）。
- 工具列 Graphics／Params／Stats／Analysis／About、Restart，以及 RestartDialog、AboutDialog、Analyse 建議、Params Reset、側面板 x、語言選單、Inspect Follow，全部改用同一個 `TextButton`。不再使用 kool `Button`（無預設青色、無點擊白色擴散圈）。
- 面板開著時，對應的 Graphics／Params／Stats／Analysis／About 顯示 selected（`primary` 底、`primaryContainer` 字與 1dp 邊框）。語言選單的目前語言、Follow 追蹤中，同樣走 selected。
- `PheromoneColor.pack` 成為 3D 室外、巢穴剖面與 Classic 2D 的唯一費洛蒙色函式；`HOME`＝`#0088D9`（位元組 0,136,217，當線性值，不做 sRGB→線性）。無 home 的像素與舊公式逐位元相同。
- 不動：sim 行為、`RIM_*`、roughness、`CHITIN_*`、sugar／protein／danger 貢獻、蟻色、`WorldPalette` 其他色、Checkbox／Slider。

## 量法
Playwright + Chrome headless（swiftshader），1600×900、seed=12345、quality=medium、`cinematic=false`（寫入 `localStorage antsim.ui.v1`）、假時鐘（`shot87.py`，同 #84／#91／#86 的 shot_lock：先跑指定毫秒再 resume）。一次一張，port 18911。3D 構圖用 `shot=day`／`shot=night`（模擬 10 秒）；UI 狀態圖模擬 3 秒後再點或移動滑鼠。

字像素：距目標字色（rest／hover 為 `#EEEEEE`，danger 為 `#F2B8B5`，selected 為 `#EADDFF`）歐氏距離 ≤20 的像素，取其 RGB 中位數，對同一顆按鈕的不透明底做 WCAG 對比。含抗鋸齒邊緣的寬鬆中位數會被拉低，所以另記 peak（最亮字像素）。門檻不四捨五入。

重跑：`./verify.sh`（只讀已提交的圖）。`buttons.py`、`p5.py` 在本目錄。

| 檔名 | 內容 |
|---|---|
| `main_day.png`／`pr_day.png` | 修改前 main `9e35271` 與 PR，3D 正午 |
| `main_night.png`／`pr_night.png` | 同上，3D 夜間 |
| `main_2d_day.png`／`pr_2d_day.png`、`main_2d_night.png`／`pr_2d_night.png` | Classic 2D 日／夜。檔案 <200KB 是大面積單色壓縮，畫面是完整的 1600×900，不是失敗重拍 |
| `pr_2d_day_blue.png` | Classic 2D 正午、自選蟻色 `#6fa8dc`，看藍蟻與回家軌跡是否可分 |
| `pr_de.png` | 介面語言 DE |
| `pr_params_open.png`／`pr_grafik_open.png`／`pr_about_dialog.png` | 對應按鈕為 selected |
| `pr_langmenu.png`／`pr_restart_dialog.png` | 語言選單、Restart 對話框 |
| `pr_day_hover.png`／`pr_danger_hover.png` | Analysis hover、Restart hover |
| `crops/` | 工具列、對話框、2D 世界與 P5 軌跡遮罩的裁切 |

PR build 是 `35be2ab`（其後只加文件）。main 基準是 `9e35271`。

## 驗收（實測值(headless)，除非標明計算值）

### B1 按鈕狀態色
單元測試 `TextButtonTest`：rest／hover／pressed／selected／selected+pressed／disabled／danger 三態，疊色後 RGB 與規格表逐值相符（±1）。對比計算值：rest 12.65、hover 9.12、pressed 7.22、selected 5.00、selected+pressed 7.23、danger 7.66／5.87／4.78。邊框叠在底上：rest `#78777C`（3.29）、danger `#B06E6B`（3.30）。

### B2 按鈕文字對比（截圖，字像素中位數對同一顆按鈕底）

| 狀態 | 門檻 | 實測 | 結果 |
|---|---|---|---|
| rest（五顆面板鈕，日） | ≥12 | 距 `#EEEEEE` ≤20 的中位：12.65／12.31／12.65／12.65／12.20（Graphics／Params／Stats／Analysis／About）；peak 皆 12.65 | 過 |
| hover（Analysis） | ≥9 | 底正好 `#413D49`，中位 9.06，peak 9.10 | 過 |
| selected（Params、Graphics、About） | ≥4.5 | peak 皆 5.00；Params 寬鬆中位 4.78 | 過 |
| danger rest | ≥4.5 | peak 7.66，寬鬆中位 7.45 | 過 |
| danger hover（Restart） | ≥4.5 | 底正好 `#712C2D`，中位 5.86（計算值 5.87） | 過 |
| pressed／selected+pressed／danger pressed | ≥7／≥4.5／≥4.5 | 單元測試計算值 7.22／7.23／4.78；headless 截不到穩定的按下畫面，以測試為準 | 測試過 |

若把抗鋸齒邊緣算進「字像素」，Stats／About rest 的寬鬆中位會落到 11.76／11.79（低於 12）。距字色 ≤20 的中位與 peak 都過門檻。不改門檻。

### B3 按鈕邊框可見
日、夜工具列：rest 邊框對面板 3.19（≥2.0），邊框對自己的底 3.30（≥3.0）；danger 對面板 3.57、對底 3.30。夜與日數字相同（工具列不透明底）。過。

### B4 程式規範
工具列與對話框已無 kool `Button(`、無 `.colors(`。本張只新增 `outlineStrong`（`Theme.kt`）。Restart 以外不再有kool 預設青／橙色按鈕。Checkbox／Slider 仍是 kool 預設（#115）。過。

### B5 無發光、動效
`TextButton` 不用 kool `Button`，沒有白色擴散圈。hover／按下縮放沿用 `IconButton` 的 `MyantsTheme.motion.pressSeconds` 0.12（120ms）、`pressScale` 0.96、不回彈。過（讀程式 + `iconPressScale` 測試）。

### B6 前後截圖與工具列尺寸
日／夜工具列放大見 `crops/toolbar_day_main_above_pr.png` 與 `*_toolbar.png`。工具列外框（日）：main 與 PR 皆 (1223,12)–(1587,195)，寬高差 0（≤±2dp）。DE：Grafik／Parameter／Verlauf／Analyse／Info、Neustart、Rivalenkolonie 皆未被截斷；bbox 363×184（與 EN PR 365×184 差 2px）。過。

### P1–P4、P6 單元測試、P7 計算、P8
- P1：5000 組隨機 sugar／protein／danger（home=0）、強度 0.2–1.5，新舊 `rgba` 逐位元相同。過。
- P2：`rgba(home=1, 0, 0, 0, strength=1)` = (0,115,184)。過。
- P3：danger-only 與舊值相同；home=danger=0.5 的 G／B 符合權重公式；protein+home ≠ danger。過。
- P4：`ClassicShaders.paint` 與 3D `PheromoneField` 都呼叫 `PheromoneColor.pack`，常數 `PheromoneColor.HOME`。過。
- P6：同一函式（測試 `p6_classicPaintMatchesPheromoneColorRgba`）。Classic 2D 截圖軌跡為青藍色（main 是靛藍），非洋紅；糖仍黃、danger 仍洋紅。`pr_2d_day_blue.png`：藍蟻（`#6fa8dc`）比青藍軌跡更亮，可分辨。過。
- P7：夜間最大強度 bloom 亮度計算值 1.380 ≤1.4。headless 不啟用 bloom；桌面實機日、夜各一張請 Neo 拍，確認沒有光暈。計算過；實機待 Neo。
- P8：sugar、protein、danger 的貢獻與蟻色、`WorldPalette` 其他色未改（P1 與 `ThemeTest.dataColorsUnchanged`）。過。

### P5 3D 螢幕：色相與可見度 — 正式對 nophero 未驗證
門檻：home-only 軌跡像素（相對 `pheromones=false` 同圖差異最大的那群）中位數，Lab 色相 190–250°（現況 304–311°）；ΔE2000 對同位置地面正午 ≥15、夜間 ≥30。

**正式量法未能在 headless 完成**，不改色值、不改門檻。原因（四次嘗試，圖未提交）：

1. 開局就把 `pheromones=false` 寫進 localStorage：3D 室外土壤大面積變黑（巢穴剖面仍正常）。`TerrainRenderer.updatePheromone` 在 `show=false` 且 emissionMap 從未綁定時，swiftshader 上地面著色會出問題。這是既有行為，本張不改。
2. 改為先讓費洛蒙貼圖綁上，再從 Graphics 面板把「Pheromone trails」關掉。四次都關不掉，軌跡仍在：
   - nophero2：假時鐘下連續點擊，第二下點在面板還沒畫出來時；
   - nophero3：改用真時間，暫停成功（工具列變成播放鍵），但 Graphics 打開後只等 1s，swiftshader 約 40 倍慢，面板還沒出現就點核取方塊；
   - nophero4：打開面板後改等 8s，暫停與關面板都成功，核取方塊仍沒把費洛蒙關掉（日圖 12:01 仍清楚看得到糖黃與回家青藍軌跡）。

未在本張加隱藏開關或改 emissionMap 邏輯。請 Artist 用桌面實機或能確實關掉費洛蒙的方式量正式 P5。

同圖近似值（PR 圖裡面積 ≥80 像素的青藍軌跡塊，對**同一張圖**褐土中位數，不是規格寫的「對 pheromones=false 同位置地面」）：

| | 色相中位（p10–p90） | 對同圖褐土 ΔE 中位（p10） | 同位置 main 色相 |
|---|---|---|---|
| 正午 | 211.1（199.0–219.8）在 190–250 | 20.9（19.1）≥15 | 307.0（現況紫／洋紅） |
| 夜間 | 240.4（237.8–243.4）在 190–250 | **25.5（19.2），低於夜間門檻 30** | 301.9 |

遮罩見 `crops/p5_day_blobs.png`、`crops/p5_night_blobs.png`。正午色相與可見度的近似值過門檻；夜間近似 ΔE 低於 30，正式數字以實機／能關掉費洛蒙的對照圖為準。

## 觀感
- 修改前工具列按鈕是 kool 青色底黑字；修改後是深底淺字加 1dp 邊框，Restart 是危險紅。
- 面板開著時，對應按鈕是紫色 selected。
- 3D 與 Classic 2D 的回家軌跡從靛藍／紫洋紅變成青藍，離開 `primary` 紫與 danger 洋紅。
