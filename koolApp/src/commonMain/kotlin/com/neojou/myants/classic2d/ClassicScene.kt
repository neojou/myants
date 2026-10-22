package com.neojou.myants.classic2d

import com.neojou.myants.AppSession
import com.neojou.myants.wallClockHm

import com.neojou.myants.render3d.RenderMode
import com.neojou.myants.render3d.WorldPalette
import com.neojou.myants.render3d.clockCellAlong
import com.neojou.myants.render3d.clockMetrics
import com.neojou.myants.render3d.clockSlots
import com.neojou.myants.render3d.clockStrokes
import com.neojou.myants.sim.AntKind
import com.neojou.myants.sim.AntPlace
import com.neojou.myants.sim.AntState
import com.neojou.myants.sim.BroodStage
import com.neojou.myants.sim.CarryKind
import com.neojou.myants.sim.InsectKind
import com.neojou.myants.sim.PerformanceManager
import com.neojou.myants.sim.PheromoneGrid
import com.neojou.myants.sim.SimConfig
import de.fabmax.kool.KoolApplication
import de.fabmax.kool.addScene
import de.fabmax.kool.math.MutableMat4f
import de.fabmax.kool.math.MutableVec3f
import de.fabmax.kool.math.QuatF
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.rad
import de.fabmax.kool.modules.ksl.KslUnlitShader
import de.fabmax.kool.modules.ksl.blocks.ColorBlockConfig
import de.fabmax.kool.pipeline.BlendMode
import de.fabmax.kool.pipeline.CullMethod
import de.fabmax.kool.pipeline.DepthCompareOp
import de.fabmax.kool.pipeline.MipMapping
import de.fabmax.kool.pipeline.SamplerSettings
import de.fabmax.kool.pipeline.TexFormat
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.pipeline.BufferedImageData2d
import de.fabmax.kool.scene.InstanceLayouts
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.MeshInstanceList
import de.fabmax.kool.scene.OrthographicCamera
import de.fabmax.kool.scene.TrsTransformF
import de.fabmax.kool.scene.VertexLayouts
import de.fabmax.kool.scene.addMesh
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.Time
import de.fabmax.kool.util.Uint8Buffer
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import de.fabmax.kool.pipeline.ClearColorFill
import de.fabmax.kool.pipeline.ClearColorLoad

private val GROUND = WorldPalette.classicGround
private val STONE = WorldPalette.stone
private val OBSTACLE = WorldPalette.obstacle
private val GRASS = WorldPalette.grass
private val SOLDIER_HEAD = WorldPalette.soldierHead2d
private val SUGAR = WorldPalette.sugar2d
private val PROTEIN = WorldPalette.protein2d
private val QUEEN_COLOR = WorldPalette.queen
private val DIGIT = WorldPalette.digit2d
private val PANE = WorldPalette.pane
private val DIVIDER = WorldPalette.divider
private val RAIN = WorldPalette.rain2d

