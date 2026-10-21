package com.neojou.myants

import com.neojou.myants.ui.HudIcons
import com.neojou.myants.ui.UiText
import com.neojou.myants.ui.addAboutDialog
import com.neojou.myants.ui.addHudPanel
import com.neojou.myants.ui.addToolbar
import com.neojou.myants.ui.panels.analyse
import com.neojou.myants.ui.panels.grafik
import com.neojou.myants.ui.panels.params
import com.neojou.myants.ui.panels.sidePanel
import com.neojou.myants.ui.panels.stats
import de.fabmax.kool.KoolApplication
import de.fabmax.kool.addUiScene

/** Assembles the 2D UI scene; each piece lives in `ui/` and `ui/panels/`. */
fun KoolApplication.addAntSimUi(session: AppSession) = addUiScene {
    session.uiScene = this
    HudIcons.loadAll(coroutineScope)
    val hud = addHudPanel(session)
    addToolbar(session)
    val grafikPanel = sidePanel(session, UiText.GRAFIK) { grafik(session) }
    val paramsPanel = sidePanel(session, UiText.PARAMS) { params(session) }
    val statsPanel = sidePanel(session, UiText.STATS) { stats(session) }
    val analysePanel = sidePanel(session, UiText.ANALYSE) { analyse(session) }
    val aboutPanel = addAboutDialog(session)
    aboutPanel.isVisible = false
    onUpdate {
        grafikPanel.isVisible = session.showGrafik
        paramsPanel.isVisible = session.showParams
        statsPanel.isVisible = session.showStats
        analysePanel.isVisible = session.showAnalyse
        aboutPanel.isVisible = session.showAbout
    }
    hud.isVisible = true
}
