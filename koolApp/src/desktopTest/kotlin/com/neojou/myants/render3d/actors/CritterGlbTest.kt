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
        assertCritter("beetle.glb", 6, marks = false)
        assertCritter("ladybug.glb", 6, marks = true)
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

    private fun glb(name: String): File {
        val found = listOf(
            File("assets/models/insects/$name"),
            File("koolApp/assets/models/insects/$name"),
        ).firstOrNull { it.isFile }
        return found ?: error("$name not found from ${File(".").absolutePath}")
    }
}
