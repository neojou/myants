package com.neojou.myants.render3d

import com.neojou.myants.classic2d.PheroLayer
import com.neojou.myants.render3d.phero.PheromoneColor
import com.neojou.myants.sim.PheromoneGrid
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** #87 P1-P3, P6-P8: pheromone "home" color becomes #0088D9, every other channel stays bit-identical. */
class PheromoneHomeColorTest {
    /** The formula as it was on main 9e35271, kept here as the P1 oracle. */
    private fun legacy(home: Double, sugar: Double, protein: Double, danger: Double, strength: Double): IntArray {
        if (home <= 0.01 && sugar <= 0.01 && protein <= 0.01 && danger <= 0.01) return intArrayOf(0, 0, 0, 0)
        val gain = min(1.0, 0.85 * strength)
        fun ch(v: Double) = min(255.0, v * 255.0).toInt().coerceIn(0, 255)
        return intArrayOf(
            ch(sqrt(protein + sugar + danger) * gain),
            ch(sqrt(sugar) * gain),
            ch(sqrt(home + danger) * gain),
            255,
        )
    }

    private fun near(want: Int, got: Int, what: String, tol: Int = 1) =
        assertTrue(abs(want - got) <= tol, "$what: want $want got $got")

    @Test
    fun p1_withoutHomeIsBitIdenticalToLegacy() {
        val rnd = Random(87)
        var n = 0
        repeat(5000) {
            val sugar = if (rnd.nextInt(4) == 0) 0.0 else rnd.nextDouble() * 1.5
            val protein = if (rnd.nextInt(4) == 0) 0.0 else rnd.nextDouble() * 1.5
            val danger = if (rnd.nextInt(4) == 0) 0.0 else rnd.nextDouble() * 1.5
            val strength = 0.2 + rnd.nextDouble() * 1.3
            assertContentEquals(
                legacy(0.0, sugar, protein, danger, strength),
                PheromoneColor.rgba(0.0, sugar, protein, danger, strength),
                "s=$sugar p=$protein d=$danger u=$strength",
            )
            n++
        }
        assertEquals(5000, n)
    }

    @Test
    fun p1_faintChannelsStayClear() {
        assertContentEquals(intArrayOf(0, 0, 0, 0), PheromoneColor.rgba(0.01, 0.01, 0.01, 0.01, 1.0))
    }

    @Test
    fun p2_homeOnlyBytes() {
        val px = PheromoneColor.rgba(1.0, 0.0, 0.0, 0.0, 1.0)
        near(0, px[0], "R")
        near(115, px[1], "G")
        near(184, px[2], "B")
        assertEquals(255, px[3])
    }

    @Test
    fun p2_homeHueIsBlueNotMagenta() {
        for (home in listOf(0.2, 0.5, 1.0)) {
            val px = PheromoneColor.rgba(home, 0.0, 0.0, 0.0, 1.0)
            assertEquals(0, px[0], "no red from home")
            assertTrue(px[2] > px[1] && px[1] > 0, "B > G > 0 for home=$home: ${px.toList()}")
        }
    }

    @Test
    fun p3_dangerOnlyKeepsLegacyMagenta() {
        for (d in listOf(0.05, 0.3, 1.0, 1.4)) {
            assertContentEquals(legacy(0.0, 0.0, 0.0, d, 1.0), PheromoneColor.rgba(0.0, 0.0, 0.0, d, 1.0))
        }
    }

    @Test
    fun p3_homeEqualsDangerBlendsByWeight() {
        val px = PheromoneColor.rgba(0.5, 0.0, 0.0, 0.5, 1.0)
        val b = sqrt(1.0) * 0.85
        near((sqrt(0.5) * 0.85 * 255).toInt(), px[0], "R from danger")
        near((b * 0.5 * 136.0 / 255.0 * 255.0).toInt(), px[1], "G")
        near((b * (0.5 + 0.5 * 217.0 / 255.0) * 255.0).toInt(), px[2], "B")
    }

    @Test
    fun p3_proteinPlusHomeIsNoLongerDanger() {
        val proteinHome = PheromoneColor.rgba(0.5, 0.0, 0.5, 0.0, 1.0)
        val danger = PheromoneColor.rgba(0.0, 0.0, 0.0, 0.5, 1.0)
        assertFalse(proteinHome.contentEquals(danger), "protein+home ${proteinHome.toList()} vs danger ${danger.toList()}")
    }

    @Test
    fun p7_nightBloomLumaAtMaxStrengthStaysUnder14() {
        // bloom luma = 0.37R + 0.41G + 0.22B, times night boost 4.0 and max gain 0.85 (kool bloom weights; Artist §6.9).
        val home = PheromoneColor.HOME
        val luma = 0.37 * home[0] + 0.41 * home[1] + 0.22 * home[2]
        val bloom = luma * 4.0 * 0.85
        println("PHERO NIGHT BLOOM %.3f".format(bloom))
        assertTrue(bloom <= 1.4, "bloom $bloom")
    }

    @Test
    fun homeConstantIs0088D9() {
        assertEquals(listOf(0.0, 136.0 / 255.0, 217.0 / 255.0), PheromoneColor.HOME.toList())
    }

    /** P4/P6: Classic 2D paints through the same function and constant as the 3D texture. */
    @Test
    fun p6_classicPaintMatchesPheromoneColorRgba() {
        val grid = PheromoneGrid(40.0, 30.0, 1.0)
        val rnd = Random(6)
        for (i in 0 until grid.width * grid.height) {
            if (rnd.nextInt(3) != 0) grid.toHome[i] = rnd.nextFloat() * 1.2f
            if (rnd.nextInt(3) == 0) grid.toSugar[i] = rnd.nextFloat()
            if (rnd.nextInt(5) == 0) grid.toProtein[i] = rnd.nextFloat()
            if (rnd.nextInt(5) == 0) grid.toDanger[i] = rnd.nextFloat()
        }
        for (strength in listOf(0.4, 0.8, 1.5)) {
            val layer = PheroLayer("test")
            layer.update(grid, strength, 1)
            val buf = layer.pixels!!
            var homeOnlyChecked = 0
            for (i in 0 until grid.width * grid.height) {
                val want = PheromoneColor.rgba(
                    grid.toHome[i].toDouble(), grid.toSugar[i].toDouble(),
                    grid.toProtein[i].toDouble(), grid.toDanger[i].toDouble(), strength,
                )
                val got = intArrayOf(buf[i * 4].toInt(), buf[i * 4 + 1].toInt(), buf[i * 4 + 2].toInt(), buf[i * 4 + 3].toInt())
                assertContentEquals(want, got, "pixel $i u=$strength")
                if (grid.toSugar[i] == 0f && grid.toProtein[i] == 0f && grid.toDanger[i] == 0f && grid.toHome[i] > 0.01f) {
                    assertEquals(0, got[0], "Classic home-only trail has no red (u=$strength, px $i)")
                    homeOnlyChecked++
                }
            }
            assertTrue(homeOnlyChecked > 50, "home-only pixels sampled: $homeOnlyChecked")
        }
    }
}
