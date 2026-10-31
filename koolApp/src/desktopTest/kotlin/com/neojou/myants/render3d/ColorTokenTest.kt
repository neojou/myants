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
    fun defaultAntColorKeepsTheBlackChitin() {
        assertEquals(WorldPalette.antChitin, outdoorAntTint(0, AntKind.WORKER, WorldPalette.worker).chitin)
        assertEquals(WorldPalette.antChitin, outdoorAntTint(0).chitin)
    }

    @Test
    fun otherAntColorRecoloursColonyZeroGltfAnts() {
        for (choice in MyantsTheme.Data.antColorChoices) {
            if (choice.color == WorldPalette.worker) continue
            val tint = outdoorAntTint(0, AntKind.WORKER, choice.color)
            assertEquals(choice.color.toLinear(), tint.chitin, choice.hex)
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
