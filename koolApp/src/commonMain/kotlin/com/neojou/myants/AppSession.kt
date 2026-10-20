package com.neojou.myants

import com.neojou.myants.render3d.RenderMode
import com.neojou.myants.render3d.parseRenderMode
import com.neojou.myants.sim.Ant
import com.neojou.myants.sim.AntKind
import com.neojou.myants.sim.AntPlace
import com.neojou.myants.sim.AntState
import com.neojou.myants.sim.FoodKind
import com.neojou.myants.sim.InsectKind
import com.neojou.myants.sim.PerformanceManager
import com.neojou.myants.sim.QualityLevel
import com.neojou.myants.sim.Rng
import com.neojou.myants.sim.SimConfig
import com.neojou.myants.sim.World
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.scene.Scene
import com.neojou.tools.LogLevel
import com.neojou.tools.MyLog
import de.fabmax.kool.modules.ui2.mutableStateOf
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.Time
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

enum class SimTool { INSPECT, SUGAR, PROTEIN, ENEMY }

class AnalyseLine(val text: String, val path: String?, val to: Double)

class GraphSample(val population: Int, val sugar: Double, val protein: Double, val energyPct: Double)

/**
 * Live session: fixed 60 Hz sim clock, camera, tools, and the UI settings file.
 * Cinema picks use [kotlin.random.Random], never [Rng].
 */
class AppSession(launch: LaunchConfig) {
    val speeds = doubleArrayOf(0.0, 0.5, 1.0, 2.0, 4.0, 8.0)
    var speedIndex: Int = 2
    var paused: Boolean = false
    var tool: SimTool = SimTool.INSPECT
    var camX: Double = SimConfig.width / 2.0
    var camY: Double = SimConfig.height / 2.0
    var zoom: Double = 1.0
    var showPheromones: Boolean = true
    var pheromoneStrength: Double = 1.0
    var antColor: Color = Color("#c4703a")
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
    var inspectedAnt: Ant? = null
    var screenshotRequested: Boolean = false
    var worldSerial: Int = 1
    var fpsShownPublic: Int = 0
    var gpuMs: Float = 0f

    var showGrafik: Boolean = false
    var showParams: Boolean = false
    var showStats: Boolean = false
    var showAnalyse: Boolean = false
    var showAbout: Boolean = false

    val revision = mutableStateOf(0)
    val hudFps = mutableStateOf("FPS: 0")
    val hudPop = mutableStateOf("Population: 0")
    val hudFood = mutableStateOf("Protein: 0 | Sugar: 0")
    val hudQueen = mutableStateOf("Queen Age: 0m 0s")
    val inspect = mutableStateOf("")
    val analyseLines = ArrayList<AnalyseLine>()
    val history = ArrayDeque<GraphSample>()

    var world: World
    val seed: Int
    private var accum: Double = 0.0
    private var halfTick: Boolean = false
    private var deadFrames: Int = 0
    private var fpsFrames: Int = 0
    private var fpsStamp: Double = 0.0
    private var fpsShown: Int = 0
    private val settings = SettingsStore()
    private val overrides = HashMap<String, Double>()

    private var cinePausedUntil: Double = 0.0
    private var cineTargetX: Double = SimConfig.width / 2.0
    private var cineTargetY: Double = SimConfig.height / 2.0
    private var cineTargetZoom: Double = 1.0
    private var cineNextPick: Double = 0.0
    private var cinePhase: Double = 0.0
    private var follow: Ant? = null
    private val shotQueue = ArrayDeque<String>()
    private val cineRandom = kotlin.random.Random(1)

