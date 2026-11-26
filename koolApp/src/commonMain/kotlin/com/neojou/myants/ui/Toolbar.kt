package com.neojou.myants.ui

import com.neojou.myants.toggleRain
import com.neojou.myants.toggleLanguageMenu
import com.neojou.myants.setColonies
import com.neojou.myants.requestScreenshot
import com.neojou.myants.refreshAnalyse
import com.neojou.myants.openPanel
import com.neojou.myants.askRestart
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
    installLanguageFont()
    val paused = session.pausedState.use()
    val speedLabel = session.speedLabelState.use()
    val tool = session.toolState.use()
    val raining = session.rainingState.use()
    val colonies = session.colonyCountState.use()
    val open = session.openPanelsState.use()
    modifier
        .align(AlignmentX.End, AlignmentY.Top)
        .margin(MyantsTheme.space.screenEdge)
        .padding(MyantsTheme.space.s2)
        .background(RoundRectBackground(T.surface, MyantsTheme.radius.lg))
        .border(RoundRectBorder(T.outline, MyantsTheme.radius.lg, MyantsTheme.stroke.selected))
        .onPointer { it.pointer.consume() }
    Row {
        modifier.width(Grow.Std)
        Text(tr(UiText.SPEED)) { modifier.alignY(AlignmentY.Center).margin(end = MyantsTheme.space.gapInGroup) }
        IconButton(HudIcon.SLOWER) { session.stepSpeed(-1) }
        Text(speedLabel) { modifier.alignY(AlignmentY.Center).margin(horizontal = MyantsTheme.space.gapInGroup) }
        IconButton(HudIcon.FASTER) { session.stepSpeed(1) }
        IconButton(if (paused) HudIcon.RESUME else HudIcon.PAUSE, startMargin = MyantsTheme.space.gapInGroup) {
            session.togglePause()
        }
        IconButton(HudIcon.STEP, enabled = paused, startMargin = MyantsTheme.space.gapInGroup) { session.stepOnce() }
        Box { modifier.width(Grow.Std).height(Dp(1f)) }
        IconButton(HudIcon.LANGUAGE, selected = session.languageMenu.use()) { session.toggleLanguageMenu() }
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
        IconButton(HudIcon.RAIN, selected = raining, startMargin = MyantsTheme.space.gapGroup) {
            session.toggleRain()
        }
    }
    Row {
        modifier.margin(top = MyantsTheme.space.gapGroup)
        TextButton(tr(UiText.RESTART), danger = true) { session.askRestart() }
        Checkbox(colonies > 1) {
            modifier.margin(start = MyantsTheme.space.gapInGroup).alignY(AlignmentY.Center).onToggle {
                session.setColonies(if (it) 2 else 1)
            }
        }
        Text(tr(UiText.RIVAL_COLONY)) { modifier.alignY(AlignmentY.Center).margin(start = MyantsTheme.space.s2) }
    }
    Row {
        modifier.margin(top = MyantsTheme.space.gapGroup)
        panelButton(UiText.GRAFIK, open, Panel.GRAFIK, first = true) { session.openPanel(Panel.GRAFIK) }
        panelButton(UiText.PARAMS, open, Panel.PARAMS) { session.openPanel(Panel.PARAMS) }
        panelButton(UiText.STATS, open, Panel.STATS) { session.openPanel(Panel.STATS) }
        panelButton(UiText.ANALYSE, open, Panel.ANALYSE) { session.refreshAnalyse() }
        panelButton(UiText.ABOUT, open, Panel.ABOUT) { session.openPanel(Panel.ABOUT) }
    }
}.also { it.applyControlRadius() }

/** Panel button: selected while its panel is open (#87). [open] is [AppSession.openPanelsState]. */
private fun UiScope.panelButton(label: UiText, open: Int, panel: Panel, first: Boolean = false, onClick: () -> Unit) {
    TextButton(
        tr(label),
        selected = open and (1 shl panel.ordinal) != 0,
        layout = { if (!first) modifier.margin(start = MyantsTheme.space.gapInGroup) },
        onClick = onClick,
    )
}

private fun UiScope.toolButton(session: AppSession, current: SimTool, tool: SimTool, icon: HudIcon) {
    val first = tool == SimTool.INSPECT
    IconButton(
        icon,
        selected = current == tool,
        startMargin = if (first) Dp(0f) else MyantsTheme.space.gapInGroup,
    ) {
        session.selectTool(tool)
    }
}
