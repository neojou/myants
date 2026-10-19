package com.neojou.myants.render3d.camera

import com.neojou.myants.AppSession
import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.scene.Scene

enum class CameraMode { TOP_DOWN, CINEMA, FOLLOW, ANT_POV }

/**
 * Only [CameraMode.ANT_POV] may rotate. That mode is reserved and is not attached.
 * Cinema and follow write [AppSession.camX] / [AppSession.camY] / [AppSession.zoom].
 * The active controller only applies those numbers to the scene camera.
 */
interface CameraController {
    val id: CameraMode
    val allowsRotation: Boolean
    fun attach(scene: Scene)
    fun detach(scene: Scene)
    fun update(dt: Float, session: AppSession)
    fun focusOn(worldX: Float, worldZ: Float, zoom: Float? = null)
    fun screenToGround(px: Float, py: Float): Vec2f?
}