    init {
        val saved = loadSettings()
        var colonies = 2
        var quality = QualityLevel.MEDIUM
        saved["colonies"]?.toIntOrNull()?.let { colonies = it.coerceIn(1, 2) }
        saved["quality"]?.let { parseQuality(it)?.let { q -> quality = q } }
        saved["speed"]?.toDoubleOrNull()?.let { spd ->
            val idx = speeds.indexOfFirst { kotlin.math.abs(it - spd) < 1e-6 }
            if (idx >= 0) speedIndex = idx
        }
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
        launch.colonies?.let { colonies = it.coerceIn(1, 2) }
        launch.quality?.let { quality = it }
        launch.renderer?.let { renderer = it }
        for ((key, value) in saved) {
            if (!key.startsWith("ov:")) continue
            val number = value.toDoubleOrNull() ?: continue
            try {
                SimConfig.setNumber(key.removePrefix("ov:"), number)
                overrides[key.removePrefix("ov:")] = number
            } catch (_: Throwable) {
                // unknown path from an older settings file
            }
        }
        seed = launch.seed ?: wallClockSeed()
        Rng.seed(seed)
        SimConfig.colonyCount = colonies
        PerformanceManager.setQuality(quality)
        world = World()
        resetCamera(fromUser = false)
        applyShotPose()
    }

    private fun applyShotPose() {
        when (shot) {
            "day" -> {
                world.age = (world.dayLength * 0.45).toInt()
                world.timeOfDay = 0.45
            }
            "night" -> {
                world.age = (world.dayLength * 0.92).toInt()
                world.timeOfDay = (world.age % world.dayLength).toDouble() / world.dayLength.toDouble()
            }
            "rain" -> {
                world.age = (world.dayLength * 0.45).toInt()
                world.timeOfDay = 0.45
                world.raining = true
                world.rainTimer = 100000
            }
            "ant" -> {
                world.age = (world.dayLength * 0.45).toInt()
                world.timeOfDay = 0.45
                zoom = 6.0
            }
            else -> Unit
        }
    }

    fun applyRendererVisibility() {
        val classic = renderer == RenderMode.CLASSIC
        classicScene?.isVisible = classic
        worldScene?.isVisible = !classic
        val showOverlay = !classic && !nest3dActive
        nestOverlay?.isVisible = showOverlay
        nestPresent?.isVisible = !classic && nest3dActive
    }

    val colonies: Int get() = SimConfig.colonyCount

    fun bump() {
        revision.value = revision.value + 1
    }

    fun notifyInput() {
        cinePausedUntil = Time.gameTime + 25.0
    }

    fun tick(dt: Float) {
        val now = Time.gameTime
        fpsFrames++
        if (fpsStamp == 0.0) fpsStamp = now
        if (now - fpsStamp >= 1.0) {
            fpsShown = fpsFrames
            fpsShownPublic = fpsShown
            fpsFrames = 0
            fpsStamp = now
            PerformanceManager.monitorFps(fpsShown.toDouble(), (now * 1000.0).toLong())
        }
        if (!paused) {
            accum += dt.toDouble()
            val step = 1.0 / 60.0
            var frames = 0
            while (accum >= step && frames < 64) {
                accum -= step
                frames++
                val spd = speeds[speedIndex]
                if (spd == 0.0) continue
                if (spd < 1.0) {
                    halfTick = !halfTick
                    if (halfTick) stepSim()
                } else {
                    repeat(spd.toInt()) { stepSim() }
                }
            }
            if (accum > step) accum = step
            val decided = world.totalAntCount() == 0 || world.colonies.any { it.queen.dead }
            if (decided) {
                deadFrames++
                if (deadFrames > 480) restart(fromUser = false)
            } else {
                deadFrames = 0
            }
        }
        updateCinema()
        updateUserFollow()
        syncHud()
    }

    private fun stepSim() {
        world.update()
        if (world.age % 60 == 0) recordSample()
    }

    fun stepOnce() {
        if (!paused) return
        stepSim()
        syncHud()
        bump()
    }

    fun togglePause() {
        paused = !paused
        notifyInput()
        bump()
    }

    fun stepSpeed(dir: Int) {
        speedIndex = (speedIndex + dir).coerceIn(0, speeds.lastIndex)
        save()
        notifyInput()
        bump()
    }

    fun zoomBy(factor: Double) {
        zoomAt(factor, camX, camY)
        notifyInput()
    }

