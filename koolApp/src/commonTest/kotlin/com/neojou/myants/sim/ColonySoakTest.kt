package com.neojou.myants.sim

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColonySoakTest {
    @Test
    fun rivalColonySurvives() {
        Rng.seed(42)
        SimConfig.colonyCount = 2
        try {
            val world = World()
            assertEquals(EntranceSide.RIGHT, world.colonies[0].entranceSide)
            assertEquals(EntranceSide.LEFT, world.colonies[1].entranceSide)
            assertTrue(world.colonies[0].outdoorField !== world.colonies[1].outdoorField)
            val peak = DoubleArray(2)
            repeat(12_000) {
                world.update()
                for (i in 0..1) {
                    val stock = world.colonies[i].sugarStockpile + world.colonies[i].proteinStockpile
                    if (stock > peak[i]) peak[i] = stock
                }
            }
            for (i in 0..1) {
                assertTrue(!world.colonies[i].queen.dead, "colony $i queen")
                assertTrue(world.colonies[i].brood.isNotEmpty(), "colony $i brood")
                assertTrue(peak[i] > 0.0, "colony $i stock")
            }
        } finally {
            SimConfig.colonyCount = 1
        }
    }

    @Test
    fun warfareLeavesCorpsesAndBothSides() {
        Rng.seed(42)
        SimConfig.colonyCount = 2
        try {
            val world = World()
            repeat(10_000) { world.update() }
            val corpses = world.foods.count { it.type == FoodKind.CORPSE && it.corpseType == "ANT" } +
                world.graveyard.count { it.corpseType == "ANT" }
            assertTrue(corpses > 0)
            assertTrue(world.colonies.all { it.ants.isNotEmpty() || !it.queen.dead })
        } finally {
            SimConfig.colonyCount = 1
        }
    }
}
