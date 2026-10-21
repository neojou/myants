package com.neojou.myants.input

import com.neojou.myants.render3d.RenderMode
import com.neojou.myants.session.CameraState
import de.fabmax.kool.math.Vec2f
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PointerRouterTest {
    private val leftW = 740f
    private val winH = 600f

    private class Fake(override var renderer: RenderMode, override var nest3dActive: Boolean = false) : PointerTarget {
        val cam = CameraState()
        val log = mutableListOf<String>()
        override fun zoomAt(factor: Double, wx: Double, wy: Double) { log += "zoom"; cam.zoomAt(factor, wx, wy) }
        override fun notifyInput() { log += "notify" }
        override fun panPixels(dxPx: Float, dyPx: Float, leftW: Float, winH: Float) {
            log += "panPx"; cam.pan(dxPx, dyPx, cam.fitScale(leftW, winH).toFloat())
        }
        override fun panGround(dx: Double, dy: Double) { log += "panGround $dx $dy" }
        override fun clickWorld(wx: Double, wy: Double) { log += "click ${wx.toInt()} ${wy.toInt()}" }
        override fun inspectNest(nx: Double, ny: Double) { log += "nest ${nx.toInt()} ${ny.toInt()}" }
        override fun screenToWorld2d(px: Float, py: Float, leftW: Float, winH: Float) = cam.screenToWorld(px, py, leftW, winH)
    }

    /** Fake ground pick: 1 px = 2 world units. */
    private val pick: (Float, Float) -> Vec2f? = { x, y -> Vec2f(x * 2f, y * 2f) }
    private val nest = NestRect(800f, 100f, 0.5f)

    private fun route(s: PointerSample, t: PointerTarget, g: ((Float, Float) -> Vec2f?)? = pick) =
        PointerRouter.route(s, t, leftW, winH, g, nest)

    @Test
    fun worldScreenRoundTrip2d() {
        val cam = CameraState().apply { zoom = 2.5; x = 300.0; y = 200.0 }
        for ((wx, wy) in listOf(0.0 to 0.0, 300.0 to 200.0, 123.4 to 456.7)) {
            val (sx, sy) = cam.worldToScreen(wx, wy, leftW, winH)
            val (bx, by) = cam.screenToWorld(sx, sy, leftW, winH)
            assertEquals(wx, bx, 1e-3)
            assertEquals(wy, by, 1e-3)
        }
        val (cx, cy) = cam.worldToScreen(300.0, 200.0, leftW, winH)
        assertEquals(leftW / 2f, cx, 1e-3f)
        assertEquals(winH / 2f, cy, 1e-3f)
    }

    @Test
    fun nestRectRoundTrip() {
        val (nx, ny) = nest.toNest(900f, 300f)!!
        assertEquals(200.0, nx, 1e-6)
        assertEquals(400.0, ny, 1e-6)
        assertEquals(900f to 300f, nest.toScreen(nx, ny))
        assertNull(NestRect(0f, 0f, 0f).toNest(1f, 1f))
    }

    @Test
    fun leftWidthIs74Percent() = assertEquals(740f, PointerRouter.leftWidth(1000f), 1e-3f)

    @Test
    fun click2dUsesCameraState() {
        val t = Fake(RenderMode.CLASSIC)
        val c = route(PointerSample(leftW / 2f, winH / 2f, leftClicked = true), t)
        val (wx, wy) = t.cam.screenToWorld(leftW / 2f, winH / 2f, leftW, winH)
        assertEquals(listOf("click ${wx.toInt()} ${wy.toInt()}"), t.log)
        assertTrue(c.all)
    }

    @Test
    fun dragIsNotClick() {
        val t = Fake(RenderMode.CLASSIC)
        val c = route(PointerSample(100f, 100f, dx = 3f, leftDown = true, leftClicked = true, drag = 6f), t)
        assertEquals(listOf("panPx"), t.log)
        assertFalse(c.all)
    }

    @Test
    fun scroll2dZoomsAndConsumesScroll() {
        val t = Fake(RenderMode.CLASSIC)
        val c = route(PointerSample(100f, 100f, scrollY = 1f), t)
        assertEquals(listOf("zoom", "notify"), t.log)
        assertEquals(1.1, t.cam.zoom, 1e-9)
        assertEquals(PointerConsume(scroll = true), c)
    }

    @Test
    fun outsideLeftIgnoredWhenNestInactive() {
        val t = Fake(RenderMode.D3, nest3dActive = false)
        assertEquals(PointerConsume.NONE, route(PointerSample(900f, 100f, leftClicked = true), t))
        assertTrue(t.log.isEmpty())
    }

    @Test
    fun click3dUsesGroundPick() {
        val t = Fake(RenderMode.D3)
        val c = route(PointerSample(10f, 20f, leftClicked = true), t)
        assertEquals(listOf("click 20 40"), t.log)
        assertTrue(c.all)
    }

    @Test
    fun click3dWithoutPickDoesNothing() {
        val t = Fake(RenderMode.D3)
        assertEquals(PointerConsume.NONE, route(PointerSample(10f, 20f, leftClicked = true, scrollY = 1f), t, g = null))
        assertTrue(t.log.isEmpty())
    }

    @Test
    fun drag3dPansByGroundDelta() {
        val t = Fake(RenderMode.D3)
        route(PointerSample(110f, 100f, dx = 10f, leftDown = true, drag = 20f), t)
        assertEquals(listOf("panGround -20.0 0.0"), t.log)
    }

    @Test
    fun nestClickIn3d() {
        val t = Fake(RenderMode.D3, nest3dActive = true)
        val c = route(PointerSample(900f, 300f, leftClicked = true), t)
        assertEquals(listOf("nest 200 400"), t.log)
        assertTrue(c.all)
        assertEquals(PointerConsume.NONE, route(PointerSample(900f, 300f, scrollY = 1f), t))
    }
}