    fun zoomAt(factor: Double, anchorX: Double, anchorY: Double) {
        if (!factor.isFinite() || !anchorX.isFinite() || !anchorY.isFinite()) return
        if (!zoom.isFinite() || !camX.isFinite() || !camY.isFinite()) return
        val next = (zoom * factor).coerceIn(1.0, 6.0)
        val change = next / zoom
        if (change == 0.0 || !change.isFinite()) return
        camX = anchorX + (camX - anchorX) / change
        camY = anchorY + (camY - anchorY) / change
        zoom = next
    }

    fun pan(dxPx: Float, dyPx: Float, fit: Float) {
        val s = fit * zoom
        if (s == 0.0) return
        camX -= dxPx / s
        camY -= dyPx / s
        notifyInput()
    }

    fun resetCamera(fromUser: Boolean = true) {
        camX = SimConfig.width / 2.0
        camY = SimConfig.height / 2.0
        zoom = 1.0
        if (fromUser) notifyInput()
    }

    fun clampCamera(leftW: Float, winH: Float) {
        if (!camX.isFinite() || !camY.isFinite() || !zoom.isFinite()) {
            resetCamera(fromUser = false)
            return
        }
        zoom = zoom.coerceIn(1.0, 6.0)
        val fit = fitScale(leftW, winH)
        val s = fit * zoom
        if (s <= 0.0) return
        val halfW = (leftW / 2.0) / s
        val halfH = (winH / 2.0) / s
        val worldW = SimConfig.width.toDouble()
        val worldH = SimConfig.height.toDouble()
        camX = if (halfW * 2.0 >= worldW) worldW / 2.0 else camX.coerceIn(halfW, worldW - halfW)
        camY = if (halfH * 2.0 >= worldH) worldH / 2.0 else camY.coerceIn(halfH, worldH - halfH)
    }

    fun fitScale(leftW: Float, winH: Float): Double {
        val sx = leftW / SimConfig.width.toDouble()
        val sy = winH / SimConfig.height.toDouble()
        return if (sx < sy) sx else sy
    }

    fun screenToWorld(px: Float, py: Float, leftW: Float, winH: Float): Pair<Double, Double> {
        val s = fitScale(leftW, winH) * zoom
        val wx = camX + (px - leftW / 2.0) / s
        val wy = camY + (py - winH / 2.0) / s
        return wx to wy
    }

    fun clickWorld(wx: Double, wy: Double) {
        notifyInput()
        when (tool) {
            SimTool.SUGAR -> world.placeFood(wx, wy, FoodKind.SUGAR)
            SimTool.PROTEIN -> world.placeFood(wx, wy, FoodKind.PROTEIN)
            SimTool.ENEMY -> world.spawnEnemyAt(wx, wy, InsectKind.PREDATOR)
            SimTool.INSPECT -> inspectNearest(wx, wy)
        }
        bump()
    }

    fun toggleRain() {
        if (world.raining) {
            world.raining = false
            world.rainTimer = 0
        } else {
            world.raining = true
            world.rainTimer = SimConfig.environment.rainMinDuration * 2
        }
        notifyInput()
        bump()
    }

    fun restart(fromUser: Boolean) {
        world = World()
        worldSerial++
        follow = null
        userFollow = false
        inspectedAnt = null
        cineNextPick = 0.0
        deadFrames = 0
        history.clear()
        inspect.value = ""
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
        PerformanceManager.setQuality(level)
        renderTierBias = 0
        save()
        notifyInput()
        bump()
    }

    fun setPheromones(on: Boolean) {
        showPheromones = on
        save()
        bump()
    }

    fun updatePheromoneStrength(value: Double) {
        pheromoneStrength = value.coerceIn(0.0, 1.5)
        save()
        bump()
    }

    fun updateDayNight(on: Boolean) {
        dayNight = on
        save()
        bump()
    }

    fun updateDayNightStrength(value: Double) {
        dayNightStrength = value.coerceIn(0.0, 1.0)
        save()
        bump()
    }

