package com.neojou.myants.render3d

import de.fabmax.kool.math.Vec3f
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * One conversion from sim space to the y-up world.
 * Sim (x, y-down) maps to (X, Y-up, Z = y). Angle 0 faces +X.
 * A mesh whose local front is +X uses [yawRad], which is `-angle`.
 */
object WorldFrames {
    fun travel(angleRad: Float): Vec3f = Vec3f(cos(angleRad), 0f, sin(angleRad))

    /** Right-handed yaw around Y that sends local +X onto [travel]. */
    fun yawRad(angleRad: Float): Float = -angleRad

    /** Pitch is degrees below the horizon. -90 looks straight down. */
    fun lookDir(pitchDeg: Float): Vec3f {
        val pitchRad = pitchDeg * (PI.toFloat() / 180f)
        return Vec3f(0f, sin(pitchRad), -cos(pitchRad))
    }

    /**
     * Screen-up hint for the locked top-down camera.
     * At -90 degrees this is (0, 0, -1), so sim +y stays at the bottom of the picture.
     * Otherwise it is world +X crossed with [lookDir].
     */
    fun screenUp(pitchDeg: Float): Vec3f {
        if (abs(pitchDeg + 90f) < 1f) return Vec3f(0f, 0f, -1f)
        val look = lookDir(pitchDeg)
        return Vec3f(
            0f,
            -look.z,
            look.y,
        )
    }
}
