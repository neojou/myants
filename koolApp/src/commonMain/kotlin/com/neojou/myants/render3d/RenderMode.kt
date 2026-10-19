package com.neojou.myants.render3d

/** World picture. Classic keeps the v0.7.0 sprite scene. */
enum class RenderMode { D3, CLASSIC }

enum class GpuBackend { VK, GL }

fun parseRenderMode(raw: String): RenderMode? = when (raw.lowercase()) {
    "3d", "d3" -> RenderMode.D3
    "2d", "classic", "classic2d" -> RenderMode.CLASSIC
    else -> null
}

fun parseBackend(raw: String): GpuBackend? = when (raw.lowercase()) {
    "vk", "vulkan" -> GpuBackend.VK
    "gl", "opengl" -> GpuBackend.GL
    else -> null
}
