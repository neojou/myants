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
        assertEquals(0.62f, c.surface.a, 1e-4f)
        assertEquals(0.86f, c.surfaceRaised.a, 1e-4f)
        assertEquals(Color("#6750A4"), c.primary)
        assertEquals(Color("#EEEEEE"), c.onSurface)
    }

    @Test
    fun dataColorsUnchanged() {
        assertEquals(listOf("#c4703a", "#222222", "#d9bf52", "#88c0a0", "#6fa8dc"), MyantsTheme.Data.antColors)
    }

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
        assertTrue(varHud >= 2.5, "onSurfaceVar on HUD card: $varHud")
    }

    @Test
    fun spacingScale() {
        assertEquals(4f, MyantsTheme.space.s1.value)
        assertEquals(12f, MyantsTheme.space.screenEdge.value)
        assertEquals(10f, MyantsTheme.radius.lg.value)
    }
}
