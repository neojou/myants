package com.neojou.myants.render3d.actors

import kotlin.math.sqrt

/**
 * A glTF 2.0 worker ant, parsed far enough to instance it.
 * Meshes are baked into model space: +X is the head, feet sit on y = 0, length is 1.
 */
internal class GltfAnt(
    val nodes: List<AntNode>,
    val materials: List<String>,
    val body: AntMesh,
    val eyes: AntMesh,
    val mandibles: AntMesh,
    val legs: List<AntLeg>,
) {
    val triangles: Int
        get() {
            var count = body.indices.size + eyes.indices.size + mandibles.indices.size
            for (leg in legs) count += leg.mesh.indices.size
            return count / 3
        }

    fun thoraxOf(legName: String): String {
        var index = nodes.indexOfFirst { it.name == legName }
        while (index >= 0) {
            val name = nodes[index].name
            if (name == "Prothorax" || name == "Mesothorax" || name == "Metathorax") return name
            index = nodes[index].parent
        }
        error("$legName is not attached to a thoracic segment")
    }
}

internal class AntNode(val name: String, val parent: Int, val x: Float, val y: Float, val z: Float)

internal class AntMesh(val positions: FloatArray, val normals: FloatArray, val indices: IntArray)

internal class AntLeg(
    val name: String,
    val socketX: Float,
    val socketY: Float,
    val socketZ: Float,
    val footX: Float,
    val footY: Float,
    val footZ: Float,
    val mesh: AntMesh,
)

internal fun parseGltfAnt(bytes: ByteArray): GltfAnt {
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
    val materials = root.array("materials").map { (it as JsonMap).str("name") }
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
    val jaws = MeshAccum()
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
            val bucket = bucketFor(i, names, parent)
            val accum = when {
                bucket == "eye" -> eyes
                bucket == "jaw" -> jaws
                bucket.startsWith("Leg") -> legAccum.getOrPut(bucket) { MeshAccum() }
                else -> body
            }
            accum.add(positions, normals, indices)
        }
    }

    val legOrder = listOf("LegL1", "LegR1", "LegL2", "LegR2", "LegL3", "LegR3")
    val legs = legOrder.map { name ->
        val accum = legAccum[name] ?: error("missing $name")
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
    val nodes = ArrayList<AntNode>(count)
    for (i in 0 until count) {
        val here = world[i] ?: floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f)
        nodes += AntNode(names[i], parent[i], here[12], here[13], here[14])
    }
    val ant = GltfAnt(nodes, materials, body.finish(), eyes.finish(), jaws.finish(), legs)
    if (ant.body.indices.isEmpty() || ant.eyes.indices.isEmpty() || ant.mandibles.indices.isEmpty()) {
        error("black ant is missing a body part")
    }
    return ant
}

private fun bucketFor(index: Int, names: Array<String>, parent: IntArray): String {
    var cursor = index
    while (cursor >= 0) {
        val name = names[cursor]
        when {
            name.startsWith("Eye") -> return "eye"
            name.startsWith("Mandible") -> return "jaw"
            name.startsWith("Leg") -> return name
        }
        cursor = parent[cursor]
    }
    return "body"
}

internal class MeshAccum {
    private val positions = ArrayList<Float>()
    private val normals = ArrayList<Float>()
    private val indices = ArrayList<Int>()

    fun add(pos: FloatArray, nrm: FloatArray, idx: IntArray) {
        val base = positions.size / 3
        for (value in pos) positions += value
        for (value in nrm) normals += value
        for (value in idx) indices += value + base
    }

    fun finish(): AntMesh = AntMesh(
        FloatArray(positions.size) { positions[it] },
        FloatArray(normals.size) { normals[it] },
        IntArray(indices.size) { indices[it] },
    )
}

