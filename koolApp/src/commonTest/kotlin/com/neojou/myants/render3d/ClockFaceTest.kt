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
        assertEquals(-20f, box.z)
        assertEquals(40f, box.sx)
        assertEquals(4f, box.sy)
        assertEquals(8f, box.sz)
    }

    @Test
    fun colonDotsStayOnTheStone() {
        val dot = flatClockDot(x = 1f, digitY = 50f, digitZ = 10f, along = 12f, size = 5f, thickness = 4f)
        assertEquals(1f, dot.x)
        assertEquals(50f, dot.y)
        assertEquals(-2f, dot.z)
        assertEquals(5f, dot.sx)
        assertEquals(4f, dot.sy)
        assertEquals(5f, dot.sz)
    }

    @Test
    fun colonIsTheMidpointBetweenHourAndMinute() {
        val metrics = clockMetrics(68f)
        val slots = clockSlots(originX = 1000f, metrics)
        val mid = (slots.hourOnes + slots.minuteTens) * 0.5f
        assertEquals(mid, slots.colonX, 0.01f)
        assertEquals(1000f, slots.colonX, 0.01f)
        val leftSpace = slots.colonX - metrics.colonSlot * 0.5f - (slots.hourOnes + metrics.digitWidth * 0.5f)
        val rightSpace = slots.minuteTens - metrics.digitWidth * 0.5f - (slots.colonX + metrics.colonSlot * 0.5f)
        assertEquals(metrics.gap, leftSpace, 0.01f)
        assertEquals(leftSpace, rightSpace, 0.01f)
    }

    @Test
    fun digitOneVerticalsSpanTheFullHeight() {
        val metrics = clockMetrics(68f)
        val strokes = clockStrokes(cx = 10f, cy = 20f, metrics, digit = 1)
        assertEquals(2, strokes.size)
        assertTrue(strokes.all { it.x > 10f })
        val length = strokes.sumOf { it.sy.toDouble() }.toFloat()
        assertEquals(metrics.digitHeight, length, 0.01f)
        val upper = strokes.maxBy { it.y }
        val lower = strokes.minBy { it.y }
        assertEquals(metrics.digitHeight * 0.5f, upper.sy, 0.01f)
        assertEquals(metrics.digitHeight * 0.5f, lower.sy, 0.01f)
        assertEquals(upper.y - upper.sy * 0.5f, lower.y + lower.sy * 0.5f, 0.01f)
        val along = clockCellAlong(metrics.digitHeight)
        assertEquals(along, upper.y - 20f, 0.01f)
        assertEquals(-along, lower.y - 20f, 0.01f)
    }

    @Test
    fun digitSevenKeepsTheTopBarAndTheSameVertical() {
        val metrics = clockMetrics(68f)
        val seven = clockStrokes(cx = 0f, cy = 0f, metrics, digit = 7)
        val bars = seven.filter { it.sx > it.sy }
        val verticals = seven.filter { it.sy > it.sx }
        assertEquals(1, bars.size)
        assertTrue(bars[0].y > 0f)
        assertEquals(metrics.digitWidth, bars[0].sx, 0.01f)
        val sevenLength = verticals.sumOf { it.sy.toDouble() }.toFloat()
        val oneLength = clockStrokes(0f, 0f, metrics, 1).sumOf { it.sy.toDouble() }.toFloat()
        assertEquals(metrics.digitHeight, sevenLength, 0.01f)
        assertEquals(oneLength, sevenLength, 0.01f)
    }
}
