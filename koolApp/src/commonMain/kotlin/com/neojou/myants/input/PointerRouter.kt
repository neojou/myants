package com.neojou.myants.input

import com.neojou.myants.noteDrag
import com.neojou.myants.inspectNest
import com.neojou.myants.AppSession
import com.neojou.myants.render3d.RenderMode
import com.neojou.myants.render3d.WorldInput
import com.neojou.myants.render3d.nest.NestFrame
import de.fabmax.kool.input.InputStack
import de.fabmax.kool.input.Pointer
import de.fabmax.kool.input.PointerInput
import de.fabmax.kool.math.Vec2f
import kotlin.math.sqrt

/** One frame of the primary pointer, decoupled from kool so routing can be unit tested. */
data class PointerSample(
    val x: Float,
    val y: Float,
    val dx: Float = 0f,
    val dy: Float = 0f,
    val scrollY: Float = 0f,
    val leftDown: Boolean = false,
    val leftClicked: Boolean = false,
    /** Total drag distance in pixels since the button went down. */
    val drag: Float = 0f,
) {
    val isDrag: Boolean get() = drag > PointerRouter.CLICK_SLOP_PX
    val isClick: Boolean get() = leftClicked && !isDrag

    companion object {
        fun of(p: Pointer): PointerSample = PointerSample(
            x = p.pos.x, y = p.pos.y, dx = p.delta.x, dy = p.delta.y, scrollY = p.scroll.y,
            leftDown = p.isLeftButtonDown, leftClicked = p.isLeftButtonClicked,
            drag = sqrt(p.dragMovement.x * p.dragMovement.x + p.dragMovement.y * p.dragMovement.y),
        )
    }
}

/** Screen rectangle of the nest image; maps screen pixels to nest coordinates. */
data class NestRect(val ox: Float, val oy: Float, val fit: Float) {
    fun toNest(px: Float, py: Float): Pair<Double, Double>? {
        if (fit <= 0f) return null
        return ((px - ox) / fit).toDouble() to ((py - oy) / fit).toDouble()
    }

    fun toScreen(nx: Double, ny: Double): Pair<Float, Float> = (ox + nx.toFloat() * fit) to (oy + ny.toFloat() * fit)
}

/** What the router asks the pointer to consume after a frame. */
data class PointerConsume(val scroll: Boolean = false, val all: Boolean = false) {
    companion object { val NONE = PointerConsume() }
}

/** Session actions the router dispatches to. [AppSession] implements it via [sessionTarget]. */
interface PointerTarget {
    val renderer: RenderMode
    val nest3dActive: Boolean
    fun zoomAt(factor: Double, wx: Double, wy: Double)
    fun notifyInput()
    /** 2D pan by screen pixels. */
    fun panPixels(dxPx: Float, dyPx: Float, leftW: Float, winH: Float)
    /** 3D pan by a ground-space delta. */
    fun panGround(dx: Double, dy: Double)
    fun clickWorld(wx: Double, wy: Double)
    fun inspectNest(nx: Double, ny: Double)
    fun screenToWorld2d(px: Float, py: Float, leftW: Float, winH: Float): Pair<Double, Double>
}

/**
 * Single entry point for world/nest pointer input (#10). The left 74 % of the window is the
 * world (2D: [com.neojou.myants.session.CameraState]; 3D: `TopDownCamera.screenToGround`, i.e.
 * OrthoGround, never `unProjectScreen`). The right side is the nest cutaway (3D only).
 * Behaviour is identical to the former `handlePointer` in AntSimScene.
 */
object PointerRouter {
    const val CLICK_SLOP_PX = 5f
    const val LEFT_FRACTION = 0.74f

    fun leftWidth(winW: Float): Float = winW * LEFT_FRACTION

