package com.neojou.myants.render3d

import de.fabmax.kool.util.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClockCloverColorTest {
    @Test
    fun clockDigitsUseSpecGold() {
        assertEquals(Color("#E9C46A"), WorldPalette.digit3d)
    }

    @Test
    fun clockStoneIsWarmGrey() {
        val c = WorldPalette.clockStone
        assertTrue(c.r > c.g && c.g > c.b, "red > green > blue")
        assertTrue(c.r - c.b >= 0.1f, "warm enough to survive blue sky light")
        assertTrue(c.r - c.b <= 0.2f, "still a grey, not brown")
    }

    @Test
    fun cloverBloomsUseOnlyRoseGoldLilacAndAllThree() {
        val allowed = setOf(WorldPalette.petalRose, WorldPalette.petalGold, WorldPalette.petalLilac)
        val seen = mutableSetOf<Color>()
        for (x in 0 until 200) for (y in 0 until 50) {
            val fx = x * 3.7f; val fy = y * 5.3f
            if (!cloverBlooms(fx, fy)) continue
            val c = cloverPetal(fx, fy)
            assertTrue(c in allowed, "unexpected petal $c")
            seen += c
        }
        assertEquals(allowed, seen)
    }

    @Test
    fun cloverPetalIsStablePerSpot() {
        assertEquals(cloverPetal(12.3f, 45.6f), cloverPetal(12.3f, 45.6f))
    }
}
