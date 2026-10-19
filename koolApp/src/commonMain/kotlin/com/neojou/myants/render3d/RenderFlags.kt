package com.neojou.myants.render3d

import com.neojou.myants.AppSession
import de.fabmax.kool.KoolSystem

/**
 * Bloom is a compute pass. Desktop GL reports computeShaders=true even when the
 * context cannot run it, so the backend class name is part of the gate.
 */
fun bloomAllowed(session: AppSession): Boolean {
    if (session.noBloom || !session.bloomUser) return false
    val ctx = KoolSystem.getContextOrNull() ?: return false
    if (!ctx.backend.features.computeShaders && !KoolSystem.features.computeShaders) {
        // Prefer the context flag. KoolSystem.features is the same source in 0.19.
    }
    if (!KoolSystem.features.computeShaders) return false
    val name = ctx.backend::class.simpleName.orEmpty()
    val gpuOk = name.contains("Vk") || name.contains("WebGpu") || name.contains("Wgpu")
    return gpuOk
}

fun backendLabel(): String {
    val ctx = KoolSystem.getContextOrNull() ?: return "none"
    return ctx.backend::class.simpleName.orEmpty()
}
