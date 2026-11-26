package com.neojou.myants.render3d

import com.neojou.myants.render3d.actors.outdoorAntTint
import com.neojou.myants.sim.AntKind
import com.neojou.myants.ui.MyantsTheme
import de.fabmax.kool.util.Color
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** #109 R1: the rival colony's outdoor shell is Gold #d9bf52 through the custom-swatch pipeline, written as a literal. */
class RivalChitinTest {
    private fun lum(c: Color) = 0.2126f * c.r + 0.7152f * c.g + 0.0722f * c.b

    @Test
    fun rivalChitinIsTheArtistLiteral() {
        assertEquals(Color(0.0857f, 0.0644f, 0.0104f, 1f), WorldPalette.rivalChitin)
    }

    @Test
    fun rivalChitinMatchesGoldThroughOutdoorChitinAndStaysUnderTheCap() {
        val goldHex = MyantsTheme.Data.antColors[2]
        assertEquals("#d9bf52", goldHex)
        val gold = WorldPalette.outdoorChitin(Color(goldHex))
        val rival = WorldPalette.rivalChitin
        for ((name, pair) in listOf("r" to (rival.r to gold.r), "g" to (rival.g to gold.g), "b" to (rival.b to gold.b))) {
            assertTrue(abs(pair.first - pair.second) <= 0.0005f, "$name rival=${pair.first} gold=${pair.second}")
        }
        // Same 1e-4 slack as ChitinLuminanceCapTest: the 4-digit literal lands at Y 0.06503.
        assertTrue(lum(rival) <= WorldPalette.CHITIN_Y_MAX + 1e-4f, "Y=${lum(rival)}")
    }

    @Test
    fun rivalColonyIgnoresEveryColonyZeroAntColor() {
        val hexes = MyantsTheme.Data.antColors + listOf("#ff00ff", "#ffffff", "#000000")
        for (hex in hexes) {
            for (kind in listOf(AntKind.WORKER, AntKind.SOLDIER)) {
                assertEquals(WorldPalette.rivalChitin, outdoorAntTint(1, kind, Color(hex)).chitin, "$hex $kind")
            }
        }
        assertEquals(WorldPalette.rivalChitin, outdoorAntTint(1).chitin)
    }

    @Test
    fun rivalEyeJawAndSoldierHeadAreUnchanged() {
        assertEquals(Color(0.08f, 0.04f, 0.03f, 1f), WorldPalette.rivalEye)
        assertEquals(Color(0.32f, 0.16f, 0.10f, 1f), WorldPalette.rivalJaw)
        assertEquals(Color(0.75f, 0.1f, 0.08f, 1f), WorldPalette.soldierHead3d)
        val worker = outdoorAntTint(1, AntKind.WORKER)
        assertEquals(WorldPalette.rivalEye, worker.eye)
        assertEquals(WorldPalette.rivalJaw, worker.jaw)
        assertEquals(WorldPalette.rivalChitin, worker.head)
        assertEquals(WorldPalette.soldierHead3d, outdoorAntTint(1, AntKind.SOLDIER).head)
    }
}
