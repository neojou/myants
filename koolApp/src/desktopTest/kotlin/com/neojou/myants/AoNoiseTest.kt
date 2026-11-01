package com.neojou.myants

import com.neojou.myants.render3d.AoNoise
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/** #83: the SSAO noise must be the same on every load (kool's own shuffle is unseeded). */
class AoNoiseTest {
    @Test
    fun anglesAreTheSameEveryTime() = assertContentEquals(AoNoise.angles(), AoNoise.angles())

    @Test
    fun anglesAreKoolsSixteenStepsReordered() {
        val n = AoNoise.SIZE * AoNoise.SIZE
        val want = (0 until n).map { 2f * PI.toFloat() * it / n }
        assertEquals(want, AoNoise.angles().sorted())
    }
}
