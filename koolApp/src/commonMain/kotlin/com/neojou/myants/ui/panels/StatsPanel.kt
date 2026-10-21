package com.neojou.myants.ui.panels

import com.neojou.myants.GraphSample
import de.fabmax.kool.pipeline.BufferedImageData2d
import de.fabmax.kool.pipeline.MipMapping
import de.fabmax.kool.pipeline.SamplerSettings
import de.fabmax.kool.pipeline.TexFormat
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.util.Uint8Buffer
import com.neojou.myants.AppSession
import com.neojou.myants.ui.MyantsTheme
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.scene.Scene

internal fun UiScope.stats(session: AppSession) {
    StatsChart.paint(session.history)
    Image(StatsChart.tex) {
        modifier.margin(top = MyantsTheme.space.s2).imageSize(ImageSize.Stretch).size(280.dp, 130.dp)
    }
    Text("Pop  Zucker  Protein  Energie") { modifier.margin(top = MyantsTheme.space.s2) }
}

private object StatsChart {
    private const val W = 280
    private const val H = 130
    private val buf = Uint8Buffer(W * H * 4)
    private var bucket = 0
    val tex: Texture2d = Texture2d(
        chartImage(),
        MipMapping.Off,
        SamplerSettings().clamped(),
        "stats-chart",
    )

    fun paint(samples: Collection<GraphSample>) {
        fill(0x11, 0x11, 0x11, 255)
        val list = samples.toList()
        if (list.size >= 2) {
            plot(list, { it.population.toDouble() }, 0, 0xdd, 0xdd, 0xdd)
            plot(list, { it.sugar }, 0, 0x6f, 0xdf, 0x6f)
            plot(list, { it.protein }, 0, 0xdf, 0x6f, 0x6f)
            plot(list, { it.energyPct }, 1, 0x6f, 0xb0, 0xdf)
        }
        tex.uploadLazy(chartImage())
    }

    private fun chartImage(): BufferedImageData2d {
        bucket = 1 - bucket
        return BufferedImageData2d(buf, W, H, TexFormat.RGBA, "stats-chart-$bucket")
    }

    private fun plot(
        list: List<GraphSample>,
        read: (GraphSample) -> Double,
        fixedMax: Int,
        r: Int,
        g: Int,
        b: Int,
    ) {
        var max = if (fixedMax > 0) fixedMax.toDouble() else 1.0
        if (fixedMax == 0) {
            for (sample in list) {
                val v = read(sample)
                if (v > max) max = v
            }
        }
        for (i in 1 until list.size) {
            val x0 = (i - 1) * (W - 4) / (list.size - 1) + 2
            val x1 = i * (W - 4) / (list.size - 1) + 2
            val y0 = H - 4 - ((read(list[i - 1]) / max).coerceIn(0.0, 1.0) * (H - 8)).toInt()
            val y1 = H - 4 - ((read(list[i]) / max).coerceIn(0.0, 1.0) * (H - 8)).toInt()
            line(x0, y0, x1, y1, r, g, b)
        }
    }

    private fun line(x0: Int, y0: Int, x1: Int, y1: Int, r: Int, g: Int, b: Int) {
        val n = maxOf(kotlin.math.abs(x1 - x0), kotlin.math.abs(y1 - y0), 1)
        for (i in 0..n) {
            val x = x0 + (x1 - x0) * i / n
            val y = y0 + (y1 - y0) * i / n
            put(x, y, r, g, b, 255)
        }
    }

    private fun fill(r: Int, g: Int, b: Int, a: Int) {
        var i = 0
        val rb = r.toUByte()
        val gb = g.toUByte()
        val bb = b.toUByte()
        val ab = a.toUByte()
        while (i < buf.capacity) {
            buf[i] = rb
            buf[i + 1] = gb
            buf[i + 2] = bb
            buf[i + 3] = ab
            i += 4
        }
    }

    private fun put(x: Int, y: Int, r: Int, g: Int, b: Int, a: Int) {
        if (x !in 0 until W || y !in 0 until H) return
        val i = (y * W + x) * 4
        buf[i] = r.toUByte()
        buf[i + 1] = g.toUByte()
        buf[i + 2] = b.toUByte()
        buf[i + 3] = a.toUByte()
    }
}
