package com.neojou.myants.session

import com.neojou.myants.render3d.LEAF_RING
import com.neojou.myants.render3d.PHEROMONE_RING
import com.neojou.myants.render3d.QUEEN_RING
import com.neojou.myants.render3d.RingSpace
import com.neojou.myants.render3d.foodPileRadius
import com.neojou.myants.render3d.insectSpan
import com.neojou.myants.sim.Ant
import com.neojou.myants.sim.AntKind
import com.neojou.myants.sim.AntPlace
import com.neojou.myants.sim.Brood
import com.neojou.myants.sim.BroodStage
import com.neojou.myants.sim.Chamber
import com.neojou.myants.sim.ChamberRole
import com.neojou.myants.sim.Colony
import com.neojou.myants.sim.EntranceSide
import com.neojou.myants.sim.Food
import com.neojou.myants.sim.FoodKind
import com.neojou.myants.sim.GrassTuft
import com.neojou.myants.sim.Insect
import com.neojou.myants.sim.InsectKind
import com.neojou.myants.sim.Nest
import com.neojou.myants.sim.Obstacle
import com.neojou.myants.sim.PheromoneGrid
import com.neojou.myants.sim.Queen
import com.neojou.myants.sim.Rng
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** InspectTarget → InspectRing 分派（#47 從 render3d 搬到 session）。 */
class InspectRingMappingTest {
    @Test
    fun worldAntKeepsTheOldScaleAndFollowsTheAnt() {
        withRngRestored {
            val ant = Ant(12.0, 34.0, AntKind.WORKER)
            ant.sizeVar = 1.25
            val ring = InspectTarget.WorldAnt(ant).toRing()
            assertEquals(12.0, ring.x)
            assertEquals(34.0, ring.y)
            assertEquals(12.5f, ring.scale)
            ant.x = 40.0
            ant.y = 8.0
            val moved = InspectTarget.WorldAnt(ant).toRing()
            assertEquals(40.0, moved.x)
            assertEquals(8.0, moved.y)
        }
    }

    @Test
    fun foodRingSitsOutsideThePile() {
        val food = Food(5.0, 6.0, FoodKind.SUGAR, 1.0)
        val small = InspectTarget.FoodItem(food).toRing()
        assertEquals(5.0, small.x)
        assertEquals(6.0, small.y)
        assertEquals(6f * 1.35f, small.scale)
        assertTrue(small.scale > foodPileRadius(food.amount))

        val big = Food(1.0, 2.0, FoodKind.PROTEIN, 400.0)
        val radius = foodPileRadius(big.amount)
        val ring = InspectTarget.FoodItem(big).toRing()
        assertEquals(radius * 1.35f, ring.scale)
        assertTrue(ring.scale > radius)
    }

    @Test
    fun insectsUseTheAntLengthRatio() {
        withRngRestored {
            val aphid = Insect(3.0, 4.0, InsectKind.APHID)
            val prey = Insect(7.0, 8.0, InsectKind.PREY)
            assertEquals(8f * 10f / 12f, InspectTarget.InsectItem(aphid).toRing().scale)
            assertEquals(3.0, InspectTarget.InsectItem(aphid).toRing().x)
            assertEquals(4.0, InspectTarget.InsectItem(aphid).toRing().y)
            assertEquals(10f * 10f / 12f, InspectTarget.InsectItem(prey).toRing().scale)
            assertEquals(7.0, InspectTarget.InsectItem(prey).toRing().x)
            for (kind in InsectKind.entries) {
                val insect = Insect(1.0, 2.0, kind)
                val ring = InspectTarget.InsectItem(insect).toRing()
                assertEquals(insectSpan(kind) * 10f / 12f, ring.scale)
            }
        }
    }

    @Test
    fun nestHitsUseTheNestRing() {
        withRngRestored {
            val ant = Ant(9.0, 11.0, AntKind.WORKER)
            ant.location = AntPlace.NEST
            ant.sizeVar = 1.5
            val nestAnt = InspectTarget.NestAnt(ant).toRing()
            assertEquals(RingSpace.NEST, nestAnt.space)
            assertEquals(9.0, nestAnt.x)
            assertEquals(11.0, nestAnt.y)
            assertEquals(15f, nestAnt.scale)

            val colony = Colony(0, Nest(), Queen(), PheromoneGrid(10.0, 10.0), EntranceSide.RIGHT)
            val queen = InspectTarget.QueenTarget(colony).toRing()
            assertEquals(RingSpace.NEST, queen.space)
            assertEquals(colony.queen.x, queen.x)
            assertEquals(colony.queen.y, queen.y)
            assertEquals(QUEEN_RING, queen.scale)

            val egg = Brood(3.0, 4.0)
            val larva = Brood(5.0, 6.0).apply { stage = BroodStage.LARVA }
            assertEquals(3.2f, InspectTarget.BroodItem(egg, colony).toRing().scale)
            assertEquals(RingSpace.NEST, InspectTarget.BroodItem(egg, colony).toRing().space)
            assertEquals(5f, InspectTarget.BroodItem(larva, colony).toRing().scale)

            val room = Chamber(8.0, 9.0, 30.0, ChamberRole.STORAGE)
            val chamber = InspectTarget.ChamberItem(room, colony).toRing()
            assertEquals(RingSpace.NEST, chamber.space)
            assertEquals(8.0, chamber.x)
            assertEquals(9.0, chamber.y)
            assertEquals(30f * 1.05f, chamber.scale)
        }
    }

    @Test
    fun terrainRingsStayOnTheWorld() {
        val stone = InspectTarget.StoneItem(Obstacle(10.0, 20.0, 25.0), clock = false).toRing()
        assertEquals(RingSpace.WORLD, stone.space)
        assertEquals(10.0, stone.x)
        assertEquals(20.0, stone.y)
        assertEquals(25f * 1.5f, stone.scale)

        val grass = InspectTarget.GrassItem(GrassTuft(1.0, 2.0, 3.2, 0.0)).toRing()
        assertEquals(3.2f * 2f, grass.scale)
        assertEquals(RingSpace.WORLD, grass.space)

        val leaf = InspectTarget.LeafItem(4f, 5f).toRing()
        assertEquals(4.0, leaf.x)
        assertEquals(5.0, leaf.y)
        assertEquals(LEAF_RING, leaf.scale)

        val sample = InspectTarget.PheromoneSample(6.0, 7.0, emptyList()).toRing()
        assertEquals(6.0, sample.x)
        assertEquals(7.0, sample.y)
        assertEquals(PHEROMONE_RING, sample.scale)
        assertEquals(RingSpace.WORLD, sample.space)
    }

    private fun withRngRestored(block: () -> Unit) {
        val saved = Rng.getState()
        try {
            block()
        } finally {
            Rng.setState(saved)
        }
    }
}