    fun updateCinema(on: Boolean) {
        cinema = on
        save()
        bump()
    }

    fun setAntColor(hex: String) {
        try {
            antColor = Color(hex)
            save()
            bump()
        } catch (_: Throwable) {
            MyLog.add("APP", "Bad ant color $hex", LogLevel.DEBUG)
        }
    }

    fun refreshAnalyse() {
        analyseLines.clear()
        val suggestions = world.observer.analyze()
        if (suggestions.isEmpty()) {
            analyseLines += AnalyseLine("(no suggestions)", null, 0.0)
        }
        for (s in suggestions) {
            analyseLines += AnalyseLine("${s.severity} | ${s.metric}: ${s.suggestion}", null, 0.0)
            analyseLines += AnalyseLine(s.effect, null, 0.0)
            for (a in s.actions) {
                analyseLines += AnalyseLine(a.label, a.path, a.to)
            }
        }
        showGrafik = false
        showParams = false
        showStats = false
        showAnalyse = true
        bump()
    }

    fun applyAnalyse(path: String, to: Double) {
        setTunable(path, to)
    }

    fun setTunable(path: String, value: Double) {
        try {
            SimConfig.setNumber(path, value)
            overrides[path] = value
            save()
        } catch (_: Throwable) {
            MyLog.add("APP", "Tuner path skipped: $path", LogLevel.DEBUG)
        }
        bump()
    }

    fun resetTunables() {
        overrides.clear()
        SimConfig.resetTunables()
        save()
        bump()
    }

    fun requestScreenshot() {
        screenshotRequested = true
    }

    fun chooseRenderer(mode: RenderMode) {
        renderer = mode
        applyRendererVisibility()
        save()
        bump()
    }

    fun choosePitch(deg: Float) {
        cameraPitchDeg = deg.coerceIn(-90f, -60f)
        save()
    }

    fun chooseBloom(on: Boolean) {
        bloomUser = on
        save()
        bump()
    }

    fun chooseSsao(on: Boolean) {
        ssaoUser = on
        save()
        bump()
    }

    fun chooseWeather(on: Boolean) {
        weatherUser = on
        save()
        bump()
    }

    fun chooseShadows(on: Boolean) {
        shadowsUser = on
        save()
        bump()
    }

    fun chooseGrassDensity(value: Float) {
        grassDensity = value.coerceIn(0f, 1.5f)
        save()
    }

    fun toggleUserFollow() {
        userFollow = !userFollow
        if (!userFollow) follow = null
        notifyInput()
        bump()
    }

    fun noteDrag() {
        userFollow = false
        notifyInput()
    }

    fun speedLabel(): String {
        val s = speeds[speedIndex]
        return if (s == 0.5) "0.5x" else "${s.toInt()}x"
    }

    private fun recordSample() {
        val ants = world.ants
        val avg = if (ants.isEmpty()) 0.0 else ants.sumOf { it.energy } / ants.size
        history.addLast(
            GraphSample(
                population = ants.size,
                sugar = world.sugarStockpile,
                protein = world.proteinStockpile,
                energyPct = avg / SimConfig.antMaxEnergy,
            ),
        )
        while (history.size > 180) history.removeFirst()
    }

    private fun syncHud() {
        hudFps.value = "FPS: $fpsShown"
        if (world.colonies.size > 1) {
            val split = world.colonies.joinToString(" vs ") { it.ants.size.toString() }
            hudPop.value = "Population: ${world.totalAntCount()} ($split)"
        } else {
            val ants = world.colonies[0].ants
            val workers = ants.count { it.type == AntKind.WORKER }
            val soldiers = ants.count { it.type == AntKind.SOLDIER }
            hudPop.value = "Population: ${ants.size} (W:$workers S:$soldiers)"
        }
        hudFood.value = "Protein: ${floor(world.proteinStockpile).toInt()} | Sugar: ${floor(world.sugarStockpile).toInt()}"
        val ageSeconds = world.queen.age / 60
        hudQueen.value = "Queen Age: ${ageSeconds / 60}m ${ageSeconds % 60}s"
    }

