package com.neojou.myants.render3d.nest

import com.neojou.myants.AppSession
import com.neojou.myants.render3d.RingSpace
import com.neojou.myants.render3d.WorldPalette
import com.neojou.myants.render3d.selectRing
import com.neojou.myants.render3d.phero.PheromoneField
import com.neojou.myants.sim.AntKind
import com.neojou.myants.sim.AntPlace
import com.neojou.myants.sim.BroodStage
import com.neojou.myants.sim.CarryKind
import com.neojou.myants.sim.NestNodeKind
import com.neojou.tools.LogLevel
import com.neojou.tools.MyLog
import de.fabmax.kool.KoolApplication
import de.fabmax.kool.addScene
import de.fabmax.kool.math.MutableMat4f
import de.fabmax.kool.math.MutableVec3f
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.rad
import de.fabmax.kool.modules.ksl.KslUnlitShader
import de.fabmax.kool.modules.ksl.blocks.ColorSpaceConversion
import de.fabmax.kool.pipeline.BlendMode
import de.fabmax.kool.pipeline.ClearColorLoad
import de.fabmax.kool.pipeline.CullMethod
import de.fabmax.kool.pipeline.DepthCompareOp
import de.fabmax.kool.pipeline.shading.AlphaMode
import de.fabmax.kool.scene.InstanceLayouts
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.MeshInstanceList
import de.fabmax.kool.scene.Node
import de.fabmax.kool.scene.OrthographicCamera
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.scene.TrsTransformF
import de.fabmax.kool.scene.VertexLayouts
import de.fabmax.kool.scene.addMesh
import de.fabmax.kool.util.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Screen rectangle of the nest image. The input scene maps clicks through it. */
object NestFrame {
    var ox: Float = 0f
    var oy: Float = 0f
    var fit: Float = 1f
}

/**
 * Colony 0 cutaway, drawn in the present scene with the same screen ortho as
 * Classic 2D. An [de.fabmax.kool.pipeline.OffscreenPass2d] color target stayed
 * black on MoltenVK: kool 0.19 `singleColorDefaultDepth` ignores the clear
 * color, and the cutaway meshes never showed up in that texture.
 */
object NestCutaway {
    private val layout = InstanceLayouts.ModelMatColor
    private val mat = MutableMat4f()
    private val scale = MutableVec3f()
    private var holes: MeshInstanceList<InstanceLayouts.ModelMatColor>? = null
    private var actors: MeshInstanceList<InstanceLayouts.ModelMatColor>? = null
    private var dirt: Mesh<*>? = null
    private var soilBand: Mesh<*>? = null
    private var pheroMesh: Mesh<*>? = null
    private var ring: Mesh<*>? = null
    private val field = PheromoneField("nest3d")
    private var ready = false
    private var logged = false

    fun mount(scene: Scene, session: AppSession) {
        if (ready) return
        val node = Node("nest-cutaway").apply { isFrustumChecked = false }
        fillRoot(node)
        scene.addNode(node)
        session.nest3dActive = true
        ready = true
    }

