package com.neojou.myants.render3d.actors

/**
 * A rigid outdoor insect. +X is the head, feet sit on y = 0, and the length is 1.
 * Spots stay on [marks]. Legs pivot at the node named LegL1, LegR1, and so on.
 */
internal class GltfCritter(
    val nodes: List<AntNode>,
    val body: AntMesh,
    val eyes: AntMesh,
    val marks: AntMesh,
    val legs: List<AntLeg>,
) {
    val triangles: Int
        get() {
            var count = body.indices.size + eyes.indices.size + marks.indices.size
            for (leg in legs) count += leg.mesh.indices.size
            return count / 3
        }
}

private val critterLegOrder = listOf(
    "LegL1", "LegR1", "LegL2", "LegR2", "LegL3", "LegR3", "LegL4", "LegR4",
)

internal fun parseGltfCritter(bytes: ByteArray): GltfCritter {
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
    val names = Array(count) { "" }
    val translation = Array(count) { floatArrayOf(0f, 0f, 0f) }
    val rotation = Array(count) { floatArrayOf(0f, 0f, 0f, 1f) }
    val scale = Array(count) { floatArrayOf(1f, 1f, 1f) }
    val meshOf = IntArray(count) { -1 }
    val children = Array(count) { IntArray(0) }
    val parent = IntArray(count) { -1 }
    for (i in 0 until count) {
        val node = nodeValues[i] as JsonMap
        names[i] = node.str("name")
        node.floats("translation")?.let { translation[i] = it }
        node.floats("rotation")?.let { rotation[i] = it }
        node.floats("scale")?.let { scale[i] = it }
        meshOf[i] = node.maybeNum("mesh")?.toInt() ?: -1
        val kids = node.maybeArray("children")?.map { (it as Double).toInt() } ?: emptyList()
        children[i] = kids.toIntArray()
        for (child in kids) parent[child] = i
    }

    val world = arrayOfNulls<FloatArray>(count)
    fun visit(index: Int, parentWorld: FloatArray?) {
        val local = trs(translation[index], rotation[index], scale[index])
        val here = if (parentWorld == null) local else mul(parentWorld, local)
        world[index] = here
        for (child in children[index]) visit(child, here)
    }
    for (rootIndex in sceneNodes) visit(rootIndex, null)

    val body = MeshAccum()
    val eyes = MeshAccum()
    val marks = MeshAccum()
    val legAccum = LinkedHashMap<String, MeshAccum>()
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
            val bucket = critterBucket(i, names, parent)
            val accum = when {
                bucket == "eye" -> eyes
                bucket == "mark" -> marks
                bucket.startsWith("Leg") -> legAccum.getOrPut(bucket) { MeshAccum() }
                else -> body
            }
            accum.add(positions, normals, indices)
        }
    }

    val legs = critterLegOrder.mapNotNull { name ->
        val accum = legAccum[name] ?: return@mapNotNull null
        val nodeIndex = names.indexOf(name)
        if (nodeIndex < 0) error("missing node $name")
        val socket = world[nodeIndex] ?: error("$name has no transform")
        val mesh = accum.finish()
        var foot = 0
        var footY = Float.POSITIVE_INFINITY
        var v = 1
        while (v < mesh.positions.size) {
            if (mesh.positions[v] < footY) {
                footY = mesh.positions[v]
                foot = v - 1
            }
            v += 3
        }
        AntLeg(
            name,
            socket[12], socket[13], socket[14],
            mesh.positions[foot], mesh.positions[foot + 1], mesh.positions[foot + 2],
            mesh,
        )
    }
    if (legs.size != 6 && legs.size != 8) error("critter has ${legs.size} legs")
    val nodes = ArrayList<AntNode>(count)
    for (i in 0 until count) {
        val here = world[i] ?: FloatArray(16) { if (it % 5 == 0) 1f else 0f }
        nodes += AntNode(names[i], parent[i], here[12], here[13], here[14])
    }
    val critter = GltfCritter(nodes, body.finish(), eyes.finish(), marks.finish(), legs)
    if (critter.body.indices.isEmpty() || critter.eyes.indices.isEmpty()) error("critter is missing a body part")
    return critter
}

private fun critterBucket(index: Int, names: Array<String>, parent: IntArray): String {
    var cursor = index
    while (cursor >= 0) {
        val name = names[cursor]
        when {
            name.startsWith("Eye") -> return "eye"
            name.startsWith("Spot") -> return "mark"
            name.startsWith("Leg") -> return name
        }
        cursor = parent[cursor]
    }
    return "body"
}
