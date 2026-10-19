package com.neojou.myants.sim

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PheromoneGridTest {
    @Test
    fun scalesGrid() {
        val g = PheromoneGrid(100, 100, 0.25)
        assertEquals(0.25, g.scale, 0.0)
        assertEquals(25, g.width)
        assertEquals(25, g.height)
    }

    @Test
    fun customScale() {
        val g = PheromoneGrid(100, 100, 0.5)
        assertEquals(0.5, g.scale, 0.0)
        assertEquals(50, g.width)
        assertEquals(50, g.height)
    }

    @Test
    fun depositRoundTrip() {
        val g = PheromoneGrid(100, 100, 0.25)
        g.deposit(40.0, 40.0, Pheromone.HOME, 0.5)
        assertTrue(abs(g.get(40.0, 40.0, Pheromone.HOME) - 0.5) < 1e-5)
        assertEquals(0.0, g.get(40.0, 40.0, Pheromone.SUGAR), 0.0)
    }

    @Test
    fun ignoresOutOfBounds() {
        val g = PheromoneGrid(100, 100, 0.25)
        g.deposit(-10.0, -10.0, Pheromone.HOME, 1.0)
        assertEquals(0.0, g.get(-10.0, -10.0, Pheromone.HOME), 0.0)
        assertEquals(0.0, g.get(99999.0, 99999.0, Pheromone.HOME), 0.0)
    }

    @Test
    fun decaysByConfiguredFactor() {
        val g = PheromoneGrid(100, 100, 0.25)
        g.diffusionEnabled = false
        g.deposit(40.0, 40.0, Pheromone.HOME, 0.5)
        g.update()
        // FloatArray store rounds to float32 on both JVM and JS. Double.toFloat() does not on JS.
        val stored = FloatArray(1)
        stored[0] = (0.5 * SimConfig.pheromone.decay).toFloat()
        assertEquals(stored[0].toDouble(), g.get(40.0, 40.0, Pheromone.HOME), 0.0)
    }

    @Test
    fun clampsTinyValues() {
        val g = PheromoneGrid(100, 100, 0.25)
        g.diffusionEnabled = false
        g.deposit(40.0, 40.0, Pheromone.HOME, SimConfig.pheromone.minThreshold * 0.5)
        g.update()
        assertEquals(0.0, g.get(40.0, 40.0, Pheromone.HOME), 0.0)
    }

    @Test
    fun diffusesHomeSpike() {
        val g = PheromoneGrid(100, 100, 0.25)
        g.diffusionEnabled = true
        g.deposit(40.0, 40.0, Pheromone.HOME, 1.0)
        val centerBefore = g.get(40.0, 40.0, Pheromone.HOME)
        assertEquals(0.0, g.get(44.0, 40.0, Pheromone.HOME), 0.0)
        g.update()
        assertTrue(g.get(44.0, 40.0, Pheromone.HOME) > 0.0)
        assertTrue(g.get(40.0, 40.0, Pheromone.HOME) < centerBefore)
    }

    @Test
    fun dangerDoesNotDiffuse() {
        val g = PheromoneGrid(100, 100, 0.25)
        g.diffusionEnabled = true
        g.deposit(40.0, 40.0, Pheromone.DANGER, 1.0)
        g.update()
        assertEquals(0.0, g.get(44.0, 40.0, Pheromone.DANGER), 0.0)
    }

    @Test
    fun depositCapsAtOne() {
        val g = PheromoneGrid(100, 100, 0.25)
        g.deposit(40.0, 40.0, Pheromone.HOME, 0.8)
        g.deposit(40.0, 40.0, Pheromone.HOME, 0.8)
        assertEquals(1.0, g.get(40.0, 40.0, Pheromone.HOME), 0.0)
    }
}
