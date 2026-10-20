package com.neojou.myants

import com.neojou.myants.render3d.RenderMode
import com.neojou.myants.render3d.bloomAllowed
import com.neojou.myants.sim.PerformanceManager
import com.neojou.myants.sim.QualityLevel
import com.neojou.myants.sim.SimConfig
import com.neojou.myants.ui.HudIcon
import com.neojou.myants.ui.HudIcons
import com.neojou.myants.ui.IconButton
import com.neojou.myants.ui.MyantsTheme
import de.fabmax.kool.KoolApplication
import de.fabmax.kool.addUiScene
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.pipeline.BufferedImageData2d
import de.fabmax.kool.pipeline.MipMapping
import de.fabmax.kool.pipeline.SamplerSettings
import de.fabmax.kool.pipeline.TexFormat
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.Uint8Buffer
import kotlin.math.roundToInt

private data class SliderSpec(val path: String, val label: String, val min: Double, val max: Double, val step: Double)

private val SLIDERS = listOf(
    SliderSpec("antSpeed", "antSpeed", 0.5, 6.0, 0.1),
    SliderSpec("antEnergyDecay", "antEnergyDecay", 0.05, 1.0, 0.01),
    SliderSpec("queenLayInterval", "queenLayInterval (Pop)", 50.0, 800.0, 10.0),
    SliderSpec("ant.lifespan", "ant.lifespan (Pop)", 6000.0, 80000.0, 2000.0),
    SliderSpec("eggCost", "eggCost", 5.0, 60.0, 1.0),
    SliderSpec("sugarValue", "sugarValue", 1.0, 40.0, 1.0),
    SliderSpec("proteinValue", "proteinValue", 1.0, 30.0, 1.0),
    SliderSpec("sugarEnergyValue", "sugarEnergyValue", 20.0, 300.0, 10.0),
    SliderSpec("colonyUpkeep", "colonyUpkeep", 0.0, 0.01, 0.0005),
    SliderSpec("broodProteinUpkeep", "broodProteinUpkeep", 0.0, 0.01, 0.0005),
    SliderSpec("pheromone.diffusionRate", "pheromone.diffusionRate", 0.0, 0.5, 0.01),
    SliderSpec("pheromone.decay", "pheromone.decay", 0.9, 0.999, 0.001),
    SliderSpec("pheromone.foodDecay", "pheromone.foodDecay (Straßen)", 0.9, 0.999, 0.001),
    SliderSpec("antSensorDist", "antSensorDist (Spur-Reichweite)", 10.0, 120.0, 5.0),
    SliderSpec("ant.dispersalRadius", "ant.dispersalRadius (Streuung)", 0.0, 600.0, 20.0),
    SliderSpec("ant.dispersalStrength", "ant.dispersalStrength", 0.0, 0.4, 0.02),
    SliderSpec("ant.memoryBias", "ant.memoryBias (Site Fidelity)", 0.0, 1.0, 0.05),
    SliderSpec("ant.proteinForagerShare", "ant.proteinForagerShare", 0.0, 0.8, 0.05),
    SliderSpec("broodProteinUpkeep", "broodProteinUpkeep (Pop-Limit)", 0.0, 0.005, 0.0005),
    SliderSpec("brood.soldierFoodThreshold", "brood.soldierFoodThreshold", 100.0, 2000.0, 100.0),
    SliderSpec("brood.soldierProteinLevel", "brood.soldierProteinLevel", 0.0, 500.0, 25.0),
    SliderSpec("sugarSourceCount", "sugarSourceCount", 1.0, 10.0, 1.0),
    SliderSpec("predatorSpawnRate", "predatorSpawnRate", 0.0, 0.005, 0.0001),
)

private val ANT_COLORS = MyantsTheme.Data.antColors
private val T = MyantsTheme.colors

