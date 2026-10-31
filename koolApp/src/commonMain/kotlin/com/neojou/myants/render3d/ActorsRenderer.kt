package com.neojou.myants.render3d

import com.neojou.myants.render3d.actors.GltfAphid
import com.neojou.myants.render3d.actors.PoseBuckets
import com.neojou.myants.render3d.actors.draw
import com.neojou.myants.render3d.actors.drawAphid
import com.neojou.myants.render3d.actors.drawCritter
import com.neojou.myants.render3d.actors.outdoorAntTint
import com.neojou.myants.render3d.actors.parseGltfAnt
import com.neojou.myants.render3d.actors.parseGltfAphid
import com.neojou.myants.render3d.actors.parseGltfCritter
import com.neojou.myants.render3d.actors.uploadAntMeshes
import com.neojou.myants.render3d.actors.uploadAphidMeshes
import com.neojou.myants.render3d.actors.uploadCritterMeshes
import com.neojou.myants.render3d.quality.QualityPreset
import com.neojou.myants.render3d.terrain.GroundHeight
import com.neojou.myants.sim.AntKind
import com.neojou.myants.sim.AntPlace
import com.neojou.myants.sim.CarryKind
import com.neojou.myants.sim.FoodKind
import com.neojou.myants.sim.InsectKind
import com.neojou.myants.sim.SimConfig
import com.neojou.tools.LogLevel
import com.neojou.tools.MyLog
import de.fabmax.kool.Assets
import de.fabmax.kool.modules.ksl.blocks.ColorBlockConfig
import de.fabmax.kool.pipeline.BlendMode
import de.fabmax.kool.pipeline.deferred.DeferredPipeline
import de.fabmax.kool.pipeline.deferred.deferredKslPbrShader
import de.fabmax.kool.scene.InstanceLayouts
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.MeshInstanceList
import de.fabmax.kool.scene.Node
import de.fabmax.kool.scene.TrsTransformF
import de.fabmax.kool.scene.VertexLayouts
import de.fabmax.kool.scene.addMesh
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.Time
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "RENDER"

