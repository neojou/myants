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
 * overhead camera sees edge-on. Straight down, screen-up is world −Z, so the
 * high end of the digit moves toward −Z. [thickness] stays up.
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
    z = digitZ - (segY - digitY),
    sx = sx,
    sy = thickness,
    sz = sy,
)

/**
 * Seven-segment masks. Bit 0 is the top bar, then upper-right, lower-right,
 * bottom, lower-left, upper-left, and the middle bar.
 */
internal val CLOCK_SEGMENTS = intArrayOf(
    0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7D, 0x07, 0x7F, 0x6F,
)

/** Shared proportions for the stone digits and the Classic 2D clock. */
internal data class ClockMetrics(
    val digitWidth: Float,
    val digitHeight: Float,
    val gap: Float,
    val colonSlot: Float,
    val thickness: Float,
    val dotSize: Float,
) {
    /** Width of four digits, the gaps around them, and the colon slot. */
    val span: Float get() = digitWidth * 4f + gap * 4f + colonSlot
}

internal fun clockMetrics(fontSize: Float): ClockMetrics {
    val h = fontSize
    return ClockMetrics(
        digitWidth = h * 0.62f,
        digitHeight = h,
        gap = h * 0.18f,
        colonSlot = h * 0.35f,
        thickness = (h * 0.12f).coerceAtLeast(1f),
        dotSize = h * 0.12f,
    )
}

/** Distance from the digit center to the middle of one 日 cell. Positive is the upper cell. */
internal fun clockCellAlong(fontSize: Float): Float = fontSize * 0.25f

/** HH:MM centers. The colon x does not depend on whether the colon is lit. */
internal class ClockSlots(
    val hourTens: Float,
    val hourOnes: Float,
    val minuteTens: Float,
    val minuteOnes: Float,
    val colonX: Float,
)

/**
 * Centers the face on [originX]. The colon is the midpoint of the hour-ones
 * center and the minute-tens center, with the same gap on each side. The slot
 * stays put when the colon is dark, so the digits do not jump.
 */
internal fun clockSlots(originX: Float, metrics: ClockMetrics): ClockSlots {
    val dw = metrics.digitWidth
    val gap = metrics.gap
    val slot = metrics.colonSlot
    val hourTens = originX - metrics.span * 0.5f + dw * 0.5f
    val hourOnes = hourTens + dw + gap
    val colonX = hourOnes + dw * 0.5f + gap + slot * 0.5f
    val minuteTens = colonX + slot * 0.5f + gap + dw * 0.5f
    val minuteOnes = minuteTens + dw + gap
    return ClockSlots(hourTens, hourOnes, minuteTens, minuteOnes, colonX)
}

/** One bar in standing coordinates. Larger [y] is the top of the digit. */
internal data class ClockStroke(
    val x: Float,
    val y: Float,
    val sx: Float,
    val sy: Float,
)

/**
 * 日 grid: three horizontal bars and two stacked cells. Each vertical is one
 * cell, so the upper and lower verticals meet and a 1 or 7 spans the full height.
 */
internal fun clockStrokes(cx: Float, cy: Float, metrics: ClockMetrics, digit: Int): List<ClockStroke> {
    val mask = CLOCK_SEGMENTS[digit.coerceIn(0, 9)]
    val h = metrics.digitHeight
    val w = metrics.digitWidth
    val t = metrics.thickness
    val hw = w * 0.5f
    val cell = h * 0.5f
    val out = ArrayList<ClockStroke>(7)
    if (mask and 0x01 != 0) out += ClockStroke(cx, cy + h * 0.5f - t * 0.5f, w, t)
    if (mask and 0x02 != 0) out += ClockStroke(cx + hw - t * 0.5f, cy + cell * 0.5f, t, cell)
    if (mask and 0x04 != 0) out += ClockStroke(cx + hw - t * 0.5f, cy - cell * 0.5f, t, cell)
    if (mask and 0x08 != 0) out += ClockStroke(cx, cy - h * 0.5f + t * 0.5f, w, t)
    if (mask and 0x10 != 0) out += ClockStroke(cx - hw + t * 0.5f, cy - cell * 0.5f, t, cell)
    if (mask and 0x20 != 0) out += ClockStroke(cx - hw + t * 0.5f, cy + cell * 0.5f, t, cell)
    if (mask and 0x40 != 0) out += ClockStroke(cx, cy, w, t)
    return out
}

/** A colon cube. Positive [along] is the upper dot, toward screen-up (−Z). */
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
    z = digitZ - along,
    sx = size,
    sy = thickness,
    sz = size,
)
