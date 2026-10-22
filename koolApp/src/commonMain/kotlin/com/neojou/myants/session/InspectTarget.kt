package com.neojou.myants.session

import com.neojou.myants.sim.Ant
import com.neojou.myants.sim.AntPlace
import com.neojou.myants.sim.Brood
import com.neojou.myants.sim.Chamber
import com.neojou.myants.sim.Colony
import com.neojou.myants.sim.Food
import com.neojou.myants.sim.GrassTuft
import com.neojou.myants.sim.Insect
import com.neojou.myants.sim.Obstacle
import com.neojou.myants.sim.World
import com.neojou.myants.ui.InspectCopy
import com.neojou.myants.ui.InspectPanel

/**
 * Read-only pick. The hit test stores one of these; it does not write the sim.
 * [present] is false once the object has left the list the tool searched.
 */
internal sealed class InspectTarget {
    abstract val ant: Ant?
    abstract fun present(world: World): Boolean
    abstract fun line(): String
    abstract fun panel(): InspectPanel

    data class WorldAnt(val subject: Ant) : InspectTarget() {
        override val ant: Ant get() = subject
        override fun present(world: World): Boolean = world.colonies.any { colony ->
            colony.ants.any { it === subject && it.location == AntPlace.WORLD }
        }
        override fun line(): String = InspectCopy.antLine(subject)
        override fun panel(): InspectPanel = InspectCopy.antPanel(subject)
    }

    data class NestAnt(val subject: Ant) : InspectTarget() {
        override val ant: Ant get() = subject
        override fun present(world: World): Boolean = world.colonies.any { colony ->
            colony.ants.any { it === subject && it.location == AntPlace.NEST }
        }
        override fun line(): String = InspectCopy.antLine(subject)
        override fun panel(): InspectPanel = InspectCopy.antPanel(subject)
    }

    data class FoodItem(val food: Food) : InspectTarget() {
        override val ant: Ant? get() = null
        override fun present(world: World): Boolean = world.foods.any { it === food }
        override fun line(): String = InspectCopy.foodLine(food.type, food.amount.toInt())
        override fun panel(): InspectPanel = InspectCopy.foodPanel(food.type, food.amount.toInt())
    }

    data class InsectItem(val insect: Insect) : InspectTarget() {
        override val ant: Ant? get() = null
        override fun present(world: World): Boolean = world.insects.any { it === insect }
        override fun line(): String = InspectCopy.insectLine(insect.type, insect.health.toInt())
        override fun panel(): InspectPanel = InspectCopy.insectPanel(insect.type, insect.health.toInt())
    }

    data class QueenTarget(val colony: Colony) : InspectTarget() {
        override val ant: Ant? get() = null
        override fun present(world: World): Boolean = world.colonies.any { it === colony }
        override fun line(): String = InspectCopy.queenLine(colony.queen.energy.toInt(), colony.queen.age)
        override fun panel(): InspectPanel = InspectCopy.queenPanel(colony.queen.energy.toInt(), colony.queen.age)
    }

    /** An egg, larva, or pupa that is not being carried. */
    data class BroodItem(val brood: Brood, val colony: Colony) : InspectTarget() {
        override val ant: Ant? get() = null
        override fun present(world: World): Boolean =
            world.colonies.any { it === colony } && colony.brood.any { it === brood && it.carrier == null }
        override fun line(): String = InspectCopy.broodLine(brood)
        override fun panel(): InspectPanel = InspectCopy.broodPanel(brood)
    }

    data class ChamberItem(val chamber: Chamber, val colony: Colony) : InspectTarget() {
        override val ant: Ant? get() = null
        override fun present(world: World): Boolean =
            world.colonies.any { it === colony } && colony.nest.chambers.any { it === chamber }
        override fun line(): String = InspectCopy.chamberLine(chamber.type, chamber.radius.toInt())
        override fun panel(): InspectPanel = InspectCopy.chamberPanel(chamber.type, chamber.radius.toInt())
    }

    data class StoneItem(val stone: Obstacle, val clock: Boolean) : InspectTarget() {
        override val ant: Ant? get() = null
        override fun present(world: World): Boolean =
            world.terrain.obstacles.any { it === stone } || stone === world.terrain.clockStone
        override fun line(): String = InspectCopy.stoneLine(stone.radius.toInt(), clock)
        override fun panel(): InspectPanel = InspectCopy.stonePanel(stone.radius.toInt(), clock)
    }

    data class GrassItem(val tuft: GrassTuft) : InspectTarget() {
        override val ant: Ant? get() = null
        override fun present(world: World): Boolean = world.grass.any { it === tuft }
        override fun line(): String = InspectCopy.grassLine(tuft.size.toInt())
        override fun panel(): InspectPanel = InspectCopy.grassPanel(tuft.size.toInt())
    }

    /** Decorative leaf. [present] is decided by the leaf list passed into refresh. */
    data class LeafItem(val x: Float, val y: Float) : InspectTarget() {
        override val ant: Ant? get() = null
        override fun present(world: World): Boolean = false
        override fun line(): String = InspectCopy.leafLine()
        override fun panel(): InspectPanel = InspectCopy.leafPanel()
    }

    /**
     * Outdoor pheromone at a clicked point. Stays until a second empty click or a restart.
     * The ring stays on the click; the numbers are read again from the live grids.
     */
    data class PheromoneSample(val x: Double, val y: Double, val colonies: List<Colony>) : InspectTarget() {
        override val ant: Ant? get() = null
        override fun present(world: World): Boolean = world.colonies === colonies
        override fun line(): String = InspectCopy.pheromoneLine(colonies, x, y)
        override fun panel(): InspectPanel = InspectCopy.pheromonePanel(colonies, x, y)
    }
}
