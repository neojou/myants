package com.neojou.myants.render3d.phero

import kotlin.math.min
import kotlin.math.sqrt

/**
 * Same channels as the Classic 2D overlay:
 * R = √(protein+sugar+danger), G = √sugar, B = √(home+danger), threshold 0.01.
 */
object PheromoneColor {
    fun rgba(
        home: Double,
        sugar: Double,
        protein: Double,
        danger: Double,
        strength: Double,
    ): IntArray {
        if (home <= 0.01 && sugar <= 0.01 && protein <= 0.01 && danger <= 0.01) {
            return intArrayOf(0, 0, 0, 0)
        }
        val gain = min(1.0, 0.85 * strength)
        return intArrayOf(
            channel(sqrt(protein + sugar + danger) * gain),
            channel(sqrt(sugar) * gain),
            channel(sqrt(home + danger) * gain),
            255,
        )
    }

    private fun channel(v: Double): Int = min(255.0, v * 255.0).toInt().coerceIn(0, 255)
}
