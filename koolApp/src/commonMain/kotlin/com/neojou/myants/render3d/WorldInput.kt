package com.neojou.myants.render3d

import de.fabmax.kool.math.Vec2f

/** Ground pick used by the input scene. Null until the 3D camera is attached. */
object WorldInput {
    var screenToGround: ((Float, Float) -> Vec2f?)? = null

    /** World (x, y) on the ground to window pixels. Null until the 3D camera is attached. */
    var groundToScreen: ((Float, Float) -> Vec2f?)? = null
}
