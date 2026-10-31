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
        assertTrue(maxOf(spot.r, spot.g, spot.b) <= 0.015f, "spot $spot")
    }

    @Test
    fun beetleBodyAndMarksAreMatteLadybugShellStaysGlossy() {
        // #81: same GGX peak as #79; the dark beetle body must not be buried by sheen.
        assertEquals(WorldPalette.CHITIN_ROUGHNESS, WorldPalette.critterBodyRoughness(InsectKind.BEETLE))
        assertEquals(WorldPalette.CHITIN_ROUGHNESS, WorldPalette.critterMarkRoughness(InsectKind.BEETLE))
        assertTrue(WorldPalette.topDownSheen(WorldPalette.critterBodyRoughness(InsectKind.BEETLE)) < critterTint(InsectKind.BEETLE).body.r)
        assertTrue(WorldPalette.topDownSheen(0.55f) > critterTint(InsectKind.BEETLE).body.r, "old 0.55 peak buried the body")
        assertEquals(0.55f, WorldPalette.critterBodyRoughness(InsectKind.LADYBUG))
    }

    @Test
    fun beetleMarkIsRedBrownAtUnchangedLuminance() {
        // #89 Artist review: separate the elytra mark from the body by hue/chroma, not by brightness.
        val m = critterTint(InsectKind.BEETLE).mark
        val y = 0.2126f * m.r + 0.7152f * m.g + 0.0722f * m.b
        assertTrue(kotlin.math.abs(y - 0.0810f) < 0.002f, "mark luminance $y")
        assertTrue(m.r > 8f * m.b, "mark must be saturated red-brown: $m")
        assertTrue(m.r / m.g > 2f * (critterTint(InsectKind.BEETLE).body.r / critterTint(InsectKind.BEETLE).body.g))
    }
}
