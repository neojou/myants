package com.neojou.myants.trial3d

import de.fabmax.kool.Assets
import de.fabmax.kool.KoolApplication
import de.fabmax.kool.addScene
import de.fabmax.kool.math.Vec3d
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.deg
import de.fabmax.kool.math.spatial.BoundingBoxD
import de.fabmax.kool.math.spatial.BoundingBoxF
import de.fabmax.kool.modules.gltf.GltfLoadConfig
import de.fabmax.kool.modules.gltf.GltfMaterialConfig
import de.fabmax.kool.modules.gltf.loadGltfModel
import de.fabmax.kool.modules.ksl.KslPbrShader
import de.fabmax.kool.modules.ksl.blocks.ColorBlockConfig
import de.fabmax.kool.pipeline.deferred.DeferredPipeline
import de.fabmax.kool.pipeline.deferred.DeferredPipelineConfig
import de.fabmax.kool.pipeline.deferred.deferredKslPbrShader
import de.fabmax.kool.pipeline.ibl.SkyCubeIblSystem
import de.fabmax.kool.pipeline.ibl.EnvironmentMap
import de.fabmax.kool.pipeline.ibl.hdriEnvironment
import de.fabmax.kool.scene.InstanceLayouts
import de.fabmax.kool.scene.Light
import de.fabmax.kool.scene.MeshInstanceList
import de.fabmax.kool.scene.OrbitInputTransform
import de.fabmax.kool.scene.OrthographicCamera
import de.fabmax.kool.scene.PerspectiveCamera
import de.fabmax.kool.scene.addColorMesh
import de.fabmax.kool.scene.addMesh
import de.fabmax.kool.scene.orbitCamera
import de.fabmax.kool.scene.VertexLayouts
import de.fabmax.kool.scene.yPlanePan
import de.fabmax.kool.util.CascadedShadowMap
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.SimpleShadowMap
import de.fabmax.kool.util.FrontendScope
import kotlinx.coroutines.launch
import de.fabmax.kool.pipeline.ao.AoPipeline
import de.fabmax.kool.modules.gltf.GltfFile
import de.fabmax.kool.modules.gltf.loadGltfFileAsync

/** Feasibility trial only: compiles key 3D APIs (deferred + IBL + CSM + glTF + bloom + locked top-down cam). */
fun KoolApplication.trial3dScene() = addScene {
    // locked top-down camera via OrbitInputTransform
    val cam = OrthographicCamera().apply { isKeepAspectRatio = true }
    camera = cam
    val rig: OrbitInputTransform = orbitCamera {
        leftDragMethod = OrbitInputTransform.DragMethod.PAN
        rightDragMethod = OrbitInputTransform.DragMethod.NONE
        zoomMethod = OrbitInputTransform.ZoomMethod.ZOOM_TRANSLATE
        panMethod = yPlanePan()
        minHorizontalRot = -60.0; maxHorizontalRot = -60.0   // pitch locked
        setRotation(0f, -60f)
        minZoom = 2.0; maxZoom = 60.0
        translationBounds = BoundingBoxD(Vec3d(0.0, 0.0, 0.0), Vec3d(140.0, 0.0, 84.0))
    }
    val persp: PerspectiveCamera = PerspectiveCamera().apply { fovY = 20f.deg }   // narrow-FOV alternative
    check(persp.fovY.deg > 0f)

    lighting.clear()
    val sun = Light.Directional().apply { setup(Vec3f(-0.4f, -1f, -0.3f)); setColor(Color.WHITE, 3f) }
    lighting.addLight(sun)

    val sky = SkyCubeIblSystem.simpleSkyCubeIblSystem(this).apply {
        skyPass.elevation = 45f; skyPass.azimuth = 120f
        skyPass.syncLights += sun
        setupOffscreenPasses()
    }
    val csm = CascadedShadowMap(this, sun, maxRange = 200f, numCascades = 3)
    val simple = SimpleShadowMap(this, sun, mapSize = 2048).apply {
        shadowBounds = BoundingBoxF(Vec3f(0f, -1f, 0f), Vec3f(140f, 10f, 84f))
    }
    removeOffscreenPass(simple)

    val defCfg = DeferredPipelineConfig().apply {
        isWithAmbientOcclusion = true
        isWithScreenSpaceReflections = false
        isWithBloom = true
        isWithVignette = true
        maxGlobalLights = 2
        useImageBasedLighting(sky.environmentMap)
        useShadowMaps(listOf(csm))
    }
    val deferred = DeferredPipeline(this, defCfg).apply {
        bloomStrength = 0.7f; bloomThreshold = 1.0f; bloomRadius = 2f
        isAoEnabled = true
    }

    val antInstances = MeshInstanceList(InstanceLayouts.ModelMatColor, 2000)
    deferred.sceneContent.apply {
        addColorMesh {
            generate { grid { sizeX = 140f; sizeY = 84f } }
            shader = deferredKslPbrShader { color { vertexColor() }; roughness(0.9f) }
        }
        // emissive instanced food orbs -> bloom
        addMesh(VertexLayouts.PositionNormal, instances = antInstances) {
            generate { icoSphere { steps = 2; radius = 0.3f } }
            shader = deferredKslPbrShader {
                vertices { instancedModelMatrix() }
                color { instanceColor(InstanceLayouts.ModelMatColor.color) }
                emission {
                    instanceColor(InstanceLayouts.ModelMatColor.color)
                    constColor(Color(4f, 4f, 4f), blendMode = ColorBlockConfig.BlendMode.Multiply)
                }
            }
        }
    }
    addNode(deferred.createDefaultOutputQuad())

    // forward-rendered transparent layer (pheromone ribbons) after output quad
    addColorMesh {
        generate { rect { size.set(10f, 1f) } }
        shader = KslPbrShader {
            color { vertexColor() }
            alphaMode = de.fabmax.kool.pipeline.shading.AlphaMode.Blend
            emission { constColor(Color(2f, 1.6f, 0.2f)) }
            lighting { addShadowMap(csm) }
            enableImageBasedLighting(sky.environmentMap)
        }
    }

    FrontendScope.launch {
        val gltfCfg = GltfLoadConfig(
            materialConfig = GltfMaterialConfig(
                shadowMaps = listOf(csm),
                environmentMap = sky.environmentMap,
                isDeferredShading = true,
                fixedNumberOfJoints = 32,
            ),
            instanceLayout = InstanceLayouts.ModelMat,
            loadAnimations = true,
            applySkins = true,
        )
        val ant = Assets.loadGltfModel("models/ant.glb", gltfCfg).getOrNull()
        if (ant != null) {
            ant.enableAnimation(0)
            ant.onUpdate { ant.applyAnimation(de.fabmax.kool.util.Time.deltaT) }
            deferred.sceneContent += ant
        }
        val file: GltfFile? = Assets.loadGltfFileAsync("models/pebble.glb").await().getOrNull()
        check(file == null || file.meshes.isNotEmpty())
        val hdri: EnvironmentMap? = Assets.defaultLoader.hdriEnvironment("hdri/meadow_2_1k.rgbe.png").getOrNull()
        hdri?.let { deferred.lightingPassShader.ambientMap = it.irradianceMap }
        val forwardAo = AoPipeline.createForward(this@addScene)
        forwardAo.isEnabled = false
    }
}
