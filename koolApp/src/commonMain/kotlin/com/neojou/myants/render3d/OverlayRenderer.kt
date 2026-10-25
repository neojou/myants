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

/** Inspect selection ring (WORLD space) and the clock-stone digits (#47). */
internal class OverlayRenderer : SubRenderer {
    private val inst = Instancer()
    private lateinit var digits: Batch
    private lateinit var ring: Mesh<*>

    override fun build(opaque: Node, forward: Node) {
        digits = inst.batch(128)
        inst.instanced(opaque, digits, "digit") { cube { size.set(1f, 1f, 1f) } }
        val ringShader = KslUnlitShader {
            color { vertexColor() }
            pipeline {
                cullMethod = CullMethod.NO_CULLING
                depthTest = DepthCompareOp.LESS
                isWriteDepth = false
            }
        }
        ring = forward.addMesh(VertexLayouts.PositionColor) {
            isFrustumChecked = false
            isVisible = false
            generate { selectRing(xz = true) }
            shader = ringShader
        }
    }

    override fun update(state: RenderState, preset: QualityPreset, dt: Float) {
        digits.clear()
        pushClock(state)
        // inspectRing allocates on every read, so read it once per frame.
        val mark = state.inspectRing?.takeIf { it.space == RingSpace.WORLD }
        if (mark != null) {
            ring.isVisible = true
            val y = GroundHeight.at(mark.x.toFloat(), mark.y.toFloat()) + 0.4f
            val t = ring.transform as TrsTransformF
            t.translation.set(mark.x.toFloat(), y, mark.y.toFloat())
            val s = mark.scale
            t.scale.set(s, s, s)
            t.markDirty()
        } else {
            ring.isVisible = false
        }
    }

    private fun pushClock(state: RenderState) {
        val (hh, mm, ss) = wallClockHms()
        val text = "${hh.toString().padStart(2, '0')}${mm.toString().padStart(2, '0')}"
        val stone = state.world.terrain.clockStone
        val metrics = clockMetrics(SimConfig.clock.fontSize.toFloat())
        val slots = clockSlots(stone.x.toFloat(), metrics)
        val z = stone.y.toFloat() + stone.radius.toFloat() * 0.15f
        val y = GroundHeight.at(stone.x.toFloat(), stone.y.toFloat()) + 52f
        val color = WorldPalette.digit3d
        if (clockColonLit(ss)) {
            val along = clockCellAlong(metrics.digitHeight)
            val dot = metrics.dotSize
            val up = flatClockDot(slots.colonX, y, z, along, dot, dot)
            val down = flatClockDot(slots.colonX, y, z, -along, dot, dot)
            inst.pose(digits, up.x, up.y, up.z, 0f, 0f, 0f, 0f, up.sx, up.sy, up.sz, color)
            inst.pose(digits, down.x, down.y, down.z, 0f, 0f, 0f, 0f, down.sx, down.sy, down.sz, color)
        }
        val centers = floatArrayOf(slots.hourTens, slots.hourOnes, slots.minuteTens, slots.minuteOnes)
        for (i in text.indices) {
            for (stroke in clockStrokes(centers[i], y, metrics, text[i] - '0')) {
                val box = flatClockSeg(y, z, stroke.x, stroke.y, stroke.sx, stroke.sy, metrics.thickness)
                inst.pose(digits, box.x, box.y, box.z, 0f, 0f, 0f, 0f, box.sx, box.sy, box.sz, color)
            }
        }
    }
}