    fun update(session: AppSession, winW: Float, winH: Float) {
        if (!ready) return
        val nest = session.world.colonies[0].nest
        val frame = layout(winW, winH, nest.width.toFloat(), nest.height.toFloat())
        place(dirt, frame.cx, frame.cy, frame.drawW, frame.drawH, 0.2f)
        val bandH = frame.drawH * SOIL_BAND_FRACTION
        place(soilBand, frame.cx, frame.oy + frame.drawH - bandH * 0.5f, frame.drawW, bandH, 0.28f)
        val dug = holes ?: return
        val ants = actors ?: return
        dug.clear()
        ants.clear()
        for (node in nest.nodes) {
            if (node.type != NestNodeKind.CHAMBER) continue
            val floor = (node.radius * 2.0 * frame.fit).toFloat()
            val rim = chamberRimDiameter(floor)
            put(dug, frame.sx(node.x.toFloat()), frame.sy(node.y.toFloat()), 0.35f, rim, rim, WorldPalette.nestChamberRim, 0f)
        }
        for (node in nest.nodes) {
            if (node.type == NestNodeKind.CHAMBER) continue
            val d = tunnelDrawDiameter((node.radius * 2.0 * frame.fit).toFloat())
            put(dug, frame.sx(node.x.toFloat()), frame.sy(node.y.toFloat()), 0.38f, d, d, WorldPalette.nestTunnel, 0f)
        }
        for (node in nest.nodes) {
            if (node.type != NestNodeKind.CHAMBER) continue
            val floor = (node.radius * 2.0 * frame.fit).toFloat()
            put(dug, frame.sx(node.x.toFloat()), frame.sy(node.y.toFloat()), 0.4f, floor, floor, WorldPalette.nestChamber, 0f)
        }
        val colony = session.world.colonies[0]
        for (brood in colony.brood) {
            if (brood.carrier != null) continue
            val jitter = broodJitter(brood.x.toFloat(), brood.y.toFloat())
            val span = broodSpan(brood.stage, frame.fit, jitter)
            val angle = if (brood.stage == BroodStage.EGG) 0f else (jitter - 0.5f) * 1.2f
            put(
                ants,
                frame.sx(brood.x.toFloat()),
                frame.sy(brood.y.toFloat()),
                0.6f,
                span.length,
                span.width,
                WorldPalette.brood3d(brood.stage),
                angle,
            )
        }
        val queen = colony.queen
        val qx = frame.sx(queen.x.toFloat())
        val qy = frame.sy(queen.y.toFloat())
        for ((i, mark) in queenMarks(frame.fit).withIndex()) {
            put(ants, qx + mark.dx, qy, if (i == 0) 0.68f else 0.72f, mark.w, mark.h, WorldPalette.queen, 0f)
        }
        for (ant in colony.ants) {
            if (ant.location != AntPlace.NEST) continue
            val soldier = ant.type == AntKind.SOLDIER
            val length = (if (soldier) 16f else 12f) * ant.sizeVar.toFloat() * frame.fit
            val ang = ant.angle.toFloat()
            val c = cos(ang)
            val s = sin(ang)
            val sx = frame.sx(ant.x.toFloat())
            val sy = frame.sy(ant.y.toFloat())
            val shape = nestAntParts(soldier)
            val parts = listOf(shape.gaster to 0.78f, shape.thorax to 0.82f, shape.head to 0.86f)
            for ((part, z) in parts) {
                val along = part.along * length
                val color = if (soldier && part === shape.head) WorldPalette.soldierHead3d else session.antColor
                put(ants, sx + c * along, sy + s * along, z, part.length * length, part.width * length, color, ang)
            }
            if (ant.carrying == CarryKind.SUGAR || ant.carrying == CarryKind.PROTEIN) {
                val color = if (ant.carrying == CarryKind.SUGAR) {
                    WorldPalette.nestSugar
                } else {
                    WorldPalette.nestProtein
                }
                val along = NEST_CARRY_ALONG * length
                val crumb = length * 0.22f
                put(ants, sx + c * along, sy + s * along, 0.9f, crumb, crumb, color, 0f)
            }
        }
        val mesh = pheroMesh
        if (mesh != null && session.showPheromones) {
            val tex = field.update(colony.nestGrid, session.pheromoneStrength, true)
            (mesh.shader as? KslUnlitShader)?.colorMap = tex
            mesh.isVisible = tex != null
            place(mesh, frame.cx, frame.cy, frame.drawW, frame.drawH, 0.5f)
        } else {
            mesh?.isVisible = false
        }
        val mark = session.inspectRingNow?.takeIf { it.space == RingSpace.NEST }
        val ringMesh = ring
        if (mark != null && ringMesh != null) {
            ringMesh.isVisible = true
            val t = ringMesh.transform as TrsTransformF
            val s = mark.scale * frame.fit
            t.translation.set(frame.sx(mark.x.toFloat()), -frame.sy(mark.y.toFloat()), 1.2f)
            t.scale.set(s, s, 1f)
            t.markDirty()
        } else {
            ringMesh?.isVisible = false
        }
        if (!logged) {
            logged = true
            MyLog.add("RENDER", "nest cutaway direct nodes=${nest.nodes.size} ants=${ants.numInstances}", LogLevel.DEBUG)
        }
    }

    fun layout(winW: Float, winH: Float, nestW: Float, nestH: Float): Frame {
        val leftW = winW * 0.74f
        val rightW = winW - leftW
        val fit = min(rightW / nestW, winH / nestH)
        val drawW = nestW * fit
        val drawH = nestH * fit
        val ox = leftW + (rightW - drawW) * 0.5f
        val oy = (winH - drawH) * 0.5f
        NestFrame.ox = ox
        NestFrame.oy = oy
        NestFrame.fit = fit
        return Frame(ox, oy, fit, drawW, drawH)
    }

    private fun fillRoot(node: Node) {
        dirt = node.addMesh(VertexLayouts.Position) {
            isFrustumChecked = false
            generate { rect { size.set(1f, 1f) } }
            shader = flat(WorldPalette.nestSoil)
        }
        soilBand = node.addMesh(VertexLayouts.Position) {
            isFrustumChecked = false
            generate { rect { size.set(1f, 1f) } }
            shader = flat(WorldPalette.nestSoilDeep)
        }
        val dug = MeshInstanceList(layout, 4096)
        node.addMesh(VertexLayouts.Position, instances = dug) {
            isFrustumChecked = false
            generate { uvSphere { radius = 0.5f; steps = 32 } }
            shader = instances()
        }
        holes = dug
        val batch = MeshInstanceList(layout, 4096)
        node.addMesh(VertexLayouts.Position, instances = batch) {
            isFrustumChecked = false
            generate { uvSphere { radius = 0.5f; steps = 16 } }
            shader = instances()
        }
        actors = batch
        val quad = node.addMesh(VertexLayouts.PositionTexCoord) {
            isFrustumChecked = false
            generate { rect { size.set(1f, 1f) } }
            shader = KslUnlitShader {
                color { textureData() }
                alphaMode = AlphaMode.Opaque
                pipeline {
                    blendMode = BlendMode.BLEND_ADDITIVE
                    depthTest = DepthCompareOp.ALWAYS
                    isWriteDepth = false
                    cullMethod = CullMethod.NO_CULLING
                }
            }
        }
        quad.isVisible = false
        pheroMesh = quad
        ring = node.addMesh(VertexLayouts.PositionColor) {
            isFrustumChecked = false
            isVisible = false
            generate { selectRing(xz = false) }
            shader = KslUnlitShader {
                color { vertexColor() }
                colorSpaceConversion = ColorSpaceConversion.LinearToSrgb()
                alphaMode = AlphaMode.Opaque
                pipeline {
                    blendMode = BlendMode.DISABLED
                    cullMethod = CullMethod.NO_CULLING
                    depthTest = DepthCompareOp.ALWAYS
                    isWriteDepth = false
                }
            }
        }
    }

