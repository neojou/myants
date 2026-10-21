package com.neojou.myants.classic2d

import com.neojou.myants.sim.PerformanceManager
import com.neojou.myants.sim.PheromoneGrid
import de.fabmax.kool.modules.ksl.KslUnlitShader
import de.fabmax.kool.modules.ksl.blocks.ColorBlockConfig
import de.fabmax.kool.pipeline.BlendMode
import de.fabmax.kool.pipeline.BufferedImageData2d
import de.fabmax.kool.pipeline.CullMethod
import de.fabmax.kool.pipeline.DepthCompareOp
import de.fabmax.kool.pipeline.MipMapping
import de.fabmax.kool.pipeline.SamplerSettings
import de.fabmax.kool.pipeline.TexFormat
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.scene.InstanceLayouts
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.TrsTransformF
import de.fabmax.kool.scene.VertexLayouts
import de.fabmax.kool.scene.addMesh
import de.fabmax.kool.math.QuatF
import de.fabmax.kool.util.Uint8Buffer
import kotlin.math.min
import kotlin.math.sqrt

internal fun classicSpriteShader(circle: Texture2d, layout: InstanceLayouts.ModelMatColor) = KslUnlitShader {
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

internal fun classicFlatShader(layout: InstanceLayouts.ModelMatColor) = KslUnlitShader {
    vertices { instancedModelMatrix(layout.modelMat) }
    color { instanceColor(layout.color) }
    pipeline {
        cullMethod = CullMethod.NO_CULLING
        depthTest = DepthCompareOp.ALWAYS
        isWriteDepth = false
        blendMode = BlendMode.BLEND_MULTIPLY_ALPHA
    }
}

internal fun placeQuad(mesh: Mesh<*>, cx: Float, cy: Float, w: Float, h: Float) {
    val t = mesh.transform as TrsTransformF
    t.translation.set(cx, -cy, 0f)
    t.rotation.set(QuatF.IDENTITY)
    t.scale.set(w, h, 1f)
    t.markDirty()
}

internal fun de.fabmax.kool.scene.Scene.addQuad(shader: KslUnlitShader): Mesh<*> = addMesh(VertexLayouts.PositionTexCoord) {
    generate { rect { size.set(1f, 1f) } }
    this.shader = shader
}

internal fun pheroShader() = KslUnlitShader {
    color { textureData() }
    pipeline {
        blendMode = BlendMode.BLEND_ADDITIVE
        depthTest = DepthCompareOp.ALWAYS
        isWriteDepth = false
        cullMethod = CullMethod.NO_CULLING
    }
}

internal class PheroLayer(val name: String) {
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
    internal fun image(): BufferedImageData2d {
        bucket = 1 - bucket
        return BufferedImageData2d(buf!!, w, h, TexFormat.RGBA, "$name-$bucket")
    }

    internal fun paint(grid: PheromoneGrid, strength: Double) {
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

internal fun circleTexture(): Texture2d {
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

internal fun pheroEvery(): Int = if (PerformanceManager.settings.simpleAnts) 6 else 3