internal fun transform(world: FloatArray, inverse: FloatArray, positions: FloatArray, normals: FloatArray) {
    var i = 0
    while (i < positions.size) {
        val x = positions[i]
        val y = positions[i + 1]
        val z = positions[i + 2]
        positions[i] = world[0] * x + world[4] * y + world[8] * z + world[12]
        positions[i + 1] = world[1] * x + world[5] * y + world[9] * z + world[13]
        positions[i + 2] = world[2] * x + world[6] * y + world[10] * z + world[14]
        val nx = inverse[0] * normals[i] + inverse[1] * normals[i + 1] + inverse[2] * normals[i + 2]
        val ny = inverse[4] * normals[i] + inverse[5] * normals[i + 1] + inverse[6] * normals[i + 2]
        val nz = inverse[8] * normals[i] + inverse[9] * normals[i + 1] + inverse[10] * normals[i + 2]
        val length = sqrt(nx * nx + ny * ny + nz * nz)
        if (length > 1e-8f) {
            normals[i] = nx / length
            normals[i + 1] = ny / length
            normals[i + 2] = nz / length
        }
        i += 3
    }
}

internal fun readVec3(accessors: List<Any?>, views: List<Any?>, bin: ByteArray, index: Int): FloatArray {
    val acc = accessors[index] as JsonMap
    val count = acc.num("count").toInt()
    val offset = viewOffset(views, acc)
    val out = FloatArray(count * 3)
    var cursor = offset
    for (i in out.indices) {
        out[i] = Float.fromBits(bin.u32(cursor))
        cursor += 4
    }
    return out
}

internal fun readIndices(accessors: List<Any?>, views: List<Any?>, bin: ByteArray, index: Int): IntArray {
    val acc = accessors[index] as JsonMap
    val count = acc.num("count").toInt()
    val offset = viewOffset(views, acc)
    val kind = acc.num("componentType").toInt()
    val out = IntArray(count)
    var cursor = offset
    for (i in 0 until count) {
        out[i] = when (kind) {
            5123 -> (bin[cursor].toInt() and 0xff) or ((bin[cursor + 1].toInt() and 0xff) shl 8)
            5125 -> bin.u32(cursor)
            else -> error("unsupported index component $kind")
        }
        cursor += if (kind == 5123) 2 else 4
    }
    return out
}

internal fun viewOffset(views: List<Any?>, accessor: JsonMap): Int {
    val view = views[accessor.num("bufferView").toInt()] as JsonMap
    return (view.maybeNum("byteOffset")?.toInt() ?: 0) + (accessor.maybeNum("byteOffset")?.toInt() ?: 0)
}

internal fun trs(t: FloatArray, q: FloatArray, s: FloatArray): FloatArray {
    val x = q[0]
    val y = q[1]
    val z = q[2]
    val w = q[3]
    val xx = x * x
    val yy = y * y
    val zz = z * z
    val r00 = 1f - 2f * (yy + zz)
    val r01 = 2f * (x * y - z * w)
    val r02 = 2f * (x * z + y * w)
    val r10 = 2f * (x * y + z * w)
    val r11 = 1f - 2f * (xx + zz)
    val r12 = 2f * (y * z - x * w)
    val r20 = 2f * (x * z - y * w)
    val r21 = 2f * (y * z + x * w)
    val r22 = 1f - 2f * (xx + yy)
    return floatArrayOf(
        r00 * s[0], r10 * s[0], r20 * s[0], 0f,
        r01 * s[1], r11 * s[1], r21 * s[1], 0f,
        r02 * s[2], r12 * s[2], r22 * s[2], 0f,
        t[0], t[1], t[2], 1f,
    )
}

internal fun mul(a: FloatArray, b: FloatArray): FloatArray {
    val out = FloatArray(16)
    for (col in 0 until 4) {
        for (row in 0 until 4) {
            out[col * 4 + row] =
                a[row] * b[col * 4] +
                a[4 + row] * b[col * 4 + 1] +
                a[8 + row] * b[col * 4 + 2] +
                a[12 + row] * b[col * 4 + 3]
        }
    }
    return out
}

internal fun invert(m: FloatArray): FloatArray {
    val a = Array(4) { row ->
        FloatArray(8) { col ->
            if (col < 4) m[col * 4 + row] else if (col - 4 == row) 1f else 0f
        }
    }
    for (col in 0 until 4) {
        var pivot = col
        var best = kotlin.math.abs(a[col][col])
        for (row in col + 1 until 4) {
            val value = kotlin.math.abs(a[row][col])
            if (value > best) {
                best = value
                pivot = row
            }
        }
        if (best < 1e-8f) error("singular ant transform")
        if (pivot != col) {
            val swap = a[col]
            a[col] = a[pivot]
            a[pivot] = swap
        }
        val div = a[col][col]
        for (j in 0 until 8) a[col][j] /= div
        for (row in 0 until 4) {
            if (row == col) continue
            val factor = a[row][col]
            if (factor == 0f) continue
            for (j in 0 until 8) a[row][j] -= factor * a[col][j]
        }
    }
    val out = FloatArray(16)
    for (row in 0 until 4) {
        for (col in 0 until 4) out[col * 4 + row] = a[row][col + 4]
    }
    return out
}