fun KoolApplication.addAntSimUi(session: AppSession) = addUiScene {
    session.uiScene = this
    HudIcons.loadAll(coroutineScope)
    val hud = addPanelSurface(
        backgroundColor = { T.surface },
        width = FitContent,
        height = FitContent,
    ) {
        session.revision.use()
        modifier
            .align(AlignmentX.Start, AlignmentY.Top)
            .margin(MyantsTheme.space.screenEdge)
            .padding(MyantsTheme.space.s2)
            .onPointer { it.pointer.consume() }
        Text(session.hudFps.use()) { }
        Text(session.hudPop.use()) { }
        Text(session.hudFood.use()) { }
        Text(session.hudQueen.use()) { }
        Text("v${AppVersion.NAME}") { modifier.font(sizes.smallText) }
        if (session.inspect.value.isNotEmpty()) {
            Text(session.inspect.use()) { modifier.margin(top = MyantsTheme.space.s2) }
        }
    }
    addPanelSurface(
        backgroundColor = { T.surface },
        width = FitContent,
        height = FitContent,
    ) {
        session.revision.use()
        modifier
            .align(AlignmentX.End, AlignmentY.Top)
            .margin(MyantsTheme.space.screenEdge)
            .padding(MyantsTheme.space.s2)
            .onPointer { it.pointer.consume() }
        Row {
            Text("Speed") { modifier.alignY(AlignmentY.Center).margin(end = MyantsTheme.space.gapGroup) }
            IconButton(HudIcon.SLOWER) { session.stepSpeed(-1) }
            Text(session.speedLabel()) { modifier.alignY(AlignmentY.Center).margin(horizontal = MyantsTheme.space.gapInGroup) }
            IconButton(HudIcon.FASTER) { session.stepSpeed(1) }
            IconButton(if (session.paused) HudIcon.RESUME else HudIcon.PAUSE, startMargin = MyantsTheme.space.gapGroup) {
                session.togglePause()
            }
            IconButton(HudIcon.STEP, enabled = session.paused, startMargin = MyantsTheme.space.gapInGroup) { session.stepOnce() }
        }
        Row {
            modifier.margin(top = MyantsTheme.space.s2)
            Text("Zoom") { modifier.alignY(AlignmentY.Center).margin(end = MyantsTheme.space.gapGroup) }
            IconButton(HudIcon.ZOOM_IN) { session.zoomBy(1.25) }
            IconButton(HudIcon.ZOOM_OUT, startMargin = MyantsTheme.space.gapInGroup) { session.zoomBy(1.0 / 1.25) }
            IconButton(HudIcon.RESET_CAMERA, startMargin = MyantsTheme.space.gapInGroup) { session.resetCamera() }
            IconButton(HudIcon.SCREENSHOT, startMargin = MyantsTheme.space.gapGroup) { session.requestScreenshot() }
        }
        Row {
            modifier.margin(top = MyantsTheme.space.s2)
            toolButton(session, SimTool.INSPECT, HudIcon.INSPECT)
            toolButton(session, SimTool.SUGAR, HudIcon.SUGAR)
            toolButton(session, SimTool.PROTEIN, HudIcon.PROTEIN)
            toolButton(session, SimTool.ENEMY, HudIcon.ENEMY)
            IconButton(HudIcon.RAIN, selected = session.world.raining, startMargin = MyantsTheme.space.gapGroup) {
                session.toggleRain()
            }
        }
        Row {
            modifier.margin(top = MyantsTheme.space.s2)
            Button("Restart") { modifier.onClick { session.restart(fromUser = true) } }
            Checkbox(session.colonies > 1) {
                modifier.margin(start = sizes.gap).alignY(AlignmentY.Center).onToggle {
                    session.setColonies(if (it) 2 else 1)
                }
            }
            Text("Rivalenkolonie") { modifier.alignY(AlignmentY.Center).margin(start = MyantsTheme.space.s2) }
        }
        Row {
            modifier.margin(top = MyantsTheme.space.s2)
            Button("Grafik") { modifier.onClick { session.openPanel("grafik") } }
            Button("Params") { modifier.margin(start = MyantsTheme.space.s2).onClick { session.openPanel("params") } }
            Button("Stats") { modifier.margin(start = MyantsTheme.space.s2).onClick { session.openPanel("stats") } }
            Button("Analyse") { modifier.margin(start = MyantsTheme.space.s2).onClick { session.refreshAnalyse() } }
            Button("About") {
                modifier.margin(start = MyantsTheme.space.s2).onClick {
                    session.showAbout = true
                    session.bump()
                }
            }
        }
    }
    val grafikPanel = sidePanel(session, "Grafik") { grafik(session) }
    val paramsPanel = sidePanel(session, "Params") { params(session) }
    val statsPanel = sidePanel(session, "Verlauf") { stats(session) }
    val analysePanel = sidePanel(session, "Analyse") { analyse(session) }
    val aboutPanel = addPanelSurface(backgroundColor = { T.surfaceRaised }, width = FitContent, height = FitContent) {
        session.revision.use()
        modifier
            .size(420.dp, 220.dp)
            .align(AlignmentX.Center, AlignmentY.Center)
            .padding(MyantsTheme.space.s6)
            .onPointer { it.pointer.consume() }
        Text(AppVersion.APP_NAME) {
            modifier.alignX(AlignmentX.Center).margin(top = MyantsTheme.space.s3).font(sizes.largeText.derive(MyantsTheme.text.lg))
        }
        Text("Version ${AppVersion.NAME}") {
            modifier.alignX(AlignmentX.Center).margin(top = MyantsTheme.space.s2)
        }
        Text("Based on AntSim_V2 by nilsgollub (MIT)") {
            modifier.alignX(AlignmentX.Center).margin(top = MyantsTheme.space.s3)
        }
        Button("OK") {
            modifier.alignX(AlignmentX.Center).margin(top = MyantsTheme.space.s6).onClick {
                session.showAbout = false
                session.bump()
            }
        }
    }
    aboutPanel.isVisible = false
    onUpdate {
        grafikPanel.isVisible = session.showGrafik
        paramsPanel.isVisible = session.showParams
        statsPanel.isVisible = session.showStats
        analysePanel.isVisible = session.showAnalyse
        aboutPanel.isVisible = session.showAbout
    }
    hud.isVisible = true
}

