package com.neojou.myants

import com.neojou.myants.render3d.GpuBackend
import de.fabmax.kool.KoolApplication
import de.fabmax.kool.KoolConfigJvm
import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.pipeline.backend.gl.RenderBackendGl
import de.fabmax.kool.pipeline.backend.vk.RenderBackendVk

/**
 * Desktop entry. kool opens its own GLFW window.
 * macOS needs -XstartOnFirstThread, set in koolApp/build.gradle.kts.
 * The backend flag is read before [KoolApplication] so Vulkan stays the default.
 */
fun main(args: Array<String>) {
    // Gradle redirects stdout to a file, which is block-buffered. Shot failures
    // must show up without waiting for the process to exit.
    System.setOut(java.io.PrintStream(System.out, true))
    System.setErr(java.io.PrintStream(System.err, true))
    val launch = parseLaunch(args.toList()).let {
        if (System.getenv("MYANTS_LOCK_QUALITY") == "1") it.copy(lockQuality = true) else it
    }
    val backend = if (launch.backend == GpuBackend.GL) RenderBackendGl else RenderBackendVk
    KoolApplication(
        config = KoolConfigJvm(
            windowTitle = appTitle,
            windowSize = Vec2i(1600, 900),
            renderBackend = backend,
        ),
    ) {
        applyMacOsApplicationName(AppVersion.APP_NAME, AppVersion.NAME)
        attachFrameLog(ctx)
        myantsApp(launch)
    }
}
