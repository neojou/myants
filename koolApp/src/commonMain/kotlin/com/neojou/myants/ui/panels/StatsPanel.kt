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

/** German names until #24 moves them into the string table. */
internal val STATS_SERIES_NAMES = listOf("Pop", "Zucker", "Protein", "Energie")

internal fun UiScope.stats(session: AppSession) {
    StatsChart.paint(session.history)
    Image(StatsChart.tex) {
        modifier.margin(top = MyantsTheme.space.s2).imageSize(ImageSize.Stretch).size(280.dp, 130.dp)
    }
    Text(STATS_SERIES_NAMES.joinToString("  ")) { modifier.margin(top = MyantsTheme.space.s2) }
}

internal enum class StatsStroke { SOLID, DASH, DOT, DASH_DOT }

/** Pixel on/off along a stroke. Colors stay the AntSim chart colors; the pattern tells the series apart. */
internal fun statsStrokeVisible(style: StatsStroke, step: Int): Boolean {
    val i = if (step < 0) 0 else step
    return when (style) {
        StatsStroke.SOLID -> true
        StatsStroke.DASH -> i % 8 < 5
        StatsStroke.DOT -> i % 4 == 0
        StatsStroke.DASH_DOT -> {
            val m = i % 11
            m < 5 || m == 7
        }
    }
}

/**
 * Stacks label tops so they stay at least [pitch] apart and inside the chart.
 * [desired] is the preferred top, usually the line end.
 */
internal fun placeStatsLabelYs(desired: List<Int>, minY: Int, maxY: Int, pitch: Int): List<Int> {
    if (desired.isEmpty()) return emptyList()
    val order = desired.indices.sortedBy { desired[it] }
    val placed = IntArray(desired.size)
    var cursor = minY
    for (index in order) {
        val y = maxOf(desired[index], cursor).coerceAtLeast(minY)
        placed[index] = y
        cursor = y + pitch
    }
    val limit = maxY - pitch + 1
    val overflow = placed.max() - limit
    if (overflow > 0) {
        for (i in placed.indices) placed[i] = (placed[i] - overflow).coerceAtLeast(minY)
    }
    return placed.toList()
}

private const val CHART_W = 280
private const val CHART_H = 130
private const val LABEL_W = 48
private const val LABEL_PITCH = 8

private object StatsChart {
    private val buf = Uint8Buffer(CHART_W * CHART_H * 4)
    private var bucket = 0
    val tex: Texture2d = Texture2d(
        chartImage(),
        MipMapping.Off,
        SamplerSettings().clamped(),
        "stats-chart",
    )

    fun paint(samples: Collection<GraphSample>) {
        drawStatsChart(buf, CHART_W, CHART_H, samples.toList())
        tex.uploadLazy(chartImage())
    }

    private fun chartImage(): BufferedImageData2d {
        bucket = 1 - bucket
        return BufferedImageData2d(buf, CHART_W, CHART_H, TexFormat.RGBA, "stats-chart-$bucket")
    }
}

internal fun drawStatsChart(buf: Uint8Buffer, w: Int, h: Int, samples: List<GraphSample>) {
    fill(buf, 0x11, 0x11, 0x11, 255)
    if (samples.size < 2) return
    val series = listOf(
        Series({ it.population.toDouble() }, 0, 0xdd, 0xdd, 0xdd, StatsStroke.SOLID, STATS_SERIES_NAMES[0]),
        Series({ it.sugar }, 0, 0x6f, 0xdf, 0x6f, StatsStroke.DASH, STATS_SERIES_NAMES[1]),
        Series({ it.protein }, 0, 0xdf, 0x6f, 0x6f, StatsStroke.DOT, STATS_SERIES_NAMES[2]),
        Series({ it.energyPct }, 1, 0x6f, 0xb0, 0xdf, StatsStroke.DASH_DOT, STATS_SERIES_NAMES[3]),
    )
    val endY = IntArray(series.size)
    for (s in series.indices) {
        endY[s] = plot(buf, w, h, samples, series[s])
    }
    val tops = placeStatsLabelYs(endY.map { it - 3 }, 1, h - 1, LABEL_PITCH)
    for (s in series.indices) {
        val spec = series[s]
        text(buf, w, h, w - LABEL_W, tops[s], spec.name, spec.r, spec.g, spec.b)
    }
}

private class Series(
    val read: (GraphSample) -> Double,
    val fixedMax: Int,
    val r: Int,
    val g: Int,
    val b: Int,
    val stroke: StatsStroke,
    val name: String,
)

