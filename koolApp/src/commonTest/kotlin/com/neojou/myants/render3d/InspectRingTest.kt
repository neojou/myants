package com.neojou.myants.render3d

import com.neojou.myants.sim.BroodStage
import com.neojou.myants.sim.InsectKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Ring scale rules owned by render3d. Dispatch from InspectTarget is tested in session/InspectRingMappingTest. */
class InspectRingTest {
    @Test
    fun ringDefaultsToTheWorld() {
        assertEquals(RingSpace.WORLD, InspectRing(1.0, 2.0, 3f).space)
    }

    @Test
    fun antRingsScaleWithSize() {
        assertEquals(12.5f, worldAntRingScale(1.25))
        assertEquals(15f, nestAntRingScale(1.5))
    }

    @Test
    fun foodRingSitsOutsideThePile() {
        assertEquals(6f, foodPileRadius(1.0))
        assertEquals(6f * 1.35f, foodRingScale(1.0))
        for (amount in listOf(1.0, 50.0, 400.0)) {
            assertTrue(foodRingScale(amount) > foodPileRadius(amount))
        }
    }

    @Test
    fun insectsUseTheAntLengthRatio() {
        for (kind in InsectKind.entries) {
            assertEquals(insectSpan(kind) * 10f / 12f, insectRingScale(kind))
        }
        assertEquals(8f * 10f / 12f, insectRingScale(InsectKind.APHID))
    }

    @Test
    fun nestAndTerrainScales() {
        assertEquals(3.2f, broodRingScale(BroodStage.EGG))
        assertEquals(5f, broodRingScale(BroodStage.LARVA))
        assertEquals(30f * 1.05f, chamberRingScale(30.0))
        assertEquals(25f * 1.5f, stoneRingScale(25.0))
        assertEquals(3.2f * 2f, grassRingScale(3.2))
    }
}
