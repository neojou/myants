package com.neojou.myants.ui

import com.neojou.myants.SimTool
import com.neojou.myants.session.Panel
import com.neojou.myants.AppSession
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.scene.Scene

private val T = MyantsTheme.colors

/** Top-right toolbar: speed, zoom, tools, restart, panel buttons. */
internal fun Scene.addToolbar(session: AppSession) = addPanelSurface(
    backgroundColor = { T.surface },
    width = FitContent,
    height = FitContent,
) {
    val paused = session.pausedState.use()
    val speedLabel = session.speedLabelState.use()
    val tool = session.toolState.use()
    val raining = session.rainingState.use()
    val colonies = session.colonyCountState.use()
    modifier
        .align(AlignmentX.End, AlignmentY.Top)
        .margin(MyantsTheme.space.screenEdge)
        .padding(MyantsTheme.space.s2)
        .onPointer { it.pointer.consume() }
    Row {
        Text("Speed") { modifier.alignY(AlignmentY.Center).margin(end = MyantsTheme.space.gapGroup) }
        IconButton(HudIcon.SLOWER) { session.stepSpeed(-1) }
        Text(speedLabel) { modifier.alignY(AlignmentY.Center).margin(horizontal = MyantsTheme.space.gapInGroup) }
        IconButton(HudIcon.FASTER) { session.stepSpeed(1) }
        IconButton(if (paused) HudIcon.RESUME else HudIcon.PAUSE, startMargin = MyantsTheme.space.gapGroup) {
            session.togglePause()
        }
        IconButton(HudIcon.STEP, enabled = paused, startMargin = MyantsTheme.space.gapInGroup) { session.stepOnce() }
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
        toolButton(session, tool, SimTool.INSPECT, HudIcon.INSPECT)
        toolButton(session, tool, SimTool.SUGAR, HudIcon.SUGAR)
        toolButton(session, tool, SimTool.PROTEIN, HudIcon.PROTEIN)
        toolButton(session, tool, SimTool.ENEMY, HudIcon.ENEMY)
        IconButton(HudIcon.RAIN, selected = raining, startMargin = MyantsTheme.space.gapGroup) {
            session.toggleRain()
        }
    }
    Row {
        modifier.margin(top = MyantsTheme.space.s2)
        Button("Restart") { modifier.onClick { session.restart(fromUser = true) } }
        Checkbox(colonies > 1) {
            modifier.margin(start = sizes.gap).alignY(AlignmentY.Center).onToggle {
                session.setColonies(if (it) 2 else 1)
            }
        }
        Text("Rivalenkolonie") { modifier.alignY(AlignmentY.Center).margin(start = MyantsTheme.space.s2) }
    }
    Row {
        modifier.margin(top = MyantsTheme.space.s2)
        Button("Grafik") { modifier.onClick { session.openPanel(Panel.GRAFIK) } }
        Button("Params") { modifier.margin(start = MyantsTheme.space.s2).onClick { session.openPanel(Panel.PARAMS) } }
        Button("Stats") { modifier.margin(start = MyantsTheme.space.s2).onClick { session.openPanel(Panel.STATS) } }
        Button("Analyse") { modifier.margin(start = MyantsTheme.space.s2).onClick { session.refreshAnalyse() } }
        Button("About") {
            modifier.margin(start = MyantsTheme.space.s2).onClick {
                session.showAbout = true
                session.bump()
            }
        }
    }
}

private fun UiScope.toolButton(session: AppSession, current: SimTool, tool: SimTool, icon: HudIcon) {
    val first = tool == SimTool.INSPECT
    IconButton(
        icon,
        selected = current == tool,
        startMargin = if (first) Dp(0f) else MyantsTheme.space.gapInGroup,
    ) {
        session.tool = tool
        session.notifyInput()
        session.publishChrome()
    }
}
