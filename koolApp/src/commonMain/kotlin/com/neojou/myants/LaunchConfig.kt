package com.neojou.myants

import com.neojou.myants.render3d.GpuBackend
import com.neojou.myants.render3d.RenderMode
import com.neojou.myants.render3d.parseBackend
import com.neojou.myants.render3d.parseRenderMode
import com.neojou.myants.sim.QualityLevel

data class LaunchConfig(
    val seed: Int? = null,
    val colonies: Int? = null,
    val quality: QualityLevel? = null,
    val renderer: RenderMode? = null,
    val backend: GpuBackend? = null,
    val noBloom: Boolean = false,
    val shot: String? = null,
    val shotOut: String? = null,
)

fun parseLaunch(tokens: List<String>): LaunchConfig {
    var seed: Int? = null
    var colonies: Int? = null
    var quality: QualityLevel? = null
    var renderer: RenderMode? = null
    var backend: GpuBackend? = null
    var noBloom = false
    var shot: String? = null
    var shotOut: String? = null
    for (raw in tokens) {
        val body = raw.removePrefix("--").removePrefix("-")
        if (body == "no-bloom" || body == "nobloom") {
            noBloom = true
            continue
        }
        val eq = body.indexOf('=')
        if (eq <= 0) continue
        val key = body.substring(0, eq)
        val value = body.substring(eq + 1)
        when (key) {
            "seed" -> seed = value.toIntOrNull()
            "colonies" -> colonies = value.toIntOrNull()?.coerceIn(1, 2)
            "quality" -> quality = parseQuality(value)
            "renderer" -> renderer = parseRenderMode(value)
            "backend" -> backend = parseBackend(value)
            "shot" -> shot = value
            "shot-out" -> shotOut = value
        }
    }
    return LaunchConfig(seed, colonies, quality, renderer, backend, noBloom, shot, shotOut)
}

fun parseQuality(raw: String): QualityLevel? = when (raw.uppercase().replace('-', '_')) {
    "ULTRA_LOW", "ULTRALOW" -> QualityLevel.ULTRA_LOW
    "LOW" -> QualityLevel.LOW
    "MEDIUM" -> QualityLevel.MEDIUM
    "HIGH" -> QualityLevel.HIGH
    "ULTRA" -> QualityLevel.ULTRA
    else -> null
}

expect fun wallClockSeed(): Int

/** Wall-clock hours and minutes for the stone display. Not part of the sim stream. */
expect fun wallClockHm(): Pair<Int, Int>

/** Local hours, minutes, and seconds. The clock stone blinks its colon from the second. */
expect fun wallClockHms(): Triple<Int, Int, Int>

expect class SettingsStore() {
    fun read(): String?
    fun write(text: String)
}
