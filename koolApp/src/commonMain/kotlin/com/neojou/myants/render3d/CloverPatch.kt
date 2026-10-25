package com.neojou.myants.render3d

import de.fabmax.kool.util.Color

/** About two patches in five. The same point always answers the same way, without the sim RNG. */
internal fun cloverBlooms(x: Float, y: Float): Boolean = cloverHash(x, y) < 0.4f

/** Bloom colour for the clover at (x, y): rose, gold or lilac, fixed per spot. */
internal fun cloverPetal(x: Float, y: Float): Color {
    val petals = WorldPalette.cloverPetals
    // Blooms use [0, 0.4); spread that range over the three colours.
    val i = (cloverHash(x, y) / 0.4f * petals.size).toInt().coerceIn(0, petals.size - 1)
    return petals[i]
}

private fun cloverHash(x: Float, y: Float): Float {
    val ix = (x * 10f).toInt()
    val iy = (y * 10f).toInt()
    var h = ix * 374761393 + iy * 668265263
    h = (h xor (h ushr 13)) * 1274126177
    return ((h ushr 16) and 0xffff) / 65535f
}
