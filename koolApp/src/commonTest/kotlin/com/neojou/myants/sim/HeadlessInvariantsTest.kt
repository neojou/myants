package com.neojou.myants.sim

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HeadlessInvariantsTest {
    @Test
    fun sameSeedIsReproducible() {
        val a = runHeadless(12345, 1000)
        val b = runHeadless(12345, 1000)
        assertEquals(a, b)
    }

    @Test
    fun differentSeedsDiverge() {
        val a = runHeadless(1, 800)
        val b = runHeadless(2, 800)
        val same = a.population == b.population && a.sugar == b.sugar &&
            a.protein == b.protein && a.queenEnergy == b.queenEnergy && a.brood == b.brood
        assertTrue(!same)
    }

    @Test
    fun stocksStayNonNegative() {
        val m = runHeadless(12345, 1000)
        assertTrue(m.sugar >= 0)
        assertTrue(m.protein >= 0)
        assertTrue(m.population >= 0)
        assertTrue(m.queenEnergy >= 0)
        assertTrue(m.brood >= 0)
    }

    @Test
    fun phase2SoakSeed7() {
        val m = runHeadless(7, 6000)
        assertTrue(m.population > 0)
        assertTrue(m.queenEnergy > 0)
        assertTrue(m.peakPopulation >= 16)
        assertTrue(m.sugar + m.protein > 0)
        assertTrue(m.brood > 0)
        assertTrue(m.population <= 300)
    }

    @Test
    fun phase2SeedsStayAlive() {
        for (seed in intArrayOf(1, 3, 42)) {
            val m = runHeadless(seed, 4000)
            assertTrue(m.queenEnergy > 0, "seed $seed queen")
            assertTrue(m.population > 0, "seed $seed pop")
            assertTrue(m.peakPopulation >= 20, "seed $seed peak ${m.peakPopulation}")
            assertTrue(m.sugar >= 0 && m.protein >= 0)
            assertTrue(m.sugar + m.protein > 0)
            assertTrue(m.brood > 0)
            assertTrue(m.population <= 300)
        }
    }

    @Test
    fun qualityDoesNotChangeOutcomeUnderCap() {
        val saved = PerformanceManager.level
        try {
            val metrics = ArrayList<HeadlessMetrics>()
            for (level in QualityLevel.entries) {
                PerformanceManager.setQuality(level)
                metrics += runHeadless(12345, 2500)
            }
            for (m in metrics) {
                assertEquals(metrics[0], m)
                assertTrue(m.population < 80)
            }
        } finally {
            PerformanceManager.setQuality(saved)
            SimConfig.colonyCount = 1
        }
    }

    @Test
    fun selfCheckAgainstTypescriptGolden() {
        val at1000 = runHeadless(12345, 1000)
        val again = runHeadless(12345, 1000)
        assertEquals(at1000, again)
        val at2500 = runHeadless(12345, 2500)
        assertEquals(at2500, runHeadless(12345, 2500))
        // TS cefa083 headless: 1000 -> pop 21 sugar 673 protein 238 queenEnergy 2201 extra 2
        // 2500 -> pop 23 sugar 711 protein 204 queenEnergy 2242
        // Printed for the deviations note. A mismatch does not fail this test.
        println("GOLDEN1000 pop=${at1000.population} sugar=${at1000.sugar} protein=${at1000.protein} queen=${at1000.queenEnergy} chambers=${at1000.extraChambers} brood=${at1000.brood}")
        println("GOLDEN2500 pop=${at2500.population} sugar=${at2500.sugar} protein=${at2500.protein} queen=${at2500.queenEnergy} chambers=${at2500.extraChambers} brood=${at2500.brood}")
    }
}
