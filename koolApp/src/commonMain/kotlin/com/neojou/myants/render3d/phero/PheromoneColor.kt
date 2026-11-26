package com.neojou.myants.render3d.phero

import kotlin.math.min
import kotlin.math.sqrt

/**
 * The one pheromone color function, shared by the 3D textures (outdoor and nest cutaway) and Classic 2D (#87).
 *
 * Channels: R = √(protein+sugar+danger), G = √sugar, B = √(home+danger), all times
 * gain = min(1, 0.85·strength); threshold 0.01. Danger rides the B channel (magenta with R).
 * Only the *home* share of B is re-tinted: w = home/(home+danger), and that share goes from pure blue
 * to [HOME] (#0088D9). With no home the output is bit-identical to the old formula.
 *
 * In the 3D outdoor pass the bytes are read as linear values (gamma 1) and multiplied by the
 * day/night boost; nest cutaway and Classic 2D add them in display space. Do not convert sRGB here.
 */
object PheromoneColor {
    /** `PHERO_HOME` #0088D9 as 0..1 per channel (texture bytes 0, 136, 217). */
    val HOME: DoubleArray = doubleArrayOf(0.0, 136.0 / 255.0, 217.0 / 255.0)

    fun rgba(
        home: Double,
        sugar: Double,
        protein: Double,
        danger: Double,
        strength: Double,
    ): IntArray = IntArray(4).also { pack(home, sugar, protein, danger, strength, it) }

    /** Allocation-free [rgba]: writes R, G, B, A (0..255) into [out] (size >= 4). */
    fun pack(
        home: Double,
        sugar: Double,
        protein: Double,
        danger: Double,
        strength: Double,
        out: IntArray,
    ) {
        if (home <= 0.01 && sugar <= 0.01 && protein <= 0.01 && danger <= 0.01) {
            out[0] = 0; out[1] = 0; out[2] = 0; out[3] = 0
            return
        }
        val gain = min(1.0, 0.85 * strength)
        val r = sqrt(protein + sugar + danger) * gain
        val g = sqrt(sugar) * gain
        val b = sqrt(home + danger) * gain
        val homeDanger = home + danger
        val w = if (homeDanger > 0.0) home / homeDanger else 0.0
        out[0] = channel(r)
        out[1] = channel(g + b * w * HOME[1])
        out[2] = channel(b * ((1.0 - w) + w * HOME[2]))
        out[3] = 255
    }

    private fun channel(v: Double): Int = min(255.0, v * 255.0).toInt().coerceIn(0, 255)
}
