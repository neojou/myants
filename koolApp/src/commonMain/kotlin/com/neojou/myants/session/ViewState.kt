package com.neojou.myants.session

import com.neojou.myants.render3d.RenderMode
import com.neojou.myants.render3d.WorldPalette
import com.neojou.myants.render3d.parseRenderMode
import de.fabmax.kool.util.Color
import kotlin.math.roundToInt

/**
 * User view preferences (persisted) and panel visibility. Moved from AppSession (#7 step 3).
 * Settings file format is unchanged: `key=value` lines, `ov:<path>` for tuner overrides.
 */
class ViewState {
    var showPheromones: Boolean = true
    var pheromoneStrength: Double = 1.0
    var antColor: Color = WorldPalette.worker
    var dayNight: Boolean = true
    var dayNightStrength: Double = 0.5
    var cinema: Boolean = true
    var renderer: RenderMode = RenderMode.D3
    var cameraPitchDeg: Float = -70f
    var bloomUser: Boolean = true
    var ssaoUser: Boolean = true
    var weatherUser: Boolean = true
    var shadowsUser: Boolean = true
    var grassDensity: Float = 1f

    var showGrafik: Boolean = false
    var showParams: Boolean = false
    var showStats: Boolean = false
    var showAnalyse: Boolean = false
    var showAbout: Boolean = false

    /** Applies the view keys of a saved settings map; invalid values keep the current value. */
    fun applySaved(saved: Map<String, String>) {
        saved["pheromones"]?.toBooleanStrictOrNull()?.let { showPheromones = it }
        saved["pheromoneIntensity"]?.toDoubleOrNull()?.let { pheromoneStrength = it }
        saved["dayNight"]?.toBooleanStrictOrNull()?.let { dayNight = it }
        saved["dayNightIntensity"]?.toDoubleOrNull()?.let { dayNightStrength = it }
        saved["cinematic"]?.toBooleanStrictOrNull()?.let { cinema = it }
        saved["renderer"]?.let { parseRenderMode(it)?.let { mode -> renderer = mode } }
        saved["cameraPitch"]?.toFloatOrNull()?.let { cameraPitchDeg = it.coerceIn(-90f, -60f) }
        saved["bloom"]?.toBooleanStrictOrNull()?.let { bloomUser = it }
        saved["ssao"]?.toBooleanStrictOrNull()?.let { ssaoUser = it }
        saved["weather"]?.toBooleanStrictOrNull()?.let { weatherUser = it }
        saved["shadows"]?.toBooleanStrictOrNull()?.let { shadowsUser = it }
        saved["grassDensity"]?.toFloatOrNull()?.let { grassDensity = it.coerceIn(0f, 1.5f) }
        saved["antColor"]?.let { hex ->
            try {
                antColor = Color(hex)
            } catch (_: Throwable) {
                // keep the default worker tint
            }
        }
    }

    /** Serializes settings in the exact key order AppSession wrote before #7. */
    fun encode(speed: Double, colonies: Int, quality: String, overrides: Map<String, Double>): String = buildString {
        append("speed=").append(speed).append('\n')
        append("colonies=").append(colonies).append('\n')
        append("quality=").append(quality).append('\n')
        append("pheromones=").append(showPheromones).append('\n')
        append("pheromoneIntensity=").append(pheromoneStrength).append('\n')
        append("antColor=").append(hex(antColor)).append('\n')
        append("dayNight=").append(dayNight).append('\n')
        append("dayNightIntensity=").append(dayNightStrength).append('\n')
        append("cinematic=").append(cinema).append('\n')
        append("renderer=").append(if (renderer == RenderMode.D3) "3d" else "2d").append('\n')
        append("cameraPitch=").append(cameraPitchDeg).append('\n')
        append("bloom=").append(bloomUser).append('\n')
        append("ssao=").append(ssaoUser).append('\n')
        append("weather=").append(weatherUser).append('\n')
        append("shadows=").append(shadowsUser).append('\n')
        append("grassDensity=").append(grassDensity).append('\n')
        for ((path, value) in overrides) {
            append("ov:").append(path).append('=').append(value).append('\n')
        }
    }

    fun isOpen(panel: Panel): Boolean = when (panel) {
        Panel.GRAFIK -> showGrafik
        Panel.PARAMS -> showParams
        Panel.STATS -> showStats
        Panel.ANALYSE -> showAnalyse
        Panel.ABOUT -> showAbout
    }

    /**
     * Opens a side [panel] exclusively (or toggles it off when already open).
     * [Panel.ABOUT] is a dialog independent of the side panels: it only opens.
     */
    fun openPanel(panel: Panel) {
        if (panel == Panel.ABOUT) {
            showAbout = true
            return
        }
        val was = isOpen(panel)
        showGrafik = panel == Panel.GRAFIK && !was
        showParams = panel == Panel.PARAMS && !was
        showStats = panel == Panel.STATS && !was
        showAnalyse = panel == Panel.ANALYSE && !was
    }

    fun closeSidePanels() {
        showGrafik = false
        showParams = false
        showStats = false
        showAnalyse = false
    }

    fun showOnlyAnalyse() {
        showGrafik = false
        showParams = false
        showStats = false
        showAnalyse = true
    }

    companion object {
        fun parse(text: String): Map<String, String> {
            val map = HashMap<String, String>()
            for (line in text.lineSequence()) {
                val i = line.indexOf('=')
                if (i <= 0) continue
                map[line.substring(0, i).trim()] = line.substring(i + 1).trim()
            }
            return map
        }

        fun hex(color: Color): String {
            fun ch(v: Float): String {
                val n = (v * 255f).roundToInt().coerceIn(0, 255)
                return n.toString(16).padStart(2, '0')
            }
            return "#${ch(color.r)}${ch(color.g)}${ch(color.b)}"
        }
    }
}
