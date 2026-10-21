# myants 交接文件（Handoff）

> [PM] 2026-10-21（台北時間）撰寫。團隊（PM／Coder／QE／CI／Artist bot）暫停，改由單一開發者用 Grok Build 接手。將來切回團隊時，從本文件與 `docs/PLAN.md` 繼續。

## 1. 目前版本與 milestone
- 版本：**0.15.0**（[v0.15.0 Release](https://github.com/neojou/myants/releases/tag/v0.15.0)，M1＋M2 一起發布）。`main` 上已合併 #7 系列（#28/#32/#33）、#30、#34，尚未發版。
- M1、M2：已關閉。
- **M3 UI/UX 與架構整理**：進行中，預計結束時發 **v0.16.0**。
- M4 Tool - Inspect（epic #12）、M5 glTF（epic #13）：尚未開始。

## 2. 開著的 PR
| PR | 內容 | 狀態 | 下一步 |
|---|---|---|---|
| [#35](https://github.com/neojou/myants/pull/35) | #10 PointerRouter（`input/PointerRouter.kt`，+11 測試） | 待 QE 驗；純重構 | QE PASS → 合併 |
| [#37](https://github.com/neojou/myants/pull/37) | #8 拆 `AntSimUi.kt`（451→35 行）、`Panel` enum | 待 QE 驗；純重構 | QE PASS → 合併 |
| [#26](https://github.com/neojou/myants/pull/26) | #22 建置維護、Dependabot | QE FAIL | Dependabot ignore 要同時含 `gradle` 與 `gradle-wrapper`，並排除 Kotlin 與 kool 更新 |

#35 與 #37 互不依賴；後合的那個會在 `CHANGELOG.md`／`docs/PLAN.md` 有文字衝突，用 `git merge origin/main` 解（不要 rebase＋force-push）。

## 3. 開著的 issue（M3）與建議順序
- 第二批：#10（PR #35）、#8（PR #37）。
- [#36](https://github.com/neojou/myants/issues/36)：2D 繪製拆到 `classic2d/`、shader 工具獨立（#10 後半），排在 #8 之後。
- 第三批：#9 細粒度 state 取代全域 revision、#18 寫死色值收斂到 Theme token（含 radius/border/size token、ANT_COLORS 命名、對比 6.63 vs 7.44 差異、pause_fill）。
- 第四批：#17 按下縮 0.96 與數字等寬、#19 Stats 線型區分、#24 文字按鈕 tooltip 與語言切換。
- 最後：#11 工具列分組、Restart 確認、快捷鍵（評估 `[` `]`）；About 按鈕改走 `openPanel(Panel.ABOUT)`。
- #22：Gradle 升級已移出範圍（違反 AGENTS.md，是否升級由 Neo 決定）。
- M3 完成後：開 `chore(release): v0.16.0` PR。

## 4. 後續
- **M4 Inspect（#12）**：點選螞蟻／食物／巢／地形顯示類型與狀態。建議在 #10 PointerRouter 之上做。不得把玩家操作寫回模擬。
- **M5 glTF（#13）**：單位與物件改用 glTF 2.0 模型美化。

## 5. 建置與測試
- JDK 25。
- 建置／測試（PR 前必過）：`./gradlew compileKotlinDesktop compileKotlinJs desktopTest`
- 執行：`./gradlew :koolApp:runDesktop`（主要平台 macOS；box 無 GPU，截圖需在 Mac 上拍）
- `HeadlessInvariantsTest` 守住模擬行為，黃金值不得改變。

## 6. 版本與 CHANGELOG 規則（ADR 0003）
- SemVer 0.x：每個 milestone 升 minor，milestone 內修 bug 升 patch。
- `./configure.sh version x.y.z` 只改 `gradle.properties`（`app.version`）與 `AppVersion.kt`（`NAME`）。
- 每個 PR 更新 `CHANGELOG.md` 的 Unreleased（Keep a Changelog 格式、繁體中文、附 issue／PR 編號）。
- milestone 結尾開 release PR；合併後從 merge commit 建 tag 與 GitHub Release。

## 7. 重要規則（AGENTS.md 與 ADR）
- 不升級 kool / Kotlin / Gradle / JDK。Kotlin Gradle Plugin 的警告只記錄，不靠升級消除。
- 不改 `sim` 行為；改行為必附測試；不得跳過失敗測試。
- 一 issue 一 branch（`feat/<issue#>-slug`）、Conventional Commits、小 PR。
- 不 force-push、不直接 push main；合併只由 Neo 決定。
- UI 語言固定英／德（目前寫死德文）。
- ADR：`docs/adr/0001-roadmap-and-role-workflow.md`（路線與角色流程）、`0002-hud-icon-pipeline.md`（SVG → 54px 單色貼圖、Theme token 上色）、`0003-versioning-and-release.md`。
- 團隊模式：每個角色用自己的目錄（`/workspace/myants-<角色>`），不在共用 `/workspace/myants` commit（見 #27）。單人開發時用一個獨立 clone 即可。
- 完成條件：CI 綠＋QE 驗證 PASS；有視覺變化時另需 Artist PASS（單人時改為自己在 Mac 截圖確認）。

## 8. 已知風險
- box 無 GPU：視覺驗收（日／夜截圖、Retina 上 1dp outline 是否清楚）仍待 Neo 在 Mac 上確認。
- `docs/PLAN.md` 的 Now 區有點過時，接手後請先更新。
- #35、#37 後合者會有文字衝突。
- PR #27 已關閉、不要合併；分支 `chore/release-0.15.0` 保留。
- 多個開發者共用同一個 checkout 會讓 commit 落到錯的分支。

## 9. 切回團隊時
1. 讀本文件與 `docs/PLAN.md`，確認開著的 PR 與 issue 現況。
2. PM 依第 3 節順序在 GitHub issue 寫派工，再用門鈴通知各角色。
3. 各角色開工前執行 `/workspace/bot-status/set-status.sh <角色> working ...`，結束後設為 resting。
