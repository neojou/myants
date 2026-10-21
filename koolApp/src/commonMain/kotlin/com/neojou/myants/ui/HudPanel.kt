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
    val hud = session.hud
    Row {
        Text("FPS: ") { }
        hudDigits(hud.fpsDigits.use(), HudSlots.FPS)
    }
    if (hud.popMany.use()) {
        Text(hud.pop.use()) {
            modifier.width(hudNumberWidth(HudSlots.MANY)).textAlignX(AlignmentX.End)
        }
    } else if (hud.popSplit.use()) {
        Row {
            Text("Population: ") { }
            hudDigits(hud.popDigits.use(), HudSlots.POP)
            Text(" (") { }
            hudDigits(hud.colonyA.use(), HudSlots.POP)
            Text(" vs ") { }
            hudDigits(hud.colonyB.use(), HudSlots.POP)
            Text(")") { }
        }
    } else {
        Row {
            Text("Population: ") { }
            hudDigits(hud.popDigits.use(), HudSlots.POP)
            Text(" (W:") { }
            hudDigits(hud.workers.use(), HudSlots.POP)
            Text(" S:") { }
            hudDigits(hud.soldiers.use(), HudSlots.POP)
            Text(")") { }
        }
    }
    Row {
        Text("Protein: ") { }
        hudDigits(hud.proteinDigits.use(), HudSlots.STOCK)
        Text(" | Sugar: ") { }
        hudDigits(hud.sugarDigits.use(), HudSlots.STOCK)
    }
    Row {
        Text("Queen Age: ") { }
        hudDigits(hud.queenMin.use(), HudSlots.QUEEN_MIN)
        Text("m ") { }
        hudDigits(hud.queenSec.use(), HudSlots.QUEEN_SEC)
        Text("s") { }
    }
    Text("v${AppVersion.NAME}") { modifier.font(sizes.smallText) }
    if (inspect.isNotEmpty()) {
        Text(inspect) { modifier.margin(top = MyantsTheme.space.s2) }
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
            .width(hudNumberWidth(digits))
            .textAlignX(AlignmentX.End)
            .alignY(AlignmentY.Center)
    }
}
