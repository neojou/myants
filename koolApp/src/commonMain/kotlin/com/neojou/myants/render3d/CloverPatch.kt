package com.neojou.myants.render3d

/** About two patches in five. The same point always answers the same way, without the sim RNG. */
internal fun cloverBlooms(x: Float, y: Float): Boolean {
    val ix = (x * 10f).toInt()
    val iy = (y * 10f).toInt()
    var h = ix * 374761393 + iy * 668265263
    h = (h xor (h ushr 13)) * 1274126177
    val unit = ((h ushr 16) and 0xffff) / 65535f
    return unit < 0.4f
}