internal fun ByteArray.u32(at: Int): Int =
    (this[at].toInt() and 0xff) or
        ((this[at + 1].toInt() and 0xff) shl 8) or
        ((this[at + 2].toInt() and 0xff) shl 16) or
        ((this[at + 3].toInt() and 0xff) shl 24)

internal class JsonMap(val values: Map<String, Any?>) {
    fun str(key: String): String = values[key] as? String ?: error("missing string $key")
    fun num(key: String): Double = (values[key] as? Double) ?: error("missing number $key")
    fun maybeNum(key: String): Double? = values[key] as? Double
    fun array(key: String): List<Any?> = values[key] as? List<*> ?: error("missing array $key")
    fun maybeArray(key: String): List<Any?>? = values[key] as? List<*>
    fun map(key: String): JsonMap = values[key] as? JsonMap ?: error("missing object $key")
    fun floats(key: String): FloatArray? {
        val list = maybeArray(key) ?: return null
        return FloatArray(list.size) { (list[it] as Double).toFloat() }
    }
}

internal class JsonReader(val text: String) {
    private var i = 0

    fun parse(): Any? {
        skip()
        return value()
    }

    private fun value(): Any? {
        skip()
        return when (val c = text[i]) {
            '{' -> obj()
            '[' -> arr()
            '"' -> str()
            't' -> { i += 4; true }
            'f' -> { i += 5; false }
            'n' -> { i += 4; null }
            else -> if (c == '-' || c in '0'..'9') num() else error("bad JSON at $i")
        }
    }

    private fun obj(): JsonMap {
        i++
        val map = LinkedHashMap<String, Any?>()
        skip()
        if (text[i] == '}') {
            i++
            return JsonMap(map)
        }
        while (true) {
            skip()
            val key = str()
            skip()
            if (text[i] != ':') error("expected colon")
            i++
            map[key] = value()
            skip()
            when (text[i]) {
                ',' -> i++
                '}' -> { i++; return JsonMap(map) }
                else -> error("expected comma or brace")
            }
        }
    }

    private fun arr(): List<Any?> {
        i++
        val list = ArrayList<Any?>()
        skip()
        if (text[i] == ']') {
            i++
            return list
        }
        while (true) {
            list += value()
            skip()
            when (text[i]) {
                ',' -> i++
                ']' -> { i++; return list }
                else -> error("expected comma or bracket")
            }
        }
    }

    private fun str(): String {
        if (text[i] != '"') error("expected string")
        i++
        val out = StringBuilder()
        while (true) {
            val c = text[i++]
            when (c) {
                '"' -> return out.toString()
                '\\' -> {
                    when (val e = text[i++]) {
                        '"', '\\', '/' -> out.append(e)
                        'b' -> out.append('\b')
                        'f' -> out.append('\u000C')
                        'n' -> out.append('\n')
                        'r' -> out.append('\r')
                        't' -> out.append('\t')
                        'u' -> {
                            val hex = text.substring(i, i + 4)
                            out.append(hex.toInt(16).toChar())
                            i += 4
                        }
                        else -> error("bad escape")
                    }
                }
                else -> out.append(c)
            }
        }
    }

    private fun num(): Double {
        val start = i
        if (text[i] == '-') i++
        while (i < text.length && text[i] in '0'..'9') i++
        if (i < text.length && text[i] == '.') {
            i++
            while (i < text.length && text[i] in '0'..'9') i++
        }
        if (i < text.length && (text[i] == 'e' || text[i] == 'E')) {
            i++
            if (text[i] == '+' || text[i] == '-') i++
            while (i < text.length && text[i] in '0'..'9') i++
        }
        return text.substring(start, i).toDouble()
    }

    private fun skip() {
        while (i < text.length && text[i].isWhitespace()) i++
    }
}
