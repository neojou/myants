package com.neojou.myants.render3d

import com.neojou.myants.render3d.quality.QualityPreset
import com.neojou.myants.sim.QualityLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScatterLayoutTest {
    @Test
    fun leavesRepeatForTheSameSeedAndChangeWithSerial() {
        val low = QualityPreset.from(QualityLevel.LOW, 0)
        val first = ScatterLayout.leafSpots(7, 1, low, 1f)
        val again = ScatterLayout.leafSpots(7, 1, low, 1f)
        assertEquals(100, first.size)
        assertEquals(first, again)
        val shifted = ScatterLayout.leafSpots(7, 2, low, 1f)
        assertTrue(first != shifted)
        val none = ScatterLayout.leafSpots(7, 1, QualityPreset.from(QualityLevel.ULTRA_LOW, 0), 1f)
        assertTrue(none.isEmpty())
    }
}
