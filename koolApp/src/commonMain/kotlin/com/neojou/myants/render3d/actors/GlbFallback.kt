package com.neojou.myants.render3d.actors

import com.neojou.tools.LogLevel
import com.neojou.tools.MyLog

/**
 * Turns a glb load result into a parsed model, or null when the renderer must keep its
 * procedural fallback (#52). A missing file, a bad header or a truncated file never throws:
 * each writes one WARN line naming the asset and the fallback.
 */
internal fun <T : Any> parseGlbOrFallback(
    label: String,
    fallback: String,
    loaded: Result<ByteArray>,
    warn: (String) -> Unit = { MyLog.add("RENDER", it, LogLevel.WARN) },
    parse: (ByteArray) -> T,
): T? {
    val bytes = loaded.getOrElse { err ->
        warn("$label glTF missing: ${err.message}. Using the $fallback.")
        return null
    }
    return try {
        parse(bytes)
    } catch (err: Throwable) {
        warn("$label glTF failed: ${err.message}. Using the $fallback.")
        null
    }
}
