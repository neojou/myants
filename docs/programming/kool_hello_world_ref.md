# kool Hello World 參考

目前的程式入口是 `myantsApp()`（`MyantsApp.kt`），不再呼叫 `koolStudyApp()`。下面是 Hello World 當時的寫法。

kool README 的 Hello World：一個會自轉的彩色立方體，加上一盞方向光，可以用滑鼠繞著看。

狀態：
- **已驗證**：下面「完整程式」原封不動放進 desktopMain 與 jsMain，Kotlin 2.3.0 加 kool 0.19.0 都能編譯。JS 版在 Chrome（WebGL2）畫出旋轉的立方體。Desktop 版用 OpenGL 加 Swing 視窗（測試機限制）也畫出了立方體。
- **推測**：macOS 用預設 GLFW 視窗執行。

## 1. 完整程式（含 import）

```kotlin
package com.neojou.myants

import de.fabmax.kool.KoolApplication
import de.fabmax.kool.addScene
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.deg
import de.fabmax.kool.modules.ksl.KslPbrShader
import de.fabmax.kool.scene.addColorMesh
import de.fabmax.kool.scene.defaultOrbitCamera
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.Time

fun main() = KoolApplication {
    addScene {
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
}
```

### import 對照

| 符號 | import | 種類 |
| --- | --- | --- |
| `KoolApplication { }` | `de.fabmax.kool.KoolApplication` | 平台各自的頂層函式。JVM 版在 desktopMain，JS 版在 jsMain，簽名相同 |
| `addScene` | `de.fabmax.kool.addScene` | `KoolApplication` 的 extension |
| `defaultOrbitCamera` | `de.fabmax.kool.scene.defaultOrbitCamera` | `Scene` 的 extension |
| `addColorMesh` | `de.fabmax.kool.scene.addColorMesh` | `Node` 的 extension |
| `KslPbrShader` | `de.fabmax.kool.modules.ksl.KslPbrShader` | class，搭配 builder lambda |
| `Vec3f` | `de.fabmax.kool.math.Vec3f` | |
| `deg` | `de.fabmax.kool.math.deg` | `Float.deg` extension property，回傳 `AngleF` |
| `Time` | `de.fabmax.kool.util.Time` | object，`Time.deltaT` 是上一幀的秒數 |
| `Color` | `de.fabmax.kool.util.Color` | |

`generate`、`cube`、`colored`、`onUpdate`、`transform`、`lighting`、`singleDirectionalLight`、`setup`、`setColor` 都是 receiver 的成員，不用 import。

## 2. 逐行說明

| 程式 | 說明 |
| --- | --- |
| `fun main() = KoolApplication { ... }` | 建立 kool 的 context 與視窗（JVM 是 GLFW 視窗；JS 是 `<canvas id="glCanvas">`），然後執行 lambda。lambda 的 receiver 是 `KoolApplication`（有 `ctx`），而且是 `suspend`。JVM 版會阻塞，直到視窗關閉。|
| `addScene { ... }` | 新增一個 `Scene`（3D 場景）並加到 context。lambda 的 receiver 是 `Scene`。|
| `defaultOrbitCamera()` | 預設軌道相機。左鍵拖曳旋轉，右鍵或中鍵平移，滾輪縮放。預設角度是 yaw 20°、pitch −30°。|
| `addColorMesh { ... }` | 新增一個帶頂點顏色屬性的 Mesh 節點。|
| `generate { cube { colored() } }` | 用 MeshBuilder 產生立方體幾何；`colored()` 讓六個面各有不同顏色（存在頂點顏色）。|
| `shader = KslPbrShader { ... }` | 用 kool Shader DSL（KSL）的 PBR 材質。|
| `color { vertexColor() }` | 底色取自頂點顏色，也就是上面的六色。|
| `metallic(0f)` / `roughness(0.25f)` | 非金屬、略帶光澤。|
| `onUpdate { transform.rotate(45f.deg * Time.deltaT, Vec3f.X_AXIS) }` | 每幀都會呼叫。繞 X 軸轉「每秒 45° × 經過秒數」，所以轉速和幀率無關。|
| `lighting.singleDirectionalLight { ... }` | 場景只放一盞方向光。|
| `setup(Vec3f(-1f, -1f, -1f))` | 光的方向往 (−1,−1,−1)，從右上前方往左下後方照。|
| `setColor(Color.WHITE, 5f)` | 白光，強度 5。|

