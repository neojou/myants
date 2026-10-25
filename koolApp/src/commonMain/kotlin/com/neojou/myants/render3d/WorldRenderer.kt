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

fun KoolApplication.addWorldScene(state: RenderState, host: RenderHost) = addScene {
    clearColor = ClearColorFill(WorldPalette.sky)
    WorldRenderer(this, state, host).install()
}

/**
 * Deferred world. Translucent rain and the selection marker are drawn
 * after [DeferredPipeline.createDefaultOutputQuad]. Bloom is requested only
 * when [bloomAllowed] is true.
 */
class WorldRenderer(
    internal val scene: de.fabmax.kool.scene.Scene,
    internal val state: RenderState,
    internal val host: RenderHost,
) {
    internal val instancer = Instancer()
    internal val layout get() = instancer.layout
    internal val mat get() = instancer.mat
    internal val scale get() = instancer.scale
    internal val phero = PheromoneField("phero3d")
    internal var groundShader: de.fabmax.kool.pipeline.deferred.DeferredKslPbrShader? = null
    internal var deferred: DeferredPipeline? = null
    internal var sky: SkyCubeIblSystem? = null
    internal var shadow: SimpleShadowMap? = null
    internal var sun: Light.Directional? = null
    internal var built = false
    internal var loggedBackend = false
    internal var serial = -1
    internal var iblStamp = -1.0
    internal var fpsSamples = 0
    internal var fpsSum = 0
    internal var lastFps = -1
    internal val lights = ArrayList<de.fabmax.kool.pipeline.deferred.DeferredPointLights.PointLight>()

    internal lateinit var ground: Mesh<*>
    internal lateinit var grass: MeshInstanceList<InstanceLayouts.ModelMatColor>
    internal lateinit var grassMesh: Mesh<*>
    internal lateinit var rocks: MeshInstanceList<InstanceLayouts.ModelMatColor>
    internal lateinit var rockMesh: Mesh<*>
    internal lateinit var leaves: MeshInstanceList<InstanceLayouts.ModelMatColor>
    internal lateinit var leafMesh: Mesh<*>
    internal lateinit var flowers: MeshInstanceList<InstanceLayouts.ModelMatColor>
    internal lateinit var flowerMesh: Mesh<*>
    internal var cloverReady = false
    internal var bloomReady = false
    internal lateinit var bodies: MeshInstanceList<InstanceLayouts.ModelMatColor>
    internal lateinit var heads: MeshInstanceList<InstanceLayouts.ModelMatColor>
    internal lateinit var legs: MeshInstanceList<InstanceLayouts.ModelMatColor>
    internal lateinit var workerAnt: AntSlot
    internal lateinit var soldierAnt: AntSlot
    internal var greenAphid: GltfAphid? = null
    internal lateinit var aphidBody: MeshInstanceList<InstanceLayouts.ModelMatColor>
    internal lateinit var aphidEyes: MeshInstanceList<InstanceLayouts.ModelMatColor>
    internal lateinit var aphidLegs: List<MeshInstanceList<InstanceLayouts.ModelMatColor>>
    internal lateinit var aphidBodyMesh: Mesh<*>
    internal lateinit var aphidEyeMesh: Mesh<*>
    internal lateinit var aphidLegMeshes: List<Mesh<*>>
    internal lateinit var molts: MeshInstanceList<InstanceLayouts.ModelMatColor>
    internal lateinit var honey: MeshInstanceList<InstanceLayouts.ModelMatColor>
    internal val aphidPrevX = HashMap<Int, Float>()
    internal val aphidPrevZ = HashMap<Int, Float>()
    internal val critters = ArrayList<CritterSlot>()
    internal val critterPrevX = HashMap<Int, Float>()
    internal val critterPrevZ = HashMap<Int, Float>()
    internal lateinit var food: MeshInstanceList<InstanceLayouts.ModelMatColor>
    internal lateinit var sugar: MeshInstanceList<InstanceLayouts.ModelMatColor>
    internal lateinit var protein: MeshInstanceList<InstanceLayouts.ModelMatColor>
    internal lateinit var carrySugar: MeshInstanceList<InstanceLayouts.ModelMatColor>
    internal lateinit var carryProtein: MeshInstanceList<InstanceLayouts.ModelMatColor>
    internal lateinit var sugarMesh: Mesh<*>
    internal lateinit var proteinMesh: Mesh<*>
    internal lateinit var carrySugarMesh: Mesh<*>
    internal lateinit var carryProteinMesh: Mesh<*>
    internal var sugarReady = false
    internal var proteinReady = false
    internal lateinit var insects: MeshInstanceList<InstanceLayouts.ModelMatColor>
    internal lateinit var digits: MeshInstanceList<InstanceLayouts.ModelMatColor>
    internal lateinit var bits: MeshInstanceList<InstanceLayouts.ModelMatColor>
    internal lateinit var rain: MeshInstanceList<InstanceLayouts.ModelMatColor>
    internal lateinit var ring: Mesh<*>
    internal var scatterKey = ""
    internal var extraGrass = FloatArray(0)
    internal var pebbles = FloatArray(0)
    internal var leafPos = FloatArray(0)
    internal var flowerPos = FloatArray(0)
    internal var rainX = FloatArray(0)
    internal var rainZ = FloatArray(0)
    internal var rainPhase = FloatArray(0)

    fun install() {
        host.onWorldSceneReady(scene)
        val cam = OrthographicCamera()
        scene.camera = cam
        val top = TopDownCamera(cam)
        top.attach(scene)
        val director = CameraDirector(top)
        director.request(com.neojou.myants.render3d.camera.CameraMode.ANT_POV)
        WorldInput.screenToGround = { x, y -> top.screenToGround(x, y) }
        WorldInput.groundToScreen = { x, y -> top.groundToScreen(x, y) }

        val content = Node("world-opaque").apply { isFrustumChecked = false }
        val forward = Node("world-forward").apply { isFrustumChecked = false }
        buildContent(content)
        buildForward(forward)
        loadAnt("models/ants/worker_black.glb", workerAnt, "worker")
        loadAnt("models/ants/soldier.glb", soldierAnt, "soldier")
        loadGreenAphid()
        for (slot in critters) loadCritter(slot)
        loadMerged("models/food/sugar.glb", listOf(sugarMesh, carrySugarMesh), "sugar") { sugarReady = true }
        loadMerged("models/food/protein.glb", listOf(proteinMesh, carryProteinMesh), "protein") { proteinReady = true }
        loadMerged("models/nature/pebble.glb", listOf(rockMesh), "pebble", "cube") { }
        loadMerged("models/nature/leaf.glb", listOf(leafMesh), "leaf", "slab") { }
        loadMerged("models/nature/clover_tuft.glb", listOf(grassMesh), "clover", "blade") { cloverReady = true }
        loadMerged("models/nature/clover_flower.glb", listOf(flowerMesh), "clover flower", "sphere") { bloomReady = true }
        val clock = stoneMesh
        if (clock != null) loadMerged("models/nature/clock_stone.glb", listOf(clock), "clock stone") { }

        scene.onUpdate {
            if (state.renderer != RenderMode.D3) return@onUpdate
            host.onFrame(Time.deltaT)
            if (!loggedBackend) {
                loggedBackend = true
                MyLog.add(TAG, "backend ${backendLabel()} compute=${de.fabmax.kool.KoolSystem.features.computeShaders}", LogLevel.DEBUG)
            }
            ensurePipeline(content, forward)
            top.update(Time.deltaT, state.camera, host)
            val preset = QualityPreset.from(PerformanceManager.level, state.renderTierBias)
            applyPreset(preset)
            val winW = it.ctx.window.size.x.toFloat().coerceAtLeast(1f)
            val winH = it.ctx.window.size.y.toFloat().coerceAtLeast(1f)
            fill(preset, winW, winH)
            watchFps()
        }
    }

    internal fun ensurePipeline(content: Node, forward: Node) {
        if (built) return
        built = true
        try {
            mountDeferred(content, forward)
        } catch (err: Throwable) {
            MyLog.add(TAG, "Deferred pipeline failed: ${err.message}. Falling back to the classic renderer.", LogLevel.DEBUG)
            host.requestRenderer(RenderMode.CLASSIC)
        }
    }

    internal fun mountDeferred(content: Node, forward: Node) {
        scene.lighting.clear()
        val light = Light.Directional()
        light.setup(Vec3f(0.3f, -1f, 0.2f)).setColor(Color.WHITE, 3f)
        scene.lighting.addLight(light)
        sun = light

        val map = try {
            SimpleShadowMap(scene, light, drawNode = content, mapSize = 2048).apply {
                shadowBounds = BoundingBoxF(
                    Vec3f(-20f, -8f, -20f),
                    Vec3f(SimConfig.width + 20f, 40f, SimConfig.height + 20f),
                )
            }
        } catch (err: Throwable) {
            MyLog.add(TAG, "Shadow map unavailable: ${err.message}", LogLevel.DEBUG)
            null
        }
        shadow = map

        sky = try {
            SkyCubeIblSystem.simpleSkyCubeIblSystem(scene).apply { isAutoUpdateIblMaps = false }
        } catch (err: Throwable) {
            MyLog.add(TAG, "Sky cube unavailable: ${err.message}", LogLevel.DEBUG)
            null
        }
        val env = sky?.environmentMap ?: try {
            EnvironmentMap.fromGradientColor(
                ColorGradient(
                    WorldPalette.skyZenith,
                    WorldPalette.skyHorizon,
                    WorldPalette.skyGround,
                ),
            )
        } catch (err: Throwable) {
            MyLog.add(TAG, "Gradient IBL unavailable: ${err.message}", LogLevel.DEBUG)
            null
        }

        val cfg = DeferredPipelineConfig().apply {
            isWithAmbientOcclusion = true
            isWithScreenSpaceReflections = false
            isWithBloom = bloomAllowed(state.view)
            isWithVignette = false
            // Reversed-Z turns LESS into GREATER. The output quad sits at clip z = 0,
            // which does not pass that test against a cleared depth buffer.
            outputDepthTest = DepthCompareOp.ALWAYS
            maxGlobalLights = 2
            if (env != null) useImageBasedLighting(env)
            if (map != null && state.view.shadowsUser) useShadowMaps(listOf(map))
        }
        val pipe = DeferredPipeline(scene, cfg)
        // MaterialPass only installs a proxy when the scene camera is perspective.
        // An orthographic camera would otherwise leave the g-buffer on the default
        // perspective camera, which never sees the ground.
        for (pass in pipe.passes) {
            pass.materialPass.camera = scene.camera
            pass.lightingPass.camera = scene.camera
        }
        pipe.bloomStrength = 0.65f
        pipe.bloomThreshold = 1f
        pipe.bloomRadius = 2f
        if (env != null) pipe.lightingPassShader.ambientMap = env.irradianceMap
        deferred = pipe
        pipe.sceneContent += content
        val quad = pipe.createDefaultOutputQuad()
        scene += quad
        scene += forward
        host.applyRendererVisibility()
    }

    internal fun applyPreset(preset: QualityPreset) {
        val pipe = deferred ?: return
        pipe.renderResolution = preset.renderResolution
        pipe.isAoEnabled = state.view.ssaoUser && preset.ssaoScale != null
        preset.ssaoScale?.let { pipe.aoMapSize = it }
        val bloom = bloomAllowed(state.view) && preset.bloomRequested
        pipe.isBloomEnabled = bloom
        shadow?.isShadowMapEnabled = state.view.shadowsUser && preset.shadow != com.neojou.myants.render3d.quality.ShadowKind.OFF
        grassMesh.isCastingShadow = preset.grassCastsShadow
    }

    internal fun fill(preset: QualityPreset, winW: Float, winH: Float) {
        if (serial != state.worldSerial) {
            serial = state.worldSerial
            GroundHeight.rebuild(state.seed, serial)
            rebuildGround()
            scatterKey = ""
        }
        rebuildScatter(preset)
        updateSun()
        updatePheromone()
        pushActors(preset)
        pushRain(preset)
        NestCutaway.update(state, winW, winH)
    }

    internal fun updateSun() {
        val light = sun ?: return
        val world = state.world
        val bright = if (state.view.dayNight) world.dayBrightness().toFloat() else 1f
        val elev = if (state.view.dayNight) 6f + bright * 64f else 70f
        val az = (world.timeOfDay * 160.0 - 80.0).toFloat()
        val elevRad = elev * (PI.toFloat() / 180f)
        val azRad = az * (PI.toFloat() / 180f)
        val dir = Vec3f(
            sin(azRad) * cos(elevRad),
            -sin(elevRad),
            cos(azRad) * cos(elevRad),
        )
        val night = bright < 0.2f
        val color = if (night) WorldPalette.moonLight else WorldPalette.sunLight
        val intensity = if (night) 0.35f else 0.4f + bright * 3.2f
        light.setup(dir).setColor(color, intensity)
        val cube = sky
        if (cube != null) {
            cube.skyPass.elevation = elev
            cube.skyPass.azimuth = az + 180f
            val now = Time.gameTime
            if (iblStamp < 0.0 || now - iblStamp > 2.0) {
                iblStamp = now
                cube.updateIblMaps()
            }
        }
    }

    internal fun updatePheromone() {
        val shader = groundShader ?: return
        val show = state.view.showPheromones && state.view.pheromoneStrength > 0.0
        val tex = phero.update(state.world.colonies[0].outdoorField, state.view.pheromoneStrength, show)
        if (tex != null) shader.emissionMap = tex
        val bright = if (state.view.dayNight) state.world.dayBrightness().toFloat() else 1f
        val boost = if (!show) 0f else 1.6f + (1f - bright) * 2.4f
        shader.emission = WorldPalette.emission(boost)
        val wet = state.world.raining && state.view.weatherUser
        val k = 0.28f + 0.72f * bright
        shader.color = WorldPalette.groundTint(k, wet)
        shader.roughness = if (wet) 0.28f else 0.92f
    }

    internal fun watchFps() {
        val fps = state.fpsShown
        if (fps <= 0 || fps == lastFps) return
        lastFps = fps
        fpsSum += fps
        fpsSamples++
        if (fpsSamples >= 5) {
            val avg = fpsSum / fpsSamples
            fpsSum = 0
            fpsSamples = 0
            if (avg < 40 && state.renderTierBias < 3) {
                host.bumpRenderTierBias()
                MyLog.add(TAG, "Visual tier bias ${state.renderTierBias} after ${avg} fps", LogLevel.DEBUG)
            }
        }
    }

    internal var stoneMesh: Mesh<*>? = null
}
