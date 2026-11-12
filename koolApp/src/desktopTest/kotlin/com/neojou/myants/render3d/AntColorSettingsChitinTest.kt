package com.neojou.myants.render3d

import com.neojou.myants.session.ViewState
import com.neojou.myants.ui.MyantsTheme
import de.fabmax.kool.util.Color
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** #86: a saved settings file reaches the same outdoor chitin as clicking the swatch. */
class AntColorSettingsChitinTest {
    private fun roundTrip(c: Color): Color {
        val v = ViewState().apply { antColor = c }
        val line = v.encode(1.0, 1, "medium", emptyMap()).lines().first { it.startsWith("antColor=") }
        return ViewState().apply { applySaved(mapOf("antColor" to line.substringAfter('='))) }.antColor
    }

    @Test
    fun savedSwatchesMapLikeClickedSwatches() {
        for (choice in MyantsTheme.Data.antColorChoices) {
            assertEquals(WorldPalette.outdoorChitin(choice.color), WorldPalette.outdoorChitin(roundTrip(choice.color)), choice.hex)
        }
        assertEquals(WorldPalette.copperChitin, WorldPalette.outdoorChitin(ViewState().antColor))
        assertEquals(WorldPalette.copperChitin, WorldPalette.outdoorChitin(Color("#C4703A")))
        assertEquals(WorldPalette.antChitin, WorldPalette.outdoorChitin(Color("#222222")))
    }

    @Test
    fun cappedSwatchesMatchArtistOracle() {
        // Artist spec #86 §2 (calculated values, 4 decimals, exact sRGB curve). kool's Color.toLinear()
        // differs from that curve by < 1e-3 per channel after the cap (Blue b: 0.1263 vs 0.1273).
        val oracle = mapOf(
            "#d9bf52" to Triple(0.0857f, 0.0644f, 0.0104f),
            "#88c0a0" to Triple(0.0352f, 0.0753f, 0.0503f),
            "#6fa8dc" to Triple(0.0283f, 0.0696f, 0.1273f),
        )
        for ((hex, want) in oracle) {
            val got = WorldPalette.outdoorChitin(Color(hex))
            assertTrue(abs(got.r - want.first) < 1.5e-3f && abs(got.g - want.second) < 1.5e-3f && abs(got.b - want.third) < 1.5e-3f, "$hex $got")
        }
    }
}
