package com.neojou.myants.render3d

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Artist targets for outdoor light (#68). */
class OutdoorLightingTest {
    @Test
    fun noonSunIsWarmAndInRange() {
        val c = OutdoorLighting.lightColor(1f)
        assertTrue(c.r >= c.g && c.g >= c.b, "sun must not be bluer than ~5000 K: $c")
        assertTrue(c.b <= 0.82f + 1e-4f)
        val noon = OutdoorLighting.lightIntensity(1f)
        assertTrue(noon in 3.0f..3.4f, "noon intensity $noon")
    }

    @Test
    fun dawnSunIsAtLeastOne() {
        for (i in 20..100) {
            val b = i / 100f
            assertTrue(OutdoorLighting.lightIntensity(b) >= 1f, "bright=$b")
        }
    }

    @Test
    fun sunElevationStaysBetweenTenAndSixty() {
        assertEquals(10f, OutdoorLighting.sunElevation(0f), 1e-4f)
        val noon = OutdoorLighting.sunElevation(1f)
        assertTrue(noon in 55f..60f, "noon elevation $noon")
    }

    @Test
    fun moonIsCoolButNotSaturatedBlue() {
        val c = OutdoorLighting.lightColor(0f)
        assertTrue(c.b > c.r, "moon is cool")
        assertTrue(c.b / c.r <= 1.3f, "moon blue/red ${c.b / c.r}")
        val i = OutdoorLighting.lightIntensity(0f)
        assertTrue(i in 0.6f..0.8f, "moon intensity $i")
    }

    @Test
    fun ambientStrengthFollowsDay() {
        val night = OutdoorLighting.ambientStrength(0f)
        val noon = OutdoorLighting.ambientStrength(1f)
        assertTrue(night in 0.20f..0.25f, "night ambient $night")
        assertTrue(noon in 0.35f..0.45f, "day ambient $noon")
        var last = 0f
        for (i in 0..100) {
            val k = OutdoorLighting.ambientStrength(i / 100f)
            assertTrue(k >= last, "ambient must not drop as day brightens")
            last = k
        }
    }

    @Test
    fun ambientGradientIsNotBlue() {
        for (c in listOf(OutdoorLighting.ambientZenith, OutdoorLighting.ambientHorizon, OutdoorLighting.ambientGround)) {
            assertTrue(c.b / c.r <= 1.10f, "B/R ${c.b / c.r} for $c")
        }
    }

    @Test
    fun bloomAndDigitsStayInBounds() {
        assertTrue(OutdoorLighting.BLOOM_STRENGTH <= 0.35f)
        assertTrue(OutdoorLighting.BLOOM_THRESHOLD >= 1.2f)
        assertTrue(OutdoorLighting.BLOOM_RADIUS <= 2f)
        assertEquals(1f, OutdoorLighting.EXPOSURE)
        assertTrue(OutdoorLighting.DIGIT_EMISSION <= 0.3f)
        // Brightest digit channel times the glow stays below the bloom threshold.
        val d = WorldPalette.digit3d
        assertTrue(maxOf(d.r, d.g, d.b) * OutdoorLighting.DIGIT_EMISSION < OutdoorLighting.BLOOM_THRESHOLD)
    }
}
