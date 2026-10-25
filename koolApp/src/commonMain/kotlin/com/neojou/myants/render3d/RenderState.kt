package com.neojou.myants.render3d

import com.neojou.myants.sim.World
import de.fabmax.kool.util.Color

/**
 * What render3d reads each frame (#47). Getters are live, not a snapshot.
 * render3d must not import session, ui or input; it only sees this and [RenderHost].
 */
interface RenderState {
    /** Treat as read-only. */
    val world: World
    val seed: Int
    val worldSerial: Int
    val renderer: RenderMode
    val renderTierBias: Int
    val fpsShown: Int
    val view: ViewFlags
    val camera: CameraView
    /** Ring for the current Inspect hit, or null. A new object on every read. */
    val inspectRing: InspectRing?
}

/** User view switches. */
interface ViewFlags {
    val showPheromones: Boolean
    val pheromoneStrength: Double
    val antColor: Color
    val dayNight: Boolean
    val bloomUser: Boolean
    val noBloom: Boolean
    val ssaoUser: Boolean
    val shadowsUser: Boolean
    val weatherUser: Boolean
    val grassDensity: Float
}

/** Camera numbers the top-down camera applies. Clamping is a command on [RenderHost]. */
interface CameraView {
    val x: Double
    val y: Double
    val zoom: Double
    val pitchDeg: Float
    fun fitScale(leftW: Float, winH: Float): Double
}
