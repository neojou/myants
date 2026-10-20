# Changelog

本檔記錄 myants 的重要變更。格式依 [Keep a Changelog](https://keepachangelog.com/zh-TW/1.1.0/)，版號採 [SemVer](https://semver.org/lang/zh-TW/) 0.x（見 `docs/adr/0003-versioning-and-release.md`）。

## [Unreleased]

## [0.15.0] - 2026-10-21

涵蓋 M1（試跑流程）與 M2（HUD 圖示）。

### 新增
- CI 工作流程：JDK 25、Gradle 快取、產出 JS 成品（#2）
- M1–M5 roadmap 與角色流程：`docs/PLAN.md`、ADR 0001（#14）
- `Theme.kt` 設計 token，並收斂 `AntSimUi` 的樣式（#16，Closes #4）
- ADR 0002：HUD 圖示實作方式（#21）
- HUD Speed／Zoom／Tool 三列改為 Material Symbols 圖示按鈕，附 EN/DE tooltip（#23，Closes #6）

### 變更
- GitHub Actions 升級到 Node 24 版本（#20）
