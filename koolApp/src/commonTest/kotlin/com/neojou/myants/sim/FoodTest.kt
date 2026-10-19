package com.neojou.myants.sim

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FoodTest {
    @Test
    fun harvestReducesAmount() {
        val food = Food(0.0, 0.0, FoodKind.SUGAR, 100.0)
        val taken = food.harvest(30.0)
        assertEquals(30.0, taken)
        assertEquals(70.0, food.amount)
    }

    @Test
    fun harvestClampsToAvailable() {
        val food = Food(0.0, 0.0, FoodKind.PROTEIN, 10.0)
        val taken = food.harvest(50.0)
        assertEquals(10.0, taken)
        assertEquals(0.0, food.amount)
    }

    @Test
    fun proteinDecays() {
        val food = Food(0.0, 0.0, FoodKind.PROTEIN, 100.0)
        food.update()
        assertTrue(food.amount < 100.0)
    }

    @Test
    fun corpseLifetimeIsSizeIndependent() {
        fun ticks(amount: Double): Int {
            val food = Food(0.0, 0.0, FoodKind.CORPSE, amount)
            var t = 0
            while (food.amount > 0.0 && t < 100_000) {
                food.update()
                t++
            }
            return t
        }
        val big = ticks(100.0)
        val small = ticks(10.0)
        assertTrue(kotlin.math.abs(big - small) <= 2)
        assertTrue(big < 2500)
    }

    @Test
    fun sugarDoesNotDecay() {
        val food = Food(0.0, 0.0, FoodKind.SUGAR, 100.0)
        food.update()
        assertEquals(100.0, food.amount)
    }
}
