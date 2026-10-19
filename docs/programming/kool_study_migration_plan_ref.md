# myants 改用 kool 引擎的遷移計畫

目前的程式入口是 `myantsApp()`（`MyantsApp.kt`）。`koolStudyApp()` 已移除。下面是當時的遷移記錄。

目標：myants 從 Compose Multiplatform 改成 kool 引擎，`./gradlew :koolApp:runDesktop` 開出旋轉的彩色立方體（kool README 的 Hello World）。JS 版也要能 build，並且保留 Neo 模板的 About、版號與 `configure.sh` 流程。

狀態標記：**已驗證**表示在 `kool_study_trial`（scratch 複本）實際做過；**推測**表示還沒驗證。完整 diff 見 `kool_study_trial_diff.patch`。

## 1. 現況（neojou/myants @ 8ecd84a「Version 0.1.0 - Kotlin Multiplatform」）

- 單一模組 `composeApp`，targets：`jvm("desktop")`、`wasmJs { browser }`。沒有 js target。
- Kotlin 2.3.0、Compose Multiplatform 1.10.0、Gradle 9.3.0、`jvmToolchain(25)`。
- `gradle.properties`：`app.displayName=myants`、`app.rootName=KoolStudy`、`app.group=com.neojou.myants`、`app.version=0.1.0`。
- `AppVersion.kt`：`APP_NAME`（標記 `configure:app.displayName`）、`NAME`（標記 `configure:app.version`）。`configure.sh` 要求兩個標記各正好一處。
- UI：`App.kt`（初始化狀態）、`HomeScreen.kt`（`MyTopMenuBar` 只有 About，中央顯示 APP_NAME，`AboutDialog` 兩行：名稱 / `Version x`）、`Theme.kt`（Noto Sans TC 字型）。`Main.kt` 用 `java.awt.Desktop.setAboutHandler` 把 macOS 系統選單的 About 接到同一個對話框。
- `configure.sh` 寫死了 `composeApp/src/{commonMain,desktopMain,wasmJsMain}/kotlin` 與 `composeApp/src/wasmJsMain/resources/index.html`。
- `docs/programming/`：`How-To-Change_Project_Name.md`、`How-To-Change_Version_number.md`，內容都是 composeApp、Compose 的路徑與說明。
- `MyLog` 用到 kotlinx-coroutines，但沒有直接宣告相依，靠 Compose 遞移帶入。改用 kool 後由 kool-core 的 `api(coroutines)` 提供（已驗證可編譯）。

## 2. 為什麼不讓 Compose 和 kool 並存

- kool 自己管視窗與渲染迴圈（GLFW + Vulkan/OpenGL；JS 用 canvas + WebGPU/WebGL2），不使用 Compose。
- 在 macOS 上，GLFW 需要 `-XstartOnFirstThread`，而 kool 會把 AWT 設成 headless。Compose Desktop 建在 AWT/Skiko 上，同一個 JVM 行程裡兩者會衝突（推測，依據 kool 原始碼註解）。
- kool 0.19.0 沒有 wasmJs 版本，Compose 的 wasmJs 前端也接不上 kool。
- 結論：**整個模組改成純 kool app**。About 用 kool 內建的 UI 框架（`de.fabmax.kool.modules.ui2`，寫法類似 Compose）重做。以後真的要把 kool 嵌進 Compose，可以研究 kool 的 `SwingWindowSubsystem(providedCanvas)` 搭配 Compose 的 `SwingPanel`（推測，屬於進階題目）。

## 3. 建議結構（已驗證）

模組 `composeApp` 改名為 `koolApp`；`wasmJsMain` 改成 `jsMain`。

