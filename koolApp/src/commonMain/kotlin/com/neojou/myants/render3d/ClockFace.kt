package com.neojou.myants.render3d

/** Colon is lit on even local seconds and dark on odd seconds. */
internal fun clockColonLit(second: Int): Boolean = second % 2 == 0

/** One seven-segment box after it has been laid on the stone top. */
internal data class ClockSeg(
    val x: Float,
    val y: Float,
    val z: Float,
    val sx: Float,
    val sy: Float,
    val sz: Float,
)

/**
 * Lays a standing segment onto the stone. The old height was world Y, which the
 * overhead camera sees edge-on. That height becomes world Z, and [thickness] stays up.
 */
internal fun flatClockSeg(
    digitY: Float,
    digitZ: Float,
    segX: Float,
    segY: Float,
    sx: Float,
    sy: Float,
    thickness: Float,
): ClockSeg = ClockSeg(
    x = segX,
    y = digitY,
    z = digitZ + (segY - digitY),
    sx = sx,
    sy = thickness,
    sz = sy,
)

/** A colon cube. [along] used to be a world-Y offset; it now runs across the stone. */
internal fun flatClockDot(
    x: Float,
    digitY: Float,
    digitZ: Float,
    along: Float,
    size: Float,
    thickness: Float,
): ClockSeg = ClockSeg(
    x = x,
    y = digitY,
    z = digitZ + along,
    sx = size,
    sy = thickness,
    sz = size,
)
