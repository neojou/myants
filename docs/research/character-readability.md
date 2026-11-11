# 角色可讀性研究：對比門檻、後處理與 myants 建議

- 日期：2026-11-11（Asia/Taipei）
- 作者：[PM]
- 狀態：方案 C 已由 Neo 於 2026-11-11 採用。現行規格見 `docs/ART_DIRECTION.md` §6.8 與 `docs/screenshots/MEASURE.md`。#84、#91、#86、#87 在該文件 PR 合併前維持暫停。
- 目的：回答「一般遊戲（尤其風格化／寫實 3D、小型生物或俯視角）怎麼確保角色在場景中看得清楚」，並對照 myants（kool 0.19、web 與 desktop、M5 這幾輪 D1／D2／D3）提出作法。

本文把「已查證」和「推測／建議」分開寫。來源連結集中在文末；內文只引用查過內容的頁面。

---

## 0. 先講結論

業界**會**為文字、UI，以及「必須被看見的遊戲元件」設量化對比門檻，常見數字來自無障礙規範：一般文字 4.5:1、大字或非文字圖形 3:1、高對比模式 7:1。工作室在角色美術上更常做的，是**剪影檢查、灰階／明度檢查、階層（什麼最搶眼）**，再用 rim／fresnel、描邊、距離加亮、環境光上下限這類「可讀性層」把角色從場景裡分出來，而不是把材質愈調愈暗去衝本體填色對比。

忠實呈現建模與物理光影**通常不夠**。光照與著色在遊戲裡本來就是被設計過的；對比不夠時，常見作法是改光照階層、加輪廓光或描邊、壓背景飽和，或提供可調對比／描邊選項，而不是只回頭改 albedo。

在最後加一層渲染或後處理是可行且常見的，而且正好適合 myants 現在的困境：甲殼 albedo 已經到 `(0.036, 0.036, 0.039)`、roughness 已是 1.0，正午 D1／D3 只剩很窄的餘裕，#84 的共用 rim 只要讓甲蟲本體 G／B 各升 1 階，D3 就從 3.28 掉到 3.24，過不了 3.25。繼續用同一套門檻去改模型和材質，會把「質感」和「可讀」綁死。

**方案 C 已採用。** 當時的建議是：把「材質與光照的樣子」和「可讀性層」分開；正午本體填色對比回到接近 #79 當初的 3:1 地板；另外用輪廓／灰階來驗「看不看得清」；夜間不要套用正午的 4.5。D2（瓢蟲斑點，物件內部識別）可以維持現有偏嚴的門檻。細節見第 4、5 節。

---

## 1. 業界會設「對比門檻」嗎？常見作法是什麼？

### 1.1 已查證：量化門檻主要來自無障礙，而且先用在 UI

下列數字都有公開規範，不是某一款遊戲自己發明的驗收表。

