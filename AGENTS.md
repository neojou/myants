# myants — Agent Guide

## 目標
用 Kotlin Multiplatform（desktop JVM + js）與 kool 0.19.0 復刻 [AntSim_V2](https://github.com/nilsgollub/AntSim_V2)。
左側世界視窗要做成高品質 3D 環境；角色與物品用 glTF 2.0，畫面力求最好。
將來可能做第一人稱：化身一隻螞蟻，進入左側 3D 場景去搬糖、搬蛋白質或作戰。這條還沒開始。
完整說法、現況與下一步在 `docs/PLAN.md`。沒有寫進該檔 Next 之前，不要做第一人稱，也不要讓玩家操作寫回模擬。
主要平台是 macOS desktop；js 能編譯即可。

## Commands
- Build/Test: `./gradlew compileKotlinDesktop compileKotlinJs desktopTest`（PR 前必過）
- Run: `./gradlew :koolApp:runDesktop`
- Version: `./configure.sh version x.y.z`

## Layout
- `com.neojou.myants.sim`：模擬邏輯（不得改行為；HeadlessInvariantsTest 黃金值不變）
- `com.neojou.myants.render3d`、`ui/`：渲染與 HUD
- `docs/programming/`：設計參考。哪一份還算數見 `docs/programming/README.md`

## 先讀（Grok Build / Grok Bot / 人都適用）
1. `docs/PLAN.md`：目標、Status、Now、Next、不要動的東西。
2. `docs/programming/README.md`：哪份設計文件還算數。
3. PLAN 點名的 programming 文件。不要一次讀完整個 `docs/programming/`。

本機 `~/.grok/memory` 與舊對話不是來源。與 repo 衝突時以 repo 為準；與 kool 行為衝突時以 kool 0.19.0 原始碼為準。

## Workflow
1. 一 issue 一 branch：`feat/<issue#>-slug`；Conventional Commits。
2. 小 PR，描述含：做了什麼、怎麼驗證（含截圖）、未完成事項。
3. 每個 PR 都更新 `CHANGELOG.md` 的 `Unreleased` 區（附 PR 編號）。
4. milestone 結尾開 release PR（`./configure.sh version x.y.z`，標題 `chore(release): vX.Y.Z`）；合併後由 PM 建 tag 與 GitHub Release。見 `docs/adr/0003-versioning-and-release.md`。
5. 換手前更新 `docs/PLAN.md` 的 Status（日期、agent、版本、SHA、下一步）並 push。

## 工作目錄（全隊規則）

- 每個角色只用自己的目錄：`/workspace/myants-pm`、`/workspace/myants-coder`、`/workspace/myants-qe`、`/workspace/myants-ci`、`/workspace/myants-artist`（獨立 clone 或 git worktree 都可以）。
- 絕不在別人的目錄或共用的 `/workspace/myants` 裡 checkout、commit 或 push。
- 原因：多個 bot 共用同一台機器，在同一個 checkout 裡切分支，會讓 commit 落到別人的分支上（見 #27）。

## pstack 流程（全隊必做，不是選用）

每個角色在下列時點一定要跑對應的 pstack skill：

| 角色 | 時點 | 必跑 skill |
|---|---|---|
| PM | 拆工、開 issue 前 | `/sharpen`、`/architect` |
| Coder | 動手前 | `/how` |
| Coder | 改動時 | `/tdd`，先寫會失敗的測試，再改到通過 |
| Coder | 開 PR 前 | `/blast-radius`、`principle-prove-it-works` |
| QE | 驗 PR 時 | `/interrogate`、`principle-test-behavior-not-implementation` |
| CI | 改 workflow 或建置時 | `principle-prove-it-works` |
| Artist | 審查或出規格時 | `principle-experience-first` |

- **PR 說明必須有「已跑的 pstack skill」一節**，列出跑過的 skill 和重點結論。少了這一節，或少了該角色必跑的 skill，QE 一律判 FAIL。
- **注意用量**：`/arena`、`/swarm`、`poteto-mode` 全套不是預設流程，只在大改動時使用，例如跨模組重構或新子系統；使用時要在 PR 說明寫明原因。

## Rules
- 不升級 kool / Kotlin / Gradle / JDK 版本。
- 改行為必附測試；不得跳過失敗測試。
- 不 commit 密鑰；不 force-push、不直接 push main、不刪分支外的東西。
