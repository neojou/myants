package com.neojou.myants.ui

import com.neojou.myants.input.NestRect
import com.neojou.myants.render3d.InspectRing
import com.neojou.myants.render3d.RingSpace
import com.neojou.myants.render3d.WorldPalette
import com.neojou.myants.render3d.selectRingBands
import com.neojou.myants.session.InspectTarget
import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.util.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InspectCardTest {
    private val c = MyantsTheme.colors

    @Test
    fun followButtonUsesIconButtonStateColors() {
        val rest = followButtonVisual(following = false, hovered = false)
        assertEquals(c.surfaceVariant, rest.background)
        assertNull(rest.border)
        val hover = followButtonVisual(following = false, hovered = true)
        assertTrue(hover.background != rest.background, "hover tints the rest color")
        assertNull(hover.border)
        val on = followButtonVisual(following = true, hovered = true)
        assertEquals(c.primary, on.background)
        assertEquals(c.primaryContainer, on.border)
    }

    @Test
    fun selectionRingIsTwoToneOuterLight() {
        val bands = selectRingBands()
        assertEquals(2, bands.size)
        val inner = bands.minBy { it.inner }
        val outer = bands.maxBy { it.outer }
        assertEquals(Color("#6750A4"), inner.color)
        assertEquals(Color("#EADDFF"), outer.color)
        assertEquals(WorldPalette.selectRingOuter, outer.color)
        assertEquals(1f, outer.outer)
        assertEquals(inner.outer, outer.inner, "bands touch")
    }

    @Test
    fun panelKeepsAtMostSixRows() {
        val rows = (1..9).map { InspectRow("l$it", "$it") }
        assertEquals(InspectPanel.MAX_ROWS, InspectPanel.of("t", rows).rows.size)
        assertEquals(6, InspectPanel.MAX_ROWS)
    }

    @Test
    fun cardSitsRightOfSelectionAndFlipsAtTheEdge() {
        val fallback = Vec2f(12f, 120f)
        val right = placeInspectCard(Vec2f(100f, 200f), 150f, 80f, 1000f, 800f, fallback)
        assertEquals(Vec2f(100f + InspectCardSpec.OFFSET_PX, 160f), right)
        val flipped = placeInspectCard(Vec2f(950f, 200f), 150f, 80f, 1000f, 800f, fallback)
        assertEquals(950f - InspectCardSpec.OFFSET_PX - 150f, flipped.x)
        val clamped = placeInspectCard(Vec2f(100f, 5f), 150f, 80f, 1000f, 800f, fallback)
        assertEquals(0f, clamped.y)
        assertEquals(fallback, placeInspectCard(null, 150f, 80f, 1000f, 800f, fallback))
    }

    @Test
    fun anchorUsesNestFrameOnlyWhenTheCutawayIsShown() {
        val ring = InspectRing(10.0, 20.0, 1f, RingSpace.NEST)
        val nest = NestRect(500f, 40f, 2f)
        assertEquals(Vec2f(520f, 80f), inspectAnchor(ring, nest3d = true, toScreen = null, nest = nest))
        assertNull(inspectAnchor(ring, nest3d = false, toScreen = null, nest = nest))
        val world = InspectRing(3.0, 4.0, 1f)
        assertEquals(Vec2f(30f, 40f), inspectAnchor(world, false, { x, y -> Vec2f(x * 10f, y * 10f) }, nest))
    }

    @Test
    fun cardLabelsAreTranslatedNotDebugKeys() {
        val en = InspectCopy.queenPanel(5, 7, UiLanguage.EN)
        assertEquals(listOf(InspectRow("Energy", "5"), InspectRow("Age", "7")), en.rows)
        val de = InspectCopy.stonePanel(4, clock = false, lang = UiLanguage.DE)
        assertEquals(InspectRow("Radius", "4"), de.rows.single())
        assertEquals("Stein", de.title)
    }

    @Test
    fun pheromoneAndColonylessStripesUseOnSurfaceVar() {
        val sample = InspectTarget.PheromoneSample(0.0, 0.0, emptyList())
        assertEquals(c.onSurfaceVar, inspectAccent(sample, Color.WHITE))
        assertTrue(c.onSurfaceVar != c.primary)
    }
}
