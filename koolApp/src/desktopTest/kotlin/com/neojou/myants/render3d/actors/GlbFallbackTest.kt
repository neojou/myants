package com.neojou.myants.render3d.actors

import java.io.File
import java.io.FileNotFoundException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** #52: a missing, bad-header or truncated glb leaves the procedural mesh, never throws, and warns once. */
class GlbFallbackTest {
    private val parsers: Map<String, Pair<String, (ByteArray) -> Any>> = mapOf(
        "worker" to ("ants/worker_black.glb" to ::parseGltfAnt),
        "soldier" to ("ants/soldier.glb" to ::parseGltfAnt),
        "ladybug" to ("insects/ladybug.glb" to ::parseGltfCritter),
        "spider" to ("insects/spider.glb" to ::parseGltfCritter),
        "green aphid" to ("insects/aphid_green.glb" to ::parseGltfAphid),
        "sugar" to ("food/sugar.glb" to ::parseGltfMerged),
        "pebble" to ("nature/pebble.glb" to ::parseGltfMerged),
        "clover" to ("nature/clover_tuft.glb" to ::parseGltfMerged),
    )

    private fun run(label: String, loaded: Result<ByteArray>): Pair<Any?, List<String>> {
        val warnings = mutableListOf<String>()
        val parse = parsers.getValue(label).second
        val model = parseGlbOrFallback(label, "block", loaded, { warnings += it }, parse)
        return model to warnings
    }

    private fun bytes(path: String): ByteArray =
        listOf(File("assets/models/$path"), File("koolApp/assets/models/$path")).first { it.isFile }.readBytes()

    @Test
    fun intactFilesParseWithoutWarnings() {
        for ((label, entry) in parsers) {
            val (model, warnings) = run(label, Result.success(bytes(entry.first)))
            assertNotNull(model, label)
            assertTrue(warnings.isEmpty(), "$label $warnings")
        }
    }

    @Test
    fun missingFileFallsBackWithOneWarning() {
        for (label in parsers.keys) {
            val (model, warnings) = run(label, Result.failure(FileNotFoundException("no such asset")))
            assertNull(model, label)
            assertEquals(1, warnings.size, label)
            assertTrue("missing" in warnings[0] && label in warnings[0] && "block" in warnings[0], warnings[0])
        }
    }

    @Test
    fun corruptHeaderFallsBackWithOneWarning() {
        for ((label, entry) in parsers) {
            val corrupt = bytes(entry.first).copyOf().also { for (i in 0 until 4) it[i] = 0 }
            val (model, warnings) = run(label, Result.success(corrupt))
            assertNull(model, label)
            assertEquals(1, warnings.size, label)
            assertTrue("failed" in warnings[0], warnings[0])
        }
    }

    @Test
    fun truncatedFileFallsBackWithOneWarning() {
        for ((label, entry) in parsers) {
            val full = bytes(entry.first)
            for (cut in listOf(0, 11, 20, full.size / 2, full.size - 1)) {
                val (model, warnings) = run(label, Result.success(full.copyOf(cut)))
                assertNull(model, "$label cut $cut")
                assertEquals(1, warnings.size, "$label cut $cut")
            }
        }
    }

    @Test
    fun rejectedModelFallsBack() {
        val warnings = mutableListOf<String>()
        val model = parseGlbOrFallback("spider", "block", Result.success(bytes("insects/spider.glb")), { warnings += it }) {
            parseGltfCritter(it).also { c -> check(c.legs.size == 99) { "${c.legs.size} legs" } }
        }
        assertNull(model)
        assertEquals(1, warnings.size)
    }
}