    private fun put(
        batch: MeshInstanceList<InstanceLayouts.ModelMatColor>,
        sx: Float,
        sy: Float,
        z: Float,
        w: Float,
        h: Float,
        color: Color,
        rotRad: Float,
    ) {
        if (w <= 0f || h <= 0f) return
        val depth = min(w, h) * 0.35f
        mat.setIdentity().translate(sx, -sy, z).rotate(rotRad.rad, Vec3f.Z_AXIS).scale(scale.set(w, h, depth))
        batch.addInstance {
            set(layout.modelMat, mat)
            set(layout.color, color)
        }
    }

    class Frame(val ox: Float, val oy: Float, val fit: Float, val drawW: Float, val drawH: Float) {
        val cx: Float get() = ox + drawW * 0.5f
        val cy: Float get() = oy + drawH * 0.5f
        fun sx(nx: Float) = ox + nx * fit
        fun sy(ny: Float) = oy + ny * fit
    }
}

fun KoolApplication.addNestPresent(session: AppSession) = addScene {
    session.nestPresent = this
    isVisible = false
    clearColor = ClearColorLoad
    // Same clip as the classic 2D scene. A positive near plane clips these
    // screen quads, which sit at z = 0 in front of the default camera.
    val cam = OrthographicCamera()
    cam.isKeepAspectRatio = false
    cam.isClipToViewport = false
    // Symmetric and wide enough for the cutaway spheres. A positive near
    // plane clips the screen quads, which sit at z = 0.
    cam.clipNear = -100f
    cam.clipFar = 100f
    camera = cam
    val pane = addMesh(VertexLayouts.Position) {
        isFrustumChecked = false
        generate { rect { size.set(1f, 1f) } }
        shader = flat(WorldPalette.pane)
    }
    NestCutaway.mount(this, session)
    val divider = addMesh(VertexLayouts.Position) {
        isFrustumChecked = false
        generate { rect { size.set(1f, 1f) } }
        shader = flat(WorldPalette.divider)
    }
    onUpdate {
        val winW = it.ctx.window.size.x.toFloat().coerceAtLeast(1f)
        val winH = it.ctx.window.size.y.toFloat().coerceAtLeast(1f)
        cam.left = 0f
        cam.right = winW
        cam.top = 0f
        cam.bottom = -winH
        val nest = session.world.colonies[0].nest
        NestCutaway.layout(winW, winH, nest.width.toFloat(), nest.height.toFloat())
        val leftW = winW * 0.74f
        val rightW = winW - leftW
        place(pane, leftW + rightW * 0.5f, winH * 0.5f, rightW, winH, 0f)
        place(divider, leftW + 2f, winH * 0.5f, 4f, winH, 1f)
    }
}

private fun flat(color: Color) = KslUnlitShader {
    color { uniformColor(color) }
    // The swapchain stores the present scene without the deferred output's
    // linear-to-sRGB step, so unlit colors would otherwise land as linear.
    colorSpaceConversion = ColorSpaceConversion.LinearToSrgb()
    alphaMode = AlphaMode.Opaque
    pipeline {
        blendMode = BlendMode.DISABLED
        cullMethod = CullMethod.NO_CULLING
        depthTest = DepthCompareOp.ALWAYS
        isWriteDepth = false
    }
}

private fun instances() = KslUnlitShader {
    vertices { instancedModelMatrix(InstanceLayouts.ModelMatColor.modelMat) }
    color { instanceColor(InstanceLayouts.ModelMatColor.color) }
    colorSpaceConversion = ColorSpaceConversion.LinearToSrgb()
    alphaMode = AlphaMode.Opaque
    pipeline {
        blendMode = BlendMode.DISABLED
        cullMethod = CullMethod.NO_CULLING
        depthTest = DepthCompareOp.ALWAYS
        isWriteDepth = false
    }
}

private fun place(mesh: Mesh<*>?, cx: Float, cy: Float, w: Float, h: Float, z: Float) {
    if (mesh == null) return
    val t = mesh.transform as TrsTransformF
    t.translation.set(cx, -cy, z)
    t.scale.set(w, h, 1f)
    t.markDirty()
}