fun KoolApplication.addAntSimScene(session: AppSession) = addScene {
    session.classicScene = this
    clearColor = ClearColorFill(WorldPalette.letterbox)
    val cam = OrthographicCamera()
    cam.isKeepAspectRatio = false
    cam.clipNear = -10f
    cam.clipFar = 10f
    camera = cam

    val circle = circleTexture()
    val layout = InstanceLayouts.ModelMatColor
    val groundBatch = MeshInstanceList(layout, 4)
    val worldBatch = MeshInstanceList(layout, 4096)
    val nestBatch = MeshInstanceList(layout, 2048)
    val digitBatch = MeshInstanceList(layout, 256)
    val chromeBatch = MeshInstanceList(layout, 16)


    // Child order is the draw order. Ground and pheromone stay under the ants.
    addMesh(VertexLayouts.Position, groundBatch) {
        generate { rect { size.set(1f, 1f) } }
        shader = classicFlatShader(layout)
    }
    val phero = addQuad(pheroShader())
    addMesh(VertexLayouts.PositionTexCoord, worldBatch) {
        generate { rect { size.set(1f, 1f) } }
        shader = classicSpriteShader(circle, layout)
    }
    addMesh(VertexLayouts.Position, digitBatch) {
        generate { rect { size.set(1f, 1f) } }
        shader = classicFlatShader(layout)
    }
    addMesh(VertexLayouts.Position, chromeBatch) {
        generate { rect { size.set(1f, 1f) } }
        shader = classicFlatShader(layout)
    }
    val nestPhero = addQuad(pheroShader())
    addMesh(VertexLayouts.PositionTexCoord, nestBatch) {
        generate { rect { size.set(1f, 1f) } }
        shader = classicSpriteShader(circle, layout)
    }
    val nightShader = KslUnlitShader {
        color { uniformColor(WorldPalette.transparent) }
        pipeline {
            blendMode = BlendMode.BLEND_MULTIPLY_ALPHA
            depthTest = DepthCompareOp.ALWAYS
            isWriteDepth = false
            cullMethod = CullMethod.NO_CULLING
        }
    }
    val night = addQuad(nightShader)
    val outdoorPhero = PheroLayer("outdoor-phero")
    val nestField = PheroLayer("nest-phero")
    val draw = SpriteDraw()

    onUpdate {
        if (session.renderer != RenderMode.CLASSIC) return@onUpdate
        val winW = it.ctx.window.size.x.toFloat().coerceAtLeast(1f)
        val winH = it.ctx.window.size.y.toFloat().coerceAtLeast(1f)
        cam.left = 0f
        cam.right = winW
        cam.top = 0f
        cam.bottom = -winH
        session.tick(Time.deltaT)
        val leftW = winW * 0.74f
        session.clampCamera(leftW, winH)

        groundBatch.clear()
        worldBatch.clear()
        nestBatch.clear()
        digitBatch.clear()
        chromeBatch.clear()

        val (x0, y0) = session.worldToScreen(0.0, 0.0, leftW, winH)
        val (x1, y1) = session.worldToScreen(SimConfig.width.toDouble(), SimConfig.height.toDouble(), leftW, winH)
        val worldRect = Rect((x0 + x1) * 0.5f, (y0 + y1) * 0.5f, abs(x1 - x0), abs(y1 - y0))
        draw.batch = groundBatch
        draw.rect(worldRect.cx, worldRect.cy, worldRect.w, worldRect.h, GROUND)

        val colony0 = session.world.colonies[0]
        if (session.showPheromones) {
            val tex = outdoorPhero.update(colony0.outdoorField, session.pheromoneStrength, pheroEvery())
            (phero.shader as KslUnlitShader).colorMap = tex
            placeQuad(phero, worldRect.cx, worldRect.cy, worldRect.w, worldRect.h)
        } else {
            placeQuad(phero, 0f, 0f, 0f, 0f)
        }

        draw.batch = worldBatch
        drawWorld(session, draw, leftW, winH)
        draw.batch = digitBatch
        drawClock(session, draw, leftW, winH)

        val rightX = leftW + 4f
        val rightW = winW - rightX
        draw.batch = chromeBatch
        if (session.world.raining) draw.rect(worldRect.cx, worldRect.cy, worldRect.w, worldRect.h, RAIN)
        draw.rect(rightX + rightW * 0.5f, winH * 0.5f, rightW, winH, PANE)
        draw.rect(leftW + 2f, winH * 0.5f, 4f, winH, DIVIDER)

        val nest = colony0.nest
        val nFit = min(rightW / nest.width.toFloat(), winH / nest.height.toFloat())
        val nOx = rightX + (rightW - nest.width.toFloat() * nFit) * 0.5f
        val nOy = (winH - nest.height.toFloat() * nFit) * 0.5f
        val nestWpx = nest.width.toFloat() * nFit
        val nestHpx = nest.height.toFloat() * nFit
        if (session.showPheromones) {
            val tex = nestField.update(colony0.nestGrid, session.pheromoneStrength, pheroEvery())
            (nestPhero.shader as KslUnlitShader).colorMap = tex
            placeQuad(nestPhero, nOx + nestWpx * 0.5f, nOy + nestHpx * 0.5f, nestWpx, nestHpx)
        } else {
            placeQuad(nestPhero, 0f, 0f, 0f, 0f)
        }
        draw.batch = nestBatch
        drawNest(session, draw, nOx, nOy, nFit)

        val bright = session.world.dayBrightness()
        val alpha = if (session.dayNight) ((1.0 - bright) * session.dayNightStrength).toFloat() else 0f
        nightShader.color = WorldPalette.nightOverlay(alpha)
        placeQuad(night, leftW * 0.5f, winH * 0.5f, leftW, winH)
    }
}

