# ADR 0002：HUD 圖示實作方式

- 狀態：已接受（PM + Artist）
- 日期：2026-10-21
- 相關：#5、#6

## 背景
kool 0.19 沒有內建 Material 圖示。M2 要把符號按鈕（`>|`、`+`、`o`、`?` 等）換成圖示，並加上 tooltip。

## 決策
每個圖示存成一個 SVG 檔，載入時轉成 54px 的單色貼圖（alpha mask），顯示時再用 Theme token 上色。
如果 kool 無法在執行期載入 SVG，就在 build 時先把 SVG 轉成 PNG，其餘流程不變。

## 考慮過但不採用
- Material Symbols 字型子集：需要額外的字型與 glyph 對照表，跟現有 Fira Sans 字型堆疊混在一起，增減圖示都要重做子集。
- PNG 圖集：每次增減圖示都要重打包，而且不能用 token 上色。
完整理由見 https://github.com/neojou/myants/issues/5#issuecomment-6298237791 。

## 範圍
- M2 只換 Speed、Zoom、Tool 三列，其餘按鈕留待 #11 工具列分組時再評估。
- UI 文字維持英／德文（見 PLAN），所有圖示按鈕都要有 tooltip。
- Artist 提議的 `[`、`]` 快捷鍵原作沒有，不在 M2 範圍，併入 #11 評估。

## 後果
- 新增圖示資產，要更新授權記錄（Material Symbols 使用 Apache-2.0 授權）。
- 圖示顏色跟著 Theme token 走，日夜主題切換不需要另一套資產。
