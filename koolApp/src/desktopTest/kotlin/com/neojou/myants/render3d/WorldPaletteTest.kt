package com.neojou.myants.render3d

import de.fabmax.kool.util.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class WorldPaletteTest {
    @Test
    fun workerDefaultMatchesTheOldHex() {
        assertEquals(Color("#c4703a"), WorldPalette.worker)
    }

    @Test
    fun classicAnd3dKeepSeparateFoodColors() {
        assertEquals(Color("#FFD700"), WorldPalette.sugar2d)
        assertEquals(Color(1f, 0.84f, 0.2f, 1f), WorldPalette.sugar3d)
        assertEquals(Color("#CD5C5C"), WorldPalette.protein2d)
        assertEquals(Color(0.8f, 0.25f, 0.22f, 1f), WorldPalette.protein3d)
    }
}
