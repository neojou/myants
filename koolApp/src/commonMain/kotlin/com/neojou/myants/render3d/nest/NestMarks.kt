package com.neojou.myants.render3d.nest

import com.neojou.myants.sim.BroodStage

/** Chamber cut is wider than the floor disk. The ring is the dug edge. */
internal const val CHAMBER_RIM_SCALE = 1.18f

/** Tunnel disks stay inside the sim radius so the chain reads as a passage. */
internal const val TUNNEL_DRAW_SCALE = 0.78f

/** Lower share of the soil panel, drawn a step darker. */
internal const val SOIL_BAND_FRACTION = 0.62f

/** Carry crumb sits ahead of the head, as a fraction of body length. */
internal const val NEST_CARRY_ALONG = 0.52f

internal fun chamberRimDiameter(floorDiameter: Float): Float = floorDiameter * CHAMBER_RIM_SCALE

internal fun tunnelDrawDiameter(nodeDiameter: Float): Float = nodeDiameter * TUNNEL_DRAW_SCALE

internal data class NestSpan(val length: Float, val width: Float)

/**
 * Egg is a small circle, larva a short capsule, pupa a larger oval.
 * [jitter] is 0..1 from [broodJitter], not the sim RNG.
 */
internal fun broodSpan(stage: BroodStage, fit: Float, jitter: Float): NestSpan {
    val j = 0.92f + jitter.coerceIn(0f, 1f) * 0.16f
    return when (stage) {
        BroodStage.EGG -> NestSpan(3.2f * fit * j, 3.2f * fit * j)
        BroodStage.LARVA -> NestSpan(7.2f * fit * j, 3.1f * fit * j)
        BroodStage.PUPA -> NestSpan(8.4f * fit * j, 5.2f * fit * j)
    }
}

/** Stable 0..1 mix of a brood position. Same point always returns the same size. */
internal fun broodJitter(x: Float, y: Float): Float {
    val ix = (x * 1000f).toInt()
    val iy = (y * 1000f).toInt()
    var h = ix * 374761393 + iy * 668265263
    h = (h xor (h ushr 13)) * 1274126177
    return ((h ushr 16) and 0xffff) / 65535f
}

/** A mark shifted along the facing axis. Positive [along] is toward the head. */
internal data class NestPart(val along: Float, val length: Float, val width: Float)

internal data class NestAntShape(val gaster: NestPart, val thorax: NestPart, val head: NestPart)

/** Fractions of body length. A soldier keeps the body and grows the head. */
internal fun nestAntParts(soldier: Boolean): NestAntShape {
    val gaster = NestPart(-0.26f, 0.40f, 0.28f)
    val thorax = NestPart(0.04f, 0.22f, 0.15f)
    val head = if (soldier) NestPart(0.29f, 0.30f, 0.26f) else NestPart(0.25f, 0.22f, 0.18f)
    return NestAntShape(gaster, thorax, head)
}

/** Screen-space queen marks. The gaster sits behind a longer body. Sizes already include [fit]. */
internal data class NestMark(val dx: Float, val w: Float, val h: Float)

internal fun queenMarks(fit: Float): List<NestMark> = listOf(
    NestMark(-11f * fit, 16f * fit, 14f * fit),
    NestMark(6f * fit, 18f * fit, 8f * fit),
)
