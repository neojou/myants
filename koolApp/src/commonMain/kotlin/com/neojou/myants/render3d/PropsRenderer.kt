package com.neojou.myants.render3d

import com.neojou.myants.render3d.actors.draw
import com.neojou.myants.render3d.quality.QualityPreset
import com.neojou.myants.render3d.terrain.GroundHeight
import com.neojou.myants.sim.PerformanceManager
import com.neojou.myants.sim.SimConfig
import de.fabmax.kool.modules.ksl.KslUnlitShader
import de.fabmax.kool.pipeline.BlendMode
import de.fabmax.kool.pipeline.CullMethod
import de.fabmax.kool.pipeline.DepthCompareOp
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.Node
import de.fabmax.kool.scene.VertexLayouts
import de.fabmax.kool.scene.addMesh
import de.fabmax.kool.util.Color
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope

/**
 * Grass, clover, pebbles, leaves, flowers, sim obstacles and rain (#47).
 * [stems] is the shared block batch that fallback flower stems draw into.
 */
internal class PropsRenderer(
    private val scope: CoroutineScope,
    private val stems: () -> Batch,
) : SubRenderer {
    private val inst = Instancer()
    private lateinit var grass: Batch
    internal lateinit var grassMesh: Mesh<*>
    private lateinit var rocks: Batch
    private lateinit var rockMesh: Mesh<*>
    private lateinit var leaves: Batch
    private lateinit var leafMesh: Mesh<*>
    private lateinit var flowers: Batch
    private lateinit var flowerMesh: Mesh<*>
    private lateinit var rain: Batch
    private var cloverReady = false
    private var bloomReady = false
    private var scatterKey = ""
    private var extraGrass = FloatArray(0)
    private var pebbles = FloatArray(0)
    private var leafPos = FloatArray(0)
    private var flowerPos = FloatArray(0)
    private var rainX = FloatArray(0)
    private var rainZ = FloatArray(0)
    private var rainPhase = FloatArray(0)

    override fun build(opaque: Node, forward: Node) {
        grass = inst.batch(8192)
        grassMesh = inst.instanced(opaque, grass, "grass", WorldPalette.LEAF_ROUGHNESS, doubleSided = true) {
            cube { size.set(0.2f, 1f, 0.45f) }
        }
        rocks = inst.batch(1024)
        rockMesh = inst.instanced(opaque, rocks, "rock") { cube { size.set(1f, 1f, 1f) } }
        leaves = inst.batch(1024)
        leafMesh = inst.instanced(opaque, leaves, "leaf") { cube { size.set(1.2f, 0.08f, 0.45f) } }
        flowers = inst.batch(4096)
        flowerMesh = inst.instanced(opaque, flowers, "flower", WorldPalette.PETAL_ROUGHNESS) { icoSphere { steps = 1; radius = 0.45f } }
        rain = inst.batch(1024)
        val rainShader = KslUnlitShader {
            vertices { instancedModelMatrix(inst.layout.modelMat) }
            color { instanceColor(inst.layout.color) }
            pipeline {
                blendMode = BlendMode.BLEND_ADDITIVE
                depthTest = DepthCompareOp.LESS
                isWriteDepth = false
                cullMethod = CullMethod.NO_CULLING
            }
        }
        forward.addMesh(VertexLayouts.Position, instances = rain) {
            isFrustumChecked = false
            generate { rect { size.set(1f, 1f) } }
            shader = rainShader
        }
        loadMergedGlb(scope, "models/nature/pebble.glb", listOf(rockMesh), "pebble", "cube") { }
        loadMergedGlb(scope, "models/nature/leaf.glb", listOf(leafMesh), "leaf", "slab") { }
        loadMergedGlb(scope, "models/nature/clover_tuft.glb", listOf(grassMesh), "clover", "blade") { cloverReady = true }
        loadMergedGlb(scope, "models/nature/clover_flower.glb", listOf(flowerMesh), "clover flower", "sphere") { bloomReady = true }
    }

    override fun update(state: RenderState, preset: QualityPreset, dt: Float) {
        rebuildScatter(state, preset)
        grass.clear(); rocks.clear(); leaves.clear(); flowers.clear()
        val time = state.animTime
        pushScatter(state, preset, time)
        pushSimGrass(state, preset, time)
        pushObstacles(state)
        pushRain(state, preset)
    }

    private fun rebuildScatter(state: RenderState, preset: QualityPreset) {
        val key = "${state.worldSerial}|${preset.fineGrass}|${preset.pebbles}|${preset.leaves}|${preset.flowers}|${state.view.grassDensity}"
        if (key == scatterKey) return
        scatterKey = key
        val fields = ScatterLayout.build(state.seed, state.worldSerial, preset, state.view.grassDensity)
        extraGrass = fields.extraGrass
        pebbles = fields.pebbles
        leafPos = fields.leaves
        flowerPos = fields.flowers
    }

    private fun pushScatter(state: RenderState, preset: QualityPreset, time: Float) {
        val sway = preset.grassSway
        var i = 0
        while (i < extraGrass.size) {
            val x = extraGrass[i]
            val z = extraGrass[i + 1]
            val yaw = extraGrass[i + 2] * 6.2f + if (sway) sin(time * 1.7f + x * 0.02f) * 0.25f else 0f
            val shade = extraGrass[i + 3]
            if (cloverReady) {
                // 3–6 world units is only a few pixels at the default camera, so the lawn
                // stayed bare. This span is what still reads as a patch, and as a white head.
                placeClover(x, z, yaw, 10f + shade * 8f, WorldPalette.scatterGrass(shade))
            } else {
                val h = 3f + shade * 4f
                val y = GroundHeight.at(x, z) + h * 0.5f
                inst.pose(grass, x, y, z, yaw, 0f, 0f, 0f, 0.35f, h, 0.7f, WorldPalette.scatterGrass(shade))
            }
            i += 4
        }
        i = 0
        while (i < pebbles.size) {
            val x = pebbles[i]; val z = pebbles[i + 1]
            val s = 1.5f + pebbles[i + 2] * 4f
            val y = GroundHeight.at(x, z) + s * 0.35f
            inst.pose(rocks, x, y, z, pebbles[i + 3] * 3f, 0f, 0f, 0f, s, s * 0.6f, s * 0.8f, WorldPalette.pebble)
            i += 4
        }
        i = 0
        while (i < leafPos.size) {
            val x = leafPos[i]; val z = leafPos[i + 1]
            val y = GroundHeight.at(x, z) + 0.2f
            inst.pose(leaves, x, y, z, leafPos[i + 2] * 6f, 0f, 0f, 0f, 2.2f, 0.15f, 1f, WorldPalette.leaf)
            i += 4
        }
        if (!bloomReady) {
            i = 0
            val petal = WorldPalette.petals
            while (i < flowerPos.size) {
                val x = flowerPos[i]; val z = flowerPos[i + 1]
                val y = GroundHeight.at(x, z) + 1.6f
                inst.pose(flowers, x, y, z, 0f, 0f, 0f, 0f, 1.4f, 1.4f, 1.4f, petal[(flowerPos[i + 2] * 4f).toInt().coerceIn(0, 3)])
                inst.pose(stems(), x, y - 1.3f, z, 0f, 0f, 0f, 0f, 0.15f, 1.6f, 0.15f, WorldPalette.stem)
                i += 4
            }
        }
    }

    private fun placeClover(x: Float, z: Float, yaw: Float, scale: Float, color: Color) {
        // A hair above the heightfield so the flat leaflets win the depth test.
        val y = GroundHeight.at(x, z) + 0.2f
        inst.pose(grass, x, y, z, yaw, 0f, 0f, 0f, scale, scale, scale, color)
        if (bloomReady && cloverBlooms(x, z)) {
            inst.pose(flowers, x, y, z, yaw, 0f, 0f, 0f, scale, scale, scale, cloverPetal(x, z))
        }
    }

    private fun pushSimGrass(state: RenderState, preset: QualityPreset, time: Float) {
        if (PerformanceManager.settings.simpleAnts) return
        val sway = preset.grassSway
        for (tuft in state.world.grass) {
            val x = tuft.x.toFloat()
            val z = tuft.y.toFloat()
            val yaw = WorldFrames.yawRad(tuft.angle.toFloat()) + if (sway) sin(time * 1.5f + x) * 0.2f else 0f
            if (cloverReady) {
                placeClover(x, z, yaw, tuft.size.toFloat().coerceAtLeast(0.4f) * 4f, WorldPalette.simGrass)
            } else {
                val h = (tuft.size * 3.0).toFloat().coerceAtLeast(1.5f)
                val y = GroundHeight.at(x, z) + h * 0.5f
                inst.pose(grass, x, y, z, yaw, 0f, 0f, 0f, tuft.size.toFloat().coerceAtLeast(0.4f), h, 0.6f, WorldPalette.simGrass)
            }
        }
    }

    private fun pushObstacles(state: RenderState) {
        for (obs in state.world.terrain.obstacles) {
            val x = obs.x.toFloat()
            val z = obs.y.toFloat()
            val r = obs.radius.toFloat()
            val y = GroundHeight.at(x, z) + r * 0.35f
            inst.pose(rocks, x, y, z, 0.4f, 0f, 0f, 0f, r * 1.4f, r * 0.7f, r * 1.1f, WorldPalette.rock)
        }
    }

    private fun pushRain(state: RenderState, preset: QualityPreset) {
        rain.clear()
        val world = state.world
        if (!world.raining || !state.view.weatherUser || preset.rainStreaks <= 0) return
        if (rainX.size != preset.rainStreaks) {
            val rnd = Random(state.seed.toLong() xor 0xA41L)
            rainX = FloatArray(preset.rainStreaks) { rnd.nextFloat() * SimConfig.width }
            rainZ = FloatArray(preset.rainStreaks) { rnd.nextFloat() * SimConfig.height }
            rainPhase = FloatArray(preset.rainStreaks) { rnd.nextFloat() }
        }
        val t = state.animTime
        val color = WorldPalette.rain3d
        for (i in rainX.indices) {
            val drop = ((t * 0.35f + rainPhase[i]) % 1f)
            val y = 80f - drop * 80f
            inst.pose(rain, rainX[i], y, rainZ[i], 0f, 0f, 0f, 0f, 0.35f, 14f, 0.35f, color)
        }
    }
}
