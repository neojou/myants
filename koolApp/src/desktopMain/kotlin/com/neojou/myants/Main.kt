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
    applyMacOsApplicationName(AppVersion.APP_NAME, AppVersion.NAME)
    koolStudyApp()
}
