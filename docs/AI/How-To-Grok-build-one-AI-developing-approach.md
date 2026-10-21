# How-To：Grok Build 單人開發方式

日期：2026-10-22。這是工作筆記，不是 ADR。M3 做完後用實際返工次數回頭改。

myants 暫停五個 Grok Bot，改由一個 Grok Build session 在本機 `main` 上做完 M3。做法像早期遊戲或現在的獨立製作：一個人依序扮企劃、設計、程式、建置、測試與畫面檢查。多角色的品質門檻留在測試與人眼，不留在五次交接。

團隊規則仍在 `AGENTS.md`。暫停期間的例外在 `handoff.md` 第 0 節。切回 bot 時以那份文件第 9 節為準。

## 兩種做法

| | 團隊（Grok Bots） | 單人（現在） |
|---|---|---|
| 誰寫程式 | Coder，在自己的目錄 | 這一個 session，在這個 clone |
| 分支 | 一 issue 一分支一 PR | 不開分支，直接 commit 在 `main` |
| 合併 | 只由人合併 | 人自己 `git push`。這個 session 不 push |
| 審查 | CI 綠、QE PASS、有畫面再 Artist PASS | 每個 commit 前跑同一組 Gradle。有畫面就列出 Mac 上要看的地方 |
| 適合 | 多個 bot 同時做、彼此不改同一批檔 | 有順序的小步、模擬行為已被黃金值鎖住 |

五個 bot 同時用一個 checkout 會把 commit 落到別人的分支上（#27）。單人模式用一個 clone，就是為了避開這件事。

## 角色收進一個人

原團隊是 PM、Coder、CI、QE、Artist。單人模式把 Architect 也收進來，不再為設計另開一隻 bot。

| 原角色 | 單人時怎麼做 | 仍要停下來問人 |
|---|---|---|
| PM | 讀 `docs/PLAN.md` 與 `handoff.md`，依已同意的順序做。一個 milestone 內不逐個 commit 問 | 改路線、改 milestone 範圍、要做 M4／M5／第一人稱 |
| Architect | 先讀將改的檔與 kool 陷阱，再小步改。行為不變的重構不另寫長設計 | 會改 `sim` 結果、會改黃金值、跨過多個子系統 |
| Coder | 一個 commit 一件事。訊息寫 issue，做完用 `Closes #n` | 沒有 |
| CI | commit 前跑下面的 Gradle。不升級 kool／Kotlin／Gradle／JDK。消不掉的警告寫進 CHANGELOG | 本機通過、遠端 CI 卻紅 |
| QA | 改行為就加測試。`HeadlessInvariantsTest` 黃金值不變。沒有 `[QE] PASS` 這道簽核 | 測試在表達「該鎖哪種行為」時有兩種讀法 |
| Artist | 色、圓角、間距只用 `Theme.kt`。按鈕用 Material icon 加 tooltip。有畫面的做完，列出 Mac 上要看的位置 | 新的美術方向、與現有畫面故意不同的取捨 |

人還是負責三件事：看有畫面的結果、push、打 tag 與 GitHub Release。

## 什麼時候用多個 agent

Lauren Tan（@poteto，SpaceXAI）的 pstack 是另一種做法：很多職責很窄的 agent，用交接串起來，人留在決策點。公開說法裡，目標拆解、研究、寫程式、審查、介面、記憶、週期工作是不同角色，而不是一隻什麼都做的 agent。pstack 以 Cursor plugin 的形式公開，這個 repo 沒有把它裝進來。

myants 現在不常駐那一組角色。原因：

- 專案小，而且 M3 的 issue 有順序。#36 要先於 #9，#24 的字串表要先於 #11 的新對話框。平行寫同一個樹，合併成本高過加速。
- 每隻 bot 都會重讀 PLAN、ADR 與 kool 陷阱。五次交接裡，同一份 diff 被讀很多遍，token 花在上下文而不是改動。
- 模擬結果被黃金值鎖住。第二隻「審查 bot」能抓到的，多半是測試該鎖的行為。

仍值得另開只讀 agent 的情況：

- 調查不寫檔。例如對照 kool 0.19.0 原始碼、掃寫死色值、讀一個很長的檔。
- 調查結果回到這一個 session，由它整成一個 commit。兩隻 agent 不同時改同一批檔。

目前的平衡點：

- 預設一個 session 做完一個 issue，再做下一個。
- 寫入不平行。調查可以平行。
- 測試代替第二隻 QA。畫面交給人，不在沒有 GPU 的環境宣稱看過。
- CHANGELOG、`handoff.md`、`docs/PLAN.md` 跟該 issue 的程式放在同一個 commit，不再花一輪文件交接。

還沒用數字量過 token。上面是這個 repo 的工作量判斷，不是 pstack 的效能數據。若以後一個 issue 大到這個 session 放不下上下文，再考慮把「只讀調查」拆出去，而不是先把五個角色叫回來。

## 每個 commit

commit 前必過：

```text
./gradlew compileKotlinDesktop compileKotlinJs desktopTest
```

`HeadlessInvariantsTest` 的黃金值不變。不跑 `jsBrowserTest`。不 force-push，不改寫歷史。

這個 session 只 commit 在本機。push、遠端 tag、GitHub Release 由人執行。commit 訊息裡的 `Closes #n` 會在 push 到 `main` 之後才關掉 issue。

milestone 收尾仍用 `./configure.sh version x.y.z`，再在本機打 `vX.Y.Z`。人 push `main` 與 tag 之後自己建 Release。見 ADR 0003；暫停期間不開 release PR。

## 切回團隊

1. 恢復 `main` 的 ruleset，拿掉暫停用的 bypass。
2. 更新 `handoff.md` 第 2、3 節：單人期間的 commit、已關 issue、目前版本。
3. 各角色回到 `AGENTS.md`：一 issue 一分支、PR、CI，再加上 QE；有畫面再加 Artist。合併仍由人決定。
4. 若屆時要並行，用各自的目錄，或用 pstack 這類窄職責編排。不要再共用一個 checkout。
