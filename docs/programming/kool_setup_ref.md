# kool 引擎設定參考（myants 用）

這份文件整理 myants 改用 kool 0.19.0 時需要的版本、Gradle 設定與 macOS 注意事項。

標記說明：
- **已驗證**：2026-10-19 在 Linux 測試機的 scratch 複本（`kool_study_trial`）實際 build 或執行過。
- **推測**：從 kool 原始碼或官方模板推得，還沒在真機上跑過，特別是 macOS 的部分。

## 1. 版本總表

| 項目 | 值 | 狀態 |
| --- | --- | --- |
| kool | `0.19.0`（Maven Central 最新正式版；GitHub main 是 0.20.0-SNAPSHOT，不要用） | 已驗證 |
| Maven 座標 | `de.fabmax.kool:kool-core:0.19.0` | 已驗證 |
| Repository | 只要 `mavenCentral()`。不用 snapshots repo，也不用 google() 或 Compose dev repo | 已驗證 |
| Kotlin | 沿用 repo 的 `2.3.0`。kool 0.19.0 是用 Kotlin 2.2.21 編的，2.3.0 可以讀它的 JVM 與 JS 產物，**不用降版** | 已驗證（desktop 與 js 都編過） |
| Gradle wrapper | 沿用 `9.3.0` | 已驗證 |
| JDK / toolchain | 沿用 `jvmToolchain(25)`（Temurin 25）。kool 本身是 JVM 17 bytecode，17 以上都能跑 | 已驗證（JDK 25） |
| Kotlin 插件 | 只要 `org.jetbrains.kotlin.multiplatform`。**拿掉** `org.jetbrains.compose` 與 `org.jetbrains.kotlin.plugin.compose` | 已驗證 |
| LWJGL | 3.3.6，由 kool-core 遞移帶入 | 已驗證 |
| 平台 targets | `jvm("desktop")` 與 `js { browser() }`。**kool 0.19.0 沒有 wasmJs 版本**，所以 wasmJs target 要拿掉 | 已驗證 |

## 2. LWJGL natives（含 macOS arm64）

kool-core 0.19.0 的 desktop 相依已經對四個平台加了 `runtimeOnly` natives：`natives-linux`、`natives-windows`、`natives-macos`、`natives-macos-arm64`。macOS 另外帶 `lwjgl-vulkan` natives（MoltenVK）。

所以 app 端**不用**自己列 lwjgl natives。這和 kool-editor-template 不同，那個模板會手動列。

已驗證：`installDesktopDist` 的 `lib/` 裡有 38 個 natives jar，包括 `lwjgl-*-3.3.6-natives-macos-arm64.jar`。

## 3. Gradle 片段（已驗證，取自 trial）

### gradle/libs.versions.toml

```toml
[versions]
kotlin = "2.3.0"
kool = "0.19.0"

[libraries]
kool-core = { module = "de.fabmax.kool:kool-core", version.ref = "kool" }

[plugins]
kotlinMultiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
```

### build.gradle.kts（root）

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
}
```

### settings.gradle.kts

```kotlin
rootProject.name = providers.gradleProperty("app.rootName").get()
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(":koolApp")
```

### koolApp/build.gradle.kts

```kotlin
// KMP : Desktop (JVM) + JS (browser), rendered by kool engine.

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

val appGroup = providers.gradleProperty("app.group").get()
val appRootName = providers.gradleProperty("app.rootName").get()
val appDisplayName = providers.gradleProperty("app.displayName").get()
val appVersion = providers.gradleProperty("app.version").get()
val isMacOs = System.getProperty("os.name").orEmpty().contains("mac", ignoreCase = true)

group = appGroup
version = appVersion

