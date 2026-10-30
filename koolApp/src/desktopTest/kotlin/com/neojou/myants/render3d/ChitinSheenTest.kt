package com.neojou.myants.render3d

import com.neojou.myants.render3d.actors.critterTint
import com.neojou.myants.sim.InsectKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** #79: the top-down specular peak must not bury dark chitin (ants read pale grey at roughness 0.42). */
class ChitinSheenTest {
    @Test
    fun oldRoughnessPeakOutshinesBlackChitin() {
        // Root cause guard: the old value's peak is several times the chitin albedo.
        assertTrue(WorldPalette.topDownSheen(0.42f) > 4f * WorldPalette.antChitin.r)
    }

    @Test
    fun chitinRoughnessKeepsSheenBelowAlbedo() {
        val sheen = WorldPalette.topDownSheen(WorldPalette.CHITIN_ROUGHNESS)
        assertTrue(sheen < WorldPalette.antChitin.r, "sheen $sheen")
        assertTrue(sheen < 0.08f, "dark chitin albedo must stay visible, sheen $sheen")
    }

    @Test
    fun sheenFallsAsRoughnessRises() {
        assertTrue(WorldPalette.topDownSheen(0.9f) < WorldPalette.topDownSheen(0.5f))
    }

    @Test
    fun ladybugSpotsUseThePaletteAndStayDarkerThanBefore() {
        assertEquals(WorldPalette.ladybugSpot, critterTint(InsectKind.LADYBUG).mark)
        val spot = WorldPalette.ladybugSpot
        assertTrue(maxOf(spot.r, spot.g, spot.b) <= 0.04f, "spot $spot")
    }
}
