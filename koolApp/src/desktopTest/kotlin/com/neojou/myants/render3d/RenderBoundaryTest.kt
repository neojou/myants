package com.neojou.myants.render3d

import de.fabmax.kool.util.Color
import kotlin.test.Test
import kotlin.test.assertFalse

/** render3d's bloom gate works off a fake [ViewFlags], no session needed (#47). */
class RenderBoundaryTest {
    private class Flags(override val bloomUser: Boolean, override val noBloom: Boolean) : ViewFlags {
        override val showPheromones = false
        override val pheromoneStrength = 0.0
        override val antColor = Color.WHITE
        override val dayNight = false
        override val ssaoUser = false
        override val shadowsUser = false
        override val weatherUser = false
        override val grassDensity = 1f
    }

    @Test
    fun noBloomOrUserOffBlocksBloom() {
        assertFalse(bloomAllowed(Flags(bloomUser = false, noBloom = false)))
        assertFalse(bloomAllowed(Flags(bloomUser = true, noBloom = true)))
    }

    @Test
    fun noContextMeansNoBloom() {
        // Headless: no kool context, so the platform gate says no even with the user switch on.
        assertFalse(bloomAllowed(Flags(bloomUser = true, noBloom = false)))
    }
}
