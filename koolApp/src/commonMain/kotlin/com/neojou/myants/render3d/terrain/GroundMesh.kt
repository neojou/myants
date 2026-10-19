package com.neojou.myants.render3d.terrain

/**
 * Index order for one heightfield cell. The winding is counter-clockwise
 * when seen from above, so the geometric normal points +Y.
 */
object GroundMesh {
    fun emitCell(ix: Int, iz: Int, cols: Int, add: (Int, Int, Int) -> Unit) {
        val v00 = iz * cols + ix
        val v10 = v00 + 1
        val v01 = v00 + cols
        val v11 = v01 + 1
        add(v00, v01, v10)
        add(v10, v01, v11)
    }

    /** Y component of the cross product (b - a) × (c - a). */
    fun normalY(
        ax: Float, ay: Float, az: Float,
        bx: Float, by: Float, bz: Float,
        cx: Float, cy: Float, cz: Float,
    ): Float {
        val e1x = bx - ax
        val e1y = by - ay
        val e1z = bz - az
        val e2x = cx - ax
        val e2y = cy - ay
        val e2z = cz - az
        return e1z * e2x - e1x * e2z
    }
}
