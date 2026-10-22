package com.neojou.myants.render3d

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClockFaceTest {
    @Test
    fun colonIsLitOnEvenSeconds() {
        assertTrue(clockColonLit(0))
        assertFalse(clockColonLit(1))
        assertTrue(clockColonLit(2))
        assertFalse(clockColonLit(59))
    }

    @Test
    fun segmentsLieFlatOnTheStone() {
        val box = flatClockSeg(
            digitY = 50f,
            digitZ = 10f,
            segX = 3f,
            segY = 80f,
            sx = 40f,
            sy = 8f,
            thickness = 4f,
        )
        assertEquals(3f, box.x)
        assertEquals(50f, box.y)
        assertEquals(40f, box.z)
        assertEquals(40f, box.sx)
        assertEquals(4f, box.sy)
        assertEquals(8f, box.sz)
    }

    @Test
    fun colonDotsStayOnTheStone() {
        val dot = flatClockDot(x = 1f, digitY = 50f, digitZ = 10f, along = 12f, size = 5f, thickness = 4f)
        assertEquals(1f, dot.x)
        assertEquals(50f, dot.y)
        assertEquals(22f, dot.z)
        assertEquals(5f, dot.sx)
        assertEquals(4f, dot.sy)
        assertEquals(5f, dot.sz)
    }
}