private fun Scene.sidePanel(session: AppSession, title: String, block: UiScope.() -> Unit) =
    addPanelSurface(backgroundColor = { T.surfaceRaised }, width = FitContent, height = FitContent) {
        session.revision.use()
        modifier
            .width(360.dp)
            .align(AlignmentX.Start, AlignmentY.Bottom)
            .margin(MyantsTheme.space.screenEdge)
            .padding(MyantsTheme.space.panelPadding)
            .onPointer { it.pointer.consume() }
        Row {
            Text(title) { modifier.alignY(AlignmentY.Center).width(Grow.Std) }
            Button("x") {
                modifier.onClick {
                    session.showGrafik = false
                    session.showParams = false
                    session.showStats = false
                    session.showAnalyse = false
                    session.bump()
                }
            }
        }
        block()
    }

private fun AppSession.openPanel(name: String) {
    val was = when (name) {
        "grafik" -> showGrafik
        "params" -> showParams
        "stats" -> showStats
        else -> showAnalyse
    }
    showGrafik = name == "grafik" && !was
    showParams = name == "params" && !was
    showStats = name == "stats" && !was
    showAnalyse = name == "analyse" && !was
    notifyInput()
    bump()
}

private fun UiScope.toolButton(session: AppSession, tool: SimTool, icon: HudIcon) {
    val first = tool == SimTool.INSPECT
    IconButton(
        icon,
        selected = session.tool == tool,
        startMargin = if (first) Dp(0f) else MyantsTheme.space.gapInGroup,
    ) {
        session.tool = tool
        session.notifyInput()
        session.bump()
    }
}

private fun UiScope.grafik(session: AppSession) {
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

private fun UiScope.params(session: AppSession) {
    Button("Reset") {
        modifier.margin(top = MyantsTheme.space.s2).onClick { session.resetTunables() }
    }
    ScrollArea(height = 360.dp, withHorizontalScrollbar = false) {
        Column {
            for (spec in SLIDERS) {
                val value = SimConfig.getNumber(spec.path)
                Text("${spec.label} ${fmt(value)}") { modifier.margin(top = MyantsTheme.space.s2) }
                Slider(value.toFloat(), spec.min.toFloat(), spec.max.toFloat()) {
                    modifier.width(Grow.Std).onChange {
                        session.setTunable(spec.path, quantize(it.toDouble(), spec.min, spec.max, spec.step))
                    }
                }
            }
        }
    }
}

private fun UiScope.stats(session: AppSession) {
    StatsChart.paint(session.history)
    Image(StatsChart.tex) {
        modifier.margin(top = MyantsTheme.space.s2).imageSize(ImageSize.Stretch).size(280.dp, 130.dp)
    }
    Text("Pop  Zucker  Protein  Energie") { modifier.margin(top = MyantsTheme.space.s2) }
}

private fun UiScope.analyse(session: AppSession) {
    ScrollArea(height = 280.dp, withHorizontalScrollbar = false) {
        Column {
            if (session.analyseLines.isEmpty()) {
                Text("(no suggestions)") { }
            }
            for (line in session.analyseLines) {
                if (line.path == null) {
                    Text(line.text) { modifier.margin(top = MyantsTheme.space.s2) }
                } else {
                    Button(line.text) {
                        modifier.margin(top = MyantsTheme.space.s2).onClick {
                            session.applyAnalyse(line.path, line.to)
                        }
                    }
                }
            }
        }
    }
}

private fun fmt(value: Double): String {
    val scaled = (value * 10000.0).roundToInt() / 10000.0
    return scaled.toString()
}

private fun quantize(value: Double, min: Double, max: Double, step: Double): Double {
    if (step <= 0.0) return value.coerceIn(min, max)
    val steps = ((value - min) / step).roundToInt()
    return (min + steps * step).coerceIn(min, max)
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

/** Non-color cue for selected state (color-vision deficiency, #4): 1dp primaryContainer outline. */
private fun selectedBorder() = RoundRectBorder(T.primaryContainer, MyantsTheme.radius.md, MyantsTheme.stroke.selected)
