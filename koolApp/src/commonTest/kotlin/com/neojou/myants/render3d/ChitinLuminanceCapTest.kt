package com.neojou.myants.render3d

import de.fabmax.kool.util.Color
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** #86: Copper is true copper, Black is the tuned black shell, any other swatch is capped at linear Y ≤ CHITIN_Y_MAX. */
class ChitinLuminanceCapTest {
    private val swatches = listOf("#c4703a", "#222222", "#d9bf52", "#88c0a0", "#6fa8dc")

    /** 64 seeded random hex colours, the same every run. */
    private val randomHex: List<String> = Random(86).let { rnd ->
        List(64) { "#" + rnd.nextInt(0, 0x1000000).toString(16).padStart(6, '0') }
    }

    private fun lum(c: Color) = 0.2126f * c.r + 0.7152f * c.g + 0.0722f * c.b

    /** The #94 formula before the cap. */
    private fun scaled(c: Color): Color {
        val l = c.toLinear()
        val s = WorldPalette.CHITIN_ALBEDO_SCALE
        return Color(l.r * s, l.g * s, l.b * s, l.a)
    }

    private fun isSpecial(c: Color) = c == WorldPalette.worker || c == WorldPalette.blackSwatch

    @Test
    fun capIsArtistValue() {
        assertEquals(0.065f, WorldPalette.CHITIN_Y_MAX)
        assertEquals(Color(0.165f, 0.040f, 0.002f, 1f), WorldPalette.copperChitin)
        assertTrue(lum(WorldPalette.copperChitin) <= WorldPalette.CHITIN_Y_MAX)
    }

    @Test
    fun copperUsesCopperChitinAndBlackUsesAntChitin() {
        assertEquals(WorldPalette.copperChitin, WorldPalette.outdoorChitin(WorldPalette.worker))
        assertEquals(WorldPalette.copperChitin, WorldPalette.outdoorChitin(Color("#c4703a")))
        assertEquals(WorldPalette.antChitin, WorldPalette.outdoorChitin(Color("#222222")))
    }

    @Test
    fun everySwatchAndRandomHexStaysUnderCap() {
        for (hex in swatches + randomHex) {
            val y = lum(WorldPalette.outdoorChitin(Color(hex)))
            assertTrue(y <= WorldPalette.CHITIN_Y_MAX + 1e-4f, "$hex Y=$y")
        }
    }

    @Test
    fun clampedColoursKeepChannelRatios() {
        var clamped = 0
        for (hex in swatches + randomHex) {
            val c = Color(hex)
            if (isSpecial(c)) continue
            val raw = scaled(c)
            if (lum(raw) <= WorldPalette.CHITIN_Y_MAX) continue
            clamped++
            val got = WorldPalette.outdoorChitin(c)
            val k = WorldPalette.CHITIN_Y_MAX / lum(raw)
            assertTrue(abs(got.r - raw.r * k) < 1e-4f && abs(got.g - raw.g * k) < 1e-4f && abs(got.b - raw.b * k) < 1e-4f, "$hex $got vs $raw×$k")
            // same channel ratios as the unclamped colour
            val m = maxOf(raw.r, raw.g, raw.b)
            val gm = maxOf(got.r, got.g, got.b)
            for ((a, b) in listOf(raw.r to got.r, raw.g to got.g, raw.b to got.b)) {
                assertTrue(abs(a / m - b / gm) < 1e-4f, "$hex ratio")
            }
            assertTrue(abs(lum(got) - WorldPalette.CHITIN_Y_MAX) < 1e-4f, "$hex lands on the cap")
        }
        assertTrue(clamped >= 10, "random set should exercise the clamp, got $clamped")
    }

    @Test
    fun darkColoursMatchTheUnclampedFormula() {
        var dark = 0
        for (hex in swatches + randomHex) {
            val c = Color(hex)
            if (isSpecial(c)) continue
            val raw = scaled(c)
            if (lum(raw) > WorldPalette.CHITIN_Y_MAX) continue
            dark++
            assertEquals(raw, WorldPalette.outdoorChitin(c), hex)
        }
        assertTrue(dark >= 5, "random set should include dark colours, got $dark")
    }

    @Test
    fun alphaIsKept() {
        val c = Color(0.9f, 0.8f, 0.3f, 0.5f)
        assertEquals(0.5f, WorldPalette.outdoorChitin(c).a)
        assertTrue(lum(WorldPalette.outdoorChitin(c)) <= WorldPalette.CHITIN_Y_MAX + 1e-4f)
    }
}
