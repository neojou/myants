package com.neojou.myants

import com.neojou.myants.input.PointerRouter
import com.neojou.myants.render3d.RenderMode
import com.neojou.myants.sim.AntKind
import com.neojou.myants.sim.AntPlace
import com.neojou.myants.sim.AntState
import com.neojou.myants.sim.BroodStage
import com.neojou.myants.sim.CarryKind
import com.neojou.myants.sim.FoodKind
import com.neojou.myants.sim.InsectKind
import com.neojou.myants.sim.PerformanceManager
import com.neojou.myants.sim.PheromoneGrid
import com.neojou.myants.sim.SimConfig
import de.fabmax.kool.KoolApplication
import de.fabmax.kool.addScene
import de.fabmax.kool.input.KeyboardInput
import de.fabmax.kool.input.UniversalKeyCode
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

private val GROUND = Color(0.11f, 0.16f, 0.09f, 1f)
private val STONE = Color(0.45f, 0.42f, 0.36f, 1f)
private val OBSTACLE = Color(0.28f, 0.27f, 0.24f, 1f)
private val GRASS = Color(0.22f, 0.45f, 0.18f, 1f)
private val WORKER = Color("#c4703a")
private val SOLDIER_HEAD = Color("#c01810")
private val SUGAR = Color("#FFD700")
private val PROTEIN = Color("#CD5C5C")
private val QUEEN_COLOR = Color(0.72f, 0.45f, 0.18f, 1f)
private val DIGIT = Color(0.92f, 0.76f, 0.43f, 1f)
private val PANE = Color(0.102f, 0.102f, 0.102f, 1f)
private val DIVIDER = Color(0.20f, 0.20f, 0.20f, 1f)
private val RAIN = Color(0.45f, 0.6f, 0.85f, 0.18f)
private val NIGHT = Color(0.02f, 0.03f, 0.08f, 1f)

private val SEGMENTS = intArrayOf(
    0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7D, 0x07, 0x7F, 0x6F,
)

