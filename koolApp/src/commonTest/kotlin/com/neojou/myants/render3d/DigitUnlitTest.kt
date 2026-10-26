package com.neojou.myants.render3d

import de.fabmax.kool.modules.ksl.KslUnlitShader
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.Node
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** #68: clock digits are unlit, so sun, moon and sky light never change their color. */
class DigitUnlitTest {
    private fun build(): Pair<Node, Node> {
        val opaque = Node("opaque")
        val forward = Node("forward")
        OverlayRenderer().build(opaque, forward)
        return opaque to forward
    }

    private fun Node.digit(): Mesh<*>? = children.filterIsInstance<Mesh<*>>().firstOrNull { it.name == "digit" }

    @Test
    fun digitsDrawUnlitInTheForwardPass() {
        val (opaque, forward) = build()
        assertNull(opaque.digit(), "digits must not go through the deferred lighting pass")
        val mesh = assertNotNull(forward.digit())
        assertTrue(mesh.shader is KslUnlitShader)
    }

    @Test
    fun digitColorIsTheArtAlbedo() {
        val c = WorldPalette.digit3d
        assertEquals(listOf(0xE9, 0xC4, 0x6A), listOf(c.r, c.g, c.b).map { (it * 255f).roundToInt() })
    }
}
