package com.neojou.myants.ui.panels

import com.neojou.myants.render3d.RenderMode
import com.neojou.myants.render3d.bloomAllowed
import com.neojou.myants.sim.PerformanceManager
import com.neojou.myants.sim.QualityLevel
import de.fabmax.kool.util.Color
import com.neojou.myants.AppSession
import com.neojou.myants.ui.MyantsTheme
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.scene.Scene

private val ANT_COLORS = MyantsTheme.Data.antColors
private val T = MyantsTheme.colors

internal fun UiScope.grafik(session: AppSession) {
    Text("Qualität") { modifier.margin(top = MyantsTheme.space.s2) }
    ComboBox {
        modifier
            .width(Grow.Std)
            .items(QualityLevel.entries.map { it.name })
            .selectedIndex(PerformanceManager.level.ordinal)
            .onItemSelected { session.setQuality(QualityLevel.entries[it]) }
    }
    Text("Renderer") { modifier.margin(top = MyantsTheme.space.s2) }
    ComboBox {
        modifier
            .width(Grow.Std)
            .items(listOf("3D", "Classic 2D"))
            .selectedIndex(if (session.renderer == RenderMode.D3) 0 else 1)
            .onItemSelected { session.chooseRenderer(if (it == 0) RenderMode.D3 else RenderMode.CLASSIC) }
    }
    Row {
        modifier.margin(top = MyantsTheme.space.s2)
        Checkbox(session.showPheromones) {
            modifier.onToggle { session.setPheromones(it) }
        }
        Text("Pheromon-Spuren") { modifier.alignY(AlignmentY.Center).margin(start = MyantsTheme.space.s2) }
    }
    Slider(session.pheromoneStrength.toFloat(), 0f, 1.5f) {
        modifier.width(Grow.Std).onChange { session.updatePheromoneStrength(it.toDouble()) }
    }
    Text("Ameisenfarbe") { modifier.margin(top = MyantsTheme.space.s2) }
    Row {
        for (hex in ANT_COLORS) {
            Button(hex) {
                modifier.margin(end = MyantsTheme.space.s2).onClick { session.setAntColor(hex) }
                if (session.antColor == Color(hex)) {
                    modifier.colors(buttonColor = T.primary, buttonHoverColor = T.primary, textColor = T.primaryContainer, textHoverColor = T.primaryContainer)
                    modifier.border(selectedBorder())
                }
            }
        }
    }
    Row {
        modifier.margin(top = MyantsTheme.space.s2)
        Checkbox(session.dayNight) { modifier.onToggle { session.updateDayNight(it) } }
        Text("Tag/Nacht-Tönung") { modifier.alignY(AlignmentY.Center).margin(start = MyantsTheme.space.s2) }
    }
    Slider(session.dayNightStrength.toFloat(), 0f, 1f) {
        modifier.width(Grow.Std).onChange { session.updateDayNightStrength(it.toDouble()) }
    }
    Row {
        modifier.margin(top = MyantsTheme.space.s2)
        Checkbox(session.cinema) { modifier.onToggle { session.updateCinema(it) } }
        Text("Kino-Kamera") { modifier.alignY(AlignmentY.Center).margin(start = MyantsTheme.space.s2) }
    }
    val bloomOk = bloomAllowed(session)
    Row {
        modifier.margin(top = MyantsTheme.space.s2)
        Checkbox(session.bloomUser && bloomOk) {
            modifier.onToggle { if (bloomOk) session.chooseBloom(it) }
        }
        Text(if (bloomOk) "Bloom / Glühen" else "Bloom / Glühen (off)") {
            modifier.alignY(AlignmentY.Center).margin(start = MyantsTheme.space.s2)
        }
    }
    Row {
        modifier.margin(top = MyantsTheme.space.s2)
        Checkbox(session.ssaoUser) { modifier.onToggle { session.chooseSsao(it) } }
        Text("SSAO") { modifier.alignY(AlignmentY.Center).margin(start = MyantsTheme.space.s2) }
    }
    Row {
        modifier.margin(top = MyantsTheme.space.s2)
        Checkbox(session.shadowsUser) { modifier.onToggle { session.chooseShadows(it) } }
        Text("Schatten") { modifier.alignY(AlignmentY.Center).margin(start = MyantsTheme.space.s2) }
    }
    Row {
        modifier.margin(top = MyantsTheme.space.s2)
        Checkbox(session.weatherUser) { modifier.onToggle { session.chooseWeather(it) } }
        Text("Wetter-Effekte") { modifier.alignY(AlignmentY.Center).margin(start = MyantsTheme.space.s2) }
    }
    Text("Gras-Dichte ${session.grassDensity}") { modifier.margin(top = MyantsTheme.space.s2) }
    Slider(session.grassDensity, 0f, 1.5f) {
        modifier.width(Grow.Std).onChange { session.chooseGrassDensity(it) }
    }
    Text("Kamera-Neigung ${session.cameraPitchDeg.toInt()}") { modifier.margin(top = MyantsTheme.space.s2) }
    Slider(session.cameraPitchDeg, -90f, -60f) {
        modifier.width(Grow.Std).onChange { session.choosePitch(it) }
    }
}

/** Non-color cue for selected state (color-vision deficiency, #4): 1dp primaryContainer outline. */
private fun selectedBorder() = RoundRectBorder(T.primaryContainer, MyantsTheme.radius.md, MyantsTheme.stroke.selected)
