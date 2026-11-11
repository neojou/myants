package com.neojou.myants.render3d

import com.neojou.myants.sim.InsectKind
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Pins the shared rim from the Artist spec on #84 (day only; night is #91). */
class RimLightTest {
    @Test
    fun dayParametersArePinned() {
        assertEquals(0.10f, OutdoorLighting.RIM_DAY)
        assertEquals(4.0f, OutdoorLighting.RIM_EXP)
        assertTrue(OutdoorLighting.RIM_DAY in 0.06f..0.15f)
        assertTrue(OutdoorLighting.RIM_EXP in 3f..5f)
    }

    @Test
    fun dayRimIsWarmSunColourAtFullStrength() {
        assertEquals(OutdoorLighting.sunColor, OutdoorLighting.rimColor(1f))
        assertEquals(OutdoorLighting.RIM_DAY, OutdoorLighting.rimStrength(1f))
        assertEquals(OutdoorLighting.RIM_EXP, OutdoorLighting.rimExp(1f))
    }

    @Test
    fun nightRimStaysAtNightValueUntil91() {
        assertEquals(OutdoorLighting.RIM_NIGHT, OutdoorLighting.rimStrength(0.1f))
        assertEquals(0f, OutdoorLighting.RIM_NIGHT)
    }

    @Test
    fun rimOnlyAtSilhouetteAndBelowBloom() {
        // Facing the camera: no rim at all, so no frontal highlight.
        assertEquals(0f, OutdoorLighting.rimFactor(1f, 1f))
        // Grazing edge is the brightest point; still far below bloom.
        val peak = OutdoorLighting.rimFactor(1f, 0f)
        assertTrue(abs(peak - OutdoorLighting.RIM_DAY) < 1e-6f)
        val c = OutdoorLighting.sunColor
        assertTrue(maxOf(c.r, c.g, c.b) * peak < OutdoorLighting.BLOOM_THRESHOLD)
    }

    @Test
    fun noSideBaseSoBodyInteriorStaysUnlit() {
        // Plan C (§6.8): no side(N, L) floor. At N·V = 0.7 (body interior seen at an angle)
        // the old 0.35 base still added ≥0.35 × k × 0.3^4; the new term must stay < 1 % of k.
        assertTrue(OutdoorLighting.rimFactor(1f, 0.7f) < 0.01f * OutdoorLighting.RIM_DAY)
        // Strictly rises towards the silhouette (monotonic edge gain).
        var prev = OutdoorLighting.rimFactor(1f, 1f)
        for (i in 9 downTo 0) {
            val f = OutdoorLighting.rimFactor(1f, i / 10f)
            assertTrue(f > prev)
            prev = f
        }
    }

    @Test
    fun rimIsOffAtNightUntil91() {
        for (ndv in listOf(0f, 0.3f, 1f)) assertEquals(0f, OutdoorLighting.rimFactor(0.1f, ndv))
    }

    @Test
    fun lightDirectionPointsDown() {
        val d = OutdoorLighting.lightDirection(1f, 0.5)
        assertTrue(d.y < 0f)
        assertTrue(abs(d.length() - 1f) < 1e-5f)
    }

    @Test
    fun rimPartsMatchArtistTable() {
        assertTrue(RimParts.ant(RimParts.AntPart.BODY))
        assertTrue(RimParts.ant(RimParts.AntPart.HEAD))
        assertFalse(RimParts.ant(RimParts.AntPart.JAW))
        assertFalse(RimParts.ant(RimParts.AntPart.LEG))
        assertFalse(RimParts.ant(RimParts.AntPart.EYE))
        assertTrue(RimParts.critterBody(InsectKind.BEETLE))
        assertFalse(RimParts.critterBody(InsectKind.LADYBUG))
        for (k in InsectKind.entries) if (k != InsectKind.BEETLE) assertFalse(RimParts.critterBody(k))
    }
}
