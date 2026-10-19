package com.neojou.myants

import com.neojou.myants.sim.QualityLevel

data class LaunchConfig(
    val seed: Int? = null,
    val colonies: Int? = null,
    val quality: QualityLevel? = null,
)

fun parseLaunch(tokens: List<String>): LaunchConfig {
    var seed: Int? = null
    var colonies: Int? = null
    var quality: QualityLevel? = null
    for (raw in tokens) {
        val body = raw.removePrefix("--").removePrefix("-")
        val eq = body.indexOf('=')
        if (eq <= 0) continue
        val key = body.substring(0, eq)
        val value = body.substring(eq + 1)
        when (key) {
            "seed" -> seed = value.toIntOrNull()
            "colonies" -> colonies = value.toIntOrNull()?.coerceIn(1, 2)
            "quality" -> quality = parseQuality(value)
        }
    }
    return LaunchConfig(seed, colonies, quality)
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

expect class SettingsStore() {
    fun read(): String?
    fun write(text: String)
}
