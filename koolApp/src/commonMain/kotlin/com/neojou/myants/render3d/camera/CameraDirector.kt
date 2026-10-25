package com.neojou.myants.render3d.camera

import com.neojou.tools.LogLevel
import com.neojou.tools.MyLog

/**
 * Normal play uses [CameraMode.TOP_DOWN]. Cinema and follow write the session
 * camera numbers; the top-down controller only applies them.
 * [CameraMode.ANT_POV] is reserved and is never attached.
 */
class CameraDirector(
    private val topDown: CameraController,
) {
    var active: CameraController = topDown
        private set

    fun request(mode: CameraMode): Boolean {
        if (mode == CameraMode.ANT_POV) {
            MyLog.add("RENDER", "Ant POV is reserved and is not attached", LogLevel.DEBUG)
            return false
        }
        active = topDown
        return mode == CameraMode.TOP_DOWN || mode == CameraMode.CINEMA || mode == CameraMode.FOLLOW
    }
}

/** Stub. The director refuses to attach this. */
class AntPovCamera : CameraController {
    override val id: CameraMode = CameraMode.ANT_POV
    override val allowsRotation: Boolean = true
    override fun attach(scene: de.fabmax.kool.scene.Scene) = Unit
    override fun detach(scene: de.fabmax.kool.scene.Scene) = Unit
    override fun update(dt: Float, cam: com.neojou.myants.render3d.CameraView, host: com.neojou.myants.render3d.RenderHost) = Unit
    override fun focusOn(worldX: Float, worldZ: Float, zoom: Float?) = Unit
    override fun screenToGround(px: Float, py: Float): de.fabmax.kool.math.Vec2f? = null
}
