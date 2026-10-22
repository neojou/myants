package com.neojou.myants.render3d.actors

import java.io.File
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class FoodGlbTest {
    @Test
    fun sugarAndProteinAreCompactOriginalMeshes() {
        assertMorsel("sugar.glb", 8..200)
        assertMorsel("protein.glb", 40..400)
    }

    private fun assertMorsel(name: String, tris: IntRange) {
        val mesh = parseGltfMerged(glb(name).readBytes())
        val count = mergedTriangles(mesh)
        assertTrue(count in tris, "$name tris=$count")
        var minX = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var i = 0
        while (i < mesh.positions.size) {
            val x = mesh.positions[i]
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            i += 3
        }
        val span = maxX - minX
        assertTrue(span <= 1.02f, "$name span=$span")
        assertTrue(abs(span) > 0.2f, "$name span=$span")
    }

    private fun glb(name: String): File {
        val found = listOf(
            File("assets/models/food/$name"),
            File("koolApp/assets/models/food/$name"),
        ).firstOrNull { it.isFile }
        return found ?: error("$name not found from ${File(".").absolutePath}")
    }
}
