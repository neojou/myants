package com.neojou.myants.render3d.camera

import com.neojou.myants.AppSession
import com.neojou.myants.render3d.WorldFrames
import com.neojou.myants.render3d.terrain.GroundHeight
import de.fabmax.kool.math.RayF
import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.scene.OrthographicCamera
import de.fabmax.kool.scene.Scene
import kotlin.math.abs

/**
 * Locked top-down (or a fixed tilt). Dragging never changes yaw.
 * Pitch -90 looks straight down. The default -70 shows the sides of grass and ants.
 * [OrthographicCamera.isKeepAspectRatio] stays false so the left 74% pane can own the focus.
 */
class TopDownCamera(
    private val camera: OrthographicCamera,
) : CameraController {
    override val id: CameraMode = CameraMode.TOP_DOWN
    override val allowsRotation: Boolean = false

    private var scene: Scene? = null
    private val ray = RayF()
    private var winW = 1f
    private var winH = 1f

    override fun attach(scene: Scene) {
        this.scene = scene
        camera.isKeepAspectRatio = false
        camera.isClipToViewport = false
        camera.clipNear = 1f
        camera.clipFar = 8000f
    }

    override fun detach(scene: Scene) {
        if (this.scene === scene) this.scene = null
    }

    override fun update(dt: Float, session: AppSession) {
        val host = scene ?: return
        winW = host.mainRenderPass.viewport.width.toFloat().coerceAtLeast(1f)
        winH = host.mainRenderPass.viewport.height.toFloat().coerceAtLeast(1f)
        if (winW < 2f) {
            // Viewport is empty until the first collect. Fall back to the window.
            winW = host.mainRenderPass.viewport.width.toFloat()
        }
        val window = host.mainRenderPass.viewport
        if (window.width < 2 || window.height < 2) {
            val ctx = de.fabmax.kool.KoolSystem.getContextOrNull() ?: return
            winW = ctx.window.size.x.toFloat().coerceAtLeast(1f)
            winH = ctx.window.size.y.toFloat().coerceAtLeast(1f)
        } else {
            winW = window.width.toFloat()
            winH = window.height.toFloat()
        }

        val leftW = winW * 0.74f
        session.clampCamera(leftW, winH)
        val zoom = session.zoom.toFloat().coerceIn(1f, 6f)
        val fit = session.fitScale(leftW, winH).toFloat()
        val pxPerUnit = (fit * zoom).coerceAtLeast(0.0001f)
        val unitPerPx = 1f / pxPerUnit
        val viewH = winH * unitPerPx
        val viewW = winW * unitPerPx
        val focusX = session.camX.toFloat()
        val focusZ = session.camY.toFloat()
        val fx = (leftW * 0.5f) / winW
        camera.left = -viewW * fx
        camera.right = viewW * (1f - fx)
        camera.bottom = -viewH * 0.5f
        camera.top = viewH * 0.5f

        val pitch = session.cameraPitchDeg.coerceIn(-90f, -60f)
        val look = WorldFrames.lookDir(pitch)
        val up = WorldFrames.screenUp(pitch)
        val ground = GroundHeight.at(focusX, focusZ)
        val distance = (viewH * 1.2f).coerceIn(200f, 4000f)
        camera.lookAt.set(focusX, ground, focusZ)
        camera.position.set(
            focusX - look.x * distance,
            ground - look.y * distance,
            focusZ - look.z * distance,
        )
        camera.up.set(up)
    }

    override fun focusOn(worldX: Float, worldZ: Float, zoom: Float?) {
        // The session owns the numbers. Callers write them before update().
    }

    override fun screenToGround(px: Float, py: Float): Vec2f? {
        val host = scene ?: return null
        if (!camera.computePickRay(ray, px, py, host.mainRenderPass.viewport)) return null
        val dirY = ray.direction.y
        if (abs(dirY) < 1e-5f) return null
        val ground = GroundHeight.at(camera.lookAt.x, camera.lookAt.z)
        val t = (ground - ray.origin.y) / dirY
        if (t < 0f) return null
        val x = ray.origin.x + ray.direction.x * t
        val z = ray.origin.z + ray.direction.z * t
        return Vec2f(x, z)
    }
}
