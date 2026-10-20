package com.neojou.myants.session

import kotlin.test.Test
import kotlin.test.assertEquals

class SimClockTest {
    private fun ticks(clock: SimClock, dt: Double, calls: Int = 1): Int {
        var n = 0
        repeat(calls) { clock.advance(dt) { n++ } }
        return n
    }

    @Test
    fun defaultIs1xAndOneTickPerFrame() {
        val c = SimClock()
        assertEquals(1.0, c.speed)
        assertEquals(60, ticks(c, 1.0 / 60.0 + 1e-9, 60))
    }

    @Test
    fun speedMultipliesTicks() {
        val c = SimClock().apply { speedIndex = 5 } // 8x
        assertEquals(8, ticks(c, 1.0 / 60.0 + 1e-9))
    }

    @Test
    fun halfSpeedTicksEveryOtherFrame() {
        val c = SimClock().apply { speedIndex = 1 }
        assertEquals(30, ticks(c, 1.0 / 60.0 + 1e-9, 60))
    }

    @Test
    fun zeroSpeedAndPauseDoNotTick() {
        assertEquals(0, ticks(SimClock().apply { speedIndex = 0 }, 1.0, 10))
        val p = SimClock().apply { togglePause() }
        assertEquals(0, ticks(p, 1.0))
        p.togglePause()
        assertEquals(1, ticks(p, 1.0 / 60.0 + 1e-9))
    }

    @Test
    fun largeDtIsCappedAt64Frames() {
        val c = SimClock()
        assertEquals(64, ticks(c, 10.0))
        // leftover accumulator is capped to one frame
        assertEquals(1, ticks(c, 0.0))
    }

    @Test
    fun stepSpeedClampsAndRestore() {
        val c = SimClock()
        repeat(10) { c.stepSpeed(1) }
        assertEquals(5, c.speedIndex)
        repeat(10) { c.stepSpeed(-1) }
        assertEquals(0, c.speedIndex)
        c.restoreSpeed(4.0)
        assertEquals(4, c.speedIndex)
        c.restoreSpeed(3.0)
        assertEquals(4, c.speedIndex)
    }
}