| 對象 | 常見門檻 | 來源 |
|---|---|---|
| 一般文字 | ≥ 4.5:1 | [WCAG 2.1 §1.4.3](https://www.w3.org/TR/WCAG21/#contrast-minimum)；[Game Accessibility Guidelines（基本：文字／UI）](https://gameaccessibilityguidelines.com/provide-high-contrast-between-text-ui-and-background/) |
| 大字、大型視覺元素 | ≥ 3:1 | 同上；[Xbox Accessibility Guideline 102](https://learn.microsoft.com/en-us/xbox/accessibility/xbox-accessibility-guidelines/102) |
| 非文字圖形、用來辨識的圖示或控件 | ≥ 3:1（對**相鄰**顏色） | [WCAG 2.1 §1.4.11](https://www.w3.org/WAI/WCAG21/Understanding/non-text-contrast.html) |
| 高對比模式（開啟後） | UI 元素 ≥ 7:1 | [XAG 102](https://learn.microsoft.com/en-us/xbox/accessibility/xbox-accessibility-guidelines/102) |

幾條對 myants 很重要的原文要點：

- WCAG 的對比是用相對亮度 \((L_1+0.05)/(L_2+0.05)\)，和 myants `measure.py` 的公式相同。1.4.11 寫明 3:1 是門檻、**不能四捨五入**（2.999 不算過）。圖形若「那種呈現方式本身就是要傳達的資訊」，可以豁免。
- XAG 102 把 4.5:1／3:1 用到「提供重要資訊或遊戲脈絡的視覺元素」，並**明確建議為角色或其他關鍵遊戲元素加描邊**；描邊顏色要可調，或預設就能對各種背景保持足夠對比。它也承認遊戲畫面一直在變，無法保證所有情境都達標，所以要提供可設定項（實心底、描邊、高對比模式等）。
- Game Accessibility Guidelines 把「文字／UI 預設對比」列為基本項，採用網頁業的 4.5:1；「提供可調對比」是中級項，作法可以是後處理（例如 gamma）、另一套較清楚的資產，或在前景與背景之間加一層可調色的分隔層。

因此：「對比門檻」在業界是真實存在的，但公開、可引用的數字，幾乎都是**無障礙／UI** 標準，再被 XAG 延伸到關鍵遊戲元素。我沒有查到大型工作室把「角色本體中位數對地面必須 ≥4.5:1，否則 PR 不能合」寫成一般美術流水線的公開規範。

### 1.2 已查證：角色可讀性更常靠剪影、明度與著色，而不是單一填色比

- [Valve，GDC 2008《Stylization with a Purpose》（Team Fortress 2）](https://cdn.cloudflare.steamstatic.com/apps/valve/2008/GDC2008_StylizationWithAPurpose_TF2.pdf)：角色剪影常用 **rim highlight（邊緣高光）而不是深色描邊** 來強調；公式包含 Fresnel 項 \((1-N\cdot V)^4\)，以及 rim 遮罩貼圖。
- [Riot《VALORANT Shaders and Gameplay Clarity》（2020）](https://www.riotgames.com/en/news/valorant-shaders-and-gameplay-clarity)：角色必須從環境中站出來。作法是在掠射角加 **fresnel 剪影光**（敵紅、友藍），並依距離加亮、加大遠處的 fresnel；環境光有明暗上限，避免角色在暗處消失或在亮處爆掉。著色本身就不是純物理：漫射用從 Valve Half-Lambert 延伸的 Gradient Lambert，讓美術能控制高光／中間調／核心陰影。
- [Riot《Clarity in League》（2021）](https://www.leagueoflegends.com/en-us/news/dev/clarity-in-league/)：俯視／斜俯視時，**剪影是識別角色最重要的一件事**；還要維持視覺階層（當下最重要的東西最搶眼）、控制雜訊。新皮膚要在各張地圖上檢查是否仍然清楚。
- [Game Developer，《How to Reduce Visual Confusion in Your Game》](https://www.gamedeveloper.com/design/how-to-reduce-visual-confusion-in-your-game)：把截圖去飽和，看能不能立刻讀懂畫面。去掉顏色之後剩下的是明度（value），它負責形狀、深度，以及把視線帶到你要玩家注意的地方。

### 1.3 推測（標明）

把「截圖去飽和、兩秒內能否找到角色」當美術必做檢查，是很常見的實務，我也建議 myants 採用；但各工作室的內部尺規（例如「明度差要幾階」）很少公開，**不應臆造數字當門檻**。3:1 可以當「本體填色不要再塌掉」的地板，因為它與 WCAG 非文字圖形、XAG 大型元素一致；4.5:1 則比較像文字／較小 UI 的標準，拿來卡小型 3D 蟲的本體中位數，會偏嚴。這是對規範適用範圍的判斷，不是規範原文替昆蟲模擬寫好的句子。

---

## 2. 忠實呈現建模材質和光影就夠嗎？對比不足時怎麼處理？

**不夠。** 這是已查證來源裡反覆出現的立場，不是推測。

VALORANT 的文章開頭就寫：著色是在模擬光的物理，但遊戲必須在效能與可讀性之間取捨，而且他們刻意不把「寫實」當主柱，改用美術可控的漫射與環境光上下限。TF2 採用 Leyendecker 式的暖冷色推移，並用 rim 強調剪影。也就是說，就算資產是寫實掃描或 PBR 工作流，**最後畫面上的光影仍然是設計過的**。

對比太低、畫面糊成一團時，公開案例裡常見的處理（由強到弱、可並用）：

1. **先檢查明度階層，而不是先改色相**：去飽和後，角色與背景是否還分得開（Game Developer 上文）。
2. **加可讀性光，而不是把物體自身顏色愈調愈暗**：TF2 的 rim、VALORANT 的 fresnel 與遠處加亮、XAG 建議的角色描邊。
3. **控制環境不要搶角色**：VALORANT 寫過，環境佔大部分畫面，玩家心裡會把它「擋掉」才能專心看角色；環境著色要便宜，角色功能不能為了畫質檔位而丟掉。
4. **給玩家選項**：可調對比、高對比模式、描邊開關（XAG 102、Game Accessibility Guidelines 中級項）。Eagle Island 被 XAG 舉為可把背景變暗、並為角色與平台加白邊的例子。
5. **這才回頭改材質或模型**：法線錯誤、roughness 把高光打成一片灰、內部圖案對比不夠——這些是資產問題，應該修資產。myants 的 #79（蟻身被高光洗成淺灰、瓢蟲斑點不夠深）和 #94（殼法線朝內）屬於這一類，修法是對的。

WCAG 1.4.11 也留了豁免：若某種圖形呈現本身就是要傳達的資訊（例如必須看起來像暗色甲殼的昆蟲），不一定要用改填色的方式去湊對比；此時更合理的是用描邊、rim、或可選的高對比模式來補「找不找得到」。

---

## 3. 最後加一層渲染或後處理，而不是回頭改模型和材質，可行嗎？

**可行，而且是主流做法之一。** 但要分清「整體調色」和「角色分離」，兩者不是同一件事。

### 3.1 各手段在做什麼（已查證＋對 kool 的對照）

| 手段 | 主要作用 | 能不能把角色從地面分開 | 在 kool 0.19／myants 的現況 |
|---|---|---|---|
| Tone mapping／曝光 | 把 HDR 壓到螢幕、保住亮暗細節 | 弱。全域一條曲線，角色和地面一起變 | 專案文件未記載可調的 ACES／filmic 控制 |
| Color grading／LUT | 統一色調與風格 | 弱。同樣是整張圖在調 | 未見現成 LUT 管線 |
| Bloom | 讓過亮的emissive 滲開 | 弱，有時反而糊邊。食物、費洛蒙在用 | 有；強度 0.35、門檻 1.2、半徑 2。需要 compute，**Vulkan／WebGPU 才開，Desktop GL 與 WebGL2 會關** |
| SSAO／接觸陰影 | 物體與地面交界變暗，位置更清楚 | 中。幫忙「貼地」，不一定夠剪影 | 有 SSAO，品質檔可開關；#83 已把雜訊改成固定種子 |
| Vignette、色差 | 風格、邊緣壓暗 | 弱，且可能傷害 HUD | deferred 設定裡有，但不是可讀性主手段 |
| Rim／fresnel（角色著色裡） | 只點亮輪廓 | **強**。TF2、VALORANT 的主力 | #84 已在試；不需要 compute，web／desktop 都能用 |
| 深度／法線描邊（全螢幕 pass） | 依 G-buffer 畫外輪廓 | **強**。XAG 直接建議；風格化遊戲常用 | 尚未做。deferred 已有 position、normal、albedo，技術上可加在 lighting 之後、output quad 上 |
| 選擇性曝光／Local Exposure | 局部壓亮暗差、保住細節 | 中。較適合日夜差很大的場景 | Unreal 有[正式文件](https://dev.epicgames.com/documentation/en-us/unreal-engine/auto-exposure-in-unreal-engine)；kool／myants 沒有對等功能，自製成本高 |
| 前景／背景之間的分隔層 | 只加在角色後方的暗或亮墊 | 中到強，實作可以很輕 | Game Accessibility Guidelines 提過這種作法；myants 尚未做 |

### 3.2 優點

- 不犧牲「這是暗色甲殼」的識別，也不必再把 albedo 往下壓。
- 日、夜、自選色可以共用同一套分離手段，只改強度與顏色（這對後續的 #91、#86 很重要）。
- Rim 寫在角色 shader 裡，所有後端行為一致；不會像 bloom 那樣在 GL／WebGL 缺席。
- 可做成品質檔或無障礙開關（描邊開／關、強度），符合 XAG 與 GAG「提供可調對比」的方向。
- 與現有原則相容：#94 已規定「不以錯誤法線把殼壓暗來換 D1」；可讀性層是同一邏輯的延伸。

### 3.3 缺點與風險

- 全螢幕調色（tone mapping、LUT、曝光、bloom）**解決不了**「黑蟲貼在中明度土上」；兩層一起變，比值幾乎不動，還可能讓 web／GL 和 Vulkan 看起來不一樣。
- Rim 若帶「側面基底」（#84 的 0.35），光會進到本體，D3 這種「本體中位數」一定掉。這不是後處理不好，是公式寫太寬。
- 深度描邊若太粗，或顏色不跟背景走，會變成卡通描邊，和目前偏寫實的室外風衝突。薄、半透明、或只在對比不夠時出現，比較安全。
- 新 pass 有填色成本；若做，應掛在既有 deferred 輸出上，並納入品質檔，避免拖到 #52 還在等的效能帳。
- 若驗收門檻仍是「本體中位數對地面 ≥4.5／3.25」，可讀性層一加就可能被自己的門檻殺掉——#84 已經發生了。所以要先改量什麼，再做效果。

---

## 4. 對照 myants 現況：三個方案

### 4.1 我們是怎麼走到這裡的（已查證，專案紀錄）

量法（`docs/screenshots/79-chitin/measure.py`、`81-beetle/measure.py`）：WCAG 相對亮度對比。

- **D1**：specimen 固定框裡，蟻身最暗 10% 像素的中位數，對同一框裡較亮（≥第 60 百分位）的地面中位數。
- **D2**：瓢蟲紅殼中位數，對非紅色像素中最暗 10%（斑點）的中位數。這是**物件內部**對比，不是對地面。
- **D3**：甲蟲固定框裡 \(L<0.15\) 的本體像素中位數，對另一個地面框的中位數。

時間線：

| 階段 | 發生什麼事 | 數字 |
|---|---|---|
| #76／#79 開工 | 工蟻被高光洗成淺灰，瓢蟲斑點不夠 | D1 約 1.44 或 1.18–1.35，D2 2.11–2.66。**當初目標是 ≥3:1** |
| #80（#79） | roughness 改 1.0，斑點加深 | D1 4.70、D2 3.31。Artist 曾以 D2 需 ≥3.3 退回 3.01 的中間版 |
| #83／#90 夜間構圖 | 只建 shot，夜間沒有門檻 | 夜間參考：D1 1.29、D2 1.84；甲蟲鞘翅／前胸對地約 1.04／1.05（#91 引 QE 在 #90 的量測） |
| #94／#95 | 殼法線翻正，albedo 降到 (0.036, 0.036, 0.039)，自選色乘 0.52 | 正午 D1 **4.62**、D2 3.31、D3 3.28。門檻寫進 ART_DIRECTION §6.7：D1≥4.5、D2≥3.3、D3≥3.25、hot%≤2% |
| #84 試做（未開 PR，已暫停） | 蟻與甲蟲共用白天暖色 rim（含 0.35 側面基底） | D1 4.55、D2 3.31 仍過；**D3 3.2434 < 3.25**。k=0.05–0.10 的 D3 都一樣；約 37–45% 變動像素在 2px 邊帶外。邊帶增益 1.066–1.092 單調，內部中位數仍是 1.000 |

現在的結構問題是：門檻從 #79 的 3:1 被抬到「已經達到的數字再留一點點緩衝」。D1 只剩 0.12，D3 只剩約 0.03。D3 量的是本體暗部中位數，所以**任何**讓甲蟲身體平均亮一點的光（即使觀感只是一圈細邊）都會踩線。這不是 rim 沒有效，是門檻在懲罰我們要用的手段。

另外，ART_DIRECTION 已禁止用錯法線換 D1；roughness 已釘在 1.0。材質側幾乎沒有合理空間了。夜間若仍想用同一套 4.5／3.25，只會逼出更不自然的夜間著色。

### 4.2 方案比較

**方案 A：繼續改材質與共用光照，守住現有 D1／D2／D3**

- 做法：再壓 albedo、改公式但不得讓本體中位數上升、或給甲蟲另一組較弱的 rim。
- 優點：不碰管線，驗收腳本不用改。
- 缺點：#84 已證明合法的輪廓光會踢到 D3；再壓 albedo 會讓受光體節更扁、更接近「一塊黑」。甲蟲另設參數則破壞「同強度同公式」。夜間問題完全沒解決。
- 評價：不建議再走。

**方案 B：只加全域後處理（tone mapping、LUT、曝光、bloom）**

- 做法：在 output quad 做調色或曝光，希望整張圖「比較好看」之後對比也會好。
- 優點：一次影響日夜與所有物件，實作面可能不大。
- 缺點：角色和地面一起被調，D1／D3 這種比值幾乎不會因此過關；bloom 在 web／GL 還會缺席，兩端畫面不一致。它適合當風格統一，不適合當可讀性主手段。
- 評價：可以當以後的調色工作，**不能**取代 #84／#91 要解決的「找得到蟲」。

**方案 C（建議）：可讀性層 + 改驗收，材質維持目前正午基準**

- 做法：
  1. 把 #94／#95 的正午畫面當「甲殼看起來應該是這樣」的基準，不再為了對比去改 albedo／roughness／法線。
  2. 另做一層只作用在蟲體輪廓的可讀性處理：優先用**沒有側面基底**的 fresnel／rim（全後端一致，與 TF2／VALORANT 同類）；若俯視小尺寸仍不夠，再試薄的深度描邊或角色後方的分隔暗墊。夜間用同一層、不同強度與色溫。
  3. 驗收拆成兩類，見下一節。
- 優點：質感和可讀脫鉤；日夜與自選色可共用；和無障礙指引一致；web／desktop 行為可以對齊。
- 缺點：要改 ART_DIRECTION 與量測腳本；rim／描邊需要 Artist 定觀感，避免卡通化；新指標要先用現有 specimen 圖校一輪，再當成門檻。
- 評價：這是和業界作法、以及 myants 已經撞牆的資料，都對得上的一條路。

---

## 5. 建議做法（含 D1／D2／D3 要不要留）

### 5.1 門檻怎麼改（已定案，Neo 2026-11-11）

**保留量化，但不要再用「本體填色 4.5」去卡輪廓光。**

| 指標 | 建議 | 理由 |
|---|---|---|
| D1 蟻身／地（本體填色） | **放寬為 ≥3.0**，回到 #79 原文目標；不再用 ≥4.5 當硬門檻 | 3:1 對齊 WCAG 非文字圖形與 XAG 大型元素。4.5 是文字／較小 UI 的標準。現在 4.62 對 4.5 沒有實作空間 |
| D3 甲蟲本體／地（本體填色） | **放寬為 ≥3.0**，不再用 ≥3.25 | 3.28 對 3.25 只剩 0.03，1 階 8-bit 就會失敗。3.0 仍能防止「又和地面糊在一起」 |
| D2 瓢蟲殼／斑點（內部識別） | **維持 ≥3.3**，或至少 ≥3.0 | 這是物件內部圖案，和 rim 較不相衝；3.3 是 Artist 在 #80 已用過的尺 |
| 輪廓可讀（新） | 用固定幾何輪廓帶量邊緣對比或邊緣增益；目標由 Artist 依 #84 已有的 1.07–1.09 這類實測校準，不先寫死 1.20 | 這才是 rim／描邊該守的東西。k=0 應接近 1，加強時單調上升；本體內部中位數應維持 1.000（#84 已能量） |
| 灰階檢查（新，偏質性） | specimen 去飽和後，在實際遊戲尺度下應能立刻指出工蟻、兵蟻、甲蟲、瓢蟲 | 對齊 Game Developer 與常見美術檢查。可當 Artist 審查項，不必第一次就自動化 |
| 夜間 | **不要套用正午 D1／D3 門檻**。本體填色可接受低於 3，改用輪廓可讀 + 灰階「找得到」；數字待 Artist 用 specimen-night 定 | 夜間目前 D1 1.29、甲蟲約 1.04，硬套 4.5／3.25 不切實際。VALORANT 的作法是加 fresnel 與明暗上限，不是把夜間 albedo 再削一截 |
| 回歸保護 | 任何可讀性層在 k=0（關閉）時，正午畫面須與 #95 基準 byte 或度量一致 | 避免「為了可讀又把材質弄壞」 |
| 瓢蟲、花朵、UI | 維持現有：瓢蟲觀感不變；花色 ΔE 走自己的標準；UI 文字繼續 ≥4.5（ART_DIRECTION §4，已有測試） | UI 不應和 3D 蟲體共用同一場討論 |

hot%≤2%、mid≤main×1.10、兩輪一致、桌面實機仍等 Neo 補拍——這些可以留。

### 5.2 定案後的實作順序

1. 先改 ART_DIRECTION 與量測說明（一個小文件 PR），把上表寫死，再重開對比相關 issue。
2. 重開 #84：只做無側面基底的共用白天 rim（必要時加 smoothstep 門檻），用新的輪廓指標驗收；D1／D3 只當「本體不要塌過 3.0」的地板。
3. 再做 #91：同一層的夜間參數，不另造一套材質。
4. 然後才是 #86（自選色是否仍達新的 D1 地板）和 #87（UI／費洛蒙色，與蟲體可讀是分開的）。

ART_DIRECTION 與量測說明的文件 PR 合併前，#84／#91／#86／#87 繼續暫停。

### 5.3 這份報告不決定的事

- rim 強度、顏色、是否加描邊：要等門檻改寫後，由 Artist 看實測圖決定。
- 要不要做玩家可開的「高對比／描邊」選項：方向正確，但不是現在這一批的範圍。
- kool 升級（#93）與效能重測（#52）不在本報告範圍。

---

## 6. 來源

### 已查證（2026-11-11 抓過，連結可開）

- W3C，[WCAG 2.1 Contrast (Minimum) 1.4.3](https://www.w3.org/TR/WCAG21/#contrast-minimum)
- W3C，[Understanding SC 1.4.11 Non-text Contrast](https://www.w3.org/WAI/WCAG21/Understanding/non-text-contrast.html)
- Microsoft，[Xbox Accessibility Guideline 102](https://learn.microsoft.com/en-us/xbox/accessibility/xbox-accessibility-guidelines/102)
- Game Accessibility Guidelines，[Provide high contrast between text/UI and background](https://gameaccessibilityguidelines.com/provide-high-contrast-between-text-ui-and-background/)
- Game Accessibility Guidelines，[Provide an option to adjust contrast](https://gameaccessibilityguidelines.com/provide-an-option-to-adjust-contrast/)
- Valve／Jason Mitchell 等，[GDC 2008, *Stylization with a Purpose*（TF2 投影片 PDF）](https://cdn.cloudflare.steamstatic.com/apps/valve/2008/GDC2008_StylizationWithAPurpose_TF2.pdf)
- Riot Games，[VALORANT Shaders and Gameplay Clarity](https://www.riotgames.com/en/news/valorant-shaders-and-gameplay-clarity)（2020-06-30；`technology.riotgames.com` 會轉到此頁）
- Riot Games，[Clarity in League](https://www.leagueoflegends.com/en-us/news/dev/clarity-in-league/)（2021-03-12）
- Peter Angstadt，[How to Reduce Visual Confusion in Your Game](https://www.gamedeveloper.com/design/how-to-reduce-visual-confusion-in-your-game)，Game Developer
- Epic Games，[Unreal Engine Auto Exposure / Local Exposure](https://dev.epicgames.com/documentation/en-us/unreal-engine/auto-exposure-in-unreal-engine)
- 本 repo：`docs/ART_DIRECTION.md` §4、§6.7；`docs/screenshots/79-chitin/`、`81-beetle/`、`83-specimen-night/`、`94-ant-normals/`；[`#79`](https://github.com/neojou/myants/issues/79)、[`#84`](https://github.com/neojou/myants/issues/84)、[`#91`](https://github.com/neojou/myants/issues/91)；`docs/programming/rendering_pipeline_kool.md`

### 推測／建議（不是出處原文）

- 3:1 適合作為蟲體本體填色地板、4.5:1 不宜再當 D1 硬門檻
- 可讀性應主要靠 rim／必要時的薄描邊，而不是全域 tone mapping
- kool deferred 的 G-buffer 足夠做深度描邊，但尚未實作、工時與風格風險只是估計
- 第 5 節的新驗收表與實作順序

（較新的個人部落格裡也有人把 3:1 或灰階檢查寫進遊戲可讀性教學；那些不是第一手工作室文件，本文不拿來當規範依據。）
