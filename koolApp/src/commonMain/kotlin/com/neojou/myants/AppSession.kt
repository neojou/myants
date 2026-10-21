package com.neojou.myants

import com.neojou.myants.render3d.RenderMode
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

enum class SimTool { INSPECT, SUGAR, PROTEIN, ENEMY }

class AnalyseLine(val text: String, val path: String?, val to: Double)

class GraphSample(val population: Int, val sugar: Double, val protein: Double, val energyPct: Double)

/**
 * Composition root of the live session (#7): [SimClock], [CameraState], [ToolController],
 * [ViewState] (prefs + panels + settings format), [HudState] and [CinemaDirector].
 * Public properties delegate to those parts so UI and render code stay unchanged.
 * Cinema picks use [kotlin.random.Random], never [Rng].
 */
class AppSession(launch: LaunchConfig) {
    val clock = SimClock()
    val camera = CameraState()
    val tools = ToolController()
    val view = ViewState()
    val hud = HudState()
    val cine = CinemaDirector()

    val speeds: DoubleArray get() = clock.speeds
    var speedIndex by clock::speedIndex
    var paused by clock::paused
    var tool by tools::tool
    var inspectedAnt by tools::inspectedAnt
    val inspect get() = tools.inspect
    var camX by camera::x
    var camY by camera::y
    var zoom by camera::zoom

    var showPheromones by view::showPheromones
    var pheromoneStrength by view::pheromoneStrength
    var antColor by view::antColor
    var dayNight by view::dayNight
    var dayNightStrength by view::dayNightStrength
    var cinema by view::cinema
    var renderer by view::renderer
    var cameraPitchDeg by view::cameraPitchDeg
    var bloomUser by view::bloomUser
    var ssaoUser by view::ssaoUser
    var weatherUser by view::weatherUser
    var shadowsUser by view::shadowsUser
    var grassDensity by view::grassDensity
    var showGrafik by view::showGrafik
    var showParams by view::showParams
    var showStats by view::showStats
    var showAnalyse by view::showAnalyse
    var showAbout by view::showAbout

    val hudFps get() = hud.fps
    val hudPop get() = hud.pop
    val hudFood get() = hud.food
    val hudQueen get() = hud.queen
    val history get() = hud.history

    var renderTierBias: Int = 0
    var userFollow: Boolean = false
    var nest3dActive: Boolean = false
    val noBloom: Boolean = launch.noBloom
    val shot: String? = launch.shot
    val shotOut: String? = launch.shotOut

    var classicScene: Scene? = null
    var worldScene: Scene? = null
    var nestOverlay: Scene? = null
    var nestPresent: Scene? = null
    var uiScene: Scene? = null
    var nestTexture: Texture2d? = null
    var screenshotRequested: Boolean = false
    var worldSerial: Int = 1
    var fpsShownPublic: Int = 0
    var gpuMs: Float = 0f

    val revision = mutableStateOf(0)

    /** Toolbar fields. Equal writes do not recompose. Side panels still use [revision]. */
    val pausedState = mutableStateOf(false)
    val speedLabelState = mutableStateOf("1x")
    val toolState = mutableStateOf(SimTool.INSPECT)
    val rainingState = mutableStateOf(false)
    val colonyCountState = mutableStateOf(1)

    val analyseLines = ArrayList<AnalyseLine>()

    var world: World
    val seed: Int
    private var deadFrames: Int = 0
    private val fps = FpsCounter()
    private val fpsShown: Int get() = fps.shown
    private val settings = SettingsStore()
    private val overrides = TunerOverrides()

    init {
        val saved = settings.read()?.let { ViewState.parse(it) } ?: emptyMap()
        var colonies = 2
        var quality = QualityLevel.MEDIUM
        saved["colonies"]?.toIntOrNull()?.let { colonies = it.coerceIn(1, 2) }
        saved["quality"]?.let { parseQuality(it)?.let { q -> quality = q } }
        saved["speed"]?.toDoubleOrNull()?.let { clock.restoreSpeed(it) }
        view.applySaved(saved)
        L10n.language.value = view.language
        launch.colonies?.let { colonies = it.coerceIn(1, 2) }
        launch.quality?.let { quality = it }
        launch.renderer?.let { renderer = it }
        overrides.load(saved)
        seed = launch.seed ?: wallClockSeed()
        Rng.seed(seed)
        SimConfig.colonyCount = colonies
        PerformanceManager.setQuality(quality)
        world = World()
        resetCamera(fromUser = false)
        ShotPose.apply(shot, world, camera)
        publishChrome()
    }

    fun applyRendererVisibility() {
        val classic = renderer == RenderMode.CLASSIC
        classicScene?.isVisible = classic
        worldScene?.isVisible = !classic
        nestOverlay?.isVisible = !classic && !nest3dActive
        nestPresent?.isVisible = !classic && nest3dActive
    }

    val colonies: Int get() = SimConfig.colonyCount

    fun bump() {
        revision.value = revision.value + 1
        publishChrome()
    }

    /** Copies the toolbar fields into their states. Call after the world exists. */
    fun publishChrome() {
        pausedState.value = paused
        speedLabelState.value = speedLabel()
        toolState.value = tool
        rainingState.value = world.raining
        colonyCountState.value = colonies
    }

    fun notifyInput() = cine.notifyInput(Time.gameTime)

