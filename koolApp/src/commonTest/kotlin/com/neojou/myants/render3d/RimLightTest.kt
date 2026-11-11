package com.neojou.myants.render3d

import com.neojou.myants.sim.InsectKind
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pins the shared rim: day from the Artist spec on #84, night (moon rim) from #91. */
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
    fun nightParametersArePinned() {
        // #91 Artist spec: RIM_NIGHT 0.30–0.60 (start 0.45), p 2.0–3.0 (start 2.5), ladybug 0.6× the ant.
        assertEquals(0.45f, OutdoorLighting.RIM_NIGHT)
        assertEquals(2.5f, OutdoorLighting.RIM_EXP_NIGHT)
        assertEquals(0.6f, OutdoorLighting.RIM_LADYBUG_NIGHT_SCALE)
        assertTrue(OutdoorLighting.RIM_NIGHT in 0.30f..0.60f)
        assertTrue(OutdoorLighting.RIM_EXP_NIGHT in 2f..3f)
    }

    @Test
    fun nightRimIsCoolMoonColourAtNightStrength() {
        for (b in listOf(0f, 0.1f, 0.19f)) {
            assertEquals(OutdoorLighting.moonColor, OutdoorLighting.rimColor(b))
            assertEquals(OutdoorLighting.RIM_NIGHT, OutdoorLighting.rimStrength(b))
            assertEquals(OutdoorLighting.RIM_EXP_NIGHT, OutdoorLighting.rimExp(b))
        }
        // Cool, not saturated blue: B ≥ R (N4 direction).
        val c = OutdoorLighting.rimColor(0f)
        assertTrue(c.b >= c.r)
    }

    @Test
    fun dawnDuskInterpolatesWithAmbientT() {
        // Same t as ambientStrength: halfway between NIGHT_BELOW and 1.
        val mid = OutdoorLighting.NIGHT_BELOW + 0.5f * (1f - OutdoorLighting.NIGHT_BELOW)
        val k = OutdoorLighting.rimStrength(mid)
        val p = OutdoorLighting.rimExp(mid)
        assertTrue(abs(k - (OutdoorLighting.RIM_NIGHT + OutdoorLighting.RIM_DAY) / 2f) < 1e-5f)
        assertTrue(abs(p - (OutdoorLighting.RIM_EXP_NIGHT + OutdoorLighting.RIM_EXP) / 2f) < 1e-5f)
        // Continuous at the night edge.
        assertTrue(abs(OutdoorLighting.rimStrength(OutdoorLighting.NIGHT_BELOW) - OutdoorLighting.RIM_NIGHT) < 1e-6f)
    }

    @Test
    fun rimOnlyAtSilhouetteAndBelowBloom() {
        for (b in listOf(0f, 1f)) {
            // Facing the camera: no rim at all, so no frontal highlight (day or night).
            assertEquals(0f, OutdoorLighting.rimFactor(b, 1f))
            // Grazing edge is the brightest point; still far below bloom.
            val peak = OutdoorLighting.rimFactor(b, 0f)
            assertTrue(abs(peak - OutdoorLighting.rimStrength(b)) < 1e-6f)
            val c = OutdoorLighting.rimColor(b)
            assertTrue(maxOf(c.r, c.g, c.b) * peak < OutdoorLighting.BLOOM_THRESHOLD)
        }
    }

    @Test
    fun noSideBaseSoBodyInteriorStaysUnlit() {
        // Plan C (§6.8): no side(N, L) floor. At N·V = 0.7 (body interior seen at an angle)
        // the old 0.35 base still added ≥0.35 × k × 0.3^4; the new term must stay < 1 % of k.
        assertTrue(OutdoorLighting.rimFactor(1f, 0.7f) < 0.01f * OutdoorLighting.RIM_DAY)
        // Strictly rises towards the silhouette (monotonic edge gain), day and night.
        for (b in listOf(0f, 1f)) {
            var prev = OutdoorLighting.rimFactor(b, 1f)
            for (i in 9 downTo 0) {
                val f = OutdoorLighting.rimFactor(b, i / 10f)
                assertTrue(f > prev)
                prev = f
            }
        }
    }

    @Test
    fun ladybugGetsSixTenthsAtNightAndNothingByDay() {
        val lb = OutdoorLighting.RimProfile.LADYBUG
        assertTrue(abs(OutdoorLighting.rimStrength(0f, lb) - 0.6f * OutdoorLighting.RIM_NIGHT) < 1e-6f)
        // Noon ladybug unchanged (D2, shell/gnd): the #84 day rim never touched it.
        assertEquals(0f, OutdoorLighting.rimStrength(1f, lb))
        for (ndv in listOf(0f, 0.3f, 1f)) assertEquals(0f, OutdoorLighting.rimFactor(1f, ndv, lb))
        // Fades out over the same dawn t.
        val mid = OutdoorLighting.NIGHT_BELOW + 0.5f * (1f - OutdoorLighting.NIGHT_BELOW)
        assertTrue(abs(OutdoorLighting.rimStrength(mid, lb) - 0.3f * OutdoorLighting.RIM_NIGHT) < 1e-5f)
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
        assertTrue(!RimParts.ant(RimParts.AntPart.JAW))
        assertTrue(!RimParts.ant(RimParts.AntPart.LEG))
        assertTrue(!RimParts.ant(RimParts.AntPart.EYE))
        assertEquals(OutdoorLighting.RimProfile.CHITIN, RimParts.critter(InsectKind.BEETLE))
        assertEquals(OutdoorLighting.RimProfile.LADYBUG, RimParts.critter(InsectKind.LADYBUG))
        for (k in InsectKind.entries) if (k != InsectKind.BEETLE && k != InsectKind.LADYBUG) assertNull(RimParts.critter(k))
    }
}
