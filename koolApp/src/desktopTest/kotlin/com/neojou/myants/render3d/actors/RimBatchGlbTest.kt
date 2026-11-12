package com.neojou.myants.render3d.actors

import com.neojou.myants.render3d.RimParts
import com.neojou.myants.sim.InsectKind
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * #91 Artist review on #105: antennae and mandibles are thinner than a pixel at night, so a rim there
 * turns into flickering dots. Every triangle the committed glb puts under an Antenna or Mandible node
 * must land in a batch whose rim profile is null.
 *
 * The oracle reads the raw glTF node tree (not the loader's buckets): a triangle is "thin" when one of
 * its node's ancestors (itself included) is named Antenna*, Mandible*, Scape* or Funiculus*.
 */
class RimBatchGlbTest {
    @Test
    fun antAntennaeAndMandiblesNeverRideARimmedBatch() {
        for (name in listOf("worker_black.glb", "soldier.glb")) {
            val bytes = file("ants", name).readBytes()
            val ant = parseGltfAnt(bytes)
            val tree = rawTree(bytes)
            val thin = tree.triangles { isThin(it) }
            assertTrue(thin > 0, "$name has no antenna or mandible triangles")
            val rimmed = RimParts.AntPart.entries.filter { RimParts.antRim(it) != null }
            val rimmedTris = rimmed.sumOf { part -> ant.mesh(part).sumOf { it.indices.size / 3 } }
            val unrimmedTris = RimParts.AntPart.entries.filter { RimParts.antRim(it) == null }
                .sumOf { part -> ant.mesh(part).sumOf { it.indices.size / 3 } }
            assertEquals(tree.triangles { !isThin(it) && !isEyeOrLeg(it) }, rimmedTris, "$name rimmed batches")
            assertTrue(unrimmedTris >= thin, "$name unrimmed=$unrimmedTris thin=$thin")
            assertEquals(ant.triangles, rimmedTris + unrimmedTris, "$name every triangle is in one batch")
        }
    }

    @Test
    fun beetleAntennaeAndMandiblesNeverRideARimmedBatch() {
        val bytes = file("insects", "beetle.glb").readBytes()
        val critter = parseGltfCritter(bytes)
        val tree = rawTree(bytes)
        val thin = tree.triangles { isThin(it) }
        assertTrue(thin > 0, "beetle has no antenna or mandible triangles")
        val parts = RimParts.CritterPart.entries
        val rimmedTris = parts.filter { RimParts.critterRim(InsectKind.BEETLE, it) != null }
            .sumOf { part -> critter.mesh(part).sumOf { it.indices.size / 3 } }
        val unrimmedTris = parts.filter { RimParts.critterRim(InsectKind.BEETLE, it) == null }
            .sumOf { part -> critter.mesh(part).sumOf { it.indices.size / 3 } }
        assertEquals(tree.triangles { !isThin(it) && !isEyeOrLeg(it) }, rimmedTris, "beetle rimmed batches")
        assertTrue(unrimmedTris >= thin, "beetle unrimmed=$unrimmedTris thin=$thin")
        assertEquals(critter.triangles, rimmedTris + unrimmedTris, "beetle every triangle is in one batch")
    }

    private fun isThin(chain: List<String>): Boolean = chain.any {
        it.startsWith("Antenna") || it.startsWith("Mandible") || it.startsWith("Scape") || it.startsWith("Funiculus")
    }

    private fun isEyeOrLeg(chain: List<String>): Boolean = chain.any { it.startsWith("Eye") || it.startsWith("Leg") }

    /** Mesh nodes of the raw glb: ancestor-name chain (self first) and triangle count. */
    private class RawTree(val nodes: List<Pair<List<String>, Int>>) {
        fun triangles(pick: (List<String>) -> Boolean): Int = nodes.filter { pick(it.first) }.sumOf { it.second }
    }

    private fun rawTree(bytes: ByteArray): RawTree {
        var jsonText = ""
        var cursor = 12
        while (cursor + 8 <= bytes.size) {
            val chunkLen = bytes.u32(cursor)
            val chunkType = bytes.u32(cursor + 4)
            if (chunkType == 0x4E4F534A) jsonText = bytes.decodeToString(cursor + 8, cursor + 8 + chunkLen)
            cursor += 8 + chunkLen
        }
        val root = JsonReader(jsonText).parse() as JsonMap
        val accessors = root.array("accessors")
        val meshes = root.array("meshes")
        val nodes = root.array("nodes").map { it as JsonMap }
        val parent = IntArray(nodes.size) { -1 }
        nodes.forEachIndexed { i, node ->
            node.maybeArray("children")?.forEach { parent[(it as Double).toInt()] = i }
        }
        val out = ArrayList<Pair<List<String>, Int>>()
        nodes.forEachIndexed { i, node ->
            val meshIndex = node.maybeNum("mesh")?.toInt() ?: return@forEachIndexed
            var tris = 0
            for (prim in (meshes[meshIndex] as JsonMap).array("primitives")) {
                val acc = accessors[(prim as JsonMap).num("indices").toInt()] as JsonMap
                tris += acc.num("count").toInt() / 3
            }
            val chain = ArrayList<String>()
            var c = i
            while (c >= 0) {
                chain += nodes[c].str("name")
                c = parent[c]
            }
            out += chain to tris
        }
        return RawTree(out)
    }

    private fun file(dir: String, name: String): File {
        val found = listOf(
            File("assets/models/$dir/$name"),
            File("koolApp/assets/models/$dir/$name"),
        ).firstOrNull { it.isFile }
        return found ?: error("$name not found from ${File(".").absolutePath}")
    }
}
