package com.neojou.myants.ui.panels

import com.neojou.myants.GraphSample
import com.neojou.myants.ui.UiLanguage
import com.neojou.myants.ui.UiText
import de.fabmax.kool.util.Uint8Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatsChartTest {
    @Test
    fun strokesDifferAndSolidIsContinuous() {
        val window = 0 until 16
        assertTrue(window.all { statsStrokeVisible(StatsStroke.SOLID, it) })
        val patterns = StatsStroke.entries.map { style -> window.map { statsStrokeVisible(style, it) } }
        assertEquals(patterns.size, patterns.toSet().size)
        assertTrue(patterns[StatsStroke.DASH.ordinal].any { !it })
        assertTrue(patterns[StatsStroke.DOT.ordinal].any { it } && patterns[StatsStroke.DOT.ordinal].any { !it })
        assertTrue(patterns[StatsStroke.DASH_DOT.ordinal].any { !it })
    }

    @Test
    fun overlappingLabelsAreSeparated() {
        val placed = placeStatsLabelYs(listOf(10, 10, 12, 40), minY = 1, maxY = 120, pitch = 8)
        val ordered = placed.sorted()
        for (i in 1 until ordered.size) {
            assertTrue(ordered[i] - ordered[i - 1] >= 8, ordered.toString())
        }
        assertEquals(40, placed[3])
    }

    @Test
    fun solidPopulationHasNoGapsAndSugarDashDoes() {
        val w = 280
        val h = 130
        val solid = chart(List(24) { GraphSample(population = 80, sugar = 0.0, protein = 0.0, energyPct = 0.0) })
        for (x in 2 until 36) {
            assertEquals(0xdddddd, statsPixel(solid, w, x, 4), "solid gap at x=$x")
        }
        val dashed = chart(List(24) { GraphSample(population = 0, sugar = 80.0, protein = 0.0, energyPct = 0.0) })
        var on = 0
        var off = 0
        for (x in 2 until 36) {
            when (statsPixel(dashed, w, x, 4)) {
                0x6fdf6f -> on++
                0x111111 -> off++
            }
        }
        assertTrue(on > 0 && off > 0, "dash on=$on off=$off")
        assertTrue((0 until w).any { x -> (0 until h).any { y -> statsPixel(dashed, w, x, y) == 0x6fdf6f && x >= w - 48 } })
    }

    @Test
    fun bitmapFallsBackToEnglishWhenAGlyphIsMissing() {
        assertEquals("Sugar", statsBitmapLabel(UiText.SERIES_SUGAR, UiLanguage.EN))
        assertEquals("Zucker", statsBitmapLabel(UiText.SERIES_SUGAR, UiLanguage.DE))
        assertEquals("Sugar", statsBitmapLabel(UiText.SERIES_SUGAR, UiLanguage.ZH_TW))
        assertEquals("Energy", statsBitmapLabel(UiText.SERIES_ENERGY, UiLanguage.EN))
        assertEquals("Energie", statsBitmapLabel(UiText.SERIES_ENERGY, UiLanguage.DE))
        assertEquals("Energy", statsBitmapLabel(UiText.SERIES_ENERGY, UiLanguage.ZH_TW))
        assertEquals("Protein", statsBitmapLabel(UiText.SERIES_PROTEIN, UiLanguage.ZH_TW))
    }

    @Test
    fun seriesColorsStayTheAntSimChartColors() {
        val w = 280
        val buf = chart(List(8) {
            GraphSample(population = 10, sugar = 20.0, protein = 30.0, energyPct = 0.4)
        })
        val seen = HashSet<Int>()
        for (y in 0 until 130) for (x in 0 until w) {
            val p = statsPixel(buf, w, x, y)
            if (p != 0x111111) seen += p
        }
        assertTrue(0xdddddd in seen)
        assertTrue(0x6fdf6f in seen)
        assertTrue(0xdf6f6f in seen)
        assertTrue(0x6fb0df in seen)
    }

    private fun chart(samples: List<GraphSample>): Uint8Buffer {
        val buf = Uint8Buffer(280 * 130 * 4)
        drawStatsChart(buf, 280, 130, samples)
        return buf
    }
}
