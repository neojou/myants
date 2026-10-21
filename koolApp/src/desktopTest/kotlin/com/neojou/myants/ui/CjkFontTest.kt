package com.neojou.myants.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CjkFontTest {
    @Test
    fun atlasCoversEveryInterfaceGlyph() {
        val dir = listOf(
            File("src/commonMain/resources/fonts"),
            File("koolApp/src/commonMain/resources/fonts"),
        ).first { it.isDirectory }
        val json = File(dir, "noto-sans-cjk.json")
        val png = File(dir, "noto-sans-cjk.png")
        assertTrue(json.isFile, "missing $json")
        assertTrue(png.length() > 10_000, "atlas png is empty")
        val codes = Regex(""""cd"\s*:\s*(\d+)""")
            .findAll(json.readText())
            .map { it.groupValues[1].toInt() }
            .toSet()
        val missing = interfaceGlyphs().filter { it.code !in codes }.toSet()
        assertEquals(emptySet<Char>(), missing)
    }
}