private fun plot(buf: Uint8Buffer, w: Int, h: Int, list: List<GraphSample>, spec: Series): Int {
    var max = if (spec.fixedMax > 0) spec.fixedMax.toDouble() else 1.0
    if (spec.fixedMax == 0) {
        for (sample in list) {
            val v = spec.read(sample)
            if (v > max) max = v
        }
    }
    val plotW = w - LABEL_W - 4
    var phase = 0
    var endY = h / 2
    for (i in 1 until list.size) {
        val x0 = (i - 1) * plotW / (list.size - 1) + 2
        val x1 = i * plotW / (list.size - 1) + 2
        val y0 = h - 4 - ((spec.read(list[i - 1]) / max).coerceIn(0.0, 1.0) * (h - 8)).toInt()
        val y1 = h - 4 - ((spec.read(list[i]) / max).coerceIn(0.0, 1.0) * (h - 8)).toInt()
        phase = line(buf, w, h, x0, y0, x1, y1, spec.r, spec.g, spec.b, spec.stroke, phase)
        endY = y1
    }
    return endY
}

private fun line(
    buf: Uint8Buffer,
    w: Int,
    h: Int,
    x0: Int,
    y0: Int,
    x1: Int,
    y1: Int,
    r: Int,
    g: Int,
    b: Int,
    stroke: StatsStroke,
    phase: Int,
): Int {
    val n = maxOf(kotlin.math.abs(x1 - x0), kotlin.math.abs(y1 - y0), 1)
    var step = phase
    for (i in 0..n) {
        if (statsStrokeVisible(stroke, step)) {
            val x = x0 + (x1 - x0) * i / n
            val y = y0 + (y1 - y0) * i / n
            put(buf, w, h, x, y, r, g, b, 255)
        }
        step++
    }
    return step
}

private fun fill(buf: Uint8Buffer, r: Int, g: Int, b: Int, a: Int) {
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

private fun put(buf: Uint8Buffer, w: Int, h: Int, x: Int, y: Int, r: Int, g: Int, b: Int, a: Int) {
    if (x !in 0 until w || y !in 0 until h) return
    val i = (y * w + x) * 4
    buf[i] = r.toUByte()
    buf[i + 1] = g.toUByte()
    buf[i + 2] = b.toUByte()
    buf[i + 3] = a.toUByte()
}

internal fun statsPixel(buf: Uint8Buffer, w: Int, x: Int, y: Int): Int {
    val i = (y * w + x) * 4
    fun channel(n: Int) = buf[i + n].toInt() and 0xFF
    return (channel(0) shl 16) or (channel(1) shl 8) or channel(2)
}

/** 5x7 glyphs, bit 4 is the left pixel. Only the stats names. */
private val GLYPHS: Map<Char, IntArray> = mapOf(
    'P' to intArrayOf(0b11110, 0b10001, 0b10001, 0b11110, 0b10000, 0b10000, 0b10000),
    'o' to intArrayOf(0b00000, 0b01110, 0b10001, 0b10001, 0b10001, 0b01110, 0b00000),
    'p' to intArrayOf(0b10000, 0b11110, 0b10001, 0b10001, 0b11110, 0b10000, 0b10000),
    'Z' to intArrayOf(0b11111, 0b00001, 0b00010, 0b00100, 0b01000, 0b10000, 0b11111),
    'u' to intArrayOf(0b00000, 0b10001, 0b10001, 0b10001, 0b10001, 0b10011, 0b01101),
    'c' to intArrayOf(0b00000, 0b01110, 0b10000, 0b10000, 0b10000, 0b01110, 0b00000),
    'k' to intArrayOf(0b10000, 0b10010, 0b10100, 0b11000, 0b10100, 0b10010, 0b10001),
    'e' to intArrayOf(0b00000, 0b01110, 0b10001, 0b11111, 0b10000, 0b01110, 0b00000),
    'r' to intArrayOf(0b00000, 0b10110, 0b11001, 0b10000, 0b10000, 0b10000, 0b00000),
    't' to intArrayOf(0b00100, 0b11111, 0b00100, 0b00100, 0b00100, 0b00110, 0b00000),
    'i' to intArrayOf(0b00100, 0b00000, 0b00100, 0b00100, 0b00100, 0b00100, 0b00000),
    'n' to intArrayOf(0b00000, 0b11010, 0b10101, 0b10001, 0b10001, 0b10001, 0b00000),
    'E' to intArrayOf(0b11111, 0b10000, 0b10000, 0b11110, 0b10000, 0b10000, 0b11111),
    'g' to intArrayOf(0b00000, 0b01111, 0b10001, 0b01111, 0b00001, 0b00001, 0b01110),
)

private fun text(buf: Uint8Buffer, w: Int, h: Int, x: Int, y: Int, label: String, r: Int, g: Int, b: Int) {
    var cursor = x
    for (ch in label) {
        val rows = GLYPHS[ch] ?: continue
        for (row in rows.indices) {
            val bits = rows[row]
            for (col in 0 until 5) {
                if ((bits shr (4 - col)) and 1 == 1) {
                    put(buf, w, h, cursor + col, y + row, r, g, b, 255)
                }
            }
        }
        cursor += 6
    }
}
