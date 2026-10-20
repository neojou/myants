package com.neojou.myants.session

import com.neojou.myants.sim.SimConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CameraStateTest {
    private val w = SimConfig.width.toDouble()
    private val h = SimConfig.height.toDouble()

    @Test
    fun startsCenteredAndResets() {
        val c = CameraState()
        assertEquals(w / 2, c.x); assertEquals(h / 2, c.y); assertEquals(1.0, c.zoom)
        c.x = 1.0; c.zoom = 3.0
        c.reset()
        assertEquals(w / 2, c.x); assertEquals(1.0, c.zoom)
    }

    @Test
    fun zoomClampsAndKeepsAnchorFixed() {
        val c = CameraState()
        c.zoomAt(100.0, c.x, c.y)
        assertEquals(6.0, c.zoom)
        c.zoomAt(0.001, c.x, c.y)
        assertEquals(1.0, c.zoom)
        c.zoomAt(2.0, 0.0, 0.0)
        assertEquals(w / 4, c.x, 1e-9)
        c.zoomAt(Double.NaN, 0.0, 0.0)
        assertEquals(2.0, c.zoom)
    }

    @Test
    fun panAndZeroScale() {
        val c = CameraState()
        assertTrue(c.pan(10f, 0f, 1f))
        assertEquals(w / 2 - 10, c.x, 1e-9)
        assertFalse(c.pan(10f, 0f, 0f))
    }

    @Test
    fun clampKeepsViewInsideWorldAndRecoversNaN() {
        val c = CameraState()
        c.zoom = 4.0; c.x = -1e6; c.y = 1e6
        c.clamp(800f, 600f)
        assertTrue(c.x > 0 && c.x < w)
        assertTrue(c.y > 0 && c.y < h)
        c.x = Double.NaN
        c.clamp(800f, 600f)
        assertEquals(w / 2, c.x); assertEquals(1.0, c.zoom)
    }

    @Test
    fun screenCenterMapsToCamera() {
        val c = CameraState()
        val (wx, wy) = c.screenToWorld(400f, 300f, 800f, 600f)
        assertEquals(c.x, wx, 1e-9); assertEquals(c.y, wy, 1e-9)
    }
}
