package com.neojou.myants.render3d

import com.neojou.myants.session.InspectTarget
import com.neojou.myants.sim.InsectKind
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Ground ring for the current Inspect hit in the left 3D world.
 * Nest ants and the queen stay unmarked; that cutaway is a different camera.
 */
internal data class InspectRing(val x: Double, val y: Double, val scale: Float)

internal fun inspectRing(target: InspectTarget): InspectRing? = when (target) {
    is InspectTarget.WorldAnt -> InspectRing(target.subject.x, target.subject.y, worldAntRingScale(target.subject.sizeVar))
    is InspectTarget.FoodItem -> InspectRing(target.food.x, target.food.y, foodRingScale(target.food.amount))
    is InspectTarget.InsectItem -> InspectRing(target.insect.x, target.insect.y, insectRingScale(target.insect.type))
    is InspectTarget.NestAnt, is InspectTarget.QueenTarget -> null
}

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
