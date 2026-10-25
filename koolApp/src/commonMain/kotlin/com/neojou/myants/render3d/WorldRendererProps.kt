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

private const val TAG = "RENDER"

// Ground, scatter, clock, obstacles and rain for [WorldRenderer].

internal fun WorldRenderer.rebuildScatter(preset: QualityPreset) {
    val key = "${state.worldSerial}|${preset.fineGrass}|${preset.pebbles}|${preset.leaves}|${preset.flowers}|${state.view.grassDensity}"
    if (key == scatterKey) return
    scatterKey = key
    val fields = ScatterLayout.build(state.seed, state.worldSerial, preset, state.view.grassDensity)
    extraGrass = fields.extraGrass
    pebbles = fields.pebbles
    leafPos = fields.leaves
    flowerPos = fields.flowers
}

internal fun WorldRenderer.pushScatter(preset: QualityPreset, time: Float) {
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
            pose(grass, x, y, z, yaw, 0f, 0f, 0f, 0.35f, h, 0.7f, WorldPalette.scatterGrass(shade))
        }
        i += 4
    }
    i = 0
    while (i < pebbles.size) {
        val x = pebbles[i]; val z = pebbles[i + 1]
        val s = 1.5f + pebbles[i + 2] * 4f
        val y = GroundHeight.at(x, z) + s * 0.35f
        pose(rocks, x, y, z, pebbles[i + 3] * 3f, 0f, 0f, 0f, s, s * 0.6f, s * 0.8f, WorldPalette.pebble)
        i += 4
    }
    i = 0
    while (i < leafPos.size) {
        val x = leafPos[i]; val z = leafPos[i + 1]
        val y = GroundHeight.at(x, z) + 0.2f
        pose(leaves, x, y, z, leafPos[i + 2] * 6f, 0f, 0f, 0f, 2.2f, 0.15f, 1f, WorldPalette.leaf)
        i += 4
    }
    if (!bloomReady) {
        i = 0
        val petal = WorldPalette.petals
        while (i < flowerPos.size) {
            val x = flowerPos[i]; val z = flowerPos[i + 1]
            val y = GroundHeight.at(x, z) + 1.6f
            pose(flowers, x, y, z, 0f, 0f, 0f, 0f, 1.4f, 1.4f, 1.4f, petal[(flowerPos[i + 2] * 4f).toInt().coerceIn(0, 3)])
            pose(bits, x, y - 1.3f, z, 0f, 0f, 0f, 0f, 0.15f, 1.6f, 0.15f, WorldPalette.stem)
            i += 4
        }
    }
}

internal fun WorldRenderer.placeClover(x: Float, z: Float, yaw: Float, scale: Float, color: Color) {
    // A hair above the heightfield so the flat leaflets win the depth test.
    val y = GroundHeight.at(x, z) + 0.2f
    pose(grass, x, y, z, yaw, 0f, 0f, 0f, scale, scale, scale, color)
    if (bloomReady && cloverBlooms(x, z)) {
        pose(flowers, x, y, z, yaw, 0f, 0f, 0f, scale, scale, scale, cloverPetal(x, z))
    }
}

internal fun WorldRenderer.pushSimGrass(preset: QualityPreset, time: Float) {
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
            pose(grass, x, y, z, yaw, 0f, 0f, 0f, tuft.size.toFloat().coerceAtLeast(0.4f), h, 0.6f, WorldPalette.simGrass)
        }
    }
}

internal fun WorldRenderer.pushObstacles() {
    for (obs in state.world.terrain.obstacles) {
        val x = obs.x.toFloat()
        val z = obs.y.toFloat()
        val r = obs.radius.toFloat()
        val y = GroundHeight.at(x, z) + r * 0.35f
        pose(rocks, x, y, z, 0.4f, 0f, 0f, 0f, r * 1.4f, r * 0.7f, r * 1.1f, WorldPalette.rock)
    }
}

internal fun WorldRenderer.pushRain(preset: QualityPreset) {
    rain.clear()
    val world = state.world
    if (!world.raining || !state.view.weatherUser || preset.rainStreaks <= 0) return
    if (rainX.size != preset.rainStreaks) {
        val rnd = Random(state.seed.toLong() xor 0xA41L)
        rainX = FloatArray(preset.rainStreaks) { rnd.nextFloat() * SimConfig.width }
        rainZ = FloatArray(preset.rainStreaks) { rnd.nextFloat() * SimConfig.height }
        rainPhase = FloatArray(preset.rainStreaks) { rnd.nextFloat() }
    }
    val t = Time.gameTime.toFloat()
    val color = WorldPalette.rain3d
    for (i in rainX.indices) {
        val drop = ((t * 0.35f + rainPhase[i]) % 1f)
        val y = 80f - drop * 80f
        pose(rain, rainX[i], y, rainZ[i], 0f, 0f, 0f, 0f, 0.35f, 14f, 0.35f, color)
    }
}
