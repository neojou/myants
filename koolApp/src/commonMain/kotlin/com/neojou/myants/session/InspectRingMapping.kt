package com.neojou.myants.session

import com.neojou.myants.render3d.InspectRing
import com.neojou.myants.render3d.LEAF_RING
import com.neojou.myants.render3d.PHEROMONE_RING
import com.neojou.myants.render3d.QUEEN_RING
import com.neojou.myants.render3d.RingSpace
import com.neojou.myants.render3d.broodRingScale
import com.neojou.myants.render3d.chamberRingScale
import com.neojou.myants.render3d.foodRingScale
import com.neojou.myants.render3d.grassRingScale
import com.neojou.myants.render3d.insectRingScale
import com.neojou.myants.render3d.nestAntRingScale
import com.neojou.myants.render3d.stoneRingScale
import com.neojou.myants.render3d.worldAntRingScale

/**
 * Inspect hit to selection ring. Lives in session so render3d never sees [InspectTarget] (#47).
 * render3d keeps [InspectRing], [RingSpace] and the ring scale functions.
 */
internal fun InspectTarget.toRing(): InspectRing = when (val target = this) {
    is InspectTarget.WorldAnt -> InspectRing(target.subject.x, target.subject.y, worldAntRingScale(target.subject.sizeVar))
    is InspectTarget.FoodItem -> InspectRing(target.food.x, target.food.y, foodRingScale(target.food.amount))
    is InspectTarget.InsectItem -> InspectRing(target.insect.x, target.insect.y, insectRingScale(target.insect.type))
    is InspectTarget.NestAnt -> InspectRing(
        target.subject.x,
        target.subject.y,
        nestAntRingScale(target.subject.sizeVar),
        RingSpace.NEST,
    )
    is InspectTarget.QueenTarget -> {
        val queen = target.colony.queen
        InspectRing(queen.x, queen.y, QUEEN_RING, RingSpace.NEST)
    }
    is InspectTarget.BroodItem -> InspectRing(
        target.brood.x,
        target.brood.y,
        broodRingScale(target.brood.stage),
        RingSpace.NEST,
    )
    is InspectTarget.ChamberItem -> InspectRing(
        target.chamber.x,
        target.chamber.y,
        chamberRingScale(target.chamber.radius),
        RingSpace.NEST,
    )
    is InspectTarget.StoneItem -> InspectRing(target.stone.x, target.stone.y, stoneRingScale(target.stone.radius))
    is InspectTarget.GrassItem -> InspectRing(target.tuft.x, target.tuft.y, grassRingScale(target.tuft.size))
    is InspectTarget.LeafItem -> InspectRing(target.x.toDouble(), target.y.toDouble(), LEAF_RING)
    is InspectTarget.PheromoneSample -> InspectRing(target.x, target.y, PHEROMONE_RING)
}
