package com.neojou.myants.sim

import com.neojou.myants.parseLaunch
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LockQualityTest {
    @AfterTest
    fun reset() {
        PerformanceManager.lockQuality = false
        PerformanceManager.setQuality(QualityLevel.MEDIUM)
    }

    private fun lowFps(startMs: Long) {
        // 200 frames at 5 fps, 20 s past the last downgrade window.
        for (i in 0 until 200) PerformanceManager.monitorFps(5.0, startMs + i * 200L)
    }

    @Test
    fun defaultStillDowngradesOnLowFps() {
        PerformanceManager.setQuality(QualityLevel.MEDIUM)
        lowFps(1_000_000)
        assertEquals(QualityLevel.LOW, PerformanceManager.level)
    }

    @Test
    fun lockedQualityStaysMedium() {
        PerformanceManager.setQuality(QualityLevel.MEDIUM)
        PerformanceManager.lockQuality = true
        lowFps(2_000_000)
        assertEquals(QualityLevel.MEDIUM, PerformanceManager.level)
    }

    @Test
    fun launchParsesLockQuality() {
        assertFalse(parseLaunch(emptyList()).lockQuality)
        assertTrue(parseLaunch(listOf("lockQuality=1")).lockQuality)
        assertTrue(parseLaunch(listOf("--lock-quality=true")).lockQuality)
        assertFalse(parseLaunch(listOf("lockQuality=0")).lockQuality)
    }
}