```
settings.gradle.kts                 include(":koolApp")，repos 只留 mavenCentral / gradlePluginPortal
build.gradle.kts                    只留 kotlinMultiplatform apply false
gradle/libs.versions.toml           kotlin 2.3.0、kool 0.19.0；移除 compose-plugin
configure.sh                        SOURCE_ROOTS 與 INDEX_HTML 改成 koolApp/...
koolApp/build.gradle.kts            jvm("desktop") + binaries.executable、js browser、kool-core
koolApp/src/commonMain/kotlin/com/neojou/myants/
    AppVersion.kt                   不變（兩個 configure 標記仍各一處）
    HelloCubeScene.kt               新：README 的場景 → KoolApplication.addHelloCubeScene()
    AppUi.kt                        新：kool ui2 About 按鈕 + 兩行 About 面板
    KoolStudyApp.kt                 新：koolStudyApp()（initOnce、log、場景、UI）+ appTitle
koolApp/src/commonMain/kotlin/com/neojou/tools/
    MyLog.kt、SystemSettings.kt     不變
    ui/menu/MyTopMenuItem.kt        保留（純資料類別，之後可給 kool 版 menu bar 用）
koolApp/src/desktopMain/kotlin/com/neojou/myants/Main.kt   KoolApplication(KoolConfigJvm(windowTitle = appTitle))
koolApp/src/jsMain/kotlin/com/neojou/myants/JsMain.kt      KoolApplication(KoolConfigJs(canvasName = "glCanvas"))
koolApp/src/jsMain/resources/index.html                         <canvas id="glCanvas"> + koolApp.js
```

刪除的檔案：`App.kt`、`HomeScreen.kt`、`Theme.kt`、`tools/ui/menu/MyTopMenuBar.kt`（全部依賴 Compose）。`composeResources/font/notosanstc_regular.otf` 在 trial 裡刪了，因為 kool UI 不讀它；若想保留，可搬到 `koolApp/src/commonMain/resources/fonts/` 備用。

另一個做法是保留模組名 `composeApp`，只改內容。這樣改動比較少，但名稱會誤導，而且 `wasmJsMain` 仍然要改成 `jsMain`。建議改名。

## 4. 檔案層級的變更清單

| 檔案 | 動作 | 狀態 |
| --- | --- | --- |
| `gradle/libs.versions.toml` | 刪 `compose-plugin`、`composeMultiplatform`、`composeCompiler`；加 `kool = "0.19.0"` 與 `kool-core` library | 已驗證 |
| `build.gradle.kts` | 只留 `alias(libs.plugins.kotlinMultiplatform) apply false`，刪掉講 Vite/npm SPA 的過時註解 | 已驗證 |
| `settings.gradle.kts` | `include(":koolApp")`；刪 google() 與 compose dev repo | 已驗證 |
| `composeApp/` → `koolApp/`（`git mv`） | 改名 | 已驗證 |
| `koolApp/build.gradle.kts` | 換成 `kool_setup_ref.md` §3 的版本 | 已驗證 |
| `src/wasmJsMain` → `src/jsMain`；`WasmMain.kt` → `JsMain.kt` | 改名並重寫 | 已驗證 |
| `jsMain/resources/index.html` | 加 `<canvas id="glCanvas" tabindex="0">`，script 改成 `koolApp.js`，`<title>` 保留讓 configure.sh 管理 | 已驗證 |
| `desktopMain/.../Main.kt` | 重寫；刪除 AWT About handler（kool 在 macOS 設 headless，用不到） | 已驗證可編譯並執行 |
| `commonMain/.../HelloCubeScene.kt`、`AppUi.kt`、`KoolStudyApp.kt` | 新增（程式碼見 §5） | 已驗證 |
| `App.kt`、`HomeScreen.kt`、`Theme.kt`、`MyTopMenuBar.kt`、字型 | 刪除 | 已驗證 |
| `AppVersion.kt` | 程式不動；KDoc 的「home headline」改寫成「視窗標題、About 面板」 | 已驗證（不動也能編譯） |
| `configure.sh` | `SOURCE_ROOTS` 改 `koolApp/src/{commonMain,desktopMain,jsMain}/kotlin`；`INDEX_HTML` 改 `koolApp/src/jsMain/resources/index.html`；說明文字的「Compose」改「kool」 | 已驗證：在複本跑 `version 0.2.0` 與 `proj_name "Kool Lab"` 都成功（含套件搬移），改完後 desktop 與 js 都能編譯 |
| `docs/programming/How-To-Change_Project_Name.md` | 路徑 composeApp → koolApp；「讀取位置」改成 `KoolConfigJvm(windowTitle)`、`AppUi.kt` 的 About 面板、`JsMain.kt` 的 `document.title`；macOS 一節改寫（GLFW 視窗、AWT headless、沒有 setAboutHandler；dmg 打包已不提供） | 推測（文件改寫） |
| `docs/programming/How-To-Change_Version_number.md` | `HomeScreen.kt` 的 `AboutDialog` 改成 `AppUi.kt`；刪掉 `nativeDistributions.packageVersion`，改成 Gradle `version` 與 jar、zip 檔名 | 推測（文件改寫） |
| `docs/programming/kool_*.md` | 放入這批參考文件 | — |
| `.gitignore` | 決定 `kotlin-js-store/yarn.lock` 要 commit 還是忽略（建議 commit） | 推測 |

