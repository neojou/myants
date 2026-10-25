package com.neojou.myants.render3d

import com.neojou.myants.render3d.actors.GltfAnt
import com.neojou.myants.render3d.actors.GltfAphid
import com.neojou.myants.render3d.actors.GltfCritter
import com.neojou.myants.render3d.actors.PoseBuckets
import com.neojou.myants.render3d.actors.draw
import com.neojou.myants.render3d.actors.drawAphid
import com.neojou.myants.render3d.actors.drawCritter
import com.neojou.myants.render3d.actors.outdoorAntTint
import com.neojou.myants.render3d.actors.parseGltfAnt
import com.neojou.myants.render3d.actors.parseGltfAphid
import com.neojou.myants.render3d.actors.mergedTriangles
import com.neojou.myants.render3d.actors.parseGltfCritter
import com.neojou.myants.render3d.actors.parseGltfMerged
import com.neojou.myants.render3d.actors.uploadAntMeshes
import com.neojou.myants.render3d.actors.uploadAphidMeshes
import com.neojou.myants.render3d.actors.uploadCritterMeshes
import com.neojou.myants.render3d.actors.uploadMerged
import com.neojou.myants.render3d.camera.CameraDirector
import com.neojou.myants.render3d.camera.TopDownCamera
import com.neojou.myants.render3d.nest.NestCutaway
import com.neojou.myants.render3d.phero.PheromoneField
import com.neojou.myants.render3d.quality.QualityPreset
import com.neojou.myants.render3d.terrain.GroundHeight
import com.neojou.myants.render3d.terrain.GroundMesh
import com.neojou.myants.sim.AntKind
import com.neojou.myants.sim.AntPlace
import com.neojou.myants.sim.CarryKind
import com.neojou.myants.sim.FoodKind
import com.neojou.myants.sim.InsectKind
import com.neojou.myants.sim.PerformanceManager
import com.neojou.myants.sim.SimConfig
import com.neojou.myants.wallClockHms
import com.neojou.tools.LogLevel
import com.neojou.tools.MyLog
import de.fabmax.kool.Assets
import de.fabmax.kool.KoolApplication
import de.fabmax.kool.addScene
import de.fabmax.kool.math.MutableMat4f
import de.fabmax.kool.math.MutableVec3f
import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.rad
import de.fabmax.kool.math.spatial.BoundingBoxF
import de.fabmax.kool.modules.ksl.KslUnlitShader
import de.fabmax.kool.modules.ksl.blocks.ColorBlockConfig
import de.fabmax.kool.pipeline.BlendMode
import de.fabmax.kool.pipeline.ClearColorFill
import de.fabmax.kool.pipeline.CullMethod
import de.fabmax.kool.pipeline.DepthCompareOp
import de.fabmax.kool.pipeline.deferred.DeferredPipeline
import de.fabmax.kool.pipeline.deferred.DeferredPipelineConfig
import de.fabmax.kool.pipeline.deferred.deferredKslPbrShader
import de.fabmax.kool.pipeline.ibl.EnvironmentMap
import de.fabmax.kool.pipeline.ibl.SkyCubeIblSystem
import de.fabmax.kool.scene.InstanceLayouts
import de.fabmax.kool.scene.Light
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.MeshInstanceList
import de.fabmax.kool.scene.Node
import de.fabmax.kool.scene.OrthographicCamera
import de.fabmax.kool.scene.TrsTransformF
import de.fabmax.kool.scene.VertexLayouts
import de.fabmax.kool.scene.addMesh
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.ColorGradient
import de.fabmax.kool.util.SimpleShadowMap
import de.fabmax.kool.util.Time
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.launch



/** One outdoor caste. A missing model leaves [model] null and the block ant draws instead. */
internal class AntSlot(
    val body: MeshInstanceList<InstanceLayouts.ModelMatColor>,
    val head: MeshInstanceList<InstanceLayouts.ModelMatColor>,
    val eyes: MeshInstanceList<InstanceLayouts.ModelMatColor>,
    val jaws: MeshInstanceList<InstanceLayouts.ModelMatColor>,
    val legs: List<MeshInstanceList<InstanceLayouts.ModelMatColor>>,
    val bodyMesh: Mesh<*>,
    val headMesh: Mesh<*>,
    val eyeMesh: Mesh<*>,
    val jawMesh: Mesh<*>,
    val legMeshes: List<Mesh<*>>,
    var model: GltfAnt? = null,
) {
    fun clear() {
        body.clear()
        head.clear()
        eyes.clear()
        jaws.clear()
        for (batch in legs) batch.clear()
    }
}

