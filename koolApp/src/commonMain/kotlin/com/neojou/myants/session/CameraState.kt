package com.neojou.myants.session

import com.neojou.myants.sim.SimConfig

/**
 * 2D view camera over the world (center + zoom 1..6). Moved verbatim from AppSession (#7).
 */
class CameraState {
    var x: Double = SimConfig.width / 2.0
    var y: Double = SimConfig.height / 2.0
    var zoom: Double = 1.0

    fun zoomAt(factor: Double, anchorX: Double, anchorY: Double) {
        if (!factor.isFinite() || !anchorX.isFinite() || !anchorY.isFinite()) return
        if (!zoom.isFinite() || !x.isFinite() || !y.isFinite()) return
        val next = (zoom * factor).coerceIn(1.0, 6.0)
        val change = next / zoom
        if (change == 0.0 || !change.isFinite()) return
        x = anchorX + (x - anchorX) / change
        y = anchorY + (y - anchorY) / change
        zoom = next
    }

    /** @return false when the scale is zero and nothing moved. */
    fun pan(dxPx: Float, dyPx: Float, fit: Float): Boolean {
        val s = fit * zoom
        if (s == 0.0) return false
        x -= dxPx / s
        y -= dyPx / s
        return true
    }

    fun reset() {
        x = SimConfig.width / 2.0
        y = SimConfig.height / 2.0
        zoom = 1.0
    }

    fun clamp(leftW: Float, winH: Float) {
        if (!x.isFinite() || !y.isFinite() || !zoom.isFinite()) {
            reset()
            return
        }
        zoom = zoom.coerceIn(1.0, 6.0)
        val fit = fitScale(leftW, winH)
        val s = fit * zoom
        if (s <= 0.0) return
        val halfW = (leftW / 2.0) / s
        val halfH = (winH / 2.0) / s
        val worldW = SimConfig.width.toDouble()
        val worldH = SimConfig.height.toDouble()
        x = if (halfW * 2.0 >= worldW) worldW / 2.0 else x.coerceIn(halfW, worldW - halfW)
        y = if (halfH * 2.0 >= worldH) worldH / 2.0 else y.coerceIn(halfH, worldH - halfH)
    }

    fun fitScale(leftW: Float, winH: Float): Double {
        val sx = leftW / SimConfig.width.toDouble()
        val sy = winH / SimConfig.height.toDouble()
        return if (sx < sy) sx else sy
    }

    fun screenToWorld(px: Float, py: Float, leftW: Float, winH: Float): Pair<Double, Double> {
        val s = fitScale(leftW, winH) * zoom
        val wx = x + (px - leftW / 2.0) / s
        val wy = y + (py - winH / 2.0) / s
        return wx to wy
    }
}