## 5. 新增的程式碼（已驗證，取自 trial）

### HelloCubeScene.kt

```kotlin
package com.neojou.myants

import de.fabmax.kool.KoolApplication
import de.fabmax.kool.addScene
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.deg
import de.fabmax.kool.modules.ksl.KslPbrShader
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.scene.addColorMesh
import de.fabmax.kool.scene.defaultOrbitCamera
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.Time

/**
 * kool README 的 Hello World 場景：旋轉的彩色立方體。
 *
 * 內容與 README 的 `addScene { ... }` 區塊逐行相同，只是包成函式，
 * 讓 desktop 與 js 的 main 共用。
 */
fun KoolApplication.addHelloCubeScene(): Scene = addScene {
    defaultOrbitCamera()
    addColorMesh {
        generate { cube { colored() } }
        shader = KslPbrShader {
            color { vertexColor() }
            metallic(0f)
            roughness(0.25f)
        }
        onUpdate { transform.rotate(45f.deg * Time.deltaT, Vec3f.X_AXIS) }
    }
    lighting.singleDirectionalLight {
        setup(Vec3f(-1f, -1f, -1f))
        setColor(Color.WHITE, 5f)
    }
}
```

### AppUi.kt

```kotlin
package com.neojou.myants

import com.neojou.tools.LogLevel
import com.neojou.tools.MyLog
import de.fabmax.kool.KoolApplication
import de.fabmax.kool.addUiScene
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.scene.Scene

private const val TAG = "AppUi"

/**
 * 疊在 3D 場景上的 UI：左上角 About 按鈕，以及兩行的 About 面板
 * （第一行 [AppVersion.APP_NAME]，第二行 `Version` + [AppVersion.NAME]）。
 */
fun KoolApplication.addAppUiScene(): Scene = addUiScene {
    lateinit var aboutSurface: UiSurface

    // Top menu bar: one leaf item "About".
    addPanelSurface(backgroundColor = { null }) {
        modifier
            .width(FitContent)
            .height(FitContent)
            .align(AlignmentX.Start, AlignmentY.Top)
            .margin(sizes.gap)
        Button("About") {
            modifier
                .padding(horizontal = sizes.largeGap, vertical = sizes.smallGap)
                .onClick {
                    MyLog.add(TAG, "About clicked", LogLevel.DEBUG)
                    aboutSurface.isVisible = true
                }
        }
    }

    // About dialog, hidden until the button is clicked.
    aboutSurface = addPanelSurface {
        modifier
            .size(360.dp, 180.dp)
            .align(AlignmentX.Center, AlignmentY.Center)
            .background(RoundRectBackground(colors.background, 12.dp))
            .padding(sizes.largeGap)
        Text(AppVersion.APP_NAME) {
            modifier.alignX(AlignmentX.Center).margin(top = sizes.largeGap).font(sizes.largeText)
        }
        Text("Version ${AppVersion.NAME}") {
            modifier.alignX(AlignmentX.Center).margin(top = sizes.gap)
        }
        Button("OK") {
            modifier
                .alignX(AlignmentX.Center)
                .margin(top = sizes.largeGap)
                .padding(horizontal = sizes.largeGap, vertical = sizes.smallGap)
                .onClick { aboutSurface.isVisible = false }
        }
    }
    aboutSurface.isVisible = false
}
```

