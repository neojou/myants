# myants 交接文件（Handoff）

> 2026-10-22 更新。團隊（PM／Coder／QE／CI／Artist bot）暫停，改由一個 Grok Build session 接手。做法見 `docs/AI/How-To-Grok-build-one-AI-developing-approach.md`。將來切回團隊時，從本文件與 `docs/PLAN.md` 繼續。

## 0. Grok Build 單人模式（暫停期間的流程）

團隊暫停期間，由一個 Grok Build session 在本機 `main` 上 commit。不開分支，不開 PR。`AGENTS.md` 的團隊規則保留不改，切回團隊後恢復。

這台機器上的 session **不 push**。commit 與本機 tag 留在工作目錄，由 Neo push。

1. 小步 commit：一次只做一件事，每個 commit 都能獨立編譯。
2. commit 前必跑：`./gradlew compileKotlinDesktop compileKotlinJs desktopTest`，全過才 commit。`HeadlessInvariantsTest` 黃金值不得改變。
3. commit 訊息用 Conventional Commits，並引用 issue。例如 `refactor(ui): split AntSimUi.kt (Closes #8)`。部分完成用 `Refs #n`。
4. 每完成一項：更新 `CHANGELOG.md` 的 Unreleased（附 issue 編號）、本文件第 2 與第 3 節、`docs/PLAN.md` 的 Status。這些文件跟該項程式放在同一個 commit。
5. milestone 完成：`./configure.sh version 0.16.0`，commit `chore(release): v0.16.0`，再在本機打 `git tag v0.16.0`。不 push。Neo 之後執行 `git push origin main`、`git push origin v0.16.0`，並建 GitHub Release。
6. 仍然遵守：不升級 kool／Kotlin／Gradle／JDK，不改 `sim` 行為，不 force-push，不改寫歷史。

### main 的保護規則（ruleset）

`main` 的 ruleset 要求透過 PR 合併，直接 push 會被拒絕。Neo 要 push 之前，在 GitHub 的 repo Settings → Rules → Rulesets 擇一處理：

- 把自己加入 bypass list；或
- 暫時把該 ruleset 設為 Disabled。

### 接手時的 PR

- [#35](https://github.com/neojou/myants/pull/35)（#10 PointerRouter）：已合併。
- [#37](https://github.com/neojou/myants/pull/37)（#8 拆 AntSimUi、`Panel` enum）：已合併，#8 已關閉。
- [#26](https://github.com/neojou/myants/pull/26)（#22 建置維護）：已關閉，在 main 上重做。Dependabot 的 ignore 要同時含 `gradle` 與 `gradle-wrapper`，並排除 Kotlin 與 kool。不升級 Gradle。
- 本文件最初由 #38 合併。第 0 節當時在未合併的 `docs/handoff`（`56a29d0`），現已寫回 main，並改成「session 不 push」。

## 1. 目前版本與 milestone

- 版本：**0.15.0**（[v0.15.0 Release](https://github.com/neojou/myants/releases/tag/v0.15.0)，M1＋M2）。接手時 `main` 為 `390d4b9`，已含 #7（#28／#32／#33）、#30、#34、#35、#37。
- M1、M2：已關閉。
- **M3 UI/UX 與架構整理**：進行中。結束時在本機打 **v0.16.0**。
- M4 Tool - Inspect（epic #12）、M5 glTF（epic #13）：這次不做。

## 2. 開著的 PR

無。

## 3. 開著的 issue（M3）與順序

單人模式依這個順序做，做完才發 v0.16.0：

1. #22 建置維護：本機已做。Dependabot ignore 含 `gradle` 與 `gradle-wrapper`，並排除 Kotlin 與 kool。Gradle 仍是 9.3.0。
2. #39 PointerRouter 的 `noteDrag` 測試：本機已做。3D 拖曳清掉 `userFollow`，2D 拖曳不呼叫 `noteDrag`。
3. #36 Classic 2D：繪製已搬到 `classic2d/ClassicScene.kt`。shader 與貼圖工具還在同一檔，下一個 commit 抽出。
4. #9 細粒度 state 取代全域 revision。HUD 數字約 4 Hz。
5. #18 寫死色值收進 token（場景色用 WorldPalette，數值不變）、圓角、對比差異註解、`pause_fill`、蟻色名稱。
6. #17 按下縮到 0.96，HUD 數字固定寬度、靠右。
7. #19 Stats 用線型區分，線尾標名稱。
8. #24 介面語言 English／繁體中文／Deutsch，預設 English。
9. #11 工具列分組、Restart 確認、快捷鍵 Space、`.`、`+`／`-`、`1`–`4`。不加 `[`、`]`。About 改走 `openPanel(Panel.ABOUT)`。

## 4. 後續

- **M4 Inspect（#12）**：點選螞蟻／食物／巢／地形，顯示類型與狀態。做在 PointerRouter 之上。不得把玩家操作寫回模擬。M3 完成前不做。
- **M5 glTF（#13）**：單位與物件改用 glTF 2.0。M3 完成前不做。

## 5. 建置與測試

- JDK 25。
- commit 前：`./gradlew compileKotlinDesktop compileKotlinJs desktopTest`
- 執行：`./gradlew :koolApp:runDesktop`（主要平台 macOS）
- `HeadlessInvariantsTest` 守住模擬行為，黃金值不得改變。

## 6. 版本與 CHANGELOG 規則（ADR 0003）

- SemVer 0.x：每個 milestone 升 minor，milestone 內修 bug 升 patch。
- `./configure.sh version x.y.z` 只改 `gradle.properties`（`app.version`）與 `AppVersion.kt`（`NAME`）。
- 每個 commit 更新 `CHANGELOG.md` 的 Unreleased（Keep a Changelog、繁體中文、附 issue 編號）。
- 團隊模式是 release PR，合併後打 tag。單人模式改為本機版號 commit 加本機 tag，由 Neo push。不改 ADR 本文。

## 7. 重要規則（AGENTS.md 與 ADR）

- 不升級 kool／Kotlin／Gradle／JDK。Kotlin Gradle Plugin 的警告只記錄，不靠升級消除。
- 不改 `sim` 行為。改行為必附測試。不得跳過失敗測試。
- 團隊模式：一 issue 一 branch（`feat/<issue#>-slug`）、Conventional Commits、小 PR。單人模式見第 0 節。
- 不 force-push。單人模式也不由 session push `main`。
- ADR：`docs/adr/0001-roadmap-and-role-workflow.md`、`0002-hud-icon-pipeline.md`、`0003-versioning-and-release.md`。
- 團隊模式每個角色用自己的目錄（見 #27）。單人模式用這一個 clone。
- 有視覺變化時，在 Mac 上確認。沒有 GPU 的環境不宣稱看過畫面。

## 8. 已知風險

- 視覺驗收（日／夜、Retina 上的 1dp 外框）要 Neo 在 Mac 上看。
- PR #27 已關閉，不要合併。分支 `chore/release-0.15.0` 保留。
- 多個開發者共用同一個 checkout 會讓 commit 落到錯的分支。

## 9. 切回團隊時

1. 恢復 `main` 的 ruleset，移除暫停期間的 bypass。
2. 把第 2、3 節更新到當時的 commit、已關 issue 與版本。
3. 回到 `AGENTS.md`：一 issue 一 branch、PR、CI 綠、QE PASS。有畫面再加 Artist PASS。合併由 Neo 決定。
4. 若要並行，用各自的目錄，或用窄職責的多 agent 編排。不要共用一個 checkout。
5. 各角色開工前執行 `/workspace/bot-status/set-status.sh <角色> working ...`，結束時設為 resting。
