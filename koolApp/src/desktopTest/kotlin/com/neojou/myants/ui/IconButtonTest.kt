package com.neojou.myants.ui

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IconButtonTest {
    private val c = MyantsTheme.colors
    private val none: (String) -> Boolean = { false }
    private val all: (String) -> Boolean = { true }

    private fun v(
        icon: HudIcon = HudIcon.SUGAR, selected: Boolean = false, enabled: Boolean = true,
        hovered: Boolean = false, pressed: Boolean = false, loaded: (String) -> Boolean = all,
    ) = iconButtonVisual(icon, selected, enabled, hovered, pressed, loaded)

    @Test
    fun missingIconFallsBackToAsciiText() {
        val r = v(HudIcon.STEP, loaded = none)
        assertNull(r.assetPath)
        assertEquals(">|", r.text)
        val ok = v(HudIcon.STEP)
        assertEquals("icons/skip_next_fill.svg", ok.assetPath)
        assertEquals("", ok.text)
    }

    @Test
    fun missingFilledVariantFallsBackToText() {
        val r = v(selected = true, loaded = { it == "icons/cookie.svg" })
        assertNull(r.assetPath)
        assertEquals("S", r.text)
    }

    @Test
    fun selectedUsesFilledIconPrimaryAndOutline() {
        val r = v(selected = true)
        assertEquals("icons/cookie_fill.svg", r.assetPath)
        assertEquals(c.primary, r.background)
        assertEquals(c.primaryContainer, r.foreground)
        assertTrue(r.outlined)
    }

    @Test
    fun stateBackgrounds() {
        assertNull(v().background)
        assertEquals(c.primaryHover, v(hovered = true).background)
        assertEquals(c.primaryPressed, v(hovered = true, pressed = true).background)
        assertEquals(c.primaryVariant, v(selected = true, pressed = true).background)
        assertFalse(v().outlined)
    }

    @Test
    fun disabledIsDimmedNotClickableAndIgnoresHover() {
        val r = v(HudIcon.STEP, enabled = false, hovered = true, pressed = true)
        assertEquals(c.onSurfaceDisabled, r.foreground)
        assertNull(r.background)
        assertFalse(r.clickable)
        assertTrue(v().clickable)
    }

    @Test
    fun loadFailureLogsWarningAndDoesNotThrow() = runBlocking {
        val warnings = mutableListOf<String>()
        val loaded = mutableMapOf<String, Int>()
        val ok = HudIcons.loadIcon<Int>("icons/nope.svg", { Result.failure(IllegalStateException("404")) }, { p, t -> loaded[p] = t }) { warnings += it }
        val threw = HudIcons.loadIcon<Int>("icons/boom.svg", { error("io") }, { p, t -> loaded[p] = t }) { warnings += it }
        assertFalse(ok)
        assertFalse(threw)
        assertTrue(loaded.isEmpty())
        assertEquals(2, warnings.size)
        assertTrue(warnings[0].contains("icons/nope.svg") && warnings[0].contains("text label"))
    }

    @Test
    fun loadSuccessStoresIconWithoutWarning() = runBlocking {
        val warnings = mutableListOf<String>()
        val loaded = mutableMapOf<String, Int>()
        assertTrue(HudIcons.loadIcon("icons/add.svg", { Result.success(7) }, { p, t -> loaded[p] = t }) { warnings += it })
        assertEquals(mapOf("icons/add.svg" to 7), loaded)
        assertTrue(warnings.isEmpty())
    }
}