private class Rect(val cx: Float, val cy: Float, val w: Float, val h: Float)

private class SpriteDraw {
    lateinit var batch: MeshInstanceList<InstanceLayouts.ModelMatColor>
    private val mat = MutableMat4f()
    private val scale = MutableVec3f()
    private val layout = InstanceLayouts.ModelMatColor

    fun rect(cx: Float, cy: Float, w: Float, h: Float, color: Color) {
        if (w <= 0f || h <= 0f) return
        mat.setIdentity().translate(cx, -cy, 0f).scale(scale.set(w, h, 1f))
        batch.addInstance {
            set(layout.modelMat, mat)
            set(layout.color, color)
        }
    }

    fun blob(cx: Float, cy: Float, w: Float, h: Float, rotRad: Float, color: Color) {
        if (w <= 0f || h <= 0f) return
        mat.setIdentity().translate(cx, -cy, 0f).rotate(rotRad.rad, Vec3f.Z_AXIS).scale(scale.set(w, h, 1f))
        batch.addInstance {
            set(layout.modelMat, mat)
            set(layout.color, color)
        }
    }
}

private fun drawWorld(session: AppSession, draw: SpriteDraw, leftW: Float, winH: Float) {
    val world = session.world
    val s = (session.fitScale(leftW, winH) * session.zoom).toFloat()
    fun at(x: Double, y: Double): Pair<Float, Float> = session.worldToScreen(x, y, leftW, winH)
    val simple = PerformanceManager.settings.simpleAnts
    val stone = world.terrain.clockStone
    val (sx, sy) = at(stone.x, stone.y)
    draw.blob(sx, sy, (stone.radius * 1.08 * 2.0 * s).toFloat(), (stone.radius * 0.60 * 2.0 * s).toFloat(), 0f, STONE)
    for (obs in world.terrain.obstacles) {
        val (ox, oy) = at(obs.x, obs.y)
        val d = (obs.radius * 2.0 * s).toFloat()
        draw.blob(ox, oy, d, d, 0f, OBSTACLE)
    }
    if (!simple) {
        for (g in world.grass) {
            val (gx, gy) = at(g.x, g.y)
            val len = (g.size * 3.0 * s).toFloat()
            draw.blob(gx, gy, len, (g.size * s).toFloat(), g.angle.toFloat(), GRASS)
        }
    }
    for (food in world.foods) {
        val (fx, fy) = at(food.x, food.y)
        val r = (kotlin.math.max(8.0, sqrt(food.amount) * 0.85) * s).toFloat()
        val color = WorldPalette.food2d(food.type)
        draw.blob(fx, fy, r * 2f, r * 2f, 0f, color)
    }
    for (insect in world.insects) {
        val (ix, iy) = at(insect.x, insect.y)
        val d = insectSize(insect.type) * s
        draw.blob(ix, iy, d, d * 0.7f, insect.angle.toFloat(), WorldPalette.insect(insect.type))
    }
    for (colony in world.colonies) {
        val tint = if (colony.id == 0) session.antColor else colorFromRgb(colony.workerTint)
        for (ant in colony.ants) {
            if (ant.location != AntPlace.WORLD) continue
            val (ax, ay) = at(ant.x, ant.y)
            drawAnt(draw, ax, ay, ant.angle.toFloat(), ant.sizeVar.toFloat() * s, ant.type, tint, ant.carrying, simple)
        }
    }
}

