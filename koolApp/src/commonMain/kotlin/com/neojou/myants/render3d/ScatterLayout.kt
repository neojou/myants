package com.neojou.myants.render3d

import com.neojou.myants.render3d.quality.QualityPreset
import com.neojou.myants.sim.SimConfig
import kotlin.random.Random

/** One decorative leaf. Coordinates match the scatter buffer, so a later pick can find the same leaf. */
internal data class LeafSpot(val x: Float, val y: Float)

internal class ScatterFields(
    val extraGrass: FloatArray,
    val pebbles: FloatArray,
    val leaves: FloatArray,
    val flowers: FloatArray,
)

/**
 * Decorative scatter shared by the world renderer and Inspect.
 * The random stream order is extra grass, pebbles, leaves, then flowers.
 */
internal object ScatterLayout {
    fun build(seed: Int, serial: Int, preset: QualityPreset, grassDensity: Float): ScatterFields {
        val rnd = Random(seed.toLong() xor serial.toLong() * 0x9E3779B9L xor 0x51A7L)
        return ScatterFields(
            pack(rnd, (preset.fineGrass * grassDensity).toInt()),
            pack(rnd, (preset.pebbles * grassDensity).toInt()),
            pack(rnd, (preset.leaves * grassDensity).toInt()),
            pack(rnd, (preset.flowers * grassDensity).toInt()),
        )
    }

    fun leafSpots(seed: Int, serial: Int, preset: QualityPreset, grassDensity: Float): List<LeafSpot> {
        val leaves = build(seed, serial, preset, grassDensity).leaves
        val out = ArrayList<LeafSpot>(leaves.size / 4)
        var i = 0
        while (i < leaves.size) {
            out += LeafSpot(leaves[i], leaves[i + 1])
            i += 4
        }
        return out
    }

    private fun pack(rnd: Random, count: Int): FloatArray {
        val out = FloatArray(count * 4)
        var i = 0
        while (i < out.size) {
            out[i] = rnd.nextFloat() * SimConfig.width
            out[i + 1] = rnd.nextFloat() * SimConfig.height
            out[i + 2] = rnd.nextFloat()
            out[i + 3] = rnd.nextFloat()
            i += 4
        }
        return out
    }
}