/** One outdoor insect kind other than the aphid. A missing model keeps the block. */
internal class CritterSlot(
    val kind: InsectKind,
    val path: String,
    val body: MeshInstanceList<InstanceLayouts.ModelMatColor>,
    val eyes: MeshInstanceList<InstanceLayouts.ModelMatColor>,
    val marks: MeshInstanceList<InstanceLayouts.ModelMatColor>,
    val legs: List<MeshInstanceList<InstanceLayouts.ModelMatColor>>,
    val bodyMesh: Mesh<*>,
    val eyeMesh: Mesh<*>,
    val markMesh: Mesh<*>,
    val legMeshes: List<Mesh<*>>,
    var model: GltfCritter? = null,
) {
    fun clear() {
        body.clear()
        eyes.clear()
        marks.clear()
        for (batch in legs) batch.clear()
    }
}

internal fun soilColor(ix: Int, iz: Int): Color {
    val h = (ix * 73856093) xor (iz * 19349663)
    val n = (h and 255) / 255f
    return WorldPalette.soil(n)
}

internal fun rgbColor(rgb: Int): Color {
    val r = ((rgb shr 16) and 255) / 255f
    val g = ((rgb shr 8) and 255) / 255f
    val b = (rgb and 255) / 255f
    return Color(r, g, b, 1f)
}

internal fun colorOr(hex: String, fallback: Color): Color = try {
    Color(hex)
} catch (_: Throwable) {
    fallback
}

internal fun de.fabmax.kool.scene.geometry.MeshBuilder<*>.moltFlake() {
    val seg = 6
    val halfW = 0.42f
    val up = Vec3f.Y_AXIS
    for (i in 0 until seg) {
        val t0 = i / seg.toFloat()
        val t1 = (i + 1) / seg.toFloat()
        val x0 = -0.5f + t0
        val x1 = -0.5f + t1
        val y0 = sin(t0 * PI.toFloat()) * 0.22f
        val y1 = sin(t1 * PI.toFloat()) * 0.22f
        val a = vertex(Vec3f(x0, y0, -halfW), up)
        val b = vertex(Vec3f(x0, y0, halfW), up)
        val c = vertex(Vec3f(x1, y1, halfW), up)
        val d = vertex(Vec3f(x1, y1, -halfW), up)
        addTriIndices(a, b, c)
        addTriIndices(a, c, d)
    }
}

/** One colored band of the selection ring, in ring-scale units (outer edge = 1). */
internal data class RingBand(val inner: Float, val outer: Float, val color: Color)

/** Two-tone selection ring (#61): a light outer band so it reads on dark soil, the brand inner band. */
internal fun selectRingBands(): List<RingBand> = listOf(
    RingBand(0.62f, 0.80f, WorldPalette.selectRingInner),
    RingBand(0.80f, 1f, WorldPalette.selectRingOuter),
)

/** Selection ring on the ground (XZ) or the nest plane (XY). Needs a layout with a color attribute. */
internal fun de.fabmax.kool.scene.geometry.MeshBuilder<*>.selectRing(xz: Boolean) {
    val n = 28
    val nrm = if (xz) Vec3f.Y_AXIS else Vec3f.Z_AXIS
    fun p(r: Float, a: Float): Vec3f =
        if (xz) Vec3f(cos(a) * r, 0f, sin(a) * r) else Vec3f(cos(a) * r, sin(a) * r, 0f)
    for (band in selectRingBands()) {
        withColor(band.color) {
            for (i in 0 until n) {
                val a0 = (i.toFloat() / n) * (PI.toFloat() * 2f)
                val a1 = ((i + 1).toFloat() / n) * (PI.toFloat() * 2f)
                val i0 = vertex(p(band.inner, a0), nrm)
                val i1 = vertex(p(band.outer, a0), nrm)
                val i2 = vertex(p(band.outer, a1), nrm)
                val i3 = vertex(p(band.inner, a1), nrm)
                addTriIndices(i0, i1, i2)
                addTriIndices(i0, i2, i3)
            }
        }
    }
}
