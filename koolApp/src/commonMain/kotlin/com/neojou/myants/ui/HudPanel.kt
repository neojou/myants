package com.neojou.myants.ui

import com.neojou.myants.AppSession
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.scene.Scene

private val T = MyantsTheme.colors

/** Top-left status HUD (fps, population, food, queen) The inspect card floats separately ([addInspectCard]). The version stays in About. */
internal fun Scene.addHudPanel(session: AppSession) = addPanelSurface(
    backgroundColor = { T.surface },
    width = FitContent,
    height = FitContent,
) {
    installLanguageFont()
    modifier
        .align(AlignmentX.Start, AlignmentY.Top)
        .margin(MyantsTheme.space.screenEdge)
        .padding(MyantsTheme.space.s2)
        .background(RoundRectBackground(T.surface, MyantsTheme.radius.lg))
        .border(RoundRectBorder(T.outline, MyantsTheme.radius.lg, MyantsTheme.stroke.selected))
        .onPointer { it.pointer.consume() }
    val hud = session.hud
    Row {
        Text("${tr(UiText.HUD_FPS)}: ") { }
        hudDigits(hud.fpsDigits.use(), HudSlots.FPS)
    }
    if (hud.popMany.use()) {
        Text(hud.pop.use()) {
            modifier.width(hudNumberWidth(HudSlots.MANY)).textAlignX(AlignmentX.End)
        }
    } else if (hud.popSplit.use()) {
        Row {
            Text("${tr(UiText.POPULATION)}: ") { }
            hudDigits(hud.popDigits.use(), HudSlots.POP)
            Text(" (") { }
            hudDigits(hud.colonyA.use(), HudSlots.POP)
            Text(" ${tr(UiText.VERSUS)} ") { }
            hudDigits(hud.colonyB.use(), HudSlots.POP)
            Text(")") { }
        }
    } else {
        Row {
            Text("${tr(UiText.POPULATION)}: ") { }
            hudDigits(hud.popDigits.use(), HudSlots.POP)
            Text(" (${tr(UiText.WORKERS)}:") { }
            hudDigits(hud.workers.use(), HudSlots.POP)
            Text(" ${tr(UiText.SOLDIERS)}:") { }
            hudDigits(hud.soldiers.use(), HudSlots.POP)
            Text(")") { }
        }
    }
    Row {
        Text("${tr(UiText.PROTEIN)}: ") { }
        hudDigits(hud.proteinDigits.use(), HudSlots.STOCK)
        Text(" | ${tr(UiText.SUGAR)}: ") { }
        hudDigits(hud.sugarDigits.use(), HudSlots.STOCK)
    }
    Row {
        Text("${tr(UiText.QUEEN_AGE)}: ") { }
        hudDigits(hud.queenMin.use(), HudSlots.QUEEN_MIN)
        Text("${tr(UiText.UNIT_MIN)} ") { }
        hudDigits(hud.queenSec.use(), HudSlots.QUEEN_SEC)
        Text(tr(UiText.UNIT_SEC)) { }
    }
}.also { it.applyControlRadius() }

/** How many digits each HUD slot reserves. The font is unchanged. */
internal object HudSlots {
    const val FPS = 4
    const val POP = 5
    const val STOCK = 6
    const val QUEEN_MIN = 4
    const val QUEEN_SEC = 2
    const val MANY = 28
}

internal fun hudNumberWidth(digits: Int) = MyantsTheme.size.hudDigit * digits

private fun UiScope.hudDigits(value: String, digits: Int) {
    Text(value) {
        modifier
            .font(UiFonts.firaDigits())
            .width(hudNumberWidth(digits))
            .textAlignX(AlignmentX.End)
            .alignY(AlignmentY.Center)
    }
}
