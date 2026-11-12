package com.neojou.myants.render3d

import com.neojou.myants.render3d.actors.outdoorAntTint
import com.neojou.myants.sim.AntKind
import com.neojou.myants.ui.MyantsTheme
import de.fabmax.kool.util.Color
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** #48 colour tokens and #82 antColor on the glTF ants. */
class ColorTokenTest {
    private val root = listOf(File("src/commonMain"), File("koolApp/src/commonMain")).first { it.isDirectory }

    @Test
    fun scopedFilesHaveNoColorLiterals() {
        val names = setOf("WorldRenderer.kt", "Critter.kt", "GrafikPanel.kt", "NestCutaway.kt", "GltfMerged.kt")
        val files = root.walkTopDown().filter { it.name in names }.toList()
        assertEquals(names, files.map { it.name }.toSet())
        val literal = Regex("""\bColor\(|"#[0-9A-Fa-f]{6}"""")
        for (f in files) {
            val hits = f.readLines().withIndex().filter { literal.containsMatchIn(it.value) }
            assertTrue(hits.isEmpty(), "${f.name}: ${hits.map { it.index + 1 }}")
        }
    }

    @Test
    fun defaultAntColorIsCopperAndBlackKeepsTheBlackChitin() {
        // #86: Copper reads as copper outdoors; Black gets the #94/#95 tuned shell.
        assertEquals(WorldPalette.copperChitin, outdoorAntTint(0, AntKind.WORKER, WorldPalette.worker).chitin)
        assertEquals(WorldPalette.copperChitin, outdoorAntTint(0).chitin)
        val black = MyantsTheme.Data.antColorChoices.single { it.en == "Black" }
        assertEquals(WorldPalette.blackSwatch, black.color)
        assertEquals(WorldPalette.antChitin, outdoorAntTint(0, AntKind.WORKER, black.color).chitin)
    }

    @Test
    fun otherAntColorRecoloursColonyZeroGltfAnts() {
        for (choice in MyantsTheme.Data.antColorChoices) {
            if (choice.color == WorldPalette.worker || choice.color == WorldPalette.blackSwatch) continue
            val tint = outdoorAntTint(0, AntKind.WORKER, choice.color)
            val linear = choice.color.toLinear()
            val s = WorldPalette.CHITIN_ALBEDO_SCALE
            val raw = Color(linear.r * s, linear.g * s, linear.b * s, linear.a)
            val y = 0.2126f * raw.r + 0.7152f * raw.g + 0.0722f * raw.b
            val k = if (y > WorldPalette.CHITIN_Y_MAX) WorldPalette.CHITIN_Y_MAX / y else 1f
            val want = Color(raw.r * k, raw.g * k, raw.b * k, raw.a)
            for ((a, b) in listOf(want.r to tint.chitin.r, want.g to tint.chitin.g, want.b to tint.chitin.b)) {
                assertTrue(kotlin.math.abs(a - b) < 1e-6f, "${choice.hex} $want vs ${tint.chitin}")
            }
            assertNotEquals(WorldPalette.antChitin, tint.chitin)
            assertEquals(tint.chitin, tint.head)
        }
        val soldier = outdoorAntTint(0, AntKind.SOLDIER, Color("#6fa8dc"))
        assertEquals(WorldPalette.soldierHead3d, soldier.head)
    }

    @Test
    fun rivalColonyIgnoresAntColor() {
        assertEquals(WorldPalette.rivalChitin, outdoorAntTint(1, AntKind.WORKER, Color("#6fa8dc")).chitin)
    }
}