    private fun inspectNearest(wx: Double, wy: Double) {
        var best = Double.MAX_VALUE
        var text = ""
        inspectedAnt = null
        fun consider(x: Double, y: Double, label: String, ant: Ant? = null) {
            val dx = x - wx
            val dy = y - wy
            val d = dx * dx + dy * dy
            if (d < best && d <= 30.0 * 30.0) {
                best = d
                text = label
                inspectedAnt = ant
            }
        }
        for (colony in world.colonies) {
            for (ant in colony.ants) {
                if (ant.location != AntPlace.WORLD) continue
                consider(ant.x, ant.y, antLine(ant), ant)
            }
        }
        for (food in world.foods) {
            consider(food.x, food.y, "${food.type} ${food.amount.toInt()}")
        }
        for (insect in world.insects) {
            consider(insect.x, insect.y, "${insect.type} hp ${insect.health.toInt()}")
        }
        inspect.value = text
    }

    /** Hit test for the 3D nest cutaway. Classic 2D still uses [clickWorld] only. */
    fun inspectNest(nx: Double, ny: Double) {
        notifyInput()
        var best = Double.MAX_VALUE
        var text = ""
        inspectedAnt = null
        val colony = world.colonies[0]
        for (ant in colony.ants) {
            if (ant.location != AntPlace.NEST) continue
            val dx = ant.x - nx
            val dy = ant.y - ny
            val d = dx * dx + dy * dy
            if (d < best && d <= 30.0 * 30.0) {
                best = d
                text = antLine(ant)
                inspectedAnt = ant
            }
        }
        val queen = colony.queen
        val qdx = queen.x - nx
        val qdy = queen.y - ny
        val qd = qdx * qdx + qdy * qdy
        if (qd < best && qd <= 40.0 * 40.0) {
            text = "Queen e=${queen.energy.toInt()} age=${queen.age}"
            inspectedAnt = null
        }
        inspect.value = text
        bump()
    }

    private fun antLine(ant: Ant): String {
        return "Ant ${ant.type} ${ant.state} e=${ant.energy.toInt()} h=${ant.health.toInt()} ${ant.carrying}"
    }

    private fun updateUserFollow() {
        if (!userFollow) return
        val ant = inspectedAnt ?: return
        if (ant.location != AntPlace.WORLD || ant.energy <= 0.0) {
            userFollow = false
            return
        }
        val k = 0.12
        camX += (ant.x - camX) * k
        camY += (ant.y - camY) * k
    }

    private fun updateCinema() {
        if (userFollow) return
        if (!cinema) return
        val now = Time.gameTime
        if (now < cinePausedUntil) return
        val ants = ArrayList<Ant>()
        for (colony in world.colonies) {
            for (ant in colony.ants) {
                if (ant.location == AntPlace.WORLD) ants += ant
            }
        }
        val followed = follow
        val gone = followed != null && (followed.location != AntPlace.WORLD || followed.energy <= 0.0)
        if (cineNextPick == 0.0 || now >= cineNextPick || gone) pickShot(ants, now)
        follow?.let {
            cineTargetX = it.x
            cineTargetY = it.y
        }
        cinePhase += 0.004
        val amp = if (follow != null) 2.0 else 7.0
        val dx = cos(cinePhase) * amp
        val dy = sin(cinePhase * 0.7) * amp * 0.7
        val k = if (follow != null) 0.08 else 0.014
        zoom += (cineTargetZoom - zoom) * k
        camX += (cineTargetX + dx - camX) * k
        camY += (cineTargetY + dy - camY) * k
    }

