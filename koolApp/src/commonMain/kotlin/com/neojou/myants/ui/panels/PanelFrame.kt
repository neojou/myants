package com.neojou.myants.ui.panels

import com.neojou.myants.AppSession
import com.neojou.myants.ui.MyantsTheme
import com.neojou.myants.ui.applyControlRadius
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.scene.Scene

private val T = MyantsTheme.colors

/** Shared side-panel frame: title row with `x` close, Theme tokens. */
internal fun Scene.sidePanel(session: AppSession, title: String, block: UiScope.() -> Unit) =
    addPanelSurface(backgroundColor = { T.surfaceRaised }, width = FitContent, height = FitContent) {
        session.revision.use() // side panels still share one revision (#9)
        modifier
            .width(360.dp)
            .align(AlignmentX.Start, AlignmentY.Bottom)
            .margin(MyantsTheme.space.screenEdge)
            .padding(MyantsTheme.space.panelPadding)
            .background(RoundRectBackground(T.surfaceRaised, MyantsTheme.radius.lg))
            .border(RoundRectBorder(T.outline, MyantsTheme.radius.lg, MyantsTheme.stroke.selected))
            .onPointer { it.pointer.consume() }
        Row {
            Text(title) { modifier.alignY(AlignmentY.Center).width(Grow.Std) }
            Button("x") {
                modifier.onClick {
                    session.closeSidePanels()
                }
            }
        }
        block()
    }.also { it.applyControlRadius() }