### KoolStudyApp.kt

```kotlin
package com.neojou.myants

import com.neojou.tools.LogLevel
import com.neojou.tools.MyLog
import com.neojou.tools.SystemSettings
import de.fabmax.kool.KoolApplication

private const val TAG = "APP"

/** 共用的 app 內容：desktop 與 js 的 main 都呼叫這裡。 */
fun KoolApplication.koolStudyApp() {
    SystemSettings.initOnce()
    MyLog.add(TAG, "System Ready", LogLevel.DEBUG)
    addHelloCubeScene()
    addAppUiScene()
}

/** 視窗與分頁標題，例如 `myants 0.1.0`。 */
val appTitle: String get() = "${AppVersion.APP_NAME} ${AppVersion.NAME}"
```

### desktopMain/Main.kt

```kotlin
package com.neojou.myants

import de.fabmax.kool.KoolApplication
import de.fabmax.kool.KoolConfigJvm
import de.fabmax.kool.math.Vec2i

/**
 * Desktop entry. kool 自己開 GLFW 視窗（不是 Compose 視窗）。
 * macOS 需要 JVM 參數 -XstartOnFirstThread，已寫在 koolApp/build.gradle.kts。
 */
fun main() = KoolApplication(
    config = KoolConfigJvm(
        windowTitle = appTitle,
        windowSize = Vec2i(1280, 800),
    )
) {
    koolStudyApp()
}
```

### jsMain/JsMain.kt

```kotlin
package com.neojou.myants

import de.fabmax.kool.KoolApplication
import de.fabmax.kool.KoolConfigJs
import kotlinx.browser.document

/** Browser entry. kool 畫在 index.html 的 <canvas id="glCanvas">。 */
fun main() {
    document.title = appTitle
    KoolApplication(config = KoolConfigJs(canvasName = "glCanvas")) {
        koolStudyApp()
    }
}
```