## 3. 各平台的啟動方式（launcher）

`KoolApplication(...)` **不是** expect/actual，而是 desktopMain 與 jsMain 各有一個同名函式。所以 `fun main() = KoolApplication { }` 不能寫在 commonMain。建議的拆法是共用場景放 commonMain，兩個平台各寫一個很薄的 main。

### commonMain：共用場景（已驗證）

```kotlin
fun KoolApplication.addHelloCubeScene(): Scene = addScene {
    // ...與上面 addScene { } 內容相同...
}
```

另外要 import `de.fabmax.kool.scene.Scene`。

### desktopMain（JVM）（已驗證可編譯）

```kotlin
import de.fabmax.kool.KoolApplication
import de.fabmax.kool.KoolConfigJvm
import de.fabmax.kool.math.Vec2i

fun main() = KoolApplication(
    config = KoolConfigJvm(
        windowTitle = appTitle,          // 例如 "myants 0.1.0"
        windowSize = Vec2i(1280, 800),
    )
) {
    koolStudyApp()                       // addHelloCubeScene() + About UI
}
```

0.19.0 的 JVM 簽名是 `fun KoolApplication(config: KoolConfigJvm = KoolConfigJvm(), appBlock: suspend KoolApplication.() -> Unit)`。不帶參數就是 README 的寫法。

常用的 `KoolConfigJvm` 參數：`windowTitle`、`windowSize`、`renderBackend`（預設 Vulkan，可改 `RenderBackendGl`）、`useOpenGlFallback`、`isVsync`、`windowSubsystem`（預設 GLFW）。

macOS 一定要 `-XstartOnFirstThread`，已經寫在 `binaries.executable.applicationDefaultJvmArgs`。

### jsMain（瀏覽器）（已驗證可編譯並畫出）

```kotlin
import de.fabmax.kool.KoolApplication
import de.fabmax.kool.KoolConfigJs
import kotlinx.browser.document

fun main() {
    document.title = appTitle
    KoolApplication(config = KoolConfigJs(canvasName = "glCanvas")) {
        koolStudyApp()
    }
}
```

JS 版先試 WebGPU，沒有可用的 adapter 時會自動退回 WebGL2（已驗證，log 會出現 `Failed creating render backend WebGPU` 後接 `GlImpl`）。

### index.html（jsMain/resources）

```html
<canvas id="glCanvas" tabindex="0"></canvas>
<script src="koolApp.js"></script>
```

- canvas id 必須和 `KoolConfigJs.canvasName` 一樣，預設是 `glCanvas`。
- `tabindex="0"` 讓 canvas 可以取得焦點、收到鍵盤事件（照抄官方模板）。
- CSS 讓 canvas 填滿視窗：`canvas { width: 100vw; height: 100vh; display: block; }`。
- script 檔名要和 `commonWebpackConfig.outputFileName` 一樣（trial 固定為 `koolApp.js`）。
- UI 文字用的 Fira Sans MSDF 圖集不在 JS klib 裡。瀏覽器要能抓到 `./assets/fonts/fira-sans-regular.png`（由 `koolApp/build.gradle.kts` 從 `kool-core-desktop` jar 抽出）。只有 `index.html` 與 `koolApp.js` 時，About 與版號會變成糊掉的色塊。

## 4. 不支援的 target

- **wasmJs**：kool 0.19.0 只發佈 jvm（desktop）、js 與 android 版，沒有 wasmJs klib，所以 wasmJs target 加 kool-core 會解析失敗（推測，依據 kool 的 `kool.lib-conventions` 只宣告 `jvm("desktop")` 與 `js`）。
