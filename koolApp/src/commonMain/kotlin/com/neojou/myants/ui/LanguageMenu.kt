package com.neojou.myants.ui

import com.neojou.myants.AppSession
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.util.Color

private val T = MyantsTheme.colors

/**
 * Click catcher for the language menu. It does not dim the world.
 * A second click on the globe hits this surface and closes the menu.
 */
internal fun Scene.addLanguageScrim(session: AppSession) = addPanelSurface(
    backgroundColor = { Color.BLACK.withAlpha(0f) },
) {
    modifier.onClick { session.closeLanguageMenu() }
}

/**
 * Endonyms under the globe. The panel's right edge lines up with the toolbar.
 * Top margin is the toolbar's screen edge, padding, button, and one group gap.
 */
internal fun Scene.addLanguageMenu(session: AppSession) = addPanelSurface(
    backgroundColor = { T.surfaceRaised },
    width = FitContent,
    height = FitContent,
) {
    installLanguageFont(forceCjk = true)
    val lang = L10n.language.use()
    modifier
        .align(AlignmentX.End, AlignmentY.Top)
        .margin(top = Dp(54f), end = MyantsTheme.space.screenEdge)
        .padding(MyantsTheme.space.s2)
        .background(RoundRectBackground(T.surfaceRaised, MyantsTheme.radius.lg))
        .border(RoundRectBorder(T.outline, MyantsTheme.radius.lg, MyantsTheme.stroke.selected))
        .onPointer { it.pointer.consume() }
    Column {
        UiLanguage.entries.forEachIndexed { index, choice ->
            Button(choice.endonym) {
                modifier
                    .width(Grow.Std)
                    .margin(top = if (index == 0) Dp(0f) else MyantsTheme.space.gapInGroup)
                    .onClick { session.setLanguage(choice) }
                if (lang == choice) {
                    modifier.colors(
                        buttonColor = T.primary,
                        buttonHoverColor = T.primary,
                        textColor = T.primaryContainer,
                        textHoverColor = T.primaryContainer,
                    )
                }
            }
        }
    }
}.also { it.applyControlRadius() }