fun KoolApplication.addAntSimScene(session: AppSession) = addScene {
    session.classicScene = this
    clearColor = ClearColorFill(Color(0x11 / 255f, 0x11 / 255f, 0x11 / 255f, 1f))
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

    fun spriteShader() = KslUnlitShader {
        vertices { instancedModelMatrix(layout.modelMat) }
        color {
            textureData(circle)
            instanceColor(layout.color, ColorBlockConfig.BlendMode.Multiply)
        }
        pipeline {
            cullMethod = CullMethod.NO_CULLING
            depthTest = DepthCompareOp.ALWAYS
            isWriteDepth = false
        }
    }
    fun flatShader() = KslUnlitShader {
        vertices { instancedModelMatrix(layout.modelMat) }
        color { instanceColor(layout.color) }
        pipeline {
            cullMethod = CullMethod.NO_CULLING
            depthTest = DepthCompareOp.ALWAYS
            isWriteDepth = false
            blendMode = BlendMode.BLEND_MULTIPLY_ALPHA
        }
    }

    // Child order is the draw order. Ground and pheromone stay under the ants.
    addMesh(VertexLayouts.Position, groundBatch) {
        generate { rect { size.set(1f, 1f) } }
        shader = flatShader()
    }
    val phero = addQuad(pheroShader())
    addMesh(VertexLayouts.PositionTexCoord, worldBatch) {
        generate { rect { size.set(1f, 1f) } }
        shader = spriteShader()
    }
    addMesh(VertexLayouts.Position, digitBatch) {
        generate { rect { size.set(1f, 1f) } }
        shader = flatShader()
    }
    addMesh(VertexLayouts.Position, chromeBatch) {
        generate { rect { size.set(1f, 1f) } }
        shader = flatShader()
    }
    val nestPhero = addQuad(pheroShader())
    addMesh(VertexLayouts.PositionTexCoord, nestBatch) {
        generate { rect { size.set(1f, 1f) } }
        shader = spriteShader()
    }
    val nightShader = KslUnlitShader {
        color { uniformColor(Color(0f, 0f, 0f, 0f)) }
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

    fun bind(ch: Char, name: String, block: () -> Unit) {
        KeyboardInput.addKeyListener(
            UniversalKeyCode(KeyboardInput.getKeyCodeForChar(ch), name),
            name,
            { it.isPressed },
        ) { block() }
    }
    bind(' ', "pause") { session.togglePause() }
    bind('=', "zoom-in") { session.zoomBy(1.25) }
    bind('+', "zoom-in-plus") { session.zoomBy(1.25) }
    bind('-', "zoom-out") { session.zoomBy(1.0 / 1.25) }
    bind('_', "zoom-out-us") { session.zoomBy(1.0 / 1.25) }

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
        nightShader.color = Color(NIGHT.r, NIGHT.g, NIGHT.b, alpha.coerceIn(0f, 1f))
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
        val color = if (food.type == FoodKind.SUGAR) SUGAR else if (food.type == FoodKind.PROTEIN) PROTEIN else Color(0.35f, 0.32f, 0.3f, 1f)
        draw.blob(fx, fy, r * 2f, r * 2f, 0f, color)
    }
    for (insect in world.insects) {
        val (ix, iy) = at(insect.x, insect.y)
        val d = insectSize(insect.type) * s
        draw.blob(ix, iy, d, d * 0.7f, insect.angle.toFloat(), insectColor(insect.type))
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
        draw.blob(cx, cy, d, d, 0f, Color(0.16f, 0.13f, 0.10f, 1f))
    }
    for (brood in colony.brood) {
        if (brood.carrier != null) continue
        val (bx, by) = at(brood.x, brood.y)
        val color = when (brood.stage) {
            BroodStage.EGG -> Color(0.9f, 0.9f, 0.85f, 1f)
            BroodStage.LARVA -> Color(0.85f, 0.75f, 0.35f, 1f)
            BroodStage.PUPA -> Color(0.72f, 0.55f, 0.32f, 1f)
        }
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
    val h = SimConfig.clock.fontSize.toFloat() * s
    val dw = h * 0.62f
    val gap = h * 0.18f
    val total = dw * 4f + gap * 3f + h * 0.35f
    val (cx, cy) = session.worldToScreen(SimConfig.width / 2.0, SimConfig.height / 2.0, leftW, winH)
    var x = cx - total * 0.5f + dw * 0.5f
    for (i in text.indices) {
        if (i == 2) {
            val dot = h * 0.12f
            draw.rect(x, cy - h * 0.18f, dot, dot, DIGIT)
            draw.rect(x, cy + h * 0.18f, dot, dot, DIGIT)
            x += h * 0.35f
        }
        drawDigit(draw, x, cy, dw, h, text[i] - '0')
        x += dw + gap
    }
}

private fun drawDigit(draw: SpriteDraw, cx: Float, cy: Float, w: Float, h: Float, digit: Int) {
    val mask = SEGMENTS[digit.coerceIn(0, 9)]
    val t = (h * 0.12f).coerceAtLeast(1f)
    val hw = w * 0.5f
    val hh = h * 0.5f
    val mid = h * 0.25f
    if (mask and 0x01 != 0) draw.rect(cx, cy - hh + t * 0.5f, w, t, DIGIT)
    if (mask and 0x02 != 0) draw.rect(cx + hw - t * 0.5f, cy - mid * 0.5f, t, mid, DIGIT)
    if (mask and 0x04 != 0) draw.rect(cx + hw - t * 0.5f, cy + mid * 0.5f, t, mid, DIGIT)
    if (mask and 0x08 != 0) draw.rect(cx, cy + hh - t * 0.5f, w, t, DIGIT)
    if (mask and 0x10 != 0) draw.rect(cx - hw + t * 0.5f, cy + mid * 0.5f, t, mid, DIGIT)
    if (mask and 0x20 != 0) draw.rect(cx - hw + t * 0.5f, cy - mid * 0.5f, t, mid, DIGIT)
    if (mask and 0x40 != 0) draw.rect(cx, cy, w, t, DIGIT)
}

/**
 * Runs after the UI scene. Button clicks consume the pointer during the UI update,
 * so this pass no longer treats them as world clicks.
 */
fun KoolApplication.addAntSimInput(session: AppSession) = addScene {
    clearColor = ClearColorLoad
    var shotFrames = 0
    onUpdate {
        val winW = it.ctx.window.size.x.toFloat().coerceAtLeast(1f)
        val winH = it.ctx.window.size.y.toFloat().coerceAtLeast(1f)
        PointerRouter.handle(session, winW, winH)
        shotFrames = shotFrame(session, shotFrames)
        captureFrame(session)
    }
}

private fun AppSession.worldToScreen(wx: Double, wy: Double, leftW: Float, winH: Float): Pair<Float, Float> =
    camera.worldToScreen(wx, wy, leftW, winH)

private fun placeQuad(mesh: Mesh<*>, cx: Float, cy: Float, w: Float, h: Float) {
    val t = mesh.transform as TrsTransformF
    t.translation.set(cx, -cy, 0f)
    t.rotation.set(QuatF.IDENTITY)
    t.scale.set(w, h, 1f)
    t.markDirty()
}

private fun de.fabmax.kool.scene.Scene.addQuad(shader: KslUnlitShader): Mesh<*> = addMesh(VertexLayouts.PositionTexCoord) {
    generate { rect { size.set(1f, 1f) } }
    this.shader = shader
}

private fun pheroShader() = KslUnlitShader {
    color { textureData() }
    pipeline {
        blendMode = BlendMode.BLEND_ADDITIVE
        depthTest = DepthCompareOp.ALWAYS
        isWriteDepth = false
        cullMethod = CullMethod.NO_CULLING
    }
}

private class PheroLayer(val name: String) {
    private var tex: Texture2d? = null
    private var buf: Uint8Buffer? = null
    private var w = 0
    private var h = 0
    private var frame = 0
    private var bucket = 0

    fun update(grid: PheromoneGrid, strength: Double, every: Int): Texture2d {
        frame++
        val fresh = tex == null || w != grid.width || h != grid.height
        if (fresh) {
            w = grid.width
            h = grid.height
            buf = Uint8Buffer(w * h * 4)
            tex = Texture2d(image(), MipMapping.Off, SamplerSettings().clamped(), name)
        }
        if (fresh || frame % every == 0) {
            paint(grid, strength)
            tex!!.uploadLazy(image())
        }
        return tex!!
    }

    // kool's GL loader caches textures by image id and hashes the bytes. An all-zero
    // buffer hashes to the same id at every size, so the nest and outdoor textures
    // shared one Gpu texture and releasing it killed the nest layer. Two stable ids
    // per layer force a reload without growing that cache.
    private fun image(): BufferedImageData2d {
        bucket = 1 - bucket
        return BufferedImageData2d(buf!!, w, h, TexFormat.RGBA, "$name-$bucket")
    }

    private fun paint(grid: PheromoneGrid, strength: Double) {
        val data = buf ?: return
        val gain = min(1.0, 0.85 * strength)
        var p = 0
        for (i in 0 until w * h) {
            val home = grid.toHome[i].toDouble()
            val sugar = grid.toSugar[i].toDouble()
            val protein = grid.toProtein[i].toDouble()
            val danger = grid.toDanger[i].toDouble()
            if (home > 0.01 || sugar > 0.01 || protein > 0.01 || danger > 0.01) {
                val r = min(255.0, sqrt(protein + sugar + danger) * 255.0 * gain)
                val g = min(255.0, sqrt(sugar) * 255.0 * gain)
                val b = min(255.0, sqrt(home + danger) * 255.0 * gain)
                data[p] = r.toInt().toUByte()
                data[p + 1] = g.toInt().toUByte()
                data[p + 2] = b.toInt().toUByte()
                data[p + 3] = 255.toUByte()
            } else {
                data[p] = 0.toUByte()
                data[p + 1] = 0.toUByte()
                data[p + 2] = 0.toUByte()
                data[p + 3] = 0.toUByte()
            }
            p += 4
        }
    }
}

private fun circleTexture(): Texture2d {
    val n = 64
    val buf = Uint8Buffer(n * n * 4)
    val c = (n - 1) / 2.0
    var i = 0
    for (y in 0 until n) {
        for (x in 0 until n) {
            val dx = x - c
            val dy = y - c
            val d = sqrt(dx * dx + dy * dy)
            val a = when {
                d <= c - 1.5 -> 255
                d >= c -> 0
                else -> ((c - d) / 1.5 * 255.0).toInt()
            }
            buf[i] = 255.toUByte()
            buf[i + 1] = 255.toUByte()
            buf[i + 2] = 255.toUByte()
            buf[i + 3] = a.coerceIn(0, 255).toUByte()
            i += 4
        }
    }
    val image = BufferedImageData2d(buf, n, n, TexFormat.RGBA)
    return Texture2d(image, MipMapping.Off, SamplerSettings().clamped(), "ant-circle")
}

private fun pheroEvery(): Int = if (PerformanceManager.settings.simpleAnts) 6 else 3

private fun insectSize(type: InsectKind): Float = when (type) {
    InsectKind.PREY -> 10f
    InsectKind.APHID -> 8f
    InsectKind.LADYBUG -> 14f
    InsectKind.SPIDER -> 16f
    InsectKind.PREDATOR -> 18f
    InsectKind.BEETLE -> 22f
}

private fun insectColor(type: InsectKind): Color = when (type) {
    InsectKind.PREY -> Color(0.35f, 0.7f, 0.3f, 1f)
    InsectKind.APHID -> Color(0.55f, 0.8f, 0.25f, 1f)
    InsectKind.LADYBUG -> Color(0.8f, 0.15f, 0.15f, 1f)
    InsectKind.SPIDER -> Color(0.35f, 0.35f, 0.38f, 1f)
    InsectKind.PREDATOR -> Color(0.45f, 0.25f, 0.55f, 1f)
    InsectKind.BEETLE -> Color(0.25f, 0.18f, 0.12f, 1f)
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
    fun spriteShader() = KslUnlitShader {
        vertices { instancedModelMatrix(layout.modelMat) }
        color {
            textureData(circle)
            instanceColor(layout.color, ColorBlockConfig.BlendMode.Multiply)
        }
        pipeline {
            cullMethod = CullMethod.NO_CULLING
            depthTest = DepthCompareOp.ALWAYS
            isWriteDepth = false
        }
    }
    fun flatShader() = KslUnlitShader {
        vertices { instancedModelMatrix(layout.modelMat) }
        color { instanceColor(layout.color) }
        pipeline {
            cullMethod = CullMethod.NO_CULLING
            depthTest = DepthCompareOp.ALWAYS
            isWriteDepth = false
            blendMode = BlendMode.BLEND_MULTIPLY_ALPHA
        }
    }
    val nestPhero = addQuad(pheroShader())
    addMesh(VertexLayouts.Position, chromeBatch) {
        generate { rect { size.set(1f, 1f) } }
        shader = flatShader()
    }
    addMesh(VertexLayouts.PositionTexCoord, nestBatch) {
        generate { rect { size.set(1f, 1f) } }
        shader = spriteShader()
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
