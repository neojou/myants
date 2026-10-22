package com.neojou.myants.render3d.actors

import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlackAntGlbTest {
    @Test
    fun gltf2WorkerAndSoldierShareTheNodeContract() {
        val worker = parseGltfAnt(glbFile("worker_black.glb").readBytes())
        val soldier = parseGltfAnt(glbFile("soldier.glb").readBytes())
        assertContract(worker)
        assertContract(soldier)
        assertTrue(headReach(soldier) > headReach(worker), "soldier head should read larger")
    }

    private fun assertContract(ant: GltfAnt) {
        val names = ant.nodes.associateBy { it.name }
        for (required in listOf(
            "Head", "EyeL", "EyeR", "AntennaL", "AntennaR", "MandibleL", "MandibleR",
            "Prothorax", "Mesothorax", "Metathorax", "Petiole", "Gaster",
            "LegL1", "LegR1", "LegL2", "LegR2", "LegL3", "LegR3",
        )) {
            assertTrue(required in names, required)
        }
        assertTrue("chitin" in ant.materials)
        assertTrue("eye" in ant.materials)
        assertTrue("mandible" in ant.materials)
        assertTrue(names.getValue("Head").x > names.getValue("Gaster").x)
        assertEquals("Prothorax", ant.thoraxOf("LegL1"))
        assertEquals("Prothorax", ant.thoraxOf("LegR1"))
        assertEquals("Mesothorax", ant.thoraxOf("LegL2"))
        assertEquals("Mesothorax", ant.thoraxOf("LegR2"))
        assertEquals("Metathorax", ant.thoraxOf("LegL3"))
        assertEquals("Metathorax", ant.thoraxOf("LegR3"))
        assertEquals(6, ant.legs.size)
        for (leg in ant.legs) {
            assertTrue(leg.footY < leg.socketY, leg.name)
            assertTrue(abs(leg.footY) < 0.04f, "${leg.name} footY=${leg.footY}")
            assertTrue(abs(leg.footZ) > abs(leg.socketZ), leg.name)
        }
        val headX = names.getValue("Head").x
        var maxX = Float.NEGATIVE_INFINITY
        var minX = Float.POSITIVE_INFINITY
        var i = 0
        while (i < ant.head.positions.size) {
            val x = ant.head.positions[i]
            if (x > maxX) maxX = x
            if (x < minX) minX = x
            i += 3
        }
        assertTrue(maxX > headX + 0.12f, "antenna maxX=$maxX head=$headX")
        assertTrue(ant.head.indices.isNotEmpty())
        assertTrue(ant.eyes.indices.isNotEmpty())
        assertTrue(ant.mandibles.indices.isNotEmpty())
        assertTrue(ant.triangles in 1500..6000, "tris=${ant.triangles}")
        var spanMin = minX
        var spanMax = maxX
        for (part in listOf(ant.body, ant.eyes, ant.mandibles) + ant.legs.map { it.mesh }) {
            var v = 0
            while (v < part.positions.size) {
                val x = part.positions[v]
                if (x < spanMin) spanMin = x
                if (x > spanMax) spanMax = x
                v += 3
            }
        }
        assertTrue(abs((spanMax - spanMin) - 1f) < 0.02f, "length=${spanMax - spanMin}")
    }

    private fun headReach(ant: GltfAnt): Float {
        val node = ant.nodes.first { it.name == "Head" }
        var max = 0f
        var i = 0
        val positions = ant.head.positions
        while (i < positions.size) {
            val dx = positions[i] - node.x
            val dy = positions[i + 1] - node.y
            val dz = positions[i + 2] - node.z
            val reach = sqrt(dx * dx + dy * dy + dz * dz)
            if (reach > max) max = reach
            i += 3
        }
        return max
    }

    private fun glbFile(name: String): File {
        val found = listOf(
            File("assets/models/ants/$name"),
            File("koolApp/assets/models/ants/$name"),
        ).firstOrNull { it.isFile }
        return found ?: error("$name not found from ${File(".").absolutePath}")
    }
}
