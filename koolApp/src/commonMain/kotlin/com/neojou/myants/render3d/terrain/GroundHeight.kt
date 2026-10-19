package com.neojou.myants.render3d.terrain

import com.neojou.myants.sim.SimConfig
import kotlin.math.floor
import kotlin.random.Random

/**
 * Render-only height. Amplitude stays within 6 units so the top-down view stays readable.
 * The seed is mixed by the caller; this function never touches [com.neojou.myants.sim.Rng].
 */
object GroundHeight {
    private var random: Random = Random(0x6D001)
    private var field: FloatArray = FloatArray(0)
    private var cols: Int = 0
    private var rows: Int = 0

    fun rebuild(seed: Int, serial: Int) {
        random = Random(seed.toLong() xor 0x6D001L xor serial.toLong() * 0x9E3779B9L)
        cols = 48
        rows = 32
        field = FloatArray(cols * rows)
        for (i in field.indices) {
            field[i] = random.nextFloat()
        }
        // A couple of smooth passes. Still a pure function of the seed.
        repeat(2) { smooth() }
        var peak = 0.0001f
        for (v in field) if (v > peak) peak = v
        for (i in field.indices) field[i] = field[i] / peak * 6f
    }

    fun at(x: Float, z: Float): Float {
        if (field.isEmpty()) return 0f
        val u = (x / SimConfig.width.toFloat()).coerceIn(0f, 0.999f) * (cols - 1)
        val v = (z / SimConfig.height.toFloat()).coerceIn(0f, 0.999f) * (rows - 1)
        return sample(u, v)
    }

    /** Bump used by tests: same seed and serial always return the same height. */
    fun sampleForTest(seed: Int, serial: Int, x: Float, z: Float): Float {
        rebuild(seed, serial)
        return at(x, z)
    }

    private fun smooth() {
        val next = FloatArray(field.size)
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                var sum = 0f
                var n = 0
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val xx = x + dx
                        val yy = y + dy
                        if (xx in 0 until cols && yy in 0 until rows) {
                            sum += field[yy * cols + xx]
                            n++
                        }
                    }
                }
                next[y * cols + x] = sum / n
            }
        }
        field = next
    }

    private fun sample(u: Float, v: Float): Float {
        val x0 = floor(u).toInt().coerceIn(0, cols - 1)
        val y0 = floor(v).toInt().coerceIn(0, rows - 1)
        val x1 = (x0 + 1).coerceAtMost(cols - 1)
        val y1 = (y0 + 1).coerceAtMost(rows - 1)
        val tx = u - x0
        val ty = v - y0
        val a = field[y0 * cols + x0]
        val b = field[y0 * cols + x1]
        val c = field[y1 * cols + x0]
        val d = field[y1 * cols + x1]
        val ab = a + (b - a) * tx
        val cd = c + (d - c) * tx
        return ab + (cd - ab) * ty
    }
}