    private fun pickShot(ants: List<Ant>, now: Double) {
        val events = ants.filter {
            it.state == AntState.RAIDING || it.state == AntState.ATTACKING ||
                it.state == AntState.FLEEING || it.state == AntState.MILKING
        }
        val kind = nextKind(events.isNotEmpty(), ants.isNotEmpty())
        follow = null
        var hold: Double
        when (kind) {
            "ENTRANCE" -> {
                val colony = world.colonies[cineRandom.nextInt(world.colonies.size)]
                cineTargetX = colony.entranceWorld.x
                cineTargetY = colony.entranceWorld.y
                cineTargetZoom = 2.1 + cineRandom.nextDouble() * 0.4
                hold = 8.0 + cineRandom.nextDouble() * 3.0
            }
            "EVENT" -> {
                val ant = events[cineRandom.nextInt(events.size)]
                follow = ant
                cineTargetX = ant.x
                cineTargetY = ant.y
                cineTargetZoom = if (ant.state == AntState.RAIDING) 2.6 else if (ant.state == AntState.MILKING) 3.2 else 3.0
                hold = 7.0 + cineRandom.nextDouble() * 4.0
            }
            "FOLLOW" -> {
                val carriers = ants.filter { it.carrying != com.neojou.myants.sim.CarryKind.NONE }
                val pool = when {
                    events.isNotEmpty() -> events
                    carriers.isNotEmpty() -> carriers
                    else -> ants
                }
                val ant = pickWithRunway(pool)
                follow = ant
                cineTargetX = ant.x
                cineTargetY = ant.y
                cineTargetZoom = 2.8 + cineRandom.nextDouble() * 0.4
                hold = 8.0 + cineRandom.nextDouble() * 4.0
            }
            else -> {
                var cx = SimConfig.width / 2.0
                var cy = SimConfig.height / 2.0
                if (ants.isNotEmpty()) {
                    cx = ants.sumOf { it.x } / ants.size
                    cy = ants.sumOf { it.y } / ants.size
                }
                cineTargetX = cx
                cineTargetY = cy
                cineTargetZoom = 1.05 + cineRandom.nextDouble() * 0.25
                hold = 11.0 + cineRandom.nextDouble() * 4.0
            }
        }
        cineNextPick = now + hold
    }

    private fun nextKind(hasEvents: Boolean, hasAnts: Boolean): String {
        if (shotQueue.isEmpty()) {
            shotQueue.addAll(listOf("FOLLOW", "ENTRANCE", "FOLLOW", "WIDE", "EVENT"))
        }
        val iter = shotQueue.iterator()
        while (iter.hasNext()) {
            val kind = iter.next()
            if (kind == "EVENT" && !hasEvents) continue
            if (kind == "FOLLOW" && !hasAnts) continue
            iter.remove()
            return kind
        }
        return "WIDE"
    }

    private fun pickWithRunway(pool: List<Ant>): Ant {
        var best = pool[cineRandom.nextInt(pool.size)]
        var bestD = -1.0
        repeat(5) {
            val ant = pool[cineRandom.nextInt(pool.size)]
            val e = ant.colony.entranceWorld
            val dx = ant.x - e.x
            val dy = ant.y - e.y
            val d = dx * dx + dy * dy
            if (d > bestD) {
                bestD = d
                best = ant
            }
        }
        return best
    }

    private fun save() {
        val text = buildString {
            append("speed=").append(speeds[speedIndex]).append('\n')
            append("colonies=").append(SimConfig.colonyCount).append('\n')
            append("quality=").append(PerformanceManager.level.name).append('\n')
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
        settings.write(text)
    }

    private fun loadSettings(): Map<String, String> {
        val text = settings.read() ?: return emptyMap()
        val map = HashMap<String, String>()
        for (line in text.lineSequence()) {
            val i = line.indexOf('=')
            if (i <= 0) continue
            map[line.substring(0, i).trim()] = line.substring(i + 1).trim()
        }
        return map
    }

    private fun hex(color: Color): String {
        fun ch(v: Float): String {
            val n = (v * 255f).roundToInt().coerceIn(0, 255)
            return n.toString(16).padStart(2, '0')
        }
        return "#${ch(color.r)}${ch(color.g)}${ch(color.b)}"
    }
}
