package com.neojou.myants.render3d

import com.neojou.myants.AppSession
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

// Per-frame actor instancing (ants, aphids, critters, food) for [WorldRenderer].

internal fun WorldRenderer.pushActors(preset: QualityPreset) {
    grass.clear(); rocks.clear(); leaves.clear(); flowers.clear()
    bodies.clear(); heads.clear(); legs.clear(); food.clear(); insects.clear(); digits.clear(); bits.clear()
    sugar.clear(); protein.clear(); carrySugar.clear(); carryProtein.clear()
    workerAnt.clear()
    soldierAnt.clear()
    for (slot in critters) slot.clear()
    aphidBody.clear(); aphidEyes.clear(); honey.clear(); molts.clear()
    for (batch in aphidLegs) batch.clear()
    val time = Time.gameTime.toFloat()
    pushScatter(preset, time)
    pushSimGrass(preset, time)
    pushObstacles()
    pushClock()
    pushFood(preset)
    pushInsects()
    pushAnts(preset, time)
    pushBits()
    val mark = session.tools.inspectTarget()?.let(::inspectRing)?.takeIf { it.space == RingSpace.WORLD }
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
    val stone = session.world.terrain.clockStone
    val sm = stoneMesh
    if (sm != null) {
        val y = GroundHeight.at(stone.x.toFloat(), stone.y.toFloat())
        val t = sm.transform as TrsTransformF
        t.translation.set(stone.x.toFloat(), y + 10f, stone.y.toFloat())
        t.markDirty()
    }
}

internal fun WorldRenderer.pushFood(preset: QualityPreset) {
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

internal fun WorldRenderer.pushInsects() {
    val seen = HashSet<Int>()
    for (insect in session.world.insects) {
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

internal fun WorldRenderer.pushAphidTraces(seq: Int, x: Float, ground: Float, z: Float, yaw: Float, length: Float) {
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

internal fun WorldRenderer.pushAnts(preset: QualityPreset, time: Float) {
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
            val slot = if (ant.type == AntKind.SOLDIER) soldierAnt else workerAnt
            val model = slot.model
            if (model != null) {
                val y = GroundHeight.at(x, z)
                model.draw(
                    slot.body, slot.head, slot.eyes, slot.jaws, slot.legs,
                    x, y, z, yaw, len, phase, moving && preset.legAnimation,
                    outdoorAntTint(colony.id, ant.type), mat, scale,
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

internal fun WorldRenderer.pushBits() {
    for (p in session.world.particles) {
        if (p.life <= 0.0) continue
        val x = p.x.toFloat()
        val z = p.y.toFloat()
        val y = GroundHeight.at(x, z) + 1.5f
        pose(bits, x, y, z, 0f, 0f, 0f, 0f, 1.2f, 1.2f, 1.2f, colorOr(p.color, WorldPalette.particle))
    }
}

internal fun WorldRenderer.pose(
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

internal fun WorldRenderer.shadedInstances(
    parent: Node,
    instances: MeshInstanceList<InstanceLayouts.ModelMatColor>,
    name: String,
    roughness: Float,
    metallic: Float,
): Mesh<*> {
    val shader = deferredKslPbrShader {
        vertices { instancedModelMatrix(layout.modelMat) }
        color { instanceColor(layout.color) }
        roughness(roughness)
        metallic(metallic)
    }
    return parent.addMesh(VertexLayouts.PositionNormal, instances = instances) {
        isFrustumChecked = false
        this.name = name
        generate { }
        this.shader = shader
    }
}

internal fun WorldRenderer.instanced(
    parent: Node,
    instances: MeshInstanceList<InstanceLayouts.ModelMatColor>,
    name: String,
    roughness: Float = 0.8f,
    doubleSided: Boolean = false,
    build: de.fabmax.kool.scene.geometry.MeshBuilder<*>.() -> Unit,
): Mesh<*> {
    val shader = deferredKslPbrShader {
        vertices { instancedModelMatrix(layout.modelMat) }
        color { instanceColor(layout.color) }
        roughness(roughness)
        if (doubleSided) {
            pipeline { cullMethod = CullMethod.NO_CULLING }
        }
    }
    return parent.addMesh(VertexLayouts.PositionNormal, instances = instances) {
        isFrustumChecked = false
        this.name = name
        generate { build() }
        this.shader = shader
    }
}
