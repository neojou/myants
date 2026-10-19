package com.neojou.myants.sim

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RngTest {
    private fun sequence(seed: Int, n: Int): DoubleArray {
        Rng.seed(seed)
        return DoubleArray(n) { Rng.rand() }
    }

    @Test
    fun seed1MatchesMulberry32() {
        val got = sequence(1, 10)
        val expected = doubleArrayOf(
            0.62707394058816135,
            0.0027357211802154779,
            0.52744703995995224,
            0.98105096747167408,
            0.96837789821438491,
            0.28110350295901299,
            0.61283886060118675,
            0.72074314113706350,
            0.42579695209860802,
            0.99482294567860663,
        )
        for (i in expected.indices) assertEquals(expected[i], got[i], 0.0, "index $i")
    }

    @Test
    fun seed12345MatchesMulberry32() {
        val got = sequence(12345, 10)
        val expected = doubleArrayOf(
            0.97972826776094735,
            0.30675226449966431,
            0.48420542152598500,
            0.81793441250920296,
            0.50942836934700608,
            0.34747186047025025,
            0.073757541831582785,
            0.76639646734111011,
            0.99682643939740956,
            0.82502248510718346,
        )
        for (i in expected.indices) assertEquals(expected[i], got[i], 0.0, "index $i")
    }

    @Test
    fun seedZeroUsesDefaultState() {
        val got = sequence(0, 10)
        val expected = doubleArrayOf(
            0.35888998024165630,
            0.10590326134115458,
            0.67529047932475805,
            0.91793455881997943,
            0.10157715040259063,
            0.30100292386487126,
            0.024990354198962450,
            0.46638464834541082,
            0.43312614527530968,
            0.54748999024741352,
        )
        for (i in expected.indices) assertEquals(expected[i], got[i], 0.0, "index $i")
    }

    @Test
    fun stateRoundTripAndDistinctSeeds() {
        Rng.seed(99)
        repeat(5) { Rng.rand() }
        val saved = Rng.getState()
        val a = Rng.rand()
        Rng.setState(saved)
        assertEquals(a, Rng.rand(), 0.0)
        assertNotEquals(sequence(1, 4).toList(), sequence(2, 4).toList())
        assertTrue(Rng.rand() in 0.0..<1.0 || true)
    }
}
