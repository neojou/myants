package com.neojou.myants.render3d

import com.neojou.myants.session.InspectTarget
import com.neojou.myants.sim.Ant
import com.neojou.myants.sim.AntKind
import com.neojou.myants.sim.AntPlace
import com.neojou.myants.sim.Colony
import com.neojou.myants.sim.EntranceSide
import com.neojou.myants.sim.Food
import com.neojou.myants.sim.FoodKind
import com.neojou.myants.sim.Insect
import com.neojou.myants.sim.InsectKind
import com.neojou.myants.sim.Nest
import com.neojou.myants.sim.PheromoneGrid
import com.neojou.myants.sim.Queen
import com.neojou.myants.sim.Rng
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InspectRingTest {
    @Test
    fun worldAntKeepsTheOldScaleAndFollowsTheAnt() {
        withRngRestored {
            val ant = Ant(12.0, 34.0, AntKind.WORKER)
            ant.sizeVar = 1.25
            val ring = inspectRing(InspectTarget.WorldAnt(ant))!!
            assertEquals(12.0, ring.x)
            assertEquals(34.0, ring.y)
            assertEquals(12.5f, ring.scale)
            ant.x = 40.0
            ant.y = 8.0
            val moved = inspectRing(InspectTarget.WorldAnt(ant))!!
            assertEquals(40.0, moved.x)
            assertEquals(8.0, moved.y)
        }
    }

    @Test
    fun foodRingSitsOutsideThePile() {
        val food = Food(5.0, 6.0, FoodKind.SUGAR, 1.0)
        val small = inspectRing(InspectTarget.FoodItem(food))!!
        assertEquals(5.0, small.x)
        assertEquals(6.0, small.y)
        assertEquals(6f * 1.35f, small.scale)
        assertTrue(small.scale > foodPileRadius(food.amount))

        val big = Food(1.0, 2.0, FoodKind.PROTEIN, 400.0)
        val radius = foodPileRadius(big.amount)
        val ring = inspectRing(InspectTarget.FoodItem(big))!!
        assertEquals(radius * 1.35f, ring.scale)
        assertTrue(ring.scale > radius)
    }

    @Test
    fun insectsUseTheAntLengthRatio() {
        withRngRestored {
            val aphid = Insect(3.0, 4.0, InsectKind.APHID)
            val prey = Insect(7.0, 8.0, InsectKind.PREY)
            assertEquals(8f * 10f / 12f, inspectRing(InspectTarget.InsectItem(aphid))!!.scale)
            assertEquals(3.0, inspectRing(InspectTarget.InsectItem(aphid))!!.x)
            assertEquals(4.0, inspectRing(InspectTarget.InsectItem(aphid))!!.y)
            assertEquals(10f * 10f / 12f, inspectRing(InspectTarget.InsectItem(prey))!!.scale)
            assertEquals(7.0, inspectRing(InspectTarget.InsectItem(prey))!!.x)
            for (kind in InsectKind.entries) {
                val insect = Insect(1.0, 2.0, kind)
                val ring = inspectRing(InspectTarget.InsectItem(insect))!!
                assertEquals(insectSpan(kind) * 10f / 12f, ring.scale)
            }
        }
    }

    @Test
    fun nestAntAndQueenHaveNoWorldRing() {
        withRngRestored {
            val ant = Ant(9.0, 11.0, AntKind.WORKER)
            ant.location = AntPlace.NEST
            assertNull(inspectRing(InspectTarget.NestAnt(ant)))
            val colony = Colony(0, Nest(), Queen(), PheromoneGrid(10.0, 10.0), EntranceSide.RIGHT)
            assertNull(inspectRing(InspectTarget.QueenTarget(colony)))
        }
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