kotlin {
    jvm("desktop") {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        binaries {
            executable {
                mainClass.set("$appGroup.MainKt")
                applicationDefaultJvmArgs = buildList {
                    add("--add-opens=java.base/java.lang=ALL-UNNAMED")
                    add("--enable-native-access=ALL-UNNAMED")
                    if (isMacOs) {
                        // GLFW must run on the macOS main thread.
                        add("-XstartOnFirstThread")
                        add("-Dapple.awt.application.name=$appDisplayName")
                        add("-Xdock:name=$appDisplayName")
                    }
                }
            }
        }
    }
    jvmToolchain(25)

    js {
        outputModuleName.set(appRootName)
        browser {
            commonWebpackConfig {
                // Fixed bundle name so index.html need not change when ./configure.sh proj_name renames the project.
                outputFileName = "koolApp.js"
                mode = KotlinWebpackConfig.Mode.DEVELOPMENT
            }
        }
        binaries.executable()
        compilerOptions {
            target.set("es2015")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kool.core)
        }
    }
}

// The MSDF atlas lives in kool-core-desktop.jar. The JS klib does not ship it, and the
// browser loader requests ./assets/fonts/fira-sans-regular.png. Without that file every
// glyph is a solid quad.
val koolJsFonts by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    isTransitive = false
}

dependencies {
    koolJsFonts("de.fabmax.kool:kool-core-desktop:${libs.versions.kool.get()}")
}

val extractKoolJsFonts = tasks.register<Copy>("extractKoolJsFonts") {
    val fontJar = koolJsFonts.elements.map { files -> files.single().asFile }
    from(fontJar.map { zipTree(it) }) {
        include("fonts/fira-sans-regular.png", "fonts/fira-sans-license.txt")
        eachFile { path = "assets/$path" }
    }
    into(layout.buildDirectory.dir("kool-js-fonts"))
    includeEmptyDirs = false
}

