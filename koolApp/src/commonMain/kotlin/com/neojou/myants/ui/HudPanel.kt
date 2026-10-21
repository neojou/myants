package com.neojou.myants.ui

import com.neojou.myants.AppSession
import com.neojou.myants.AppVersion
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.scene.Scene

private val T = MyantsTheme.colors

/** Top-left status HUD (fps, population, food, queen, version, inspect). */
internal fun Scene.addHudPanel(session: AppSession) = addPanelSurface(
    backgroundColor = { T.surface },
    width = FitContent,
    height = FitContent,
) {
    val inspect = session.inspect.use()
    modifier
        .align(AlignmentX.Start, AlignmentY.Top)
        .margin(MyantsTheme.space.screenEdge)
        .padding(MyantsTheme.space.s2)
        .background(RoundRectBackground(T.surface, MyantsTheme.radius.lg))
        .border(RoundRectBorder(T.outline, MyantsTheme.radius.lg, MyantsTheme.stroke.selected))
        .onPointer { it.pointer.consume() }
    Text(session.hudFps.use()) { }
    Text(session.hudPop.use()) { }
    Text(session.hudFood.use()) { }
    Text(session.hudQueen.use()) { }
    Text("v${AppVersion.NAME}") { modifier.font(sizes.smallText) }
    if (inspect.isNotEmpty()) {
        Text(inspect) { modifier.margin(top = MyantsTheme.space.s2) }
    }
}.also { it.applyControlRadius() }
