package com.neojou.myants.sim

import kotlin.test.Test
import kotlin.test.assertEquals

class EnvironmentTest {
    @Test
    fun dayBrightnessSchedule() {
        Rng.seed(1)
        SimConfig.colonyCount = 1
        try {
            val world = World()
            world.timeOfDay = 0.45
            assertEquals(1.0, world.dayBrightness(), 1e-9)
            world.timeOfDay = 0.9
            assertEquals(0.0, world.dayBrightness(), 1e-9)
            world.timeOfDay = 0.1
            assertEquals(0.5, world.dayBrightness(), 1e-9)
            world.timeOfDay = 0.75
            // (0.75 - 0.7) / 0.1 is not a binary 0.5; dusk still lands on the midpoint.
            assertEquals(0.5, world.dayBrightness(), 1e-9)
            world.timeOfDay = 0.9
            assertEquals(SimConfig.environment.nightActivityMin, world.activityFactor(), 1e-9)
        } finally {
            SimConfig.colonyCount = 1
        }
    }
}