    fun route(
        s: PointerSample,
        t: PointerTarget,
        leftW: Float,
        winH: Float,
        groundPick: ((Float, Float) -> Vec2f?)?,
        nest: NestRect,
        pointerBlocked: Boolean = false,
    ): PointerConsume {
        // Toolbar and panels sit on the nest strip. A click there must not count as a missed inspect.
        if (pointerBlocked) return PointerConsume.NONE
        if (t.renderer == RenderMode.D3 && t.nest3dActive && s.x > leftW && s.y in 0f..winH) {
            if (s.isClick) {
                val n = nest.toNest(s.x, s.y) ?: return PointerConsume.NONE
                t.inspectNest(n.first, n.second)
                return PointerConsume(all = true)
            }
            return PointerConsume.NONE
        }
        if (!(s.x in 0f..leftW && s.y in 0f..winH)) return PointerConsume.NONE
        var scroll = false
        var all = false
        if (t.renderer == RenderMode.D3) {
            val hit = groundPick?.invoke(s.x, s.y)
            if (s.scrollY != 0f && hit != null) {
                t.zoomAt(zoomFactor(s.scrollY), hit.x.toDouble(), hit.y.toDouble())
                t.notifyInput()
                scroll = true
            }
            if (s.leftDown && s.isDrag) {
                val prev = groundPick?.invoke(s.x - s.dx, s.y - s.dy)
                if (hit != null && prev != null) {
                    t.panGround((prev.x - hit.x).toDouble(), (prev.y - hit.y).toDouble())
                }
            }
            if (s.isClick && hit != null) {
                t.clickWorld(hit.x.toDouble(), hit.y.toDouble())
                all = true
            }
            return PointerConsume(scroll, all)
        }
        if (s.scrollY != 0f) {
            val (wx, wy) = t.screenToWorld2d(s.x, s.y, leftW, winH)
            t.zoomAt(zoomFactor(s.scrollY), wx, wy)
            t.notifyInput()
            scroll = true
        }
        if (s.leftDown && s.isDrag) t.panPixels(s.dx, s.dy, leftW, winH)
        if (s.isClick) {
            val (wx, wy) = t.screenToWorld2d(s.x, s.y, leftW, winH)
            t.clickWorld(wx, wy)
            all = true
        }
        return PointerConsume(scroll, all)
    }

    fun zoomFactor(scrollY: Float): Double = if (scrollY > 0f) 1.1 else 1.0 / 1.1

    /** Reads kool's primary pointer, routes it and applies consumption. */
    fun handle(session: AppSession, winW: Float, winH: Float) {
        val p = PointerInput.primaryPointer
        if (!p.isValid || p.isConsumed()) return
        val blocked = InputStack.handlerStack.any { it.blockAllPointerInput }
        val c = route(
            PointerSample.of(p), session.sessionTarget(), leftWidth(winW), winH,
            WorldInput.screenToGround, NestRect(NestFrame.ox, NestFrame.oy, NestFrame.fit),
            pointerBlocked = blocked,
        )
        if (c.scroll) p.consume(PointerInput.CONSUMED_SCROLL_Y)
        if (c.all) p.consume()
    }
}

fun AppSession.sessionTarget(): PointerTarget {
    val s = this
    return object : PointerTarget {
        override val renderer get() = s.renderer
        override val nest3dActive get() = s.nest3dActive
        override fun zoomAt(factor: Double, wx: Double, wy: Double) = s.zoomAt(factor, wx, wy)
        override fun notifyInput() = s.notifyInput()
        override fun panPixels(dxPx: Float, dyPx: Float, leftW: Float, winH: Float) =
            s.pan(dxPx, dyPx, s.fitScale(leftW, winH).toFloat())
        override fun panGround(dx: Double, dy: Double) {
            s.camX += dx
            s.camY += dy
            s.noteDrag()
        }
        override fun clickWorld(wx: Double, wy: Double) = s.clickWorld(wx, wy)
        override fun inspectNest(nx: Double, ny: Double) = s.inspectNest(nx, ny)
        override fun screenToWorld2d(px: Float, py: Float, leftW: Float, winH: Float) = s.screenToWorld(px, py, leftW, winH)
    }
}
