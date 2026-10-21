package com.neojou.myants.session

import com.neojou.myants.render3d.RenderMode
import com.neojou.myants.ui.UiLanguage
import de.fabmax.kool.util.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ViewStateTest {
    @Test
    fun encodeKeepsLegacyKeyOrderAndFormat() {
        val text = ViewState().encode(1.0, 2, "MEDIUM", linkedMapOf("antSpeed" to 2.5))
        val expected = """
            speed=1.0
            colonies=2
            quality=MEDIUM
            pheromones=true
            pheromoneIntensity=1.0
            antColor=#c4703a
            dayNight=true
            dayNightIntensity=0.5
            cinematic=true
            renderer=3d
            cameraPitch=-70.0
            bloom=true
            ssao=true
            weather=true
            shadows=true
            grassDensity=1.0
            language=EN
            ov:antSpeed=2.5
        """.trimIndent() + "\n"
        assertEquals(expected, text)
    }

    @Test
    fun roundTripThroughParse() {
        val a = ViewState().apply {
            showPheromones = false; pheromoneStrength = 0.7; antColor = Color("#6fa8dc")
            dayNight = false; dayNightStrength = 0.2; cinema = false; renderer = RenderMode.CLASSIC
            cameraPitchDeg = -80f; bloomUser = false; ssaoUser = false; weatherUser = false
            shadowsUser = false; grassDensity = 0.5f
        }
        val b = ViewState()
        b.applySaved(ViewState.parse(a.encode(2.0, 1, "LOW", emptyMap())))
        assertEquals(a.encode(2.0, 1, "LOW", emptyMap()), b.encode(2.0, 1, "LOW", emptyMap()))
    }

    @Test
    fun languageDefaultsToEnglishAndIgnoresUnknown() {
        val fresh = ViewState()
        assertEquals(UiLanguage.EN, fresh.language)
        fresh.applySaved(emptyMap())
        assertEquals(UiLanguage.EN, fresh.language)
        fresh.applySaved(mapOf("language" to "FR"))
        assertEquals(UiLanguage.EN, fresh.language)
        fresh.applySaved(mapOf("language" to "ZH_TW"))
        assertEquals(UiLanguage.ZH_TW, fresh.language)
        val restored = ViewState()
        restored.applySaved(ViewState.parse(fresh.encode(1.0, 1, "MEDIUM", emptyMap())))
        assertEquals(UiLanguage.ZH_TW, restored.language)
        restored.applySaved(mapOf("language" to "DE"))
        assertEquals(UiLanguage.DE, restored.language)
        restored.applySaved(mapOf("language" to "JA"))
        assertEquals(UiLanguage.JA, restored.language)
        val jaFile = ViewState()
        jaFile.applySaved(ViewState.parse(restored.encode(1.0, 1, "MEDIUM", emptyMap())))
        assertEquals(UiLanguage.JA, jaFile.language)
    }

    @Test
    fun applySavedClampsAndIgnoresGarbage() {
        val v = ViewState()
        v.applySaved(mapOf("cameraPitch" to "-10", "grassDensity" to "9", "bloom" to "yes", "antColor" to "zzz", "dayNight" to "false"))
        assertEquals(-60f, v.cameraPitchDeg)
        assertEquals(1.5f, v.grassDensity)
        assertTrue(v.bloomUser)
        assertEquals("#c4703a", ViewState.hex(v.antColor))
        assertFalse(v.dayNight)
    }

    @Test
    fun parseSkipsBadLines() {
        assertEquals(mapOf("a" to "1", "b" to "x=y"), ViewState.parse("a = 1\n=oops\nnoeq\nb=x=y\n"))
    }

    @Test
    fun panelsAreExclusiveAndToggle() {
        val v = ViewState()
        v.openPanel(Panel.GRAFIK)
        assertTrue(v.showGrafik)
        v.openPanel(Panel.STATS)
        assertFalse(v.showGrafik); assertTrue(v.showStats)
        v.openPanel(Panel.STATS)
        assertFalse(v.showStats)
        v.showParams = true
        v.showOnlyAnalyse()
        assertFalse(v.showParams); assertTrue(v.showAnalyse)
    }

    @Test
    fun closeSidePanelsAndAboutIsIndependent() {
        val v = ViewState()
        v.openPanel(Panel.PARAMS)
        v.openPanel(Panel.ABOUT)
        assertTrue(v.showAbout && v.showParams)
        assertTrue(v.isOpen(Panel.ABOUT))
        v.openPanel(Panel.ANALYSE)
        assertTrue(v.isOpen(Panel.ANALYSE)); assertFalse(v.showParams)
        v.closeSidePanels()
        assertFalse(v.showGrafik || v.showParams || v.showStats || v.showAnalyse)
        assertTrue(v.showAbout)
    }
}
