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
        while (i < ant.antennae.positions.size) {
            val x = ant.antennae.positions[i]
            if (x > maxX) maxX = x
            if (x < minX) minX = x
            i += 3
        }
        assertTrue(maxX > headX + 0.12f, "antenna maxX=$maxX head=$headX")
        assertTrue(ant.head.indices.isNotEmpty())
        assertTrue(ant.antennae.indices.isNotEmpty())
        assertTrue(ant.eyes.indices.isNotEmpty())
        assertTrue(ant.mandibles.indices.isNotEmpty())
        assertTrue(ant.triangles in 1500..6000, "tris=${ant.triangles}")
        var spanMin = minX
        var spanMax = maxX
        for (part in listOf(ant.body, ant.head, ant.eyes, ant.mandibles) + ant.legs.map { it.mesh }) {
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

    /**
     * #94: shell NORMALs must agree with outward winding in the committed glb.
     * Checks raw glTF accessors (before the node transform) via the same JsonReader
     * / readVec3 path as the loader. Do not shell out to Python (CI has no numpy).
     */
    @Test
    fun shellNormalsFaceOutwardWithWinding() {
        val shells = setOf(
            "HeadShell", "Neck", "Clypeus", "EyeL", "EyeR",
            "ProthoraxShell", "MesothoraxShell", "MetathoraxShell",
            "PetioleShell", "PostpetioleShell", "GasterRear", "GasterFront", "GasterTip",
        )
        for (name in listOf("worker_black.glb", "soldier.glb")) {
            val bytes = glbFile(name).readBytes()
            var jsonText = ""
            var bin = ByteArray(0)
            var cursor = 12
            while (cursor + 8 <= bytes.size) {
                val chunkLen = bytes.u32(cursor)
                val chunkType = bytes.u32(cursor + 4)
                val start = cursor + 8
                val end = start + chunkLen
                when (chunkType) {
                    0x4E4F534A -> jsonText = bytes.decodeToString(start, end)
                    0x004E4942 -> bin = bytes.copyOfRange(start, end)
                }
                cursor = end
            }
            val root = JsonReader(jsonText).parse() as JsonMap
            val accessors = root.array("accessors")
            val views = root.array("bufferViews")
            val meshes = root.array("meshes")
            var seen = 0
            for (nodeEl in root.array("nodes")) {
                val node = nodeEl as JsonMap
                val nodeName = node.str("name")
                if (nodeName !in shells) continue
                val meshIndex = node.maybeNum("mesh")?.toInt() ?: continue
                val mesh = meshes[meshIndex] as JsonMap
                for (primEl in mesh.array("primitives")) {
                    val prim = primEl as JsonMap
                    val attrs = prim.map("attributes")
                    val pos = readVec3(accessors, views, bin, attrs.num("POSITION").toInt())
                    val nrm = readVec3(accessors, views, bin, attrs.num("NORMAL").toInt())
                    val idx = readIndices(accessors, views, bin, prim.num("indices").toInt())
                    var cx = 0f; var cy = 0f; var cz = 0f
                    val verts = pos.size / 3
                    var i = 0
                    while (i < pos.size) {
                        cx += pos[i]; cy += pos[i + 1]; cz += pos[i + 2]
                        i += 3
                    }
                    cx /= verts; cy /= verts; cz /= verts
                    var tris = 0; var agree = 0; var out = 0
                    var t = 0
                    while (t < idx.size) {
                        val a = idx[t] * 3; val b = idx[t + 1] * 3; val c = idx[t + 2] * 3
                        val ax = pos[a]; val ay = pos[a + 1]; val az = pos[a + 2]
                        val bx = pos[b]; val by = pos[b + 1]; val bz = pos[b + 2]
                        val dx = pos[c]; val dy = pos[c + 1]; val dz = pos[c + 2]
                        val fx = (by - ay) * (dz - az) - (bz - az) * (dy - ay)
                        val fy = (bz - az) * (dx - ax) - (bx - ax) * (dz - az)
                        val fz = (bx - ax) * (dy - ay) - (by - ay) * (dx - ax)
                        val nx = (nrm[a] + nrm[b] + nrm[c]) / 3f
                        val ny = (nrm[a + 1] + nrm[b + 1] + nrm[c + 1]) / 3f
                        val nz = (nrm[a + 2] + nrm[b + 2] + nrm[c + 2]) / 3f
                        if (fx * nx + fy * ny + fz * nz > 0f) agree++
                        val mx = (ax + bx + dx) / 3f - cx
                        val my = (ay + by + dy) / 3f - cy
                        val mz = (az + bz + dz) / 3f - cz
                        if (fx * mx + fy * my + fz * mz > 0f) out++
                        tris++; t += 3
                    }
                    assertEquals(tris, agree, "$name $nodeName winding·N $agree/$tris")
                    assertEquals(tris, out, "$name $nodeName winding outward $out/$tris")
                    seen++
                }
            }
            assertEquals(13, seen, "$name shell primitives")
        }
    }
}
