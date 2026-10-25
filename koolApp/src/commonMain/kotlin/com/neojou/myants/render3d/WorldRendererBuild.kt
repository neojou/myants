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

// Scene graph construction and model loading for [WorldRenderer].

internal fun WorldRenderer.buildContent(content: Node) {
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
    grass = MeshInstanceList(layout, 8192)
    grassMesh = instanced(content, grass, "grass", doubleSided = true) {
        cube { size.set(0.2f, 1f, 0.45f) }
    }
    rocks = MeshInstanceList(layout, 1024)
    rockMesh = instanced(content, rocks, "rock") { cube { size.set(1f, 1f, 1f) } }
    leaves = MeshInstanceList(layout, 1024)
    leafMesh = instanced(content, leaves, "leaf") { cube { size.set(1.2f, 0.08f, 0.45f) } }
    flowers = MeshInstanceList(layout, 4096)
    flowerMesh = instanced(content, flowers, "flower") { icoSphere { steps = 1; radius = 0.45f } }
    bodies = MeshInstanceList(layout, 2048)
    instanced(content, bodies, "ant-body") { cube { size.set(1f, 0.32f, 0.38f) } }
    heads = MeshInstanceList(layout, 2048)
    instanced(content, heads, "ant-head") { icoSphere { steps = 1; radius = 0.22f } }
    legs = MeshInstanceList(layout, 8192)
    instanced(content, legs, "ant-leg") { cube { size.set(0.16f, 0.55f, 0.1f) } }
    workerAnt = antSlot(content, "worker")
    soldierAnt = antSlot(content, "soldier")
    aphidBody = MeshInstanceList(layout, 64)
    aphidBodyMesh = shadedInstances(content, aphidBody, "aphid-cuticle", 0.42f, 0.02f)
    aphidEyes = MeshInstanceList(layout, 64)
    aphidEyeMesh = shadedInstances(content, aphidEyes, "aphid-eye", 0.22f, 0.02f)
    aphidLegs = List(6) { MeshInstanceList(layout, 64) }
    aphidLegMeshes = aphidLegs.mapIndexed { index, batch ->
        shadedInstances(content, batch, "aphid-leg-$index", 0.55f, 0.02f)
    }
    molts = MeshInstanceList(layout, 64)
    instanced(content, molts, "aphid-molt", 0.92f) { moltFlake() }
    honey = MeshInstanceList(layout, 64)
    instanced(content, honey, "aphid-honey", 0.22f) { uvSphere { radius = 1f; steps = 6 } }
    food = MeshInstanceList(layout, 256)
    val foodShader = deferredKslPbrShader {
        vertices { instancedModelMatrix(layout.modelMat) }
        color { instanceColor(layout.color) }
        emission {
            instanceColor(layout.color)
            constColor(WorldPalette.foodGlow, blendMode = ColorBlockConfig.BlendMode.Multiply)
        }
        roughness(0.35f)
    }
    content.addMesh(VertexLayouts.PositionNormal, instances = food) {
        isFrustumChecked = false
        generate { uvSphere { radius = 1f; steps = 8 } }
        shader = foodShader
    }
    sugar = MeshInstanceList(layout, 256)
    protein = MeshInstanceList(layout, 256)
    carrySugar = MeshInstanceList(layout, 2048)
    carryProtein = MeshInstanceList(layout, 2048)
    sugarMesh = glowingFood(content, sugar, "sugar")
    proteinMesh = glowingFood(content, protein, "protein")
    carrySugarMesh = glowingFood(content, carrySugar, "carry-sugar")
    carryProteinMesh = glowingFood(content, carryProtein, "carry-protein")
    insects = MeshInstanceList(layout, 256)
    instanced(content, insects, "insect") { cube { size.set(1f, 0.45f, 0.7f) } }
    critters += critterSlot(content, InsectKind.PREY, "models/insects/prey.glb", 6)
    critters += critterSlot(content, InsectKind.PREDATOR, "models/insects/predator.glb", 6)
    critters += critterSlot(content, InsectKind.SPIDER, "models/insects/spider.glb", 8)
    critters += critterSlot(content, InsectKind.BEETLE, "models/insects/beetle.glb", 6)
    critters += critterSlot(content, InsectKind.LADYBUG, "models/insects/ladybug.glb", 6)
    digits = MeshInstanceList(layout, 128)
    instanced(content, digits, "digit") { cube { size.set(1f, 1f, 1f) } }
    bits = MeshInstanceList(layout, 2048)
    instanced(content, bits, "bit") { cube { size.set(1f, 1f, 1f) } }
    content.addMesh(VertexLayouts.PositionNormal) {
        isFrustumChecked = false
        generate { uvSphere { radius = 1f; steps = 10 } }
        shader = deferredKslPbrShader {
            color { uniformColor(WorldPalette.clockStone) }
            roughness(0.85f)
        }
        (transform as TrsTransformF).scale.set(SimConfig.clock.radius.toFloat(), 36f, SimConfig.clock.radius.toFloat() * 0.72f)
    }.also { stoneMesh = it }
}

