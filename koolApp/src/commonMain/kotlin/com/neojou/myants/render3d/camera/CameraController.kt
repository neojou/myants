package com.neojou.myants.render3d.camera

import com.neojou.myants.render3d.CameraView
import com.neojou.myants.render3d.RenderHost
import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.scene.Scene

enum class CameraMode { TOP_DOWN, CINEMA, FOLLOW, ANT_POV }

/**
 * Only [CameraMode.ANT_POV] may rotate. That mode is reserved and is not attached.
 * Cinema and follow write the camera numbers upstream; the active controller only
 * applies [CameraView] to the scene camera. Clamping goes through [RenderHost.clampCamera].
 */
interface CameraController {
    val id: CameraMode
    val allowsRotation: Boolean
    fun attach(scene: Scene)
    fun detach(scene: Scene)
    fun update(dt: Float, cam: CameraView, host: RenderHost)
    fun focusOn(worldX: Float, worldZ: Float, zoom: Float? = null)
    fun screenToGround(px: Float, py: Float): Vec2f?
}
