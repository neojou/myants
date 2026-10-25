package com.neojou.myants.render3d

import com.neojou.myants.sim.BroodStage
import com.neojou.myants.sim.InsectKind
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Selection ring for the current Inspect hit.
 * [RingSpace.WORLD] is the left ground ring. [RingSpace.NEST] is drawn by the nest cutaway.
 */
enum class RingSpace { WORLD, NEST }

data class InspectRing(
    val x: Double,
    val y: Double,
    val scale: Float,
    val space: RingSpace = RingSpace.WORLD,
)

/** Same scale the world ant ring has always used. */
internal fun worldAntRingScale(sizeVar: Double): Float = 10f * sizeVar.toFloat()

/** Half-extent of the food pile mesh. The ring sits just outside it. */
internal fun foodPileRadius(amount: Double): Float = max(6.0, sqrt(amount) * 0.85).toFloat()

internal fun foodRingScale(amount: Double): Float = foodPileRadius(amount) * 1.35f

/** Body length used by the insect meshes. Ants use 12 * sizeVar against a ring of 10 * sizeVar. */
internal fun insectSpan(type: InsectKind): Float = when (type) {
    InsectKind.PREY -> 10f
    InsectKind.APHID -> 8f
    InsectKind.LADYBUG -> 14f
    InsectKind.SPIDER -> 16f
    InsectKind.PREDATOR -> 18f
    InsectKind.BEETLE -> 22f
}

internal fun insectRingScale(type: InsectKind): Float = insectSpan(type) * 10f / 12f

/** Nest units, before the cutaway multiplies by [com.neojou.myants.render3d.nest.NestCutaway.Frame.fit]. */
internal fun nestAntRingScale(sizeVar: Double): Float = 10f * sizeVar.toFloat()

internal const val QUEEN_RING = 16f

internal fun broodRingScale(stage: BroodStage): Float = if (stage == BroodStage.EGG) 3.2f else 5f

internal fun chamberRingScale(radius: Double): Float = radius.toFloat() * 1.05f

/** Sits outside the rock mesh. The clock stone uses the same radius rule. */
internal fun stoneRingScale(radius: Double): Float = radius.toFloat() * 1.5f

internal fun grassRingScale(size: Double): Float = size.toFloat() * 2f

internal const val LEAF_RING = 4f

internal const val PHEROMONE_RING = 12f