    fun tick(dt: Float) {
        val now = Time.gameTime
        if (fps.frame(now)) {
            fpsShownPublic = fpsShown
            PerformanceManager.monitorFps(fpsShown.toDouble(), (now * 1000.0).toLong())
        }
        if (!paused) {
            clock.advance(dt.toDouble()) { stepSim() }
            val decided = world.totalAntCount() == 0 || world.colonies.any { it.queen.dead }
            if (decided) {
                deadFrames++
                if (deadFrames > 480) restart(fromUser = false)
            } else {
                deadFrames = 0
            }
        }
        if (!userFollow && cinema) cine.update(world, camera, now)
        if (userFollow) inspectedAnt?.let { if (!cine.updateUserFollow(camera, it)) userFollow = false }
        hud.sync(world, fpsShown, now)
    }

    private fun stepSim() {
        world.update()
        if (world.age % 60 == 0) hud.recordSample(world)
    }

    fun stepOnce() {
        if (!paused) return
        stepSim()
        hud.sync(world, fpsShown, Time.gameTime, force = true)
        bump()
    }

    fun togglePause() { clock.togglePause(); notifyInput(); publishChrome() }
    fun stepSpeed(dir: Int) { clock.stepSpeed(dir); save(); notifyInput(); publishChrome() }
    fun zoomBy(factor: Double) { zoomAt(factor, camX, camY); notifyInput() }
    fun zoomAt(factor: Double, anchorX: Double, anchorY: Double) = camera.zoomAt(factor, anchorX, anchorY)
    fun pan(dxPx: Float, dyPx: Float, fit: Float) { if (camera.pan(dxPx, dyPx, fit)) notifyInput() }
    fun resetCamera(fromUser: Boolean = true) { camera.reset(); if (fromUser) notifyInput() }
    fun clampCamera(leftW: Float, winH: Float) = camera.clamp(leftW, winH)
    fun fitScale(leftW: Float, winH: Float): Double = camera.fitScale(leftW, winH)
    fun screenToWorld(px: Float, py: Float, leftW: Float, winH: Float): Pair<Double, Double> =
        camera.screenToWorld(px, py, leftW, winH)
    fun clickWorld(wx: Double, wy: Double) { notifyInput(); tools.apply(world, wx, wy); bump() }
    /** Hit test for the 3D nest cutaway. Classic 2D still uses [clickWorld] only. */
    fun inspectNest(nx: Double, ny: Double) { notifyInput(); tools.inspectNest(world, nx, ny); bump() }

    fun toggleRain() {
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

    fun restart(fromUser: Boolean) {
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

    fun setColonies(count: Int) {
        SimConfig.colonyCount = count.coerceIn(1, 2)
        save()
        restart(fromUser = true)
    }

    fun setQuality(level: QualityLevel) {
        PerformanceManager.setQuality(level); renderTierBias = 0; save(); notifyInput(); bump()
    }

    /** Runs [change], persists settings, then bumps the UI revision when [bump] is true. */
    private inline fun persist(bump: Boolean = true, change: () -> Unit) {
        change()
        save()
        if (bump) bump()
    }

    fun setPheromones(on: Boolean) = persist { showPheromones = on }
    fun updatePheromoneStrength(value: Double) = persist { pheromoneStrength = value.coerceIn(0.0, 1.5) }
    fun updateDayNight(on: Boolean) = persist { dayNight = on }
    fun updateDayNightStrength(value: Double) = persist { dayNightStrength = value.coerceIn(0.0, 1.0) }
    fun updateCinema(on: Boolean) = persist { cinema = on }
    fun chooseRenderer(mode: RenderMode) = persist { renderer = mode; applyRendererVisibility() }
    fun choosePitch(deg: Float) = persist(bump = false) { cameraPitchDeg = deg.coerceIn(-90f, -60f) }
    fun chooseBloom(on: Boolean) = persist { bloomUser = on }
    fun chooseSsao(on: Boolean) = persist { ssaoUser = on }
    fun chooseWeather(on: Boolean) = persist { weatherUser = on }
    fun chooseShadows(on: Boolean) = persist { shadowsUser = on }
    fun chooseGrassDensity(value: Float) = persist(bump = false) { grassDensity = value.coerceIn(0f, 1.5f) }

    fun setAntColor(hex: String) {
        try {
            antColor = Color(hex)
            save()
            bump()
        } catch (_: Throwable) {
            MyLog.add("APP", "Bad ant color $hex", LogLevel.DEBUG)
        }
    }

    fun openPanel(panel: Panel) { view.openPanel(panel); notifyInput(); bump() }

    fun closeSidePanels() { view.closeSidePanels(); bump() }

    fun refreshAnalyse() {
        analyseLines.clear()
        analyseLines += buildAnalyseLines(world)
        view.showOnlyAnalyse()
        bump()
    }

    fun applyAnalyse(path: String, to: Double) = setTunable(path, to)

    fun setTunable(path: String, value: Double) {
        if (overrides.set(path, value)) save() else MyLog.add("APP", "Tuner path skipped: $path", LogLevel.DEBUG)
        bump()
    }

    fun setLanguage(lang: UiLanguage) {
        view.language = lang
        L10n.language.value = lang
        hud.sync(world, fpsShown, Time.gameTime, force = true)
        save()
        bump()
    }

    fun resetTunables() = persist { overrides.reset() }

    fun requestScreenshot() { screenshotRequested = true }

    fun toggleUserFollow() {
        userFollow = !userFollow
        if (!userFollow) cine.follow = null
        notifyInput()
        bump()
    }

    fun noteDrag() { userFollow = false; notifyInput() }

    fun speedLabel(): String = HudState.speedLabel(speeds[speedIndex])

    private fun save() {
        settings.write(view.encode(speeds[speedIndex], SimConfig.colonyCount, PerformanceManager.level.name, overrides.values))
    }
}
