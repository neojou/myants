package com.neojou.myants.ui

import com.neojou.myants.SimTool
import com.neojou.myants.session.Panel
import com.neojou.myants.AppSession
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.scene.Scene

private val T = MyantsTheme.colors

/**
 * Top-right toolbar in five groups: playback, camera, tools, world, panels.
 * [MyantsTheme.space.gapGroup] separates groups. [MyantsTheme.space.gapInGroup] separates controls inside a group.
 */
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
        .background(RoundRectBackground(T.surface, MyantsTheme.radius.lg))
        .border(RoundRectBorder(T.outline, MyantsTheme.radius.lg, MyantsTheme.stroke.selected))
        .onPointer { it.pointer.consume() }
    Row {
        Text(tr(UiText.SPEED)) { modifier.alignY(AlignmentY.Center).margin(end = MyantsTheme.space.gapInGroup) }
        IconButton(HudIcon.SLOWER) { session.stepSpeed(-1) }
        Text(speedLabel) { modifier.alignY(AlignmentY.Center).margin(horizontal = MyantsTheme.space.gapInGroup) }
        IconButton(HudIcon.FASTER) { session.stepSpeed(1) }
        IconButton(if (paused) HudIcon.RESUME else HudIcon.PAUSE, startMargin = MyantsTheme.space.gapInGroup) {
            session.togglePause()
        }
        IconButton(HudIcon.STEP, enabled = paused, startMargin = MyantsTheme.space.gapInGroup) { session.stepOnce() }
    }
    Row {
        modifier.margin(top = MyantsTheme.space.gapGroup)
        Text(tr(UiText.ZOOM)) { modifier.alignY(AlignmentY.Center).margin(end = MyantsTheme.space.gapInGroup) }
        IconButton(HudIcon.ZOOM_IN) { session.zoomBy(1.25) }
        IconButton(HudIcon.ZOOM_OUT, startMargin = MyantsTheme.space.gapInGroup) { session.zoomBy(1.0 / 1.25) }
        IconButton(HudIcon.RESET_CAMERA, startMargin = MyantsTheme.space.gapInGroup) { session.resetCamera() }
        IconButton(HudIcon.SCREENSHOT, startMargin = MyantsTheme.space.gapInGroup) { session.requestScreenshot() }
    }
    Row {
        modifier.margin(top = MyantsTheme.space.gapGroup)
        toolButton(session, tool, SimTool.INSPECT, HudIcon.INSPECT)
        toolButton(session, tool, SimTool.SUGAR, HudIcon.SUGAR)
        toolButton(session, tool, SimTool.PROTEIN, HudIcon.PROTEIN)
        toolButton(session, tool, SimTool.ENEMY, HudIcon.ENEMY)
    }
    Row {
        modifier.margin(top = MyantsTheme.space.gapGroup)
        IconButton(HudIcon.RAIN, selected = raining) { session.toggleRain() }
        Button(tr(UiText.RESTART)) {
            modifier
                .margin(start = MyantsTheme.space.gapInGroup)
                .colors(
                    buttonColor = T.danger,
                    buttonHoverColor = T.danger,
                    textColor = T.onDanger,
                    textHoverColor = T.onDanger,
                )
                .onClick { session.askRestart() }
        }
        Checkbox(colonies > 1) {
            modifier.margin(start = MyantsTheme.space.gapInGroup).alignY(AlignmentY.Center).onToggle {
                session.setColonies(if (it) 2 else 1)
            }
        }
        Text("Rivalenkolonie") { modifier.alignY(AlignmentY.Center).margin(start = MyantsTheme.space.s2) }
    }
    Row {
        modifier.margin(top = MyantsTheme.space.gapGroup)
        Button(tr(UiText.GRAFIK)) { modifier.onClick { session.openPanel(Panel.GRAFIK) } }
        Button(tr(UiText.PARAMS)) {
            modifier.margin(start = MyantsTheme.space.gapInGroup).onClick { session.openPanel(Panel.PARAMS) }
        }
        Button(tr(UiText.STATS)) {
            modifier.margin(start = MyantsTheme.space.gapInGroup).onClick { session.openPanel(Panel.STATS) }
        }
        Button(tr(UiText.ANALYSE)) {
            modifier.margin(start = MyantsTheme.space.gapInGroup).onClick { session.refreshAnalyse() }
        }
        Button(tr(UiText.ABOUT)) {
            modifier.margin(start = MyantsTheme.space.gapInGroup).onClick { session.openPanel(Panel.ABOUT) }
        }
    }
}.also { it.applyControlRadius() }

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
