package com.neojou.myants.render3d.phero

import com.neojou.myants.sim.PerformanceManager
import com.neojou.myants.sim.PheromoneGrid
import de.fabmax.kool.pipeline.BufferedImageData2d
import de.fabmax.kool.pipeline.MipMapping
import de.fabmax.kool.pipeline.SamplerSettings
import de.fabmax.kool.pipeline.TexFormat
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.util.Uint8Buffer

/**
 * Outdoor pheromone texture. Two stable image ids avoid the GL loader's
 * all-zero hash collision. The ground shader samples this as emission.
 */
class PheromoneField(private val name: String) {
    private var buf: Uint8Buffer? = null
    private var w = 0
    private var h = 0
    private var frame = 0
    private var bucket = 0
    private var tex: Texture2d? = null

    val texture: Texture2d?
        get() = tex

    fun update(grid: PheromoneGrid, strength: Double, show: Boolean): Texture2d? {
        if (!show || strength <= 0.0) return tex
        frame++
        val fresh = tex == null || w != grid.width || h != grid.height
        if (fresh) {
            w = grid.width
            h = grid.height
            buf = Uint8Buffer(w * h * 4)
            // One non-zero byte so a fresh buffer is not the all-zero hash.
            buf!![0] = 1u
            tex = Texture2d(image(), MipMapping.Off, SamplerSettings().clamped(), name)
        }
        val every = PerformanceManager.settings.pheromoneUpdateSkip.coerceAtLeast(1)
        if (fresh || frame % every == 0) {
            paint(grid, strength)
            tex!!.uploadLazy(image())
        }
        return tex
    }

    private fun image(): BufferedImageData2d {
        bucket = 1 - bucket
        return BufferedImageData2d(buf!!, w, h, TexFormat.RGBA, "$name-$bucket")
    }

    private fun paint(grid: PheromoneGrid, strength: Double) {
        val data = buf ?: return
        var p = 0
        val n = w * h
        for (i in 0 until n) {
            val px = PheromoneColor.rgba(
                grid.toHome[i].toDouble(),
                grid.toSugar[i].toDouble(),
                grid.toProtein[i].toDouble(),
                grid.toDanger[i].toDouble(),
                strength,
            )
            data[p] = px[0].toUByte()
            data[p + 1] = px[1].toUByte()
            data[p + 2] = px[2].toUByte()
            data[p + 3] = px[3].toUByte()
            p += 4
        }
    }
}
