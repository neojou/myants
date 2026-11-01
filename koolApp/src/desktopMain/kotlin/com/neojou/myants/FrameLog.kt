package com.neojou.myants

import de.fabmax.kool.KoolContext
import java.io.File

/**
 * Opt-in frame timing for fps measurement (#52): when the environment variable
 * MYANTS_FRAME_LOG names a file, each rendered frame appends its System.nanoTime().
 * Off by default; it does not touch the sim.
 */
internal fun attachFrameLog(ctx: KoolContext, path: String? = System.getenv("MYANTS_FRAME_LOG")) {
    if (path.isNullOrBlank()) return
    val out = File(path).bufferedWriter()
    Runtime.getRuntime().addShutdownHook(Thread { runCatching { out.flush(); out.close() } })
    var frames = 0
    ctx.onRender += {
        out.write(System.nanoTime().toString())
        out.newLine()
        if (++frames % 120 == 0) out.flush()
    }
}
