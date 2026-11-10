package com.neojou.myants.render3d

import de.fabmax.kool.util.Color
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** #94: default shell albedo and non-default scale under correct outward normals. */
class AntChitinAlbedoTest {
    @Test
    fun defaultAntChitinIsArtistTarget() {
        val c = WorldPalette.antChitin
        assertEquals(0.036f, c.r)
        assertEquals(0.036f, c.g)
        assertEquals(0.039f, c.b)
        assertTrue(c.r in 0.035f..0.038f)
    }

    @Test
    fun chitinAlbedoScaleMatchesDefaultDrop() {
        assertEquals(0.52f, WorldPalette.CHITIN_ALBEDO_SCALE)
        assertTrue(abs(WorldPalette.CHITIN_ALBEDO_SCALE - WorldPalette.antChitin.r / 0.07f) < 0.01f)
    }

    @Test
    fun outdoorChitinDefaultUsesAntChitin() {
        assertEquals(WorldPalette.antChitin, WorldPalette.outdoorChitin(WorldPalette.worker))
    }

    @Test
    fun outdoorChitinNonDefaultAppliesScale() {
        val swatch = Color(0.2f, 0.4f, 0.8f, 1f)
        val linear = swatch.toLinear()
        val s = WorldPalette.CHITIN_ALBEDO_SCALE
        val got = WorldPalette.outdoorChitin(swatch)
        assertEquals(linear.r * s, got.r)
        assertEquals(linear.g * s, got.g)
        assertEquals(linear.b * s, got.b)
    }
}
