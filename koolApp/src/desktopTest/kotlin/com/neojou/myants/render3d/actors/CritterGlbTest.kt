package com.neojou.myants.render3d.actors

import java.io.File
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CritterGlbTest {
    @Test
    fun outdoorInsectsKeepHeadFeetAndLegCount() {
        assertCritter("prey.glb", 6, marks = false)
        assertCritter("predator.glb", 6, marks = false)
        assertCritter("spider.glb", 8, marks = false)
        assertCritter("beetle.glb", 6, marks = true)
        assertCritter("ladybug.glb", 6, marks = true)
        assertLadybugDome()
    }

    private fun assertLadybugDome() {
        val critter = parseGltfCritter(glb("ladybug.glb").readBytes())
        val names = critter.nodes.map { it.name }.toSet()
        for (name in listOf(
            "Dome",
            "Spot1", "Spot2", "Spot3", "Spot4", "Spot5", "Spot6", "Spot7",
            "SpotHead", "SpotPronotum",
        )) {
            assertTrue(name in names, name)
        }
        val shell = horizontalSpan(critter.body)
        val length = modelLength(critter)
        assertTrue(shell.dx > 0.6f * length, "shell=${shell.dx} length=$length")
        assertTrue(
            abs(shell.dz - shell.dx) < 0.15f * shell.dx,
            "shell x=${shell.dx} z=${shell.dz}",
        )
    }

    private fun assertCritter(name: String, legs: Int, marks: Boolean) {
        val critter = parseGltfCritter(glb(name).readBytes())
        val nodes = critter.nodes.associateBy { it.name }
        assertTrue("Head" in nodes, name)
        assertTrue("Thorax" in nodes, name)
        assertTrue("EyeL" in nodes && "EyeR" in nodes, name)
        assertTrue(nodes.getValue("Head").x > nodes.getValue("Thorax").x, name)
        assertEquals(legs, critter.legs.size, name)
        assertEquals(marks, critter.marks.indices.isNotEmpty(), name)
        assertTrue(critter.triangles in 700..2200, "$name tris=${critter.triangles}")
        for (leg in critter.legs) {
            assertTrue(leg.footY < leg.socketY, "$name ${leg.name}")
            assertTrue(abs(leg.footY) < 0.08f, "$name ${leg.name} footY=${leg.footY}")
            assertTrue(abs(leg.footZ) > abs(leg.socketZ), "$name ${leg.name}")
        }
        var spanMin = Float.POSITIVE_INFINITY
        var spanMax = Float.NEGATIVE_INFINITY
        val parts = listOf(critter.body, critter.eyes, critter.marks) + critter.legs.map { it.mesh }
        for (part in parts) {
            var v = 0
            while (v < part.positions.size) {
                val x = part.positions[v]
                if (x < spanMin) spanMin = x
                if (x > spanMax) spanMax = x
                v += 3
            }
        }
        assertTrue(abs((spanMax - spanMin) - 1f) < 0.02f, "$name length=${spanMax - spanMin}")
    }

    private class Span(val dx: Float, val dz: Float)

    private fun horizontalSpan(part: AntMesh): Span {
        var minX = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY
        var v = 0
        val positions = part.positions
        while (v < positions.size) {
            val x = positions[v]
            val z = positions[v + 2]
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (z < minZ) minZ = z
            if (z > maxZ) maxZ = z
            v += 3
        }
        return Span(maxX - minX, maxZ - minZ)
    }

    private fun modelLength(critter: GltfCritter): Float {
        var minX = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        val parts = listOf(critter.body, critter.eyes, critter.marks) + critter.legs.map { it.mesh }
        for (part in parts) {
            var v = 0
            while (v < part.positions.size) {
                val x = part.positions[v]
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                v += 3
            }
        }
        return maxX - minX
    }

    private fun glb(name: String): File {
        val found = listOf(
            File("assets/models/insects/$name"),
            File("koolApp/assets/models/insects/$name"),
        ).firstOrNull { it.isFile }
        return found ?: error("$name not found from ${File(".").absolutePath}")
    }
}