### jsMain/resources/index.html

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>myants</title>
    <style>
        html, body { width: 100%; height: 100%; margin: 0; padding: 0; overflow: hidden; background: #000000; }
        canvas { width: 100vw; height: 100vh; display: block; }
    </style>
</head>
<body>
<!-- kool renders into this canvas. tabindex makes it focusable for key events. -->
<canvas id="glCanvas" tabindex="0"></canvas>
<script src="koolApp.js"></script>
</body>
</html>
```

UI 的重點（已驗證）：
- `addUiScene { }` 的預設 `clearColor = ClearColorLoad`，不會清掉畫面，所以 UI 疊在 3D 場景上面。
- 頂部按鈕面板用 `backgroundColor = { null }` 加上 `FitContent`，只佔按鈕的大小，不會擋住相機的滑鼠操作。
- About 面板是獨立的 `UiSurface`，用 `isVisible` 切換顯示。點 About 會打開面板（log 出現 `[AppUi] About clicked`），點 OK 會關閉。
- kool UI 的預設字型 Fira Sans（MSDF）**沒有中文字**，所以 About 只放 ASCII（名稱與 `Version x.y.z`）。要顯示中文得另外產生 MSDF 字型；desktop 有 `MsdfFontGenerator`，但這部分沒有驗證。

## 6. About 與版號的保留方式

| 原本（Compose） | 改成（kool） | 狀態 |
| --- | --- | --- |
| Top menu「About」→ `AboutDialog`（名稱 / `Version x`） | 左上角 kool ui2 按鈕「About」→ 置中面板（名稱 / `Version x` / OK） | 已驗證 |
| 視窗標題 `APP_NAME` | `KoolConfigJvm(windowTitle = "APP_NAME NAME")`，例如 `myants 0.1.0` | 已驗證（程式寫入；GLFW 標題列在 Mac 上未看到） |
| 網頁標題 | `index.html` 的 `<title>`（configure.sh 管）＋ `JsMain` 設 `document.title = appTitle` | 已驗證可編譯 |
| macOS 系統選單 About → 同一個對話框 | **做不到**：kool 在 macOS 把 AWT 設成 headless，`Desktop.setAboutHandler` 無效。系統選單由 GLFW 建立。選單名稱與標準 About 面板的版號後來由 `MacOsApplicationName` 改成 `APP_NAME` / `AppVersion.NAME` | 見 desktop 的 `MacOsApplicationName.kt` |
| 中央顯示 APP_NAME | 改成旋轉立方體（需求本身） | — |

## 7. 版號建議

這是換渲染框架的大改動，建議做完後執行 `./configure.sh version 0.2.0`（`gradle.properties` 與 `AppVersion.NAME` 一起改），commit 訊息例如 `Version 0.2.0 - kool engine hello world`。

## 8. 驗收條件

1. `./gradlew :koolApp:compileKotlinDesktop :koolApp:compileKotlinJs` 成功（測試機已驗證）。
2. macOS：`./gradlew :koolApp:runDesktop` 開出 kool 視窗，標題是 `myants 0.2.0`，看到彩色立方體繞 X 軸自轉（約每 8 秒一圈）。滑鼠左鍵拖曳可以繞著看，滾輪可以縮放（推測，要在 Mac 上確認）。
3. 左上角「About」打開兩行面板 `myants` / `Version 0.2.0`，OK 可以關閉（測試機已驗證）。
4. `./gradlew :koolApp:jsBrowserDistribution` 成功，`koolApp/build/dist/js/productionExecutable/` 有 `index.html`、`koolApp.js`，以及 `assets/fonts/fira-sans-regular.png`。用任意靜態伺服器開啟（例如 `python3 -m http.server`），可以看到旋轉的立方體（測試機已驗證；Chrome SwiftShader WebGL2）。UI 文字要等圖集有被送進這個目錄才清楚。
5. `./configure.sh version 0.2.0` 與 `./configure.sh proj_name "<名稱>"` 仍然正常，改完後仍可編譯（測試機已驗證）。
6. repo 裡不再有 Compose 相依（`rg -n "compose" --glob '*.kts' --glob '*.toml'` 沒有結果）。

## 9. 風險與注意事項

- **macOS 沒有實機驗證**。`-XstartOnFirstThread`、GLFW Cocoa、MoltenVK（Vulkan）都屬推測。若 Vulkan 失敗後退回 OpenGL 時當掉（kool 0.19.0 的 multi-window bug），在 `KoolConfigJvm` 加 `renderBackend = RenderBackendGl`。
- **Linux X11 不能跑**：kool 0.19.0 的 GLFW 強制用 Wayland。純 X11 會出現 `Unable to initialize GLFW`；main 分支已修。Neo 用 Mac 不受影響。
- **macOS 系統選單**原本會顯示 `java`，About 面板原本顯示 JDK 版號。desktop 的 `MacOsApplicationName` 在啟動後把選單名稱改成 `APP_NAME`，標準 About 的版號改成 `AppVersion.NAME`。它仍不是 app 內的 About 面板。
- **打包**：Compose 的 `nativeDistributions`（dmg）沒有了，只剩 `desktopDistZip`。要 dmg 屬於後續工作（jpackage）。
- **JS 文字**：預設 MSDF 圖集 `fonts/fira-sans-regular.png` 只在 `kool-core-desktop` jar。JS 會抓 `./assets/fonts/fira-sans-regular.png`；檔案不在時 kool 改用全黑貼圖，每個字變成糊掉的色塊，立方體不受影響。`koolApp/build.gradle.kts` 的 `extractKoolJsFonts` 把 png 與授權從 desktop jar 抽到 `jsProcessResources` 的 `assets/fonts/`。這與 SwiftShader 或真 GPU 無關。
- **中文**：kool UI 預設字型沒有 CJK 字形。
- 不要升到 kool main（0.20.0-SNAPSHOT），API 與 repo 都不穩定。
