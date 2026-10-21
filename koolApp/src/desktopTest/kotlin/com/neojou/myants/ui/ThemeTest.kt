package com.neojou.myants.ui

import de.fabmax.kool.util.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThemeTest {
    private val c = MyantsTheme.colors
    private val brightSky = Color("#C8D8E8")

    @Test
    fun surfaceTokensMatchSpec() {
        assertEquals(0.72f, c.surface.a, 1e-4f)
        assertEquals(0.86f, c.surfaceRaised.a, 1e-4f)
        assertEquals(Color("#6750A4"), c.primary)
        assertEquals(Color("#EEEEEE"), c.onSurface)
        assertEquals(Color("#BDBDBD"), c.onSurfaceVar)
    }

    @Test
    fun dataColorsUnchanged() {
        assertEquals(listOf("#c4703a", "#222222", "#d9bf52", "#88c0a0", "#6fa8dc"), MyantsTheme.Data.antColors)
        val choices = MyantsTheme.Data.antColorChoices
        assertEquals(MyantsTheme.Data.antColors, choices.map { it.hex })
        assertEquals(listOf("Kupfer", "Schwarz", "Gold", "Salbei", "Blau"), choices.map { it.name })
        assertEquals(choices.size, choices.map { it.name }.toSet().size)
        val rival = MyantsTheme.Data.rivalColonyLabel
        assertTrue("2" in rival && "Gold" in rival && "#d9bf52" in rival)
    }

    /**
     * PR #16 reported 6.63:1 for onSurfaceVar on a panel. Artist's script reported 7.44:1.
     * Both use [MyantsTheme.over] (straight alpha in encoded sRGB) and then WCAG [MyantsTheme.contrast].
     * 6.63 is #BDBDBD on surfaceRaised #1C1822 at α 0.86, over day sky #C8D8E8.
     * 7.44 is the same text on surface #120F17 at α 0.86, over the same sky. That card is not a token.
     * Both are above 4.5. The colors stay as they are.
     */
    @Test
    fun contrastOnBrightestDaySky() {
        val hud = MyantsTheme.over(c.surface, brightSky)
        val panel = MyantsTheme.over(c.surfaceRaised, brightSky)
        val main = MyantsTheme.contrast(c.onSurface, hud)
        val varHud = MyantsTheme.contrast(c.onSurfaceVar, hud)
        val varPanel = MyantsTheme.contrast(c.onSurfaceVar, panel)
        println("CONTRAST onSurface/hud=%.2f onSurfaceVar/hud=%.2f onSurfaceVar/panel=%.2f".format(main, varHud, varPanel))
        assertTrue(main >= 4.5, "onSurface on HUD card: $main")
        assertTrue(varPanel >= 4.5, "onSurfaceVar on panel: $varPanel")
        assertTrue(varHud >= 4.5, "onSurfaceVar on HUD card: $varHud")
        assertEquals(6.63, varPanel, 0.02)
        val artistCard = MyantsTheme.over(Color("#120F17").withAlpha(0.86f), brightSky)
        val artist = MyantsTheme.contrast(c.onSurfaceVar, artistCard)
        assertEquals(7.44, artist, 0.02)
    }

    @Test
    fun spacingScale() {
        assertEquals(4f, MyantsTheme.space.s1.value)
        assertEquals(12f, MyantsTheme.space.screenEdge.value)
        assertEquals(10f, MyantsTheme.radius.lg.value)
    }

    @Test
    fun selectedOutlineIsVisibleOnHudAndPrimary() {
        assertEquals(1f, MyantsTheme.stroke.selected.value)
        val day = MyantsTheme.over(c.surface, brightSky)
        assertTrue(MyantsTheme.contrast(c.primaryContainer, day) >= 4.5)
        assertTrue(MyantsTheme.contrast(c.primaryContainer, c.primary) >= 4.5)
    }
}
