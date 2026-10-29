package com.neojou.myants.render3d

import de.fabmax.kool.util.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** #75: clover and soil colours follow the Artist's target hue / saturation / value. */
class CloverPaletteTest {
    private fun hsv(c: Color): Triple<Double, Double, Double> {
        val r = c.r.toDouble(); val g = c.g.toDouble(); val b = c.b.toDouble()
        val mx = max(r, max(g, b)); val mn = min(r, min(g, b)); val d = mx - mn
        val h = when {
            d == 0.0 -> 0.0
            mx == r -> 60.0 * (((g - b) / d) % 6.0)
            mx == g -> 60.0 * ((b - r) / d + 2.0)
            else -> 60.0 * ((r - g) / d + 4.0)
        }.let { if (it < 0) it + 360.0 else it }
        return Triple(h, if (mx == 0.0) 0.0 else d / mx, mx)
    }

    private fun near(want: Triple<Double, Double, Double>, c: Color, name: String) {
        val got = hsv(c)
        assertTrue(kotlin.math.abs(got.first - want.first) <= 2.0, "$name hue $got")
        assertTrue(kotlin.math.abs(got.second - want.second) <= 0.02, "$name sat $got")
        assertTrue(kotlin.math.abs(got.third - want.third) <= 0.02, "$name val $got")
    }

    @Test
    fun petalsLeafAndStemMatchArtistTargets() {
        near(Triple(344.0, 0.77, 0.95), WorldPalette.petalRose, "rose")
        near(Triple(41.0, 0.94, 1.00), WorldPalette.petalGold, "gold")
        near(Triple(234.0, 0.60, 1.00), WorldPalette.petalLilac, "lilac")
        near(Triple(131.0, 0.66, 0.32), WorldPalette.simGrass, "leaf")
        near(Triple(100.0, 0.63, 0.38), WorldPalette.stem, "stem")
    }

    @Test
    fun scatterGrassStaysInLeafFamily() {
        for (s in listOf(0f, 0.5f, 1f)) {
            val (h, _, v) = hsv(WorldPalette.scatterGrass(s))
            assertTrue(h in 120.0..140.0, "scatter hue $h")
            assertTrue(kotlin.math.abs(v - 0.32) <= 0.04 + 1e-6, "scatter value $v")
        }
    }

    @Test
    fun soilStaysWarmBrownWithLowerSaturation() {
        for (n in listOf(0f, 0.5f, 1f)) {
            val (h, s, v) = hsv(WorldPalette.soil(n))
            assertTrue(h in 28.0..34.0, "soil hue $h")
            assertTrue(s in 0.45..0.55, "soil sat $s")
            assertTrue(v in 0.34..0.46, "soil value $v")
        }
    }

    @Test
    fun roughnessTargets() {
        assertEquals(0.85f, WorldPalette.PETAL_ROUGHNESS)
        assertEquals(0.70f, WorldPalette.LEAF_ROUGHNESS)
    }
}
