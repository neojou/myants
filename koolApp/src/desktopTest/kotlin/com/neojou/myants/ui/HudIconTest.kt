package com.neojou.myants.ui

import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.platform.ImageDecoder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HudIconTest {
    private val assetsDir = listOf(File("assets"), File("koolApp/assets")).first { it.isDirectory }

    @Test
    fun everyIconHasEnAndDeTooltip() {
        for (icon in HudIcon.entries) {
            assertTrue(icon.tooltip(UiLang.EN).isNotBlank(), "$icon EN")
            assertTrue(icon.tooltip(UiLang.DE).isNotBlank(), "$icon DE")
        }
        assertEquals("Einzelschritt (nur pausiert)", HudIcon.STEP.tooltip(UiLang.DE, disabled = true))
    }

    @Test
    fun noUnboundBracketShortcutsInTooltips() {
        for (icon in HudIcon.entries) {
            val t = icon.tooltip(UiLang.EN) + icon.tooltip(UiLang.DE)
            assertTrue('[' !in t && ']' !in t, "$icon must not advertise [ ] shortcuts (#11)")
        }
    }

    @Test
    fun fallbackLabelsMatchPreviousAsciiButtons() {
        assertEquals(">|", HudIcon.STEP.label(iconLoaded = false))
        assertEquals("PNG", HudIcon.SCREENSHOT.label(iconLoaded = false))
        assertEquals("", HudIcon.STEP.label(iconLoaded = true))
        assertEquals(listOf("?", "S", "P", "E"),
            listOf(HudIcon.INSPECT, HudIcon.SUGAR, HudIcon.PROTEIN, HudIcon.ENEMY).map { it.fallback })
    }

    @Test
    fun selectedToolsUseFilledVariant() {
        assertEquals("icons/cookie_fill.svg", HudIcon.SUGAR.assetPath(filled = true))
        assertEquals("icons/zoom_in.svg", HudIcon.ZOOM_IN.assetPath(filled = true))
    }

    @Test
    fun pauseUsesFilledGlyphEvenWhenUnselected() {
        assertEquals("icons/pause_fill.svg", HudIcon.PAUSE.assetPath(filled = false))
        assertEquals("icons/pause_fill.svg", HudIcon.PAUSE.assetPath(filled = true))
    }

    @Test
    fun allSvgAssetsExistAndRasterizeWhite() {
        for (path in HudIcon.allAssetPaths()) {
            val f = File(assetsDir, path)
            assertTrue(f.isFile, "missing $f")
            // Same path kool 0.19 uses for Assets.loadTexture2d("*.svg", resolveSize) on desktop.
            val img = f.inputStream().use { ImageDecoder.loadSvgAsBufferedImage(it, Vec2i(54, 54)) }
            assertEquals(54, img.width)
            assertEquals(54, img.height)
            var opaque = 0
            for (y in 0 until 54) for (x in 0 until 54) {
                val argb = img.getRGB(x, y)
                if ((argb ushr 24) > 200) {
                    opaque++
                    assertEquals(0xFFFFFF, argb and 0xFFFFFF, "$path must be white for tinting")
                }
            }
            assertTrue(opaque > 20, "$path rendered empty")
        }
    }
}
