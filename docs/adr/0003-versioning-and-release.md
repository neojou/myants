# ADR 0003：版本管理與發布流程

- 狀態：提議（PM）
- 日期：2026-10-21
- 相關：ADR 0001（roadmap 與角色流程）

## 背景
About 目前顯示 0.14.0，但 M1、M2 的工作已合併進 main，版號沒有跟著動，也沒有 CHANGELOG、tag 或 GitHub Release，事後很難知道哪一版包含哪些變更。

## 決策

### 版號規則
- 採 SemVer 0.x：每完成一個 milestone 升 minor；milestone 內的 bugfix 升 patch。
- M1、M2 已合併，補發一版 **0.15.0** 一起涵蓋，避免為已完成的工作連發兩版。
- 之後 M3 對應 0.16.0、M4 對應 0.17.0，以此類推。

### 改版號
- 用既有的 `./configure.sh version x.y.z`（不含 `v` 前綴）。它只改兩個檔案：
  - `gradle.properties` 的 `app.version`（Gradle jar／zip 檔名讀這個值）
  - `koolApp/src/commonMain/kotlin/com/neojou/myants/AppVersion.kt` 中標記 `// configure:app.version` 的 `NAME`（About、視窗與分頁標題、macOS 系統 About 讀這個值）
- 版號變更是每個 milestone 的**最後一個 PR**，標題 `chore(release): vX.Y.Z`（PM 開的照規則加 `[PM]` 前綴），內容只有改版號，以及把 CHANGELOG 的 Unreleased 移到新版本區並填日期。

### CHANGELOG
- 根目錄 `CHANGELOG.md`，採 [Keep a Changelog](https://keepachangelog.com/zh-TW/1.1.0/) 格式，繁體中文，每條附 PR 編號。
- 每個 PR 都要順手更新 `Unreleased` 區（新增／變更／修正／移除等分類）。

### Tag 與 GitHub Release
- Neo 合併 release PR 後，由 PM 用 gh 在該 merge commit 建 tag `vX.Y.Z` 與 GitHub Release，內容取自 CHANGELOG 對應版本區，例如：
  `gh release create vX.Y.Z --target <merge-commit-sha> --title vX.Y.Z --notes-file <該版 CHANGELOG 段落>`
- 理由：tag 不會改到 main 的內容，而合併權限仍在 Neo 手上。
- 可選：Neo 想自己打 tag 也可以，PM 改為只準備 release notes。

## 考慮過但不採用
- M1、M2 各發一版（0.15.0、0.16.0）：工作都已完成，連發兩版沒有額外資訊。
- 每個 PR 都升 patch：版號雜訊太多，milestone 才是可驗收的單位。

## 後果
- 每個 PR 多一個小步驟（更新 CHANGELOG）。
- 0.15.0 的 release PR 在本提案通過後另開，不在本 PR 中。