private fun drawNest(session: AppSession, draw: SpriteDraw, ox: Float, oy: Float, fit: Float) {
    val colony = session.world.colonies[0]
    val simple = PerformanceManager.settings.simpleAnts
    fun at(x: Double, y: Double) = (ox + x.toFloat() * fit) to (oy + y.toFloat() * fit)
    for (chamber in colony.nest.chambers) {
        val (cx, cy) = at(chamber.x, chamber.y)
        val d = (chamber.radius * 2.0 * fit.toDouble()).toFloat()
        draw.blob(cx, cy, d, d, 0f, WorldPalette.chamberFloor)
    }
    for (brood in colony.brood) {
        if (brood.carrier != null) continue
        val (bx, by) = at(brood.x, brood.y)
        val color = WorldPalette.brood2d(brood.stage)
        val d = if (brood.stage == BroodStage.EGG) 4f else 7f
        draw.blob(bx, by, d * fit, d * 0.6f * fit, 0f, color)
    }
    val (qx, qy) = at(colony.queen.x, colony.queen.y)
    draw.blob(qx, qy, 22f * fit, 14f * fit, 0f, QUEEN_COLOR)
    val tint = session.antColor
    for (ant in colony.ants) {
        if (ant.location != AntPlace.NEST) continue
        val (ax, ay) = at(ant.x, ant.y)
        drawAnt(draw, ax, ay, ant.angle.toFloat(), ant.sizeVar.toFloat() * fit, ant.type, tint, ant.carrying, simple)
    }
}

private fun drawAnt(
    draw: SpriteDraw,
    x: Float,
    y: Float,
    angle: Float,
    scale: Float,
    type: AntKind,
    tint: Color,
    carrying: CarryKind,
    simple: Boolean,
) {
    val length = (if (type == AntKind.SOLDIER) 16f else 12f) * scale
    val width = length * 0.45f
    draw.blob(x, y, length, width, angle, if (type == AntKind.SOLDIER) tint else tint)
    if (simple) return
    if (type == AntKind.SOLDIER) {
        val hx = x + cos(angle) * length * 0.45f
        val hy = y + sin(angle) * length * 0.45f
        draw.blob(hx, hy, width * 0.9f, width * 0.9f, angle, SOLDIER_HEAD)
    }
    if (carrying == CarryKind.SUGAR || carrying == CarryKind.PROTEIN) {
        val cx = x - cos(angle) * length * 0.2f
        val cy = y - sin(angle) * length * 0.2f
        val color = if (carrying == CarryKind.SUGAR) SUGAR else PROTEIN
        draw.blob(cx, cy, width * 0.7f, width * 0.7f, 0f, color)
    }
}

private fun drawClock(session: AppSession, draw: SpriteDraw, leftW: Float, winH: Float) {
    val (hh, mm) = wallClockHm()
    val text = "${hh.toString().padStart(2, '0')}${mm.toString().padStart(2, '0')}"
    val s = (session.fitScale(leftW, winH) * session.zoom).toFloat()
    val metrics = clockMetrics(SimConfig.clock.fontSize.toFloat() * s)
    val (cx, cy) = session.worldToScreen(SimConfig.width / 2.0, SimConfig.height / 2.0, leftW, winH)
    val slots = clockSlots(cx, metrics)
    val along = clockCellAlong(metrics.digitHeight)
    val dot = metrics.dotSize
    // Classic keeps the colon lit. Screen-up is the smaller cy.
    draw.rect(slots.colonX, cy - along, dot, dot, DIGIT)
    draw.rect(slots.colonX, cy + along, dot, dot, DIGIT)
    val centers = floatArrayOf(slots.hourTens, slots.hourOnes, slots.minuteTens, slots.minuteOnes)
    for (i in text.indices) {
        for (stroke in clockStrokes(centers[i], cy, metrics, text[i] - '0')) {
            draw.rect(stroke.x, cy - (stroke.y - cy), stroke.sx, stroke.sy, DIGIT)
        }
    }
}

