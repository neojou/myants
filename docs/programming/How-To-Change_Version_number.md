# 如何更改版號

產品版號是使用者在 About 裡看到的版本。它和 Kotlin、kool、Gradle wrapper 的版號無關。

改產品版號請用 `./configure.sh version`。不要只改其中一個檔案，否則 About 與 Gradle 版號會不一致。

## 兩個寫入點

兩處必須是同一個字串。目前產品版號以 `gradle.properties` 的 `app.version` 為準。

1. `gradle.properties` 的 `app.version`
2. `koolApp/src/commonMain/kotlin/com/neojou/myants/AppVersion.kt` 的 `NAME`

```kotlin
const val NAME: String = "0.2.0" // configure:app.version
```

行尾的 `configure:app.version` 是 `configure.sh` 的標記。腳本只改 Kotlin 原始碼裡這一行的字串，而且必須正好一處。上面的程式片段是說明用的範例，腳本不會把它當成第二處標記。版號不含 `v` 前綴。

`version` 指令不會改寫這份說明。改完後以 `gradle.properties` 的 `app.version` 為準。

## 哪些地方讀這個版號

- About 第二行。`AntSimUi.kt` 顯示 `Version ` 加上 `AppVersion.NAME`，例如 `Version 0.7.0`。這是 app 內的面板。macOS 系統選單的 About 是系統標準面板，版號同樣是 `AppVersion.NAME`，不再顯示 JDK 版號。
- 視窗與分頁標題。`appTitle` 是 `APP_NAME` 後面空一格再接 `NAME`，例如 `myants 0.2.0`。
- Gradle 專案版本。`koolApp/build.gradle.kts` 的 `version` 讀 `app.version`。jar 與 `desktopDistZip` 的檔名會帶這個版號。

目前沒有 dmg。以後若用 jpackage 打包，沿用同一個 `app.version`。

## 用指令改版號

```bash
./configure.sh version 0.2
./configure.sh version 0.2.0
```

格式是 `MAJOR`、`MAJOR.MINOR` 或 `MAJOR.MINOR.PATCH`。每一段都是非負整數，除了單獨的 `0` 以外不可有前導零。`0.2` 與 `0.2.0` 合法。`v0.2`、空白與先行號不合法。

已經是這個版號時，指令只印出現況，不改檔案。

這個格式以後可以交給 jpackage。Windows 的 msi 與 exe 要求正好三段，例如 `0.2.0`。以後若要打 Windows 安裝檔，請把參數寫成三段。

## 指令故意不改的版號

下列版號是工具鏈，不是產品版號：

- `gradle/libs.versions.toml` 裡的 Kotlin、kool 與函式庫版本
- `gradle/wrapper/gradle-wrapper.properties` 的 Gradle 版本

專案顯示名稱見 `How-To-Change_Project_Name.md`。