/** Ants, aphids, critters, food, insects, particles and the clock stone (#47). */
internal class ActorsRenderer(
    private val scope: CoroutineScope,
    private val pipeline: () -> DeferredPipeline?,
) : SubRenderer {
    private val inst = Instancer()
    private val layout get() = inst.layout
    private val mat get() = inst.mat
    private val scale get() = inst.scale
    private lateinit var state: RenderState
    private val lights = ArrayList<de.fabmax.kool.pipeline.deferred.DeferredPointLights.PointLight>()
    private lateinit var bodies: MeshInstanceList<InstanceLayouts.ModelMatColor>
    private lateinit var heads: MeshInstanceList<InstanceLayouts.ModelMatColor>
    private lateinit var legs: MeshInstanceList<InstanceLayouts.ModelMatColor>
    private lateinit var workerAnt: AntSlot
    private lateinit var soldierAnt: AntSlot
    private var greenAphid: GltfAphid? = null
    private lateinit var aphidBody: MeshInstanceList<InstanceLayouts.ModelMatColor>
    private lateinit var aphidEyes: MeshInstanceList<InstanceLayouts.ModelMatColor>
    private lateinit var aphidLegs: List<MeshInstanceList<InstanceLayouts.ModelMatColor>>
    private lateinit var aphidBodyMesh: Mesh<*>
    private lateinit var aphidEyeMesh: Mesh<*>
    private lateinit var aphidLegMeshes: List<Mesh<*>>
    private lateinit var molts: MeshInstanceList<InstanceLayouts.ModelMatColor>
    private lateinit var honey: MeshInstanceList<InstanceLayouts.ModelMatColor>
    private val aphidPrevX = HashMap<Int, Float>()
    private val aphidPrevZ = HashMap<Int, Float>()
    private val critters = ArrayList<CritterSlot>()
    private val critterPrevX = HashMap<Int, Float>()
    private val critterPrevZ = HashMap<Int, Float>()
    private lateinit var food: MeshInstanceList<InstanceLayouts.ModelMatColor>
    private lateinit var sugar: MeshInstanceList<InstanceLayouts.ModelMatColor>
    private lateinit var protein: MeshInstanceList<InstanceLayouts.ModelMatColor>
    private lateinit var carrySugar: MeshInstanceList<InstanceLayouts.ModelMatColor>
    private lateinit var carryProtein: MeshInstanceList<InstanceLayouts.ModelMatColor>
    private lateinit var sugarMesh: Mesh<*>
    private lateinit var proteinMesh: Mesh<*>
    private lateinit var carrySugarMesh: Mesh<*>
    private lateinit var carryProteinMesh: Mesh<*>
    private var sugarReady = false
    private var proteinReady = false
    private lateinit var insects: MeshInstanceList<InstanceLayouts.ModelMatColor>
    lateinit var bits: MeshInstanceList<InstanceLayouts.ModelMatColor>
    private var stoneMesh: Mesh<*>? = null

    override fun build(opaque: Node, forward: Node) {
        buildContent(opaque)
        loadAnt("models/ants/worker_black.glb", workerAnt, "worker")
        loadAnt("models/ants/soldier.glb", soldierAnt, "soldier")
        loadGreenAphid()
        for (slot in critters) loadCritter(slot)
        loadMerged("models/food/sugar.glb", listOf(sugarMesh, carrySugarMesh), "sugar") { sugarReady = true }
        loadMerged("models/food/protein.glb", listOf(proteinMesh, carryProteinMesh), "protein") { proteinReady = true }
        val clock = stoneMesh
        if (clock != null) loadMerged("models/nature/clock_stone.glb", listOf(clock), "clock stone") { }
    }

    override fun update(state: RenderState, preset: QualityPreset, dt: Float) {
        this.state = state
        pushActors(preset)
    }

    private fun buildContent(content: Node) {
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

    private fun antSlot(content: Node, name: String): AntSlot {
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
            shadedInstances(content, body, "$name-chitin", WorldPalette.CHITIN_ROUGHNESS, 0f),
            shadedInstances(content, head, "$name-head", WorldPalette.CHITIN_ROUGHNESS, 0f),
            shadedInstances(content, eyes, "$name-eye", 0.12f, 0.02f),
            shadedInstances(content, jaws, "$name-jaw", WorldPalette.CHITIN_ROUGHNESS, 0f),
            legs.mapIndexed { index, batch ->
                shadedInstances(content, batch, "$name-leg-$index", WorldPalette.CHITIN_ROUGHNESS, 0f)
            },
        )
    }

    private fun critterSlot(content: Node, kind: InsectKind, path: String, legCount: Int): CritterSlot {
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
                if (kind == InsectKind.BEETLE) 0.22f else WorldPalette.CHITIN_ROUGHNESS,
                0.02f,
            ),
            legs.mapIndexed { index, batch ->
                shadedInstances(content, batch, "${kind.name}-leg-$index", WorldPalette.CHITIN_ROUGHNESS, 0.02f)
            },
        )
    }

    private fun loadCritter(slot: CritterSlot) {
        scope.launch {
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

    private fun loadAnt(path: String, slot: AntSlot, label: String) {
        scope.launch {
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

    private fun glowingFood(
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

    fun loadMerged(
        path: String,
        meshes: List<Mesh<*>>,
        label: String,
        fallback: String = "sphere",
        onReady: () -> Unit,
    ) = loadMergedGlb(scope, path, meshes, label, fallback, onReady)

    private fun carryBatch(kind: CarryKind): MeshInstanceList<InstanceLayouts.ModelMatColor> = when {
        kind == CarryKind.SUGAR && sugarReady -> carrySugar
        kind == CarryKind.PROTEIN && proteinReady -> carryProtein
        else -> bits
    }

    private fun loadGreenAphid() {
        scope.launch {
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

    private fun pushActors(preset: QualityPreset) {
        bodies.clear(); heads.clear(); legs.clear(); food.clear(); insects.clear(); bits.clear()
        sugar.clear(); protein.clear(); carrySugar.clear(); carryProtein.clear()
        workerAnt.clear()
        soldierAnt.clear()
        for (slot in critters) slot.clear()
        aphidBody.clear(); aphidEyes.clear(); honey.clear(); molts.clear()
        for (batch in aphidLegs) batch.clear()
        val time = Time.gameTime.toFloat()
        pushFood(preset)
        pushInsects()
        pushAnts(preset, time)
        pushBits()
        val stone = state.world.terrain.clockStone
        val sm = stoneMesh
        if (sm != null) {
            val y = GroundHeight.at(stone.x.toFloat(), stone.y.toFloat())
            val t = sm.transform as TrsTransformF
            t.translation.set(stone.x.toFloat(), y + 10f, stone.y.toFloat())
            t.markDirty()
        }
    }

    private fun pushFood(preset: QualityPreset) {
        val pipe = pipeline()
        if (pipe != null && lights.size < preset.foodLights) {
            while (lights.size < preset.foodLights) {
                lights += pipe.dynamicPointLights.addPointLight { intensity = 0f; radius = 50f }
            }
        }
        var n = 0
        for (item in state.world.foods) {
            val x = item.x.toFloat()
            val z = item.y.toFloat()
            val r = foodPileRadius(item.amount)
            val y = GroundHeight.at(x, z) + r * 0.45f
            val color = WorldPalette.food3d(item.type)
            val batch = when {
                item.type == FoodKind.SUGAR && sugarReady -> sugar
                item.type == FoodKind.PROTEIN && proteinReady -> protein
                else -> food
            }
            pose(batch, x, y, z, 0f, 0f, 0f, 0f, r, r * 0.65f, r, color)
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
        val seen = HashSet<Int>()
        for (insect in state.world.insects) {
            val x = insect.x.toFloat()
            val z = insect.y.toFloat()
            val yaw = WorldFrames.yawRad(insect.angle.toFloat())
            val size = insectSpan(insect.type)
            if (insect.type == InsectKind.APHID) {
                seen += insect.seq
                val prevX = aphidPrevX[insect.seq]
                val prevZ = aphidPrevZ[insect.seq]
                val moved = prevX != null && prevZ != null && hypot(x - prevX, z - prevZ) > 0.05f
                aphidPrevX[insect.seq] = x
                aphidPrevZ[insect.seq] = z
                val ground = GroundHeight.at(x, z)
                val model = greenAphid
                if (model != null) {
                    val phase = (Time.gameTime.toFloat() * 0.25f + insect.seq * 0.173f) % 1f
                    drawAphid(
                        model, aphidBody, aphidEyes, aphidLegs,
                        x, ground, z, yaw, size, phase, moved, mat, scale,
                    )
                } else {
                    pose(
                        insects, x, ground + size * 0.35f, z, yaw,
                        0f, 0f, 0f, size, size * 0.45f, size * 0.7f, WorldPalette.aphidFallback,
                    )
                }
                pushAphidTraces(insect.seq, x, ground, z, yaw, size)
                continue
            }
            val slot = critters.firstOrNull { it.kind == insect.type }
            val model = slot?.model
            if (slot != null && model != null) {
                val prevX = critterPrevX[insect.seq]
                val prevZ = critterPrevZ[insect.seq]
                val moved = prevX != null && prevZ != null && hypot(x - prevX, z - prevZ) > 0.05f
                critterPrevX[insect.seq] = x
                critterPrevZ[insect.seq] = z
                seen += insect.seq
                val phase = (Time.gameTime.toFloat() * 0.35f + insect.seq * 0.173f) % 1f
                drawCritter(
                    model, insect.type, slot.body, slot.eyes, slot.marks, slot.legs,
                    x, GroundHeight.at(x, z), z, yaw, size, phase, moved, mat, scale,
                )
            } else {
                val y = GroundHeight.at(x, z) + size * 0.35f
                pose(insects, x, y, z, yaw, 0f, 0f, 0f, size, size * 0.45f, size * 0.7f, WorldPalette.insect(insect.type))
            }
        }
        aphidPrevX.keys.retainAll(seen)
        aphidPrevZ.keys.retainAll(seen)
        critterPrevX.keys.retainAll(seen)
        critterPrevZ.keys.retainAll(seen)
    }

    private fun pushAphidTraces(seq: Int, x: Float, ground: Float, z: Float, yaw: Float, length: Float) {
        val bits = (seq * 1103515245 xor 12345).toUInt()
        val ang = (bits and 255u).toFloat() / 255f * (PI.toFloat() * 2f)
        val dist = 2.4f + ((bits shr 8) and 255u).toFloat() / 255f * 1.8f
        val flakeYaw = ((bits shr 16) and 255u).toFloat() / 255f * (PI.toFloat() * 2f)
        pose(
            molts, x + cos(ang) * dist, ground + 0.05f, z + sin(ang) * dist, flakeYaw,
            0f, 0f, 0f, 3.1f, 0.8f, 1.35f, WorldPalette.molt,
        )
        if (seq and 1 == 0) {
            val side = if ((bits and 1u) == 0u) 0.55f else -0.45f
            pose(
                honey, x, ground, z, yaw,
                -length * 0.62f, 0.28f, side,
                0.36f, 0.30f, 0.36f, WorldPalette.honey,
            )
        }
    }

    private fun pushAnts(preset: QualityPreset, time: Float) {
        val buckets = preset.poseBuckets.coerceAtLeast(1)
        for (colony in state.world.colonies) {
            val tint = if (colony.id == 0) state.view.antColor else rgbColor(colony.workerTint)
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
                val slot = if (ant.type == AntKind.SOLDIER) soldierAnt else workerAnt
                val model = slot.model
                if (model != null) {
                    val y = GroundHeight.at(x, z)
                    model.draw(
                        slot.body, slot.head, slot.eyes, slot.jaws, slot.legs,
                        x, y, z, yaw, len, phase, moving && preset.legAnimation,
                        outdoorAntTint(colony.id, ant.type, state.view.antColor), mat, scale,
                    )
                    if (ant.carrying == CarryKind.SUGAR || ant.carrying == CarryKind.PROTEIN) {
                        val carry = WorldPalette.food3d(if (ant.carrying == CarryKind.SUGAR) FoodKind.SUGAR else FoodKind.PROTEIN)
                        pose(carryBatch(ant.carrying), x, y, z, yaw, len * 0.46f, len * 0.07f, 0f, len * 0.18f, len * 0.18f, len * 0.18f, carry)
                    }
                } else {
                    val y = GroundHeight.at(x, z) + len * 0.18f
                    pose(bodies, x, y, z, yaw, 0f, 0f, 0f, len, len * 0.28f, len * 0.36f, tint)
                    val headColor = if (ant.type == AntKind.SOLDIER) WorldPalette.soldierHead3d else tint
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
                        val carry = WorldPalette.food3d(if (ant.carrying == CarryKind.SUGAR) FoodKind.SUGAR else FoodKind.PROTEIN)
                        pose(carryBatch(ant.carrying), x, y, z, yaw, -len * 0.15f, len * 0.16f, 0f, len * 0.22f, len * 0.22f, len * 0.22f, carry)
                    }
                }
            }
        }
    }

    private fun pushBits() {
        for (p in state.world.particles) {
            if (p.life <= 0.0) continue
            val x = p.x.toFloat()
            val z = p.y.toFloat()
            val y = GroundHeight.at(x, z) + 1.5f
            pose(bits, x, y, z, 0f, 0f, 0f, 0f, 1.2f, 1.2f, 1.2f, colorOr(p.color, WorldPalette.particle))
        }
    }

    private fun pose(
        batch: Batch,
        x: Float, y: Float, z: Float, yaw: Float,
        lx: Float, ly: Float, lz: Float,
        sx: Float, sy: Float, sz: Float,
        color: Color,
    ) = inst.pose(batch, x, y, z, yaw, lx, ly, lz, sx, sy, sz, color)

    private fun shadedInstances(parent: Node, instances: Batch, name: String, roughness: Float, metallic: Float): Mesh<*> =
        inst.shaded(parent, instances, name, roughness, metallic)

    private fun instanced(
        parent: Node,
        instances: Batch,
        name: String,
        roughness: Float = 0.8f,
        doubleSided: Boolean = false,
        build: de.fabmax.kool.scene.geometry.MeshBuilder<*>.() -> Unit,
    ): Mesh<*> = inst.instanced(parent, instances, name, roughness, doubleSided, build)
}
