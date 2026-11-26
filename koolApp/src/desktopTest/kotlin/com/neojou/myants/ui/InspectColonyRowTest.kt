package com.neojou.myants.ui

import com.neojou.myants.sim.Ant
import com.neojou.myants.sim.AntKind
import com.neojou.myants.sim.Colony
import com.neojou.myants.sim.EntranceSide
import com.neojou.myants.sim.Nest
import com.neojou.myants.sim.PheromoneGrid
import com.neojou.myants.sim.Queen
import com.neojou.myants.sim.Rng
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** #109 R7: the ant card names its colony (non-colour cue, ART_DIRECTION §4), in all four languages. */
class InspectColonyRowTest {
    private fun ant(colonyId: Int, kind: AntKind = AntKind.WORKER): Ant {
        val saved = Rng.getState()
        try {
            return Ant(1.0, 2.0, kind).also {
                it.colony = Colony(colonyId, Nest(), Queen(), PheromoneGrid(10.0, 10.0), EntranceSide.RIGHT)
            }
        } finally {
            Rng.setState(saved)
        }
    }

    private val expectedLabel = mapOf(
        UiLanguage.EN to "Colony",
        UiLanguage.ZH_TW to "群",
        UiLanguage.JA to "コロニー",
        UiLanguage.DE to "Kolonie",
    )

    @Test
    fun antCardEndsWithItsColonyInEveryLanguage() {
        for (lang in UiLanguage.entries) {
            val label = expectedLabel.getValue(lang)
            assertEquals(InspectRow(label, "1"), InspectCopy.antPanel(ant(0), lang).rows.last(), "$lang own")
            assertEquals(
                InspectRow(label, MyantsTheme.Data.rivalColonyLabel(lang)),
                InspectCopy.antPanel(ant(1, AntKind.SOLDIER), lang).rows.last(),
                "$lang rival",
            )
        }
    }

    @Test
    fun colonyRowKeepsTheOtherRowsAndFitsTheCard() {
        val rows = InspectCopy.antPanel(ant(1), UiLanguage.EN).rows
        assertEquals(listOf("State", "Energy", "Health", "Carrying", "Colony"), rows.map { it.label })
        assertTrue(rows.size <= InspectPanel.MAX_ROWS)
        assertEquals("Rival colony 2 · Gold #d9bf52", rows.last().value)
    }

    @Test
    fun onlyTheColonyRowIsWideSoTheLongNameIsNotClipped() {
        for (lang in UiLanguage.entries) {
            val rows = InspectCopy.antPanel(ant(1), lang).rows
            assertTrue(rows.last().wide, "$lang colony row keeps its own width")
            assertTrue(rows.dropLast(1).none { it.wide }, "$lang other rows keep the fixed value column")
        }
    }
}
