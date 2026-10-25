package com.neojou.myants.render3d.camera

import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.math.Vec3f
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Ground hit for the top-down orthographic camera.
 *
 * kool 0.19 [de.fabmax.kool.scene.Camera.unProjectScreen] treats reversed depth as an
 * infinite perspective projection. On the Vulkan ortho camera that makes screen depth 0
 * a zero homogeneous w, and [de.fabmax.kool.scene.Camera.computePickRay] returns a NaN ray.
 * Writing that hit into the app camera leaves the deferred g-buffer empty, so the
 * lighting pass stays black.
 */
internal object OrthoGround {
    fun hit(
        px: Float,
        py: Float,
        vpX: Float,
        vpY: Float,
        vpW: Float,
        vpH: Float,
        left: Float,
        right: Float,
        bottom: Float,
        top: Float,
        eye: Vec3f,
        lookAt: Vec3f,
        up: Vec3f,
        groundY: Float,
    ): Vec2f? {
        if (vpW < 2f || vpH < 2f) return null
        val ndcX = 2f * (px - vpX) / vpW - 1f
        val ndcY = 2f * (vpY + vpH - py) / vpH - 1f
        val camX = (left + right) * 0.5f + ndcX * (right - left) * 0.5f
        val camY = (bottom + top) * 0.5f + ndcY * (top - bottom) * 0.5f

        var fx = lookAt.x - eye.x
        var fy = lookAt.y - eye.y
        var fz = lookAt.z - eye.z
        val fl = sqrt(fx * fx + fy * fy + fz * fz)
        if (fl < 1e-6f || !fl.isFinite()) return null
        fx /= fl
        fy /= fl
        fz /= fl

        var ux = up.x
        var uy = up.y
        var uz = up.z
        val ul = sqrt(ux * ux + uy * uy + uz * uz)
        if (ul < 1e-6f || !ul.isFinite()) return null
        ux /= ul
        uy /= ul
        uz /= ul

        // Same right axis as kool's lookAt: forward × up.
        var rx = fy * uz - fz * uy
        var ry = fz * ux - fx * uz
        var rz = fx * uy - fy * ux
        val rl = sqrt(rx * rx + ry * ry + rz * rz)
        if (rl < 1e-6f || !rl.isFinite()) return null
        rx /= rl
        ry /= rl
        rz /= rl

        val ox = eye.x + rx * camX + ux * camY
        val oy = eye.y + ry * camX + uy * camY
        val oz = eye.z + rz * camX + uz * camY
        if (abs(fy) < 1e-5f) return null
        val t = (groundY - oy) / fy
        if (!t.isFinite() || t < 0f) return null
        val hx = ox + fx * t
        val hz = oz + fz * t
        if (!hx.isFinite() || !hz.isFinite()) return null
        return Vec2f(hx, hz)
    }
}
