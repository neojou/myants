package com.neojou.myants.render3d.actors

import com.neojou.myants.render3d.WorldPalette
import com.neojou.myants.sim.InsectKind
import kotlin.test.Test
import kotlin.test.assertEquals

class CritterTintTest {
    @Test
    fun ladybugShellIsScarletOnlyInTheLitView() {
        val palette = WorldPalette.insect(InsectKind.LADYBUG)
        assertEquals(0.8f, palette.r, 1e-4f)
        assertEquals(0.15f, palette.g, 1e-4f)
        assertEquals(0.15f, palette.b, 1e-4f)
        val tint = critterTint(InsectKind.LADYBUG)
        assertEquals(1.0f, tint.body.r, 1e-4f)
        assertEquals(0.02f, tint.body.g, 1e-4f)
        assertEquals(0.02f, tint.body.b, 1e-4f)
        assertEquals(0.03f, tint.mark.r, 1e-4f)
        assertEquals(0.02f, tint.mark.g, 1e-4f)
        assertEquals(0.02f, tint.mark.b, 1e-4f)
    }
}
