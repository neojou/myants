package com.neojou.myants.render3d.actors

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class PropGlbTest {
    @Test
    fun pebbleLeafAndClockStoneStayCheap() {
        val pebble = parseGltfMerged(glb("pebble.glb").readBytes())
        assertTrue(mergedTriangles(pebble) <= 200, "pebble tris=${mergedTriangles(pebble)}")
        val pebbleSpan = span(pebble)
        assertTrue(pebbleSpan[0] in 0.8f..1.4f, "pebble x=${pebbleSpan[0]}")

        val leaf = parseGltfMerged(glb("leaf.glb").readBytes())
        assertTrue(mergedTriangles(leaf) <= 150, "leaf tris=${mergedTriangles(leaf)}")
        val leafSpan = span(leaf)
        assertTrue(leafSpan[0] > 1f, "leaf x=${leafSpan[0]}")
        assertTrue(leafSpan[1] < 0.2f, "leaf y=${leafSpan[1]}")

        val clock = parseGltfMerged(glb("clock_stone.glb").readBytes())
        assertTrue(mergedTriangles(clock) in 40..200, "clock tris=${mergedTriangles(clock)}")
        val clockSpan = span(clock)
        assertTrue(clockSpan[0] in 1.5f..2.3f, "clock x=${clockSpan[0]}")
    }

    private fun span(mesh: AntMesh): FloatArray {
        val min = floatArrayOf(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        val max = floatArrayOf(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)
        var i = 0
        while (i < mesh.positions.size) {
            for (axis in 0 until 3) {
                val value = mesh.positions[i + axis]
                if (value < min[axis]) min[axis] = value
                if (value > max[axis]) max[axis] = value
            }
            i += 3
        }
        return FloatArray(3) { max[it] - min[it] }
    }

    private fun glb(name: String): File {
        val found = listOf(
            File("assets/models/nature/$name"),
            File("koolApp/assets/models/nature/$name"),
        ).firstOrNull { it.isFile }
        return found ?: error("$name not found from ${File(".").absolutePath}")
    }
}
