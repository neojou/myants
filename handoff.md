# myants 交接文件（Handoff）

> 2026-10-22 更新。團隊（PM／Coder／QE／CI／Artist bot）暫停，改由一個 Grok Build session 接手。做法見 `docs/AI/How-To-Grok-build-one-AI-developing-approach.md`。將來切回團隊時，從本文件與 `docs/PLAN.md` 繼續。

## 0. Grok Build 單人模式（暫停期間的流程）

團隊暫停期間，由一個 Grok Build session 在本機 `main` 上 commit。不開分支，不開 PR。`AGENTS.md` 的團隊規則保留不改，切回團隊後恢復。

這台機器上的 session **不 push**。commit 與本機 tag 留在工作目錄，由 Neo push。

1. 小步 commit：一次只做一件事，每個 commit 都能獨立編譯。
2. commit 前必跑：`./gradlew compileKotlinDesktop compileKotlinJs desktopTest`，全過才 commit。`HeadlessInvariantsTest` 黃金值不得改變。
3. commit 訊息用 Conventional Commits，並引用 issue。例如 `refactor(ui): split AntSimUi.kt (Closes #8)`。部分完成用 `Refs #n`。
4. 每完成一項：更新 `CHANGELOG.md` 的 Unreleased（附 issue 編號）、本文件第 2 與第 3 節、`docs/PLAN.md` 的 Status。這些文件跟該項程式放在同一個 commit。
5. milestone 的版號：`./configure.sh version x.y.z`，commit `chore(release): vX.Y.Z`，再在本機打 `git tag vX.Y.Z`。不 push。Neo 之後 push `main` 與 tag，並建 GitHub Release。0.16.0 與 0.17.0 都依這步。0.18.0 改走團隊流程的 release PR（#45），Neo 合併後再在 merge commit 建 tag 與 Release。0.17.0 保持 #12 與 M4 開啟，因為 CI、[QE] PASS、[Artist] PASS 還沒有。
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

- 版本：**0.18.0**（`gradle.properties` 與 CHANGELOG；tag `v0.18.0` 與 Release 待 Neo 合併 #45 後建立）。v0.17.0 tag（15510eb）與 GitHub Release 已在 GitHub。v0.16.0（642f082）已在 origin，涵蓋 M3。v0.15.0 涵蓋 M1＋M2。接手時 `main` 為 `390d4b9`。
- M1、M2：已關閉。
- **M3 UI/UX 與架構整理**：已在 origin，tag `v0.16.0`（642f082）。GitHub Release v0.16.0 已存在（2026-10-26 確認）。
- **M4 Tool - Inspect（epic #12）**：產品行為在本機 0.17.0。四類都可點選並顯示類型與狀態，約每模擬秒更新，有選取圈。有世界座標的選取可從 HUD 跟隨。巢內選取不移動左側相機。Inspect 不寫回模擬。2026-10-22 再查 GitHub：milestone M4 仍是 Open、完成度 0%、沒有到期日；#12 仍是 Open，是 milestone 裡唯一的 issue。CI、[QE] PASS、[Artist] PASS 還沒有，所以這版不關 #12，也不關 milestone。v0.17.0 tag 與 GitHub Release 已在 GitHub。
- M5 glTF（epic #13）：左側室外模型已在 origin/main，收進 0.18.0；#13 仍開。

## 2. 開著的 PR

無。

## 3. 開著的 issue（M3）與順序

單人模式依這個順序做完，並發本機 v0.16.0：

1. #22 建置維護：本機已做。Dependabot ignore 含 `gradle` 與 `gradle-wrapper`，並排除 Kotlin 與 kool。Gradle 仍是 9.3.0。
2. #39 PointerRouter 的 `noteDrag` 測試：本機已做。3D 拖曳清掉 `userFollow`，2D 拖曳不呼叫 `noteDrag`。
3. #36 Classic 2D：本機已做。繪製在 `classic2d/ClassicScene.kt`，shader 與貼圖在 `classic2d/ClassicShaders.kt`。
4. #9 細粒度 state：本機已做。工具列與 HUD 訂閱各自的 state。HUD 字串約每 0.25 秒寫一次，暫停單步立即寫。暫停、速度、下雨、切工具不增加 revision。側邊面板仍共用一個 revision。`syncPublishes` 計次，沒有量 fps。
5. #18 色值與 token：本機已做。WorldPalette、圓角與對比註解、實心 `pause_fill`、蟻色色塊的名稱與 tooltip。色值不變。
6. #17 按下縮到 0.96、HUD 數字固定寬度靠右：本機已做。字型仍是 Fira。
7. #19 Stats 線型與線尾標籤：本機已做。顏色不變。線尾標籤在 #24 進字串表。英文是 Sugar。
8. #24 介面語言：本機已做。預設 English。Grafik、HUD、About 的 Version、圖示 tooltip、蟻色名稱、敵群行、Stats 圖下標籤都走字串表。圖上的點陣字沒有中文，繁中時線尾改畫英文。Fira 沒有中文字。
9. #11 工具列：本機已做。五組間距、About 走 `openPanel`、Restart 危險色與確認、快捷鍵空白鍵、`.`、`+`／`-`（速度）、`1`–`4`。`=`／`_` 仍是縮放。`[`、`]` 評估後不加，原作沒有。

## 4. 後續

- 介面語言現為 English／繁體中文／日本語／Deutsch，預設 English（40aa960，已在 origin）。入口在工具列右上角的地球圖示，Grafik 不再有語言按鈕。下雨在骷髏頭右邊。敵群、Params、品質、分析、Inspect 跟所選語言走。MIT 行仍是英文。繁中與日文用 Noto Sans CJK 圖集，HUD 數字仍是 Fira。切換語言不重建世界。
- **M4 Inspect（#12）**：點選螞蟻／食物／巢／地形，顯示類型與狀態，約每模擬秒更新，有選取圈，世界選取可跟隨。做在 PointerRouter 之上。不得把玩家操作寫回模擬。本機 0.17.0 已包含這段產品行為。GitHub #12 與 milestone M4 仍開。CI、[QE] PASS、[Artist] PASS 還沒有。
- **M5 glTF（#13）**：單位與物件改用 glTF 2.0。這次不做。

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