private fun AppSession.worldToScreen(wx: Double, wy: Double, leftW: Float, winH: Float): Pair<Float, Float> =
    camera.worldToScreen(wx, wy, leftW, winH)

private fun insectSize(type: InsectKind): Float = when (type) {
    InsectKind.PREY -> 10f
    InsectKind.APHID -> 8f
    InsectKind.LADYBUG -> 14f
    InsectKind.SPIDER -> 16f
    InsectKind.PREDATOR -> 18f
    InsectKind.BEETLE -> 22f
}

private fun colorFromRgb(rgb: Int): Color {
    val r = ((rgb shr 16) and 255) / 255f
    val g = ((rgb shr 8) and 255) / 255f
    val b = (rgb and 255) / 255f
    return Color(r, g, b, 1f)
}

/**
 * Right-hand nest used while the 3D cutaway is not ready.
 * Classic mode draws its own nest inside [addAntSimScene] and hides this scene.
 */
fun KoolApplication.addLegacyNestOverlay(session: AppSession) = addScene {
    session.nestOverlay = this
    isVisible = false
    clearColor = ClearColorLoad
    val cam = OrthographicCamera()
    cam.isKeepAspectRatio = false
    cam.clipNear = -10f
    cam.clipFar = 10f
    camera = cam
    val circle = circleTexture()
    val layout = InstanceLayouts.ModelMatColor
    val nestBatch = MeshInstanceList(layout, 2048)
    val chromeBatch = MeshInstanceList(layout, 8)
    val nestPhero = addQuad(pheroShader())
    addMesh(VertexLayouts.Position, chromeBatch) {
        generate { rect { size.set(1f, 1f) } }
        shader = classicFlatShader(layout)
    }
    addMesh(VertexLayouts.PositionTexCoord, nestBatch) {
        generate { rect { size.set(1f, 1f) } }
        shader = classicSpriteShader(circle, layout)
    }
    val nestField = PheroLayer("overlay-nest-phero")
    val draw = SpriteDraw()
    onUpdate {
        val winW = it.ctx.window.size.x.toFloat().coerceAtLeast(1f)
        val winH = it.ctx.window.size.y.toFloat().coerceAtLeast(1f)
        cam.left = 0f
        cam.right = winW
        cam.top = 0f
        cam.bottom = -winH
        nestBatch.clear()
        chromeBatch.clear()
        val leftW = winW * 0.74f
        val rightX = leftW + 4f
        val rightW = winW - rightX
        draw.batch = chromeBatch
        draw.rect(rightX + rightW * 0.5f, winH * 0.5f, rightW, winH, PANE)
        draw.rect(leftW + 2f, winH * 0.5f, 4f, winH, DIVIDER)
        val colony0 = session.world.colonies[0]
        val nest = colony0.nest
        val nFit = min(rightW / nest.width.toFloat(), winH / nest.height.toFloat())
        val nOx = rightX + (rightW - nest.width.toFloat() * nFit) * 0.5f
        val nOy = (winH - nest.height.toFloat() * nFit) * 0.5f
        val nestWpx = nest.width.toFloat() * nFit
        val nestHpx = nest.height.toFloat() * nFit
        if (session.showPheromones) {
            val tex = nestField.update(colony0.nestGrid, session.pheromoneStrength, pheroEvery())
            (nestPhero.shader as KslUnlitShader).colorMap = tex
            placeQuad(nestPhero, nOx + nestWpx * 0.5f, nOy + nestHpx * 0.5f, nestWpx, nestHpx)
        } else {
            placeQuad(nestPhero, 0f, 0f, 0f, 0f)
        }
        draw.batch = nestBatch
        drawNest(session, draw, nOx, nOy, nFit)
    }
}
