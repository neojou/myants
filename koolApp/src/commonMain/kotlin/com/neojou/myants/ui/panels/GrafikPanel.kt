package com.neojou.myants.ui.panels

import com.neojou.myants.updatePheromoneStrength
import com.neojou.myants.updateDayNightStrength
import com.neojou.myants.updateDayNight
import com.neojou.myants.updateCinema
import com.neojou.myants.setQuality
import com.neojou.myants.setPheromones
import com.neojou.myants.setAntColor
import com.neojou.myants.chooseWeather
import com.neojou.myants.chooseSsao
import com.neojou.myants.chooseShadows
import com.neojou.myants.chooseRenderer
import com.neojou.myants.choosePitch
import com.neojou.myants.chooseGrassDensity
import com.neojou.myants.chooseBloom
import com.neojou.myants.render3d.RenderMode
import com.neojou.myants.render3d.bloomAllowed
import com.neojou.myants.sim.PerformanceManager
import com.neojou.myants.sim.QualityLevel
import de.fabmax.kool.util.Color
import com.neojou.myants.AppSession
import com.neojou.myants.ui.L10n
import com.neojou.myants.ui.MyantsTheme
import com.neojou.myants.ui.UiText
import com.neojou.myants.ui.tr
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.scene.Scene

private val ANT_COLORS = MyantsTheme.Data.antColorChoices
private val T = MyantsTheme.colors

internal fun UiScope.grafik(session: AppSession) {
    val lang = L10n.language.use()
    Text(tr(UiText.QUALITY)) { modifier.margin(top = MyantsTheme.space.s2) }
    ComboBox {
        modifier
            .width(Grow.Std)
            .items(QualityLevel.entries.map { qualityName(it) })
            .selectedIndex(PerformanceManager.level.ordinal)
            .onItemSelected { session.setQuality(QualityLevel.entries[it]) }
    }
    Text(tr(UiText.RENDERER)) { modifier.margin(top = MyantsTheme.space.s2) }
    ComboBox {
        modifier
            .width(Grow.Std)
            .items(listOf("3D", tr(UiText.CLASSIC_2D)))
            .selectedIndex(if (session.renderer == RenderMode.D3) 0 else 1)
            .onItemSelected { session.chooseRenderer(if (it == 0) RenderMode.D3 else RenderMode.CLASSIC) }
    }
    Row {
        modifier.margin(top = MyantsTheme.space.s2)
        Checkbox(session.showPheromones) {
            modifier.onToggle { session.setPheromones(it) }
        }
        Text(tr(UiText.PHEROMONES)) { modifier.alignY(AlignmentY.Center).margin(start = MyantsTheme.space.s2) }
    }
    Slider(session.pheromoneStrength.toFloat(), 0f, 1.5f) {
        modifier.width(Grow.Std).onChange { session.updatePheromoneStrength(it.toDouble()) }
    }
    Text(tr(UiText.ANT_COLOR)) { modifier.margin(top = MyantsTheme.space.s2) }
    Row {
        for (choice in ANT_COLORS) {
            val selected = session.antColor == Color(choice.hex)
            Column {
                modifier.margin(end = MyantsTheme.space.s2).onClick { session.setAntColor(choice.hex) }
                Box {
                    modifier
                        .size(MyantsTheme.size.button, MyantsTheme.size.button)
                        .alignX(AlignmentX.Center)
                        .background(RoundRectBackground(Color(choice.hex), MyantsTheme.radius.md))
                    if (selected) modifier.border(selectedBorder())
                }
                Text(choice.name(lang)) {
                    modifier
                        .alignX(AlignmentX.Center)
                        .margin(top = MyantsTheme.space.s1)
                        .font(sizes.smallText)
                        .textColor(if (selected) T.primaryContainer else T.onSurface)
                }
                Tooltip(
                    "${choice.name(lang)} ${choice.hex}",
                    tooltipState = remember { TooltipState(MyantsTheme.motion.tooltipDelaySec) },
                    backgroundColor = T.surfaceRaised,
                    borderColor = T.outline,
                )
            }
        }
    }
    Text(MyantsTheme.Data.rivalColonyLabel(lang)) {
        modifier.margin(top = MyantsTheme.space.s1).font(sizes.smallText).textColor(T.onSurfaceVar)
    }
    Row {
        modifier.margin(top = MyantsTheme.space.s2)
        Checkbox(session.dayNight) { modifier.onToggle { session.updateDayNight(it) } }
        Text(tr(UiText.DAY_NIGHT)) { modifier.alignY(AlignmentY.Center).margin(start = MyantsTheme.space.s2) }
    }
    Slider(session.dayNightStrength.toFloat(), 0f, 1f) {
        modifier.width(Grow.Std).onChange { session.updateDayNightStrength(it.toDouble()) }
    }
    Row {
        modifier.margin(top = MyantsTheme.space.s2)
        Checkbox(session.cinema) { modifier.onToggle { session.updateCinema(it) } }
        Text(tr(UiText.CINEMA)) { modifier.alignY(AlignmentY.Center).margin(start = MyantsTheme.space.s2) }
    }
    val bloomOk = bloomAllowed(session.renderBridge)
    Row {
        modifier.margin(top = MyantsTheme.space.s2)
        Checkbox(session.bloomUser && bloomOk) {
            modifier.onToggle { if (bloomOk) session.chooseBloom(it) }
        }
        Text(if (bloomOk) tr(UiText.BLOOM) else tr(UiText.BLOOM_OFF)) {
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
        Text(tr(UiText.SHADOWS)) { modifier.alignY(AlignmentY.Center).margin(start = MyantsTheme.space.s2) }
    }
    Row {
        modifier.margin(top = MyantsTheme.space.s2)
        Checkbox(session.weatherUser) { modifier.onToggle { session.chooseWeather(it) } }
        Text(tr(UiText.WEATHER)) { modifier.alignY(AlignmentY.Center).margin(start = MyantsTheme.space.s2) }
    }
    Text("${tr(UiText.GRASS)} ${session.grassDensity}") { modifier.margin(top = MyantsTheme.space.s2) }
    Slider(session.grassDensity, 0f, 1.5f) {
        modifier.width(Grow.Std).onChange { session.chooseGrassDensity(it) }
    }
    Text("${tr(UiText.PITCH)} ${session.cameraPitchDeg.toInt()}") { modifier.margin(top = MyantsTheme.space.s2) }
    Slider(session.cameraPitchDeg, -90f, -60f) {
        modifier.width(Grow.Std).onChange { session.choosePitch(it) }
    }
}

private fun UiScope.qualityName(level: QualityLevel): String = when (level) {
    QualityLevel.ULTRA_LOW -> tr(UiText.QUALITY_ULTRA_LOW)
    QualityLevel.LOW -> tr(UiText.QUALITY_LOW)
    QualityLevel.MEDIUM -> tr(UiText.QUALITY_MEDIUM)
    QualityLevel.HIGH -> tr(UiText.QUALITY_HIGH)
    QualityLevel.ULTRA -> tr(UiText.QUALITY_ULTRA)
}

/** Non-color cue for selected state (color-vision deficiency, #4): 1dp primaryContainer outline. */
private fun selectedBorder() = RoundRectBorder(T.primaryContainer, MyantsTheme.radius.md, MyantsTheme.stroke.selected)
