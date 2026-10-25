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
    fun bloomStaysInBounds() {
        assertTrue(OutdoorLighting.BLOOM_STRENGTH <= 0.35f)
        assertTrue(OutdoorLighting.BLOOM_THRESHOLD >= 1.2f)
        assertTrue(OutdoorLighting.BLOOM_RADIUS <= 2f)
        assertEquals(1f, OutdoorLighting.EXPOSURE)
    }

    private fun luma(c: de.fabmax.kool.util.Color) = 0.2126f * c.r + 0.7152f * c.g + 0.0722f * c.b

    @Test
    fun nightAmbientIsCoolButNotSaturated() {
        val n = OutdoorLighting.ambientColor(0f)
        assertTrue(n.b > n.r, "night ambient must be cool: $n")
        assertTrue(n.b / n.r <= 1.6f, "night ambient must not be saturated blue: $n")
        assertTrue(luma(n) in 0.20f..0.25f, "night ambient strength ${luma(n)}")
    }

    @Test
    fun dayAmbientIsNeutralAndBlendIsSmooth() {
        val d = OutdoorLighting.ambientColor(1f)
        assertTrue(d.r == d.g && d.g == d.b, "day keeps the warm gradient untinted: $d")
        assertTrue(luma(d) in 0.35f..0.45f)
        var last = -1f
        for (i in 0..100) {
            val c = OutdoorLighting.ambientColor(i / 100f)
            assertTrue(c.b / c.r >= 1f - 1e-4f, "never warmer than the gradient")
            assertTrue(luma(c) >= last - 1e-4f); last = luma(c)
        }
    }
}
