# ADR 0001：Roadmap 排序與角色化 PR 流程

- 狀態：提議（待 Neo 核可）
- 日期：2026-10-21
- 作者：[PM] neojou-bot

## 背景
專案由多個 agent 角色（CI、Coder、QE、Artist、PM）協作，需要一致的品質門檻與排序。

## 決策
1. Milestone 順序 M1 流程 → M2 圖示化 → M3 架構整理 → M4 Inspect → M5 glTF。
2. 一 issue 一分支一 PR；完成條件為 CI 綠 + `[QE] PASS` + 畫面相關 `[Artist] PASS`；只有 Neo 合併。
3. UI 視覺值集中於 `Theme.kt` token；M3 先拆 `AppSession`/`AntSimScene` 輸入，再做 Inspect。

## 後果
- 每個 PR 多一道 QE/Artist 審查，速度略降，回歸風險降低。
- M4 開工時間延後，但拾取與狀態結構較乾淨。
