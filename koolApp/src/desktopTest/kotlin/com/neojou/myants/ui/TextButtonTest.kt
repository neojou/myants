package com.neojou.myants.ui

import de.fabmax.kool.util.Color
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** #87 B1/B2: states of the text button against the Artist table (ART_DIRECTION §3, "文字按鈕狀態"). */
class TextButtonTest {
    private val c = MyantsTheme.colors

    private fun v(
        selected: Boolean = false, enabled: Boolean = true, danger: Boolean = false,
        hovered: Boolean = false, pressed: Boolean = false,
    ) = textButtonVisual(selected, enabled, danger, hovered, pressed)

    private fun hex(color: Color): String {
        fun b(x: Float) = (x * 255f + 0.5f).toInt().coerceIn(0, 255)
        return "#%02X%02X%02X".format(b(color.r), b(color.g), b(color.b))
    }

    /** Within ±1 per channel of the spec hex. */
    private fun assertRgb(expected: String, actual: Color, what: String) {
        val e = Color(expected)
        for ((n, x, y) in listOf(Triple("r", e.r, actual.r), Triple("g", e.g, actual.g), Triple("b", e.b, actual.b))) {
            assertTrue(abs(x * 255f - y * 255f) <= 1.0f, "$what $n: want $expected got ${hex(actual)}")
        }
    }

    private fun bg(vis: TextButtonVisual): Color = assertNotNull(vis.background, "background")

    @Test
    fun outlineStrongTokenIsEeeeeeAlpha040() {
        assertEquals(Color("#EEEEEE"), c.outlineStrong.withAlpha(1f))
        assertEquals(0.40f, c.outlineStrong.a, 1e-4f)
    }

    @Test
    fun restHoverPressedBackgrounds() {
        assertRgb("#2A2730", bg(v()), "rest bg")
        assertRgb("#413D49", bg(v(hovered = true)), "hover bg")
        assertRgb("#504B59", bg(v(hovered = true, pressed = true)), "pressed bg")
    }

    @Test
    fun selectedBackgrounds() {
        assertRgb("#6750A4", bg(v(selected = true)), "selected bg")
        assertRgb("#6750A4", bg(v(selected = true, hovered = true)), "selected hover bg")
        assertRgb("#4F378B", bg(v(selected = true, hovered = true, pressed = true)), "selected pressed bg")
        assertEquals(c.primaryContainer, v(selected = true).foreground)
        assertEquals(c.primaryContainer, v(selected = true).border)
    }

    @Test
    fun dangerBackgrounds() {
        assertRgb("#601410", bg(v(danger = true)), "danger rest bg")
        assertRgb("#712C2D", bg(v(danger = true, hovered = true)), "danger hover bg")
        assertRgb("#7C3C40", bg(v(danger = true, hovered = true, pressed = true)), "danger pressed bg")
        assertEquals(c.onDanger, v(danger = true).foreground)
    }

    @Test
    fun restBorderIsOutlineStrongAndSameAcrossHoverPressed() {
        assertEquals(c.outlineStrong, v().border)
        assertEquals(c.outlineStrong, v(hovered = true).border)
        assertEquals(c.outlineStrong, v(hovered = true, pressed = true).border)
        assertRgb("#78777C", MyantsTheme.over(v().border, bg(v())), "rest border over bg")
    }

    @Test
    fun dangerBorderIsOnDangerAlpha055() {
        val b = v(danger = true).border
        assertEquals(0.55f, b.a, 1e-4f)
        assertRgb("#B06E6B", MyantsTheme.over(b, bg(v(danger = true))), "danger border over bg")
        assertEquals(b, v(danger = true, hovered = true).border)
    }

    @Test
    fun disabledHasNoFillDisabledTextAndFaintBorder() {
        val d = v(enabled = false, hovered = true, pressed = true)
        assertNull(d.background)
        assertEquals(c.onSurfaceDisabled, d.foreground)
        assertEquals(c.outline, d.border)
        assertEquals(false, d.clickable)
        assertEquals(true, v().clickable)
    }

    private fun ratio(vis: TextButtonVisual) = MyantsTheme.contrast(vis.foreground, bg(vis))

    @Test
    fun textContrastMeetsArtistFloors() {
        val rest = ratio(v())
        val hover = ratio(v(hovered = true))
        val pressed = ratio(v(hovered = true, pressed = true))
        val sel = ratio(v(selected = true))
        val selPressed = ratio(v(selected = true, pressed = true))
        val dRest = ratio(v(danger = true))
        val dHover = ratio(v(danger = true, hovered = true))
        val dPressed = ratio(v(danger = true, hovered = true, pressed = true))
        println("TEXTBUTTON CONTRAST rest=%.2f hover=%.2f pressed=%.2f selected=%.2f selPressed=%.2f danger=%.2f/%.2f/%.2f"
            .format(rest, hover, pressed, sel, selPressed, dRest, dHover, dPressed))
        assertTrue(rest >= 12.0, "rest $rest")
        assertTrue(hover >= 9.0, "hover $hover")
        assertTrue(pressed >= 7.0, "pressed $pressed")
        assertTrue(sel >= 4.5, "selected $sel")
        assertTrue(selPressed >= 4.5, "selected pressed $selPressed")
        assertTrue(dRest >= 4.5 && dHover >= 4.5 && dPressed >= 4.5, "danger $dRest/$dHover/$dPressed")
    }

    @Test
    fun borderAgainstOwnFillAtLeast3() {
        val rest = MyantsTheme.contrast(MyantsTheme.over(v().border, bg(v())), bg(v()))
        val danger = MyantsTheme.contrast(MyantsTheme.over(v(danger = true).border, bg(v(danger = true))), bg(v(danger = true)))
        println("TEXTBUTTON BORDER/OWN FILL rest=%.2f danger=%.2f".format(rest, danger))
        assertTrue(rest >= 3.0, "rest $rest")
        assertTrue(danger >= 3.0, "danger $danger")
    }

    @Test
    fun pressScaleSharedWithIconButton() {
        assertEquals(0.96f, iconPressScale(pressed = true, enabled = true))
    }
}
