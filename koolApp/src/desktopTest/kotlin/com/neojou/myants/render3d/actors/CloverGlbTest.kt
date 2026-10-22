package com.neojou.myants.render3d.actors

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class CloverGlbTest {
    @Test
    fun cloverTuftIsALowRosetteAndTheFlowerHeadSitsAboveIt() {
        val tuft = parseGltfMerged(glb("clover_tuft.glb").readBytes())
        val tris = mergedTriangles(tuft)
        assertTrue(tris in 80..180, "tuft tris=$tris")
        val tuftSpan = span(tuft)
        assertTrue(tuftSpan[0] > tuftSpan[1], "tuft width is not above its height: ${tuftSpan[0]} x ${tuftSpan[1]}")
        assertTrue(tuftSpan[0] > 0.8f, "tuft x=${tuftSpan[0]}")
        assertTrue(minY(tuft) > -0.02f, "tuft floor")
        assertTrue(facesUp(tuft), "tuft faces the ground instead of the camera")

        val flower = parseGltfMerged(glb("clover_flower.glb").readBytes())
        val flowerTris = mergedTriangles(flower)
        assertTrue(flowerTris <= 80, "flower tris=$flowerTris")
        assertTrue(maxY(flower) > maxY(tuft), "flower head is not above the leaves")
        assertTrue(minY(flower) > -0.02f, "flower floor")
        val flowerSpan = span(flower)
        assertTrue(flowerSpan[0] > 0.35f, "flower head is too narrow: ${flowerSpan[0]}")
        assertTrue(headFacesTheSky(flower), "flower head is lit from underneath")
    }

    /** The top of the head has to point up, or the bloom is a black ball. */
    private fun headFacesTheSky(mesh: AntMesh): Boolean {
        var top = Float.NEGATIVE_INFINITY
        var i = 1
        while (i < mesh.positions.size) {
            if (mesh.positions[i] > top) top = mesh.positions[i]
            i += 3
        }
        var sum = 0f
        var count = 0
        i = 1
        while (i < mesh.positions.size) {
            if (mesh.positions[i] > top - 0.05f) {
                sum += mesh.normals[i]
                count++
            }
            i += 3
        }
        return count > 0 && sum / count > 0.5f
    }

    /** Geometric normal of the leaflets. A downward face is culled by the top-down camera. */
    private fun facesUp(mesh: AntMesh): Boolean {
        var up = 0
        var down = 0
        var t = 0
        val p = mesh.positions
        val idx = mesh.indices
        while (t + 2 < idx.size) {
            val a = idx[t] * 3
            val b = idx[t + 1] * 3
            val c = idx[t + 2] * 3
            val ny = (p[b + 2] - p[a + 2]) * (p[c] - p[a]) - (p[b] - p[a]) * (p[c + 2] - p[a + 2])
            if (ny > 1e-8f) up++ else if (ny < -1e-8f) down++
            t += 3
        }
        return up > down
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

    private fun minY(mesh: AntMesh): Float {
        var y = Float.POSITIVE_INFINITY
        var i = 1
        while (i < mesh.positions.size) {
            if (mesh.positions[i] < y) y = mesh.positions[i]
            i += 3
        }
        return y
    }

    private fun maxY(mesh: AntMesh): Float {
        var y = Float.NEGATIVE_INFINITY
        var i = 1
        while (i < mesh.positions.size) {
            if (mesh.positions[i] > y) y = mesh.positions[i]
            i += 3
        }
        return y
    }

    private fun glb(name: String): File {
        val found = listOf(
            File("assets/models/nature/$name"),
            File("koolApp/assets/models/nature/$name"),
        ).firstOrNull { it.isFile }
        return found ?: error("$name not found from ${File(".").absolutePath}")
    }
}
