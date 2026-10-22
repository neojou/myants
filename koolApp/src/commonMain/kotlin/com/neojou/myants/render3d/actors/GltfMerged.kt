package com.neojou.myants.render3d.actors

import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.scene.Mesh

/** Every primitive in a glTF, baked into one mesh in model space. */
internal fun parseGltfMerged(bytes: ByteArray): AntMesh {
    if (bytes.size < 20) error("glTF file is too small")
    val magic = bytes.u32(0)
    val version = bytes.u32(4)
    val length = bytes.u32(8)
    if (magic != 0x46546C67) error("not a glTF binary")
    if (version != 2) error("glTF version $version")
    if (length != bytes.size) error("glTF length $length does not match ${bytes.size} bytes")

    var jsonText = ""
    var bin = ByteArray(0)
    var cursor = 12
    while (cursor + 8 <= bytes.size) {
        val chunkLen = bytes.u32(cursor)
        val chunkType = bytes.u32(cursor + 4)
        val start = cursor + 8
        val end = start + chunkLen
        if (end > bytes.size) error("glTF chunk overruns the file")
        when (chunkType) {
            0x4E4F534A -> jsonText = bytes.decodeToString(start, end)
            0x004E4942 -> bin = bytes.copyOfRange(start, end)
        }
        cursor = end
    }
    if (jsonText.isEmpty() || bin.isEmpty()) error("glTF is missing JSON or BIN")

    val root = JsonReader(jsonText).parse() as? JsonMap ?: error("glTF JSON root is not an object")
    val accessors = root.array("accessors")
    val views = root.array("bufferViews")
    val meshes = root.array("meshes")
    val nodeValues = root.array("nodes")
    val sceneIndex = root.num("scene").toInt()
    val sceneNodes = (root.array("scenes")[sceneIndex] as JsonMap).array("nodes").map { (it as Double).toInt() }

    val count = nodeValues.size
    val translation = Array(count) { floatArrayOf(0f, 0f, 0f) }
    val rotation = Array(count) { floatArrayOf(0f, 0f, 0f, 1f) }
    val scale = Array(count) { floatArrayOf(1f, 1f, 1f) }
    val meshOf = IntArray(count) { -1 }
    val children = Array(count) { IntArray(0) }
    for (i in 0 until count) {
        val node = nodeValues[i] as JsonMap
        node.floats("translation")?.let { translation[i] = it }
        node.floats("rotation")?.let { rotation[i] = it }
        node.floats("scale")?.let { scale[i] = it }
        meshOf[i] = node.maybeNum("mesh")?.toInt() ?: -1
        val kids = node.maybeArray("children")?.map { (it as Double).toInt() } ?: emptyList()
        children[i] = kids.toIntArray()
    }

    val world = arrayOfNulls<FloatArray>(count)
    fun visit(index: Int, parentWorld: FloatArray?) {
        val local = trs(translation[index], rotation[index], scale[index])
        val here = if (parentWorld == null) local else mul(parentWorld, local)
        world[index] = here
        for (child in children[index]) visit(child, here)
    }
    for (rootIndex in sceneNodes) visit(rootIndex, null)

    val merged = MeshAccum()
    for (i in 0 until count) {
        val meshIndex = meshOf[i]
        if (meshIndex < 0) continue
        val here = world[i] ?: continue
        val inverse = invert(here)
        val mesh = meshes[meshIndex] as JsonMap
        for (primValue in mesh.array("primitives")) {
            val prim = primValue as JsonMap
            val attrs = prim.map("attributes")
            val positions = readVec3(accessors, views, bin, attrs.num("POSITION").toInt())
            val normals = readVec3(accessors, views, bin, attrs.num("NORMAL").toInt())
            val indices = readIndices(accessors, views, bin, prim.num("indices").toInt())
            transform(here, inverse, positions, normals)
            merged.add(positions, normals, indices)
        }
    }
    val out = merged.finish()
    if (out.indices.isEmpty()) error("glTF has no triangles")
    return out
}

internal fun Mesh<*>.uploadMerged(part: AntMesh) {
    generate {
        var i = 0
        while (i < part.positions.size) {
            vertex(
                Vec3f(part.positions[i], part.positions[i + 1], part.positions[i + 2]),
                Vec3f(part.normals[i], part.normals[i + 1], part.normals[i + 2]),
            )
            i += 3
        }
        var t = 0
        while (t < part.indices.size) {
            addTriIndices(part.indices[t], part.indices[t + 1], part.indices[t + 2])
            t += 3
        }
    }
}

internal fun mergedTriangles(part: AntMesh): Int = part.indices.size / 3
