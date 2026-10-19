package com.neojou.myants.render3d

import com.neojou.myants.AppSession
import com.neojou.myants.render3d.actors.PoseBuckets
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
import com.neojou.myants.wallClockHm
import com.neojou.tools.LogLevel
import com.neojou.tools.MyLog
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
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val TAG = "RENDER"

private val SEGMENTS = intArrayOf(
    0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7D, 0x07, 0x7F, 0x6F,
)

fun KoolApplication.addWorldScene(session: AppSession) = addScene {
    clearColor = ClearColorFill(Color(0.45f, 0.62f, 0.82f, 1f))
    WorldRenderer(this, session).install()
}

/**
 * Deferred world. Translucent rain and the selection marker are drawn
 * after [DeferredPipeline.createDefaultOutputQuad]. Bloom is requested only
 * when [bloomAllowed] is true.
 */
class WorldRenderer(
    private val scene: de.fabmax.kool.scene.Scene,
    private val session: AppSession,
) {
    private val layout = InstanceLayouts.ModelMatColor
    private val mat = MutableMat4f()
    private val scale = MutableVec3f()
    private val phero = PheromoneField("phero3d")
    private var groundShader: de.fabmax.kool.pipeline.deferred.DeferredKslPbrShader? = null
    private var deferred: DeferredPipeline? = null
    private var sky: SkyCubeIblSystem? = null
    private var shadow: SimpleShadowMap? = null
    private var sun: Light.Directional? = null
    private var built = false
    private var loggedBackend = false
    private var serial = -1
    private var iblStamp = -1.0
    private var fpsSamples = 0
    private var fpsSum = 0
    private var lastFps = -1
    private val lights = ArrayList<de.fabmax.kool.pipeline.deferred.DeferredPointLights.PointLight>()

    private lateinit var ground: Mesh<*>
    private lateinit var grass: MeshInstanceList<InstanceLayouts.ModelMatColor>
    private lateinit var grassMesh: Mesh<*>
    private lateinit var rocks: MeshInstanceList<InstanceLayouts.ModelMatColor>
    private lateinit var leaves: MeshInstanceList<InstanceLayouts.ModelMatColor>
    private lateinit var flowers: MeshInstanceList<InstanceLayouts.ModelMatColor>
    private lateinit var bodies: MeshInstanceList<InstanceLayouts.ModelMatColor>
    private lateinit var heads: MeshInstanceList<InstanceLayouts.ModelMatColor>
    private lateinit var legs: MeshInstanceList<InstanceLayouts.ModelMatColor>
    private lateinit var food: MeshInstanceList<InstanceLayouts.ModelMatColor>
    private lateinit var insects: MeshInstanceList<InstanceLayouts.ModelMatColor>
    private lateinit var digits: MeshInstanceList<InstanceLayouts.ModelMatColor>
    private lateinit var bits: MeshInstanceList<InstanceLayouts.ModelMatColor>
    private lateinit var rain: MeshInstanceList<InstanceLayouts.ModelMatColor>
    private lateinit var ring: Mesh<*>
    private var scatterKey = ""
    private var extraGrass = FloatArray(0)
    private var pebbles = FloatArray(0)
    private var leafPos = FloatArray(0)
    private var flowerPos = FloatArray(0)
    private var rainX = FloatArray(0)
    private var rainZ = FloatArray(0)
    private var rainPhase = FloatArray(0)

    fun install() {
        session.worldScene = scene
        val cam = OrthographicCamera()
        scene.camera = cam
        val top = TopDownCamera(cam)
        top.attach(scene)
        val director = CameraDirector(top)
        director.request(com.neojou.myants.render3d.camera.CameraMode.ANT_POV)
        WorldInput.screenToGround = { x, y -> top.screenToGround(x, y) }

        val content = Node("world-opaque").apply { isFrustumChecked = false }
        val forward = Node("world-forward").apply { isFrustumChecked = false }
        buildContent(content)
        buildForward(forward)

        scene.onUpdate {
            if (session.renderer != RenderMode.D3) return@onUpdate
            session.tick(Time.deltaT)
            if (!loggedBackend) {
                loggedBackend = true
                MyLog.add(TAG, "backend ${backendLabel()} compute=${de.fabmax.kool.KoolSystem.features.computeShaders}", LogLevel.DEBUG)
            }
            ensurePipeline(content, forward)
            top.update(Time.deltaT, session)
            val preset = QualityPreset.from(PerformanceManager.level, session.renderTierBias)
            applyPreset(preset)
            val winW = it.ctx.window.size.x.toFloat().coerceAtLeast(1f)
            val winH = it.ctx.window.size.y.toFloat().coerceAtLeast(1f)
            fill(preset, winW, winH)
            watchFps()
        }
    }

    private fun ensurePipeline(content: Node, forward: Node) {
        if (built) return
        built = true
        try {
            mountDeferred(content, forward)
        } catch (err: Throwable) {
            MyLog.add(TAG, "Deferred pipeline failed: ${err.message}. Falling back to the classic renderer.", LogLevel.DEBUG)
            session.chooseRenderer(RenderMode.CLASSIC)
        }
    }

    private fun mountDeferred(content: Node, forward: Node) {
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
                    Color(0.03f, 0.04f, 0.08f),
                    Color(0.45f, 0.62f, 0.85f),
                    Color(0.95f, 0.86f, 0.7f),
                ),
            )
        } catch (err: Throwable) {
            MyLog.add(TAG, "Gradient IBL unavailable: ${err.message}", LogLevel.DEBUG)
            null
        }

        val cfg = DeferredPipelineConfig().apply {
            isWithAmbientOcclusion = true
            isWithScreenSpaceReflections = false
            isWithBloom = bloomAllowed(session)
            isWithVignette = false
            // Reversed-Z turns LESS into GREATER. The output quad sits at clip z = 0,
            // which does not pass that test against a cleared depth buffer.
            outputDepthTest = DepthCompareOp.ALWAYS
            maxGlobalLights = 2
            if (env != null) useImageBasedLighting(env)
            if (map != null && session.shadowsUser) useShadowMaps(listOf(map))
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
        session.applyRendererVisibility()
    }

    private fun applyPreset(preset: QualityPreset) {
        val pipe = deferred ?: return
        pipe.renderResolution = preset.renderResolution
        pipe.isAoEnabled = session.ssaoUser && preset.ssaoScale != null
        preset.ssaoScale?.let { pipe.aoMapSize = it }
        val bloom = bloomAllowed(session) && preset.bloomRequested
        pipe.isBloomEnabled = bloom
        shadow?.isShadowMapEnabled = session.shadowsUser && preset.shadow != com.neojou.myants.render3d.quality.ShadowKind.OFF
        grassMesh.isCastingShadow = preset.grassCastsShadow
    }

    private fun fill(preset: QualityPreset, winW: Float, winH: Float) {
        if (serial != session.worldSerial) {
            serial = session.worldSerial
            GroundHeight.rebuild(session.seed, serial)
            rebuildGround()
            scatterKey = ""
        }
        rebuildScatter(preset)
        updateSun()
        updatePheromone()
        pushActors(preset)
        pushRain(preset)
        NestCutaway.update(session, winW, winH)
    }

    private fun updateSun() {
        val light = sun ?: return
        val world = session.world
        val bright = if (session.dayNight) world.dayBrightness().toFloat() else 1f
        val elev = if (session.dayNight) 6f + bright * 64f else 70f
        val az = (world.timeOfDay * 160.0 - 80.0).toFloat()
        val elevRad = elev * (PI.toFloat() / 180f)
        val azRad = az * (PI.toFloat() / 180f)
        val dir = Vec3f(
            sin(azRad) * cos(elevRad),
            -sin(elevRad),
            cos(azRad) * cos(elevRad),
        )
        val night = bright < 0.2f
        val color = if (night) Color(0.55f, 0.62f, 0.8f, 1f) else Color(1f, 0.95f, 0.85f, 1f)
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

    private fun updatePheromone() {
        val shader = groundShader ?: return
        val show = session.showPheromones && session.pheromoneStrength > 0.0
        val tex = phero.update(session.world.colonies[0].outdoorField, session.pheromoneStrength, show)
        if (tex != null) shader.emissionMap = tex
        val bright = if (session.dayNight) session.world.dayBrightness().toFloat() else 1f
        val boost = if (!show) 0f else 1.6f + (1f - bright) * 2.4f
        shader.emission = Color(boost, boost, boost, 1f)
        val wet = session.world.raining && session.weatherUser
        val k = 0.28f + 0.72f * bright
        shader.color = if (wet) Color(k * 0.62f, k * 0.7f, k * 0.78f, 1f) else Color(k, k, k, 1f)
        shader.roughness = if (wet) 0.28f else 0.92f
    }

    private fun watchFps() {
        val fps = session.fpsShownPublic
        if (fps <= 0 || fps == lastFps) return
        lastFps = fps
        fpsSum += fps
        fpsSamples++
        if (fpsSamples >= 5) {
            val avg = fpsSum / fpsSamples
            fpsSum = 0
            fpsSamples = 0
            if (avg < 40 && session.renderTierBias < 3) {
                session.renderTierBias++
                MyLog.add(TAG, "Visual tier bias ${session.renderTierBias} after ${avg} fps", LogLevel.DEBUG)
            }
        }
    }

    private fun buildContent(content: Node) {
        val gshader = deferredKslPbrShader {
            color {
                vertexColor()
                uniformColor(Color.WHITE, blendMode = ColorBlockConfig.BlendMode.Multiply)
            }
            emission {
                textureColor(gamma = 1f, blendMode = ColorBlockConfig.BlendMode.Set)
                constColor(Color.WHITE, blendMode = ColorBlockConfig.BlendMode.Multiply)
            }
            roughness(0.92f)
            metallic(0f)
        }
        groundShader = gshader
        ground = content.addMesh(VertexLayouts.PositionNormalTexCoordColor) {
            isFrustumChecked = false
            shader = gshader
        }
        grass = MeshInstanceList(layout, 4096)
        grassMesh = instanced(content, grass, "grass") {
            cube { size.set(0.2f, 1f, 0.45f) }
        }
        rocks = MeshInstanceList(layout, 1024)
        instanced(content, rocks, "rock") { cube { size.set(1f, 1f, 1f) } }
        leaves = MeshInstanceList(layout, 1024)
        instanced(content, leaves, "leaf") { cube { size.set(1.2f, 0.08f, 0.45f) } }
        flowers = MeshInstanceList(layout, 512)
        instanced(content, flowers, "flower") { icoSphere { steps = 1; radius = 0.45f } }
        bodies = MeshInstanceList(layout, 2048)
        instanced(content, bodies, "ant-body") { cube { size.set(1f, 0.32f, 0.38f) } }
        heads = MeshInstanceList(layout, 2048)
        instanced(content, heads, "ant-head") { icoSphere { steps = 1; radius = 0.22f } }
        legs = MeshInstanceList(layout, 8192)
        instanced(content, legs, "ant-leg") { cube { size.set(0.16f, 0.55f, 0.1f) } }
        food = MeshInstanceList(layout, 256)
        val foodShader = deferredKslPbrShader {
            vertices { instancedModelMatrix(layout.modelMat) }
            color { instanceColor(layout.color) }
            emission {
                instanceColor(layout.color)
                constColor(Color(4f, 4f, 4f), blendMode = ColorBlockConfig.BlendMode.Multiply)
            }
            roughness(0.35f)
        }
        content.addMesh(VertexLayouts.PositionNormal, instances = food) {
            isFrustumChecked = false
            generate { uvSphere { radius = 1f; steps = 8 } }
            shader = foodShader
        }
        insects = MeshInstanceList(layout, 256)
        instanced(content, insects, "insect") { cube { size.set(1f, 0.45f, 0.7f) } }
        digits = MeshInstanceList(layout, 128)
        instanced(content, digits, "digit") { cube { size.set(1f, 1f, 1f) } }
        bits = MeshInstanceList(layout, 2048)
        instanced(content, bits, "bit") { cube { size.set(1f, 1f, 1f) } }
        content.addMesh(VertexLayouts.PositionNormal) {
            isFrustumChecked = false
            generate { uvSphere { radius = 1f; steps = 10 } }
            shader = deferredKslPbrShader {
                color { uniformColor(Color(0.42f, 0.4f, 0.36f, 1f)) }
                roughness(0.85f)
            }
            (transform as TrsTransformF).scale.set(SimConfig.clock.radius.toFloat(), 36f, SimConfig.clock.radius.toFloat() * 0.72f)
        }.also { stoneMesh = it }
    }

    private var stoneMesh: Mesh<*>? = null

    private fun buildForward(forward: Node) {
        rain = MeshInstanceList(layout, 1024)
        val rainShader = KslUnlitShader {
            vertices { instancedModelMatrix(layout.modelMat) }
            color { instanceColor(layout.color) }
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
        val ringShader = KslUnlitShader {
            color { uniformColor(Color("#6750A4")) }
            pipeline {
                cullMethod = CullMethod.NO_CULLING
                depthTest = DepthCompareOp.LESS
                isWriteDepth = false
            }
        }
        ring = forward.addMesh(VertexLayouts.Position) {
            isFrustumChecked = false
            isVisible = false
            generate { ringXZ() }
            shader = ringShader
        }
    }

    private fun instanced(
        parent: Node,
        instances: MeshInstanceList<InstanceLayouts.ModelMatColor>,
        name: String,
        build: de.fabmax.kool.scene.geometry.MeshBuilder<*>.() -> Unit,
    ): Mesh<*> {
        val shader = deferredKslPbrShader {
            vertices { instancedModelMatrix(layout.modelMat) }
            color { instanceColor(layout.color) }
            roughness(0.8f)
        }
        return parent.addMesh(VertexLayouts.PositionNormal, instances = instances) {
            isFrustumChecked = false
            this.name = name
            generate { build() }
            this.shader = shader
        }
    }

    private fun rebuildGround() {
        val cols = 71
        val rows = 43
        val dx = SimConfig.width.toFloat() / (cols - 1)
        val dz = SimConfig.height.toFloat() / (rows - 1)
        ground.generate {
            for (iz in 0 until rows) {
                for (ix in 0 until cols) {
                    val x = ix * dx
                    val z = iz * dz
                    val y = GroundHeight.at(x, z)
                    val hL = GroundHeight.at(x - dx, z)
                    val hR = GroundHeight.at(x + dx, z)
                    val hD = GroundHeight.at(x, z - dz)
                    val hU = GroundHeight.at(x, z + dz)
                    val n = MutableVec3f(hL - hR, dx * 2f, hD - hU)
                    if (n.length() < 1e-4f) n.set(Vec3f.Y_AXIS) else n.norm()
                    color = soilColor(ix, iz)
                    vertex(Vec3f(x, y, z), n, Vec2f(x / SimConfig.width, z / SimConfig.height))
                }
            }
            for (iz in 0 until rows - 1) {
                for (ix in 0 until cols - 1) {
                    GroundMesh.emitCell(ix, iz, cols) { a, b, c -> addTriIndices(a, b, c) }
                }
            }
        }
    }

    private fun rebuildScatter(preset: QualityPreset) {
        val key = "$serial|${preset.fineGrass}|${preset.pebbles}|${preset.leaves}|${preset.flowers}|${session.grassDensity}"
        if (key == scatterKey) return
        scatterKey = key
        val rnd = Random(session.seed.toLong() xor serial.toLong() * 0x9E3779B9L xor 0x51A7L)
        extraGrass = pack(rnd, (preset.fineGrass * session.grassDensity).toInt(), 4)
        pebbles = pack(rnd, (preset.pebbles * session.grassDensity).toInt(), 4)
        leafPos = pack(rnd, (preset.leaves * session.grassDensity).toInt(), 4)
        flowerPos = pack(rnd, (preset.flowers * session.grassDensity).toInt(), 4)
    }

    private fun pack(rnd: Random, count: Int, stride: Int): FloatArray {
        val out = FloatArray(count * stride)
        var i = 0
        while (i < out.size) {
            out[i] = rnd.nextFloat() * SimConfig.width
            out[i + 1] = rnd.nextFloat() * SimConfig.height
            out[i + 2] = rnd.nextFloat()
            out[i + 3] = rnd.nextFloat()
            i += stride
        }
        return out
    }

    private fun pushActors(preset: QualityPreset) {
        grass.clear(); rocks.clear(); leaves.clear(); flowers.clear()
        bodies.clear(); heads.clear(); legs.clear(); food.clear(); insects.clear(); digits.clear(); bits.clear()
        val time = Time.gameTime.toFloat()
        pushScatter(preset, time)
        pushSimGrass(preset, time)
        pushObstacles()
        pushClock()
        pushFood(preset)
        pushInsects()
        pushAnts(preset, time)
        pushBits()
        val ant = session.inspectedAnt
        if (ant != null && ant.location == AntPlace.WORLD) {
            ring.isVisible = true
            val y = GroundHeight.at(ant.x.toFloat(), ant.y.toFloat()) + 0.4f
            val t = ring.transform as TrsTransformF
            t.translation.set(ant.x.toFloat(), y, ant.y.toFloat())
            val s = 10f * ant.sizeVar.toFloat()
            t.scale.set(s, s, s)
            t.markDirty()
        } else {
            ring.isVisible = false
        }
        val stone = session.world.terrain.clockStone
        val sm = stoneMesh
        if (sm != null) {
            val y = GroundHeight.at(stone.x.toFloat(), stone.y.toFloat())
            val t = sm.transform as TrsTransformF
            t.translation.set(stone.x.toFloat(), y + 10f, stone.y.toFloat())
            t.markDirty()
        }
    }

    private fun pushScatter(preset: QualityPreset, time: Float) {
        val sway = preset.grassSway
        var i = 0
        while (i < extraGrass.size) {
            val x = extraGrass[i]
            val z = extraGrass[i + 1]
            val yaw = extraGrass[i + 2] * 6.2f + if (sway) sin(time * 1.7f + x * 0.02f) * 0.25f else 0f
            val h = 3f + extraGrass[i + 3] * 4f
            val y = GroundHeight.at(x, z) + h * 0.5f
            pose(grass, x, y, z, yaw, 0f, 0f, 0f, 0.35f, h, 0.7f, Color(0.18f, 0.42f + extraGrass[i + 3] * 0.2f, 0.14f, 1f))
            i += 4
        }
        i = 0
        while (i < pebbles.size) {
            val x = pebbles[i]; val z = pebbles[i + 1]
            val s = 1.5f + pebbles[i + 2] * 4f
            val y = GroundHeight.at(x, z) + s * 0.35f
            pose(rocks, x, y, z, pebbles[i + 3] * 3f, 0f, 0f, 0f, s, s * 0.6f, s * 0.8f, Color(0.35f, 0.33f, 0.3f, 1f))
            i += 4
        }
        i = 0
        while (i < leafPos.size) {
            val x = leafPos[i]; val z = leafPos[i + 1]
            val y = GroundHeight.at(x, z) + 0.2f
            pose(leaves, x, y, z, leafPos[i + 2] * 6f, 0f, 0f, 0f, 2.2f, 0.15f, 1f, Color(0.45f, 0.32f, 0.12f, 1f))
            i += 4
        }
        i = 0
        val petal = arrayOf(
            Color(0.85f, 0.35f, 0.45f, 1f),
            Color(0.95f, 0.85f, 0.35f, 1f),
            Color(0.65f, 0.4f, 0.75f, 1f),
            Color(0.95f, 0.95f, 0.95f, 1f),
        )
        while (i < flowerPos.size) {
            val x = flowerPos[i]; val z = flowerPos[i + 1]
            val y = GroundHeight.at(x, z) + 1.6f
            pose(flowers, x, y, z, 0f, 0f, 0f, 0f, 1.4f, 1.4f, 1.4f, petal[(flowerPos[i + 2] * 4f).toInt().coerceIn(0, 3)])
            pose(bits, x, y - 1.3f, z, 0f, 0f, 0f, 0f, 0.15f, 1.6f, 0.15f, Color(0.2f, 0.45f, 0.16f, 1f))
            i += 4
        }
    }

    private fun pushSimGrass(preset: QualityPreset, time: Float) {
        if (PerformanceManager.settings.simpleAnts) return
        val sway = preset.grassSway
        for (tuft in session.world.grass) {
            val x = tuft.x.toFloat()
            val z = tuft.y.toFloat()
            val h = (tuft.size * 3.0).toFloat().coerceAtLeast(1.5f)
            val yaw = WorldFrames.yawRad(tuft.angle.toFloat()) + if (sway) sin(time * 1.5f + x) * 0.2f else 0f
            val y = GroundHeight.at(x, z) + h * 0.5f
            pose(grass, x, y, z, yaw, 0f, 0f, 0f, tuft.size.toFloat().coerceAtLeast(0.4f), h, 0.6f, Color(0.22f, 0.48f, 0.18f, 1f))
        }
    }

    private fun pushObstacles() {
        for (obs in session.world.terrain.obstacles) {
            val x = obs.x.toFloat()
            val z = obs.y.toFloat()
            val r = obs.radius.toFloat()
            val y = GroundHeight.at(x, z) + r * 0.35f
            pose(rocks, x, y, z, 0.4f, 0f, 0f, 0f, r * 1.4f, r * 0.7f, r * 1.1f, Color(0.32f, 0.3f, 0.27f, 1f))
        }
    }

    private fun pushClock() {
        val (hh, mm) = wallClockHm()
        val text = "${hh.toString().padStart(2, '0')}${mm.toString().padStart(2, '0')}"
        val stone = session.world.terrain.clockStone
        val h = SimConfig.clock.fontSize.toFloat()
        val dw = h * 0.62f
        val gap = h * 0.18f
        val total = dw * 4f + gap * 3f + h * 0.35f
        var x = stone.x.toFloat() - total * 0.5f + dw * 0.5f
        val z = stone.y.toFloat() + stone.radius.toFloat() * 0.15f
        val y = GroundHeight.at(stone.x.toFloat(), stone.y.toFloat()) + 48f
        val color = Color(0.95f, 0.78f, 0.42f, 1f)
        for (i in text.indices) {
            if (i == 2) {
                val dot = h * 0.12f
                pose(digits, x, y + h * 0.18f, z, 0f, 0f, 0f, 0f, dot, dot, dot, color)
                pose(digits, x, y - h * 0.18f, z, 0f, 0f, 0f, 0f, dot, dot, dot, color)
                x += h * 0.35f
            }
            digitBoxes(x, y, z, dw, h, text[i] - '0', color)
            x += dw + gap
        }
    }

    private fun digitBoxes(cx: Float, cy: Float, z: Float, w: Float, h: Float, digit: Int, color: Color) {
        val mask = SEGMENTS[digit.coerceIn(0, 9)]
        val t = (h * 0.12f).coerceAtLeast(1f)
        val hw = w * 0.5f
        val mid = h * 0.25f
        fun seg(x: Float, y: Float, sx: Float, sy: Float) {
            pose(digits, x, y, z, 0f, 0f, 0f, 0f, sx, sy, t, color)
        }
        if (mask and 0x01 != 0) seg(cx, cy + h * 0.5f - t * 0.5f, w, t)
        if (mask and 0x02 != 0) seg(cx + hw - t * 0.5f, cy + mid * 0.5f, t, mid)
        if (mask and 0x04 != 0) seg(cx + hw - t * 0.5f, cy - mid * 0.5f, t, mid)
        if (mask and 0x08 != 0) seg(cx, cy - h * 0.5f + t * 0.5f, w, t)
        if (mask and 0x10 != 0) seg(cx - hw + t * 0.5f, cy - mid * 0.5f, t, mid)
        if (mask and 0x20 != 0) seg(cx - hw + t * 0.5f, cy + mid * 0.5f, t, mid)
        if (mask and 0x40 != 0) seg(cx, cy, w, t)
    }

    private fun pushFood(preset: QualityPreset) {
        val pipe = deferred
        if (pipe != null && lights.size < preset.foodLights) {
            while (lights.size < preset.foodLights) {
                lights += pipe.dynamicPointLights.addPointLight { intensity = 0f; radius = 50f }
            }
        }
        var n = 0
        for (item in session.world.foods) {
            val x = item.x.toFloat()
            val z = item.y.toFloat()
            val r = max(6.0, sqrt(item.amount) * 0.85).toFloat()
            val y = GroundHeight.at(x, z) + r * 0.45f
            val color = when (item.type) {
                FoodKind.SUGAR -> Color(1f, 0.84f, 0.2f, 1f)
                FoodKind.PROTEIN -> Color(0.8f, 0.25f, 0.22f, 1f)
                else -> Color(0.35f, 0.32f, 0.28f, 1f)
            }
            pose(food, x, y, z, 0f, 0f, 0f, 0f, r, r * 0.65f, r, color)
            if (n < lights.size && item.type != FoodKind.CORPSE) {
                val light = lights[n]
                light.position.set(x, y + 4f, z)
                light.color.set(color)
                light.radius = 40f + r
                light.intensity = if (item.amount > 1.0) 6f else 0f
                n++
            }
        }
        while (n < lights.size) {
            lights[n].intensity = 0f
            n++
        }
    }

    private fun pushInsects() {
        for (insect in session.world.insects) {
            val x = insect.x.toFloat()
            val z = insect.y.toFloat()
            val yaw = WorldFrames.yawRad(insect.angle.toFloat())
            val size = insectSpan(insect.type)
            val y = GroundHeight.at(x, z) + size * 0.35f
            pose(insects, x, y, z, yaw, 0f, 0f, 0f, size, size * 0.45f, size * 0.7f, insectTint(insect.type))
        }
    }

    private fun pushAnts(preset: QualityPreset, time: Float) {
        val buckets = preset.poseBuckets.coerceAtLeast(1)
        for (colony in session.world.colonies) {
            val tint = if (colony.id == 0) session.antColor else rgbColor(colony.workerTint)
            for (ant in colony.ants) {
                if (ant.location != AntPlace.WORLD) continue
                val x = ant.x.toFloat()
                val z = ant.y.toFloat()
                val yaw = WorldFrames.yawRad(ant.angle.toFloat())
                val speed = hypot(ant.x - ant.lastX, ant.y - ant.lastY).toFloat()
                val moving = speed > 0.15f
                val bucket = PoseBuckets.index(ant.forageSeed.toFloat(), speed, buckets)
                val phase = PoseBuckets.phase(bucket, buckets, time, moving && preset.legAnimation)
                val len = (if (ant.type == AntKind.SOLDIER) 16f else 12f) * ant.sizeVar.toFloat()
                val y = GroundHeight.at(x, z) + len * 0.18f
                pose(bodies, x, y, z, yaw, 0f, 0f, 0f, len, len * 0.28f, len * 0.36f, tint)
                val headColor = if (ant.type == AntKind.SOLDIER) Color(0.75f, 0.1f, 0.08f, 1f) else tint
                pose(heads, x, y, z, yaw, len * 0.42f, len * 0.05f, 0f, len * 0.28f, len * 0.28f, len * 0.28f, headColor)
                if (preset.legAnimation) {
                    val along = floatArrayOf(0.22f, 0f, -0.22f)
                    val side = floatArrayOf(-1f, 1f)
                    var leg = 0
                    for (a in along) {
                        for (s in side) {
                            val lift = sin(phase * PI.toFloat() * 2f + leg) * if (moving) len * 0.08f else 0f
                            val swing = cos(phase * PI.toFloat() * 2f + leg) * if (moving) len * 0.05f else 0f
                            pose(
                                legs, x, y, z, yaw,
                                len * a + swing, -len * 0.16f + lift, s * len * 0.18f,
                                len * 0.08f, len * 0.28f, len * 0.06f, tint,
                            )
                            leg++
                        }
                    }
                }
                if (ant.carrying == CarryKind.SUGAR || ant.carrying == CarryKind.PROTEIN) {
                    val carry = if (ant.carrying == CarryKind.SUGAR) Color(1f, 0.84f, 0.2f, 1f) else Color(0.8f, 0.25f, 0.22f, 1f)
                    pose(bits, x, y, z, yaw, -len * 0.15f, len * 0.16f, 0f, len * 0.22f, len * 0.22f, len * 0.22f, carry)
                }
            }
        }
    }

    private fun pushBits() {
        for (p in session.world.particles) {
            if (p.life <= 0.0) continue
            val x = p.x.toFloat()
            val z = p.y.toFloat()
            val y = GroundHeight.at(x, z) + 1.5f
            pose(bits, x, y, z, 0f, 0f, 0f, 0f, 1.2f, 1.2f, 1.2f, colorOr(p.color, Color(0.8f, 0.8f, 0.8f, 1f)))
        }
    }

    private fun pushRain(preset: QualityPreset) {
        rain.clear()
        val world = session.world
        if (!world.raining || !session.weatherUser || preset.rainStreaks <= 0) return
        if (rainX.size != preset.rainStreaks) {
            val rnd = Random(session.seed.toLong() xor 0xA41L)
            rainX = FloatArray(preset.rainStreaks) { rnd.nextFloat() * SimConfig.width }
            rainZ = FloatArray(preset.rainStreaks) { rnd.nextFloat() * SimConfig.height }
            rainPhase = FloatArray(preset.rainStreaks) { rnd.nextFloat() }
        }
        val t = Time.gameTime.toFloat()
        val color = Color(0.65f, 0.75f, 0.95f, 0.35f)
        for (i in rainX.indices) {
            val drop = ((t * 0.35f + rainPhase[i]) % 1f)
            val y = 80f - drop * 80f
            pose(rain, rainX[i], y, rainZ[i], 0f, 0f, 0f, 0f, 0.35f, 14f, 0.35f, color)
        }
    }

    private fun pose(
        batch: MeshInstanceList<InstanceLayouts.ModelMatColor>,
        x: Float, y: Float, z: Float, yaw: Float,
        lx: Float, ly: Float, lz: Float,
        sx: Float, sy: Float, sz: Float,
        color: Color,
    ) {
        mat.setIdentity().translate(x, y, z).rotate(yaw.rad, Vec3f.Y_AXIS).translate(lx, ly, lz).scale(scale.set(sx, sy, sz))
        batch.addInstance {
            set(layout.modelMat, mat)
            set(layout.color, color)
        }
    }
}

private fun soilColor(ix: Int, iz: Int): Color {
    val h = (ix * 73856093) xor (iz * 19349663)
    val n = (h and 255) / 255f
    return Color(0.32f + n * 0.12f, 0.22f + n * 0.06f, 0.12f + n * 0.04f, 1f)
}

private fun insectSpan(type: InsectKind): Float = when (type) {
    InsectKind.PREY -> 10f
    InsectKind.APHID -> 8f
    InsectKind.LADYBUG -> 14f
    InsectKind.SPIDER -> 16f
    InsectKind.PREDATOR -> 18f
    InsectKind.BEETLE -> 22f
}

private fun insectTint(type: InsectKind): Color = when (type) {
    InsectKind.PREY -> Color(0.35f, 0.7f, 0.3f, 1f)
    InsectKind.APHID -> Color(0.55f, 0.8f, 0.25f, 1f)
    InsectKind.LADYBUG -> Color(0.8f, 0.15f, 0.15f, 1f)
    InsectKind.SPIDER -> Color(0.35f, 0.35f, 0.38f, 1f)
    InsectKind.PREDATOR -> Color(0.45f, 0.25f, 0.55f, 1f)
    InsectKind.BEETLE -> Color(0.25f, 0.18f, 0.12f, 1f)
}

private fun rgbColor(rgb: Int): Color {
    val r = ((rgb shr 16) and 255) / 255f
    val g = ((rgb shr 8) and 255) / 255f
    val b = (rgb and 255) / 255f
    return Color(r, g, b, 1f)
}

private fun colorOr(hex: String, fallback: Color): Color = try {
    Color(hex)
} catch (_: Throwable) {
    fallback
}

private fun de.fabmax.kool.scene.geometry.MeshBuilder<*>.ringXZ() {
    val n = 28
    val inner = 0.65f
    val outer = 1f
    for (i in 0 until n) {
        val a0 = (i.toFloat() / n) * (PI.toFloat() * 2f)
        val a1 = ((i + 1).toFloat() / n) * (PI.toFloat() * 2f)
        val x0 = cos(a0)
        val z0 = sin(a0)
        val x1 = cos(a1)
        val z1 = sin(a1)
        val up = Vec3f.Y_AXIS
        val i0 = vertex(Vec3f(x0 * inner, 0f, z0 * inner), up)
        val i1 = vertex(Vec3f(x0 * outer, 0f, z0 * outer), up)
        val i2 = vertex(Vec3f(x1 * outer, 0f, z1 * outer), up)
        val i3 = vertex(Vec3f(x1 * inner, 0f, z1 * inner), up)
        addTriIndices(i0, i1, i2)
        addTriIndices(i0, i2, i3)
    }
}
