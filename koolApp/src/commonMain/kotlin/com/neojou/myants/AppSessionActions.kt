package com.neojou.myants

import com.neojou.myants.render3d.RenderMode
import com.neojou.myants.render3d.RingSpace
import com.neojou.myants.render3d.ScatterLayout
import com.neojou.myants.render3d.quality.QualityPreset
import com.neojou.myants.session.CameraState
import com.neojou.myants.session.CinemaDirector
import com.neojou.myants.session.HudState
import com.neojou.myants.session.SimClock
import com.neojou.myants.session.ToolController
import com.neojou.myants.session.FpsCounter
import com.neojou.myants.session.ShotPose
import com.neojou.myants.session.TunerOverrides
import com.neojou.myants.session.Panel
import com.neojou.myants.session.ViewState
import com.neojou.myants.ui.L10n
import com.neojou.myants.ui.UiLanguage
import com.neojou.myants.session.buildAnalyseLines
import com.neojou.myants.sim.Ant
import com.neojou.myants.sim.AntPlace
import com.neojou.myants.sim.PerformanceManager
import com.neojou.myants.sim.QualityLevel
import com.neojou.myants.sim.Rng
import com.neojou.myants.sim.SimConfig
import com.neojou.myants.sim.World
import com.neojou.tools.LogLevel
import com.neojou.tools.MyLog
import de.fabmax.kool.modules.ui2.mutableStateOf
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.Time


// User actions on [AppSession]: restart, prefs, panels, language, follow.

/** Hit test for the 3D nest cutaway. Classic 2D still uses [clickWorld] only. */
fun AppSession.inspectNest(nx: Double, ny: Double) {
    notifyInput()
    tools.inspectNest(world, nx, ny)
    userFollow = false
    bump()
}

fun AppSession.toggleRain() {
    if (world.raining) {
        world.raining = false
        world.rainTimer = 0
    } else {
        world.raining = true
        world.rainTimer = SimConfig.environment.rainMinDuration * 2
    }
    notifyInput()
    publishChrome()
}

fun AppSession.restart(fromUser: Boolean) {
    world = World()
    worldSerial++
    cine.follow = null
    userFollow = false
    tools.clear()
    cine.nextPick = 0.0
    deadFrames = 0
    history.clear()
    resetCamera(fromUser = false)
    if (fromUser) notifyInput()
    bump()
}

/** Opens the restart dialog. The world stays until [confirmRestart]. */
fun AppSession.askRestart() {
    restartPrompt = true
    notifyInput()
    bump()
}

fun AppSession.dismissRestart() {
    restartPrompt = false
    bump()
}

fun AppSession.confirmRestart() {
    restartPrompt = false
    restart(fromUser = true)
}

fun AppSession.setColonies(count: Int) {
    SimConfig.colonyCount = count.coerceIn(1, 2)
    save()
    restart(fromUser = true)
}

fun AppSession.setQuality(level: QualityLevel) {
    PerformanceManager.setQuality(level); renderTierBias = 0; save(); notifyInput(); bump()
}

/** Runs [change], persists settings, then bumps the UI revision when [bump] is true. */
internal inline fun AppSession.persist(bump: Boolean = true, change: () -> Unit) {
    change()
    save()
    if (bump) bump()
}

fun AppSession.setPheromones(on: Boolean) = persist { showPheromones = on }
fun AppSession.updatePheromoneStrength(value: Double) = persist { pheromoneStrength = value.coerceIn(0.0, 1.5) }
fun AppSession.updateDayNight(on: Boolean) = persist { dayNight = on }
fun AppSession.updateDayNightStrength(value: Double) = persist { dayNightStrength = value.coerceIn(0.0, 1.0) }
fun AppSession.updateCinema(on: Boolean) = persist { cinema = on }
fun AppSession.chooseRenderer(mode: RenderMode) = persist { renderer = mode; applyRendererVisibility() }
fun AppSession.choosePitch(deg: Float) = persist { cameraPitchDeg = deg.coerceIn(-90f, -60f) }
fun AppSession.chooseBloom(on: Boolean) = persist { bloomUser = on }
fun AppSession.chooseSsao(on: Boolean) = persist { ssaoUser = on }
fun AppSession.chooseWeather(on: Boolean) = persist { weatherUser = on }
fun AppSession.chooseShadows(on: Boolean) = persist { shadowsUser = on }
fun AppSession.chooseGrassDensity(value: Float) = persist { grassDensity = value.coerceIn(0f, 1.5f) }

fun AppSession.setAntColor(hex: String) {
    try {
        antColor = Color(hex)
        save()
        bump()
    } catch (_: Throwable) {
        MyLog.add("APP", "Bad ant color $hex", LogLevel.DEBUG)
    }
}

fun AppSession.openPanel(panel: Panel) { view.openPanel(panel); notifyInput(); bump() }

fun AppSession.closeSidePanels() { view.closeSidePanels(); bump() }

fun AppSession.refreshAnalyse() {
    analyseLines.clear()
    analyseLines += buildAnalyseLines(world)
    view.showOnlyAnalyse()
    bump()
}

fun AppSession.applyAnalyse(path: String, to: Double) = setTunable(path, to)

fun AppSession.setTunable(path: String, value: Double) {
    if (overrides.set(path, value)) save() else MyLog.add("APP", "Tuner path skipped: $path", LogLevel.DEBUG)
    bump()
}

fun AppSession.toggleLanguageMenu() {
    languageMenu.value = !languageMenu.value
}

fun AppSession.closeLanguageMenu() {
    languageMenu.value = false
}

fun AppSession.setLanguage(lang: UiLanguage) {
    languageMenu.value = false
    view.language = lang
    L10n.language.value = lang
    tools.refresh(world, leafSpots())
    hud.sync(world, fpsShown, Time.gameTime, force = true)
    save()
    bump()
}

fun AppSession.resetTunables() = persist { overrides.reset() }

fun AppSession.requestScreenshot() { screenshotRequested = true }

fun AppSession.toggleUserFollow() {
    userFollow = !userFollow
    if (!userFollow) cine.follow = null
    notifyInput()
    bump()
}

fun AppSession.noteDrag() { userFollow = false; notifyInput() }

fun AppSession.speedLabel(): String = HudState.speedLabel(speeds[speedIndex])