tasks.named<ProcessResources>("jsProcessResources") {
    from(extractKoolJsFonts)
}
```

重點：

- `binaries { executable { ... } }` 產生 `runDesktop`、`installDesktopDist`、`desktopDistZip` 等 task。Compose 的 `compose.desktop.application` 不能再用，因為 Compose 插件已經拿掉。
- 打包成 `.dmg` 的 `nativeDistributions` 是 Compose 插件提供的功能，拿掉 Compose 後就沒有了。目前只剩 zip/tar 發佈（`desktopDistZip`）。要 dmg 得另外處理（例如 jpackage），屬於後續工作。
- `--add-opens` 與 `--enable-native-access` 照抄 kool-basic-template。少了 `--enable-native-access`，JDK 25 會對 LWJGL 印出警告。
- JS 的 `mode = DEVELOPMENT` 照抄模板，bundle 約 4 MiB。正式發佈可以改成 `PRODUCTION`（推測可行，未驗證）。
- 瀏覽器的 UI 字型圖集不在 `kool-core-js` klib 裡，只在 `kool-core-desktop` jar 的 `fonts/fira-sans-regular.png`。JS 會向 `./assets/fonts/fira-sans-regular.png` 要這張圖。`extractKoolJsFonts` 從 desktop jar 抽出 png 與 OFL 授權，`jsProcessResources` 放到 `assets/fonts/`，開發伺服器與 `jsBrowserDistribution` 才讀得到。少了這張圖，每個字都是一塊糊掉的色塊，立方體仍然正常。
- 第一次 build JS 會產生 `kotlin-js-store/yarn.lock`。Kotlin 官方建議 commit 它；現有 `.gitignore` 只忽略 `kotlin-js-store/wasm/yarn.lock`。

## 4. 常用指令

| 目的 | 指令 | 狀態 |
| --- | --- | --- |
| 編 desktop | `./gradlew :koolApp:compileKotlinDesktop` | 已驗證 |
| 跑 desktop | `./gradlew :koolApp:runDesktop` | task 存在且會帶正確 JVM 參數啟動（已驗證）。測試機因為缺 GPU/桌面環境而沒開出 GLFW 視窗，見第 6 節。macOS 推測可跑 |
| 打包 desktop | `./gradlew :koolApp:installDesktopDist`，執行 `koolApp/build/install/koolApp-desktop/bin/koolApp` | 已驗證可產出 |
| JS 發佈檔 | `./gradlew :koolApp:jsBrowserDistribution`，輸出在 `koolApp/build/dist/js/productionExecutable/` | 已驗證，並在 Chrome 畫出立方體 |
| JS 開發伺服器 | `./gradlew :koolApp:jsBrowserDevelopmentRun` | 推測（task 存在） |

注意：task 名稱是 `runDesktop`，不是 Compose 時代的 `run`。

## 5. macOS 注意事項

1. **`-XstartOnFirstThread` 必要**（推測，依據官方模板與 GLFW 規則）。GLFW 在 macOS 必須跑在主執行緒。kool-basic-template 只在 macOS 加這個參數；上面的 build.gradle.kts 也是同樣寫法。直接執行 jar 時要自己加。
2. **kool 在 macOS 會把 AWT 設成 headless**（已從原始碼確認：`GlfwWindowSubsystem.onEarlyInit` 會設 `java.awt.headless=true`）。結果：
   - 原本 `Main.kt` 用的 `java.awt.Desktop.setAboutHandler` 不能用。headless 下 `Desktop.isDesktopSupported()` 回傳 false，所以要刪掉。
   - About 改由 kool UI 自己畫，見 migration plan。
3. **系統選單名稱**。視窗由 GLFW（Cocoa）建立，不是 AWT。GLFW 在 `glfwInit()` 的 `createMenuBar` 依序讀 `CFBundleDisplayName`、`CFBundleName`、`CFBundleExecutable`，都沒有才用執行檔名稱。`runDesktop` 跑的是 `java`，所以選單原本是 `java`，項目是 `About java`。`-Xdock:name` 與 `-Dapple.awt.application.name` 只影響 AWT / Dock，改不到這個選單；kool 還會把 AWT 設成 headless。desktop 的 `MacOsApplicationName.kt` 在啟動後把選單標題與 `About` / `Hide` / `Quit` 改成 `AppVersion.APP_NAME`，並設定 `NSProcessInfo` 的行程名稱。系統 About 仍是 macOS 標準面板；點下去時改走 `orderFrontStandardAboutPanelWithOptions:`，名稱是 `APP_NAME`，版號是 `AppVersion.NAME`，不再顯示 JDK 版號。
4. **渲染後端**。kool 0.19.0 desktop 預設用 Vulkan（macOS 走 MoltenVK），失敗時退回 OpenGL（`useOpenGlFallback = true`）。測試機看到一個 kool bug：Vulkan 在**視窗建立後**才失敗時，退回 OpenGL 會丟 `Multi-window operation is still not implemented` 而當掉。如果 Mac 上遇到，可以在 `KoolConfigJvm` 加 `renderBackend = RenderBackendGl`（`import de.fabmax.kool.pipeline.backend.gl.RenderBackendGl`）強制用 OpenGL。macOS 的 OpenGL 最高 4.1，kool 會關掉需要 4.3 以上的功能（推測仍能畫 Hello World）。
5. JDK 25 會印 `sun.misc.Unsafe` deprecated 警告，來源是 LWJGL 3.3.6，可以忽略（已驗證只是警告）。

## 6. 測試環境的限制（為什麼 desktop 的驗證是用替代視窗）

- 測試機是沒有 GPU 的 Linux（Xvfb + Mesa llvmpipe，OpenGL 4.5，沒有 Vulkan ICD）。
- kool 0.19.0 在 Linux 上只要 GLFW 支援 Wayland，就會強制用 Wayland（`glfwInitHint(GLFW_PLATFORM, GLFW_PLATFORM_WAYLAND)`），所以在純 X11 的 Xvfb 上 `Unable to initialize GLFW`。kool main 分支已經拿掉這段。
- 改用 headless Weston（Wayland）時，GLFW 初始化成功，OpenGL 4.5 也起來了，但 `glfwGetKeyName` 在沒有鍵盤的 Wayland seat 上 segfault（libxkbcommon）。
- 最後用**同一份 app 程式碼**，換成 `SwingWindowSubsystem` 加 `RenderBackendGl`，並設 `asyncSceneUpdate = false`，成功畫出旋轉的彩色立方體與 About UI，滑鼠點擊也正常。這只用於測試；Swing 搭配非同步場景更新時會卡住。
- 以上都是 Linux 專屬問題。macOS 走 GLFW Cocoa，不會經過 Wayland 這段程式碼。
