package com.neojou.myants.render3d

import com.neojou.myants.render3d.camera.CameraDirector
import com.neojou.myants.render3d.camera.TopDownCamera
import com.neojou.myants.render3d.nest.NestCutaway
import com.neojou.myants.render3d.quality.QualityPreset
import com.neojou.myants.sim.PerformanceManager
import com.neojou.myants.sim.SimConfig
import com.neojou.tools.LogLevel
import com.neojou.tools.MyLog
import de.fabmax.kool.KoolApplication
import de.fabmax.kool.addScene
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.spatial.BoundingBoxF
import de.fabmax.kool.pipeline.ClearColorFill
import de.fabmax.kool.pipeline.DepthCompareOp
import de.fabmax.kool.pipeline.deferred.DeferredPipeline
import de.fabmax.kool.pipeline.deferred.DeferredPipelineConfig
import de.fabmax.kool.pipeline.ibl.EnvironmentMap
import de.fabmax.kool.pipeline.ibl.SkyCubeIblSystem
import de.fabmax.kool.scene.Light
import de.fabmax.kool.scene.Node
import de.fabmax.kool.scene.OrthographicCamera
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.ColorGradient
import de.fabmax.kool.util.SimpleShadowMap
import de.fabmax.kool.util.Time

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
    internal val terrain = TerrainRenderer()
    internal val overlay = OverlayRenderer()
    internal val actors = ActorsRenderer(scene.coroutineScope) { deferred }
    internal val props = PropsRenderer(scene.coroutineScope) { actors.bits }
    internal var deferred: DeferredPipeline? = null
    internal var shadow: SimpleShadowMap? = null
    internal var built = false
    internal var loggedBackend = false
    internal var fpsSamples = 0
    internal var fpsSum = 0
    internal var lastFps = -1


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
        terrain.build(content, forward)
        props.build(content, forward)
        actors.build(content, forward)
        overlay.build(content, forward)

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
        terrain.sun = light

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

        terrain.sky = try {
            SkyCubeIblSystem.simpleSkyCubeIblSystem(scene).apply { isAutoUpdateIblMaps = false }
        } catch (err: Throwable) {
            MyLog.add(TAG, "Sky cube unavailable: ${err.message}", LogLevel.DEBUG)
            null
        }
        val env = terrain.sky?.environmentMap ?: try {
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
        props.grassMesh.isCastingShadow = preset.grassCastsShadow
    }

    internal fun fill(preset: QualityPreset, winW: Float, winH: Float) {
        terrain.update(state, preset, Time.deltaT)
        actors.update(state, preset, Time.deltaT)
        props.update(state, preset, Time.deltaT)
        overlay.update(state, preset, Time.deltaT)
        NestCutaway.update(state, winW, winH)
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

}