internal fun WorldRenderer.buildForward(forward: Node) {
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

internal fun WorldRenderer.antSlot(content: Node, name: String): AntSlot {
    val body = MeshInstanceList(layout, 2048)
    val head = MeshInstanceList(layout, 2048)
    val eyes = MeshInstanceList(layout, 2048)
    val jaws = MeshInstanceList(layout, 2048)
    val legs = List(6) { MeshInstanceList(layout, 2048) }
    return AntSlot(
        body,
        head,
        eyes,
        jaws,
        legs,
        shadedInstances(content, body, "$name-chitin", 0.42f, 0.08f),
        shadedInstances(content, head, "$name-head", 0.42f, 0.08f),
        shadedInstances(content, eyes, "$name-eye", 0.12f, 0.02f),
        shadedInstances(content, jaws, "$name-jaw", 0.38f, 0.12f),
        legs.mapIndexed { index, batch ->
            shadedInstances(content, batch, "$name-leg-$index", 0.42f, 0.08f)
        },
    )
}

internal fun WorldRenderer.critterSlot(content: Node, kind: InsectKind, path: String, legCount: Int): CritterSlot {
    val body = MeshInstanceList(layout, 128)
    val eyes = MeshInstanceList(layout, 128)
    val marks = MeshInstanceList(layout, 128)
    val legs = List(legCount) { MeshInstanceList(layout, 128) }
    return CritterSlot(
        kind,
        path,
        body,
        eyes,
        marks,
        legs,
        shadedInstances(content, body, "${kind.name}-body", 0.55f, 0.02f),
        shadedInstances(content, eyes, "${kind.name}-eye", 0.25f, 0.02f),
        shadedInstances(
            content,
            marks,
            "${kind.name}-mark",
            if (kind == InsectKind.BEETLE) 0.22f else 0.45f,
            0.02f,
        ),
        legs.mapIndexed { index, batch ->
            shadedInstances(content, batch, "${kind.name}-leg-$index", 0.55f, 0.02f)
        },
    )
}

internal fun WorldRenderer.loadCritter(slot: CritterSlot) {
    scene.coroutineScope.launch {
        val loaded = Assets.defaultLoader.loadBlob(slot.path)
        loaded.onSuccess { buf ->
            try {
                val parsed = parseGltfCritter(buf.toArray())
                if (parsed.legs.size != slot.legMeshes.size) {
                    MyLog.add(TAG, "${slot.kind} glTF has ${parsed.legs.size} legs. Using the block.", LogLevel.DEBUG)
                    return@onSuccess
                }
                uploadCritterMeshes(parsed, slot.bodyMesh, slot.eyeMesh, slot.markMesh, slot.legMeshes)
                slot.model = parsed
                MyLog.add(TAG, "${slot.kind} gltf tris=${parsed.triangles}", LogLevel.DEBUG)
            } catch (err: Throwable) {
                MyLog.add(TAG, "${slot.kind} glTF failed: ${err.message}. Using the block.", LogLevel.DEBUG)
            }
        }.onFailure { err ->
            MyLog.add(TAG, "${slot.kind} glTF missing: ${err.message}. Using the block.", LogLevel.DEBUG)
        }
    }
}

internal fun WorldRenderer.loadAnt(path: String, slot: AntSlot, label: String) {
    scene.coroutineScope.launch {
        val loaded = Assets.defaultLoader.loadBlob(path)
        loaded.onSuccess { buf ->
            try {
                val parsed = parseGltfAnt(buf.toArray())
                uploadAntMeshes(parsed, slot.bodyMesh, slot.headMesh, slot.eyeMesh, slot.jawMesh, slot.legMeshes)
                slot.model = parsed
                MyLog.add(TAG, "$label gltf tris=${parsed.triangles}", LogLevel.DEBUG)
            } catch (err: Throwable) {
                MyLog.add(TAG, "$label glTF failed: ${err.message}. Using block ants.", LogLevel.DEBUG)
            }
        }.onFailure { err ->
            MyLog.add(TAG, "$label glTF missing: ${err.message}. Using block ants.", LogLevel.DEBUG)
        }
    }
}

internal fun WorldRenderer.glowingFood(
    content: Node,
    instances: MeshInstanceList<InstanceLayouts.ModelMatColor>,
    name: String,
): Mesh<*> {
    val shader = deferredKslPbrShader {
        vertices { instancedModelMatrix(layout.modelMat) }
        color { instanceColor(layout.color) }
        emission {
            instanceColor(layout.color)
            constColor(WorldPalette.foodGlow, blendMode = ColorBlockConfig.BlendMode.Multiply)
        }
        roughness(0.35f)
    }
    return content.addMesh(VertexLayouts.PositionNormal, instances = instances) {
        isFrustumChecked = false
        this.name = name
        generate { }
        this.shader = shader
    }
}

internal fun WorldRenderer.loadMerged(
    path: String,
    meshes: List<Mesh<*>>,
    label: String,
    fallback: String = "sphere",
    onReady: () -> Unit,
) {
    scene.coroutineScope.launch {
        val loaded = Assets.defaultLoader.loadBlob(path)
        loaded.onSuccess { buf ->
            try {
                val parsed = parseGltfMerged(buf.toArray())
                for (mesh in meshes) mesh.uploadMerged(parsed)
                onReady()
                MyLog.add(TAG, "$label gltf tris=${mergedTriangles(parsed)}", LogLevel.DEBUG)
            } catch (err: Throwable) {
                MyLog.add(TAG, "$label glTF failed: ${err.message}. Using the $fallback.", LogLevel.DEBUG)
            }
        }.onFailure { err ->
            MyLog.add(TAG, "$label glTF missing: ${err.message}. Using the $fallback.", LogLevel.DEBUG)
        }
    }
}

internal fun WorldRenderer.carryBatch(kind: CarryKind): MeshInstanceList<InstanceLayouts.ModelMatColor> = when {
    kind == CarryKind.SUGAR && sugarReady -> carrySugar
    kind == CarryKind.PROTEIN && proteinReady -> carryProtein
    else -> bits
}

internal fun WorldRenderer.loadGreenAphid() {
    scene.coroutineScope.launch {
        val loaded = Assets.defaultLoader.loadBlob("models/insects/aphid_green.glb")
        loaded.onSuccess { buf ->
            try {
                val parsed = parseGltfAphid(buf.toArray())
                uploadAphidMeshes(parsed, aphidBodyMesh, aphidEyeMesh, aphidLegMeshes)
                greenAphid = parsed
                MyLog.add(TAG, "green aphid gltf tris=${parsed.triangles}", LogLevel.DEBUG)
            } catch (err: Throwable) {
                MyLog.add(TAG, "Green aphid glTF failed: ${err.message}. Using block aphids.", LogLevel.DEBUG)
            }
        }.onFailure { err ->
            MyLog.add(TAG, "Green aphid glTF missing: ${err.message}. Using block aphids.", LogLevel.DEBUG)
        }
    }
}
