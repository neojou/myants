package com.neojou.myants.ui

import com.neojou.myants.AppVersion
import com.neojou.myants.AppSession
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.scene.Scene

private val T = MyantsTheme.colors

/** Centered About dialog (name, version, attribution). */
internal fun Scene.addAboutDialog(session: AppSession) = addPanelSurface(backgroundColor = { T.surfaceRaised }, width = FitContent, height = FitContent) {
    modifier
        .size(420.dp, 220.dp)
        .align(AlignmentX.Center, AlignmentY.Center)
        .padding(MyantsTheme.space.s6)
        .background(RoundRectBackground(T.surfaceRaised, MyantsTheme.radius.lg))
        .border(RoundRectBorder(T.outline, MyantsTheme.radius.lg, MyantsTheme.stroke.selected))
        .onPointer { it.pointer.consume() }
    Text(AppVersion.APP_NAME) {
        modifier.alignX(AlignmentX.Center).margin(top = MyantsTheme.space.s3).font(sizes.largeText.derive(MyantsTheme.text.lg))
    }
    Text("${tr(UiText.VERSION)} ${AppVersion.NAME}") {
        modifier.alignX(AlignmentX.Center).margin(top = MyantsTheme.space.s2)
    }
    Text("Based on AntSim_V2 by nilsgollub (MIT)") {
        modifier.alignX(AlignmentX.Center).margin(top = MyantsTheme.space.s3)
    }
    Button(tr(UiText.OK)) {
        modifier.alignX(AlignmentX.Center).margin(top = MyantsTheme.space.s6).onClick {
            session.showAbout = false
            session.bump()
        }
    }
}.also { it.applyControlRadius() }
