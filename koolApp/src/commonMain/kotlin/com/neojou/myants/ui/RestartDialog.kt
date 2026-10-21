package com.neojou.myants.ui

import com.neojou.myants.AppSession
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.scene.Scene

private val T = MyantsTheme.colors

/** Asks before [AppSession.confirmRestart]. Cancel calls [AppSession.dismissRestart]. */
internal fun Scene.addRestartDialog(session: AppSession) = addPanelSurface(
    backgroundColor = { T.surfaceRaised },
    width = FitContent,
    height = FitContent,
) {
    installLanguageFont()
    modifier
        .size(420.dp, 220.dp)
        .align(AlignmentX.Center, AlignmentY.Center)
        .padding(MyantsTheme.space.s6)
        .background(RoundRectBackground(T.surfaceRaised, MyantsTheme.radius.lg))
        .border(RoundRectBorder(T.outline, MyantsTheme.radius.lg, MyantsTheme.stroke.selected))
        .onPointer { it.pointer.consume() }
    Text(tr(UiText.RESTART_TITLE)) {
        modifier.alignX(AlignmentX.Center).margin(top = MyantsTheme.space.s3).font(sizes.largeText.derive(MyantsTheme.text.lg))
    }
    Text(tr(UiText.RESTART_BODY)) {
        modifier.alignX(AlignmentX.Center).margin(top = MyantsTheme.space.s3)
    }
    Row {
        modifier.alignX(AlignmentX.Center).margin(top = MyantsTheme.space.s6)
        Button(tr(UiText.CANCEL)) { modifier.onClick { session.dismissRestart() } }
        Button(tr(UiText.RESTART)) {
            modifier
                .margin(start = MyantsTheme.space.gapGroup)
                .colors(
                    buttonColor = T.danger,
                    buttonHoverColor = T.danger,
                    textColor = T.onDanger,
                    textHoverColor = T.onDanger,
                )
                .onClick { session.confirmRestart() }
        }
    }
}.also { it.applyControlRadius() }
