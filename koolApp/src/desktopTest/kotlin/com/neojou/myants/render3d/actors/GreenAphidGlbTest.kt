package com.neojou.myants.render3d.actors

import java.io.File
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GreenAphidGlbTest {
    @Test
    fun gltf2GreenAphidHasPearBodyCorniclesAndSixLegs() {
        val aphid = parseGltfAphid(glbFile().readBytes())
        val names = aphid.nodes.associateBy { it.name }
        for (required in listOf(
            "Head", "EyeL", "EyeR", "AntennaL", "AntennaR",
            "Thorax", "Abdomen", "CornicleL", "CornicleR", "Cauda",
            "LegL1", "LegR1", "LegL2", "LegR2", "LegL3", "LegR3",
        )) {
            assertTrue(required in names, required)
        }
        assertTrue("cuticle" in aphid.materials)
        assertTrue("eye" in aphid.materials)
        assertTrue("leg" in aphid.materials)
        val head = names.getValue("Head")
        val abdomen = names.getValue("Abdomen")
        val cornL = names.getValue("CornicleL")
        val cornR = names.getValue("CornicleR")
        val cauda = names.getValue("Cauda")
        assertTrue(head.x > abdomen.x)
        assertTrue(cornL.x < abdomen.x && cornR.x < abdomen.x)
        assertTrue(cornL.y > abdomen.y && cornR.y > abdomen.y)
        assertTrue(cornL.z > 0f && cornR.z < 0f)
        assertTrue(cauda.z < cornL.z && cauda.z > cornR.z)
        assertTrue(cauda.x < cornL.x && cauda.x < cornR.x)
        assertTrue(cauda.y < cornL.y && cauda.y < cornR.y)
        assertEquals(6, aphid.legs.size)
        for (leg in aphid.legs) {
            assertEquals("Thorax", aphid.thoraxOf(leg.name))
            assertTrue(leg.footY < leg.socketY, leg.name)
            assertTrue(abs(leg.footY) < 0.04f, "${leg.name} footY=${leg.footY}")
            assertTrue(abs(leg.footZ) > abs(leg.socketZ), leg.name)
        }
        assertTrue(aphid.eyes.indices.isNotEmpty())
        assertTrue(aphid.body.indices.isNotEmpty())
        assertTrue(aphid.triangles in 700..2200, "tris=${aphid.triangles}")
        var spanMin = Float.POSITIVE_INFINITY
        var spanMax = Float.NEGATIVE_INFINITY
        fun span(part: AntMesh) {
            var v = 0
            while (v < part.positions.size) {
                val x = part.positions[v]
                if (x < spanMin) spanMin = x
                if (x > spanMax) spanMax = x
                v += 3
            }
        }
        span(aphid.body)
        span(aphid.eyes)
        for (leg in aphid.legs) span(leg.mesh)
        assertTrue(abs((spanMax - spanMin) - 1f) < 0.02f, "length=${spanMax - spanMin}")
        assertTrue(spanMax > head.x + 0.05f, "antenna maxX=$spanMax head=${head.x}")
    }

    private fun glbFile(): File {
        val found = listOf(
            File("assets/models/insects/aphid_green.glb"),
            File("koolApp/assets/models/insects/aphid_green.glb"),
        ).firstOrNull { it.isFile }
        return found ?: error("aphid_green.glb not found from ${File(".").absolutePath}")
    }
}
