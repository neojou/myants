package com.neojou.myants.ui.panels

import com.neojou.myants.AppSession
import com.neojou.myants.ui.MyantsTheme
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.scene.Scene

internal fun UiScope.analyse(session: AppSession) {
    ScrollArea(height = 280.dp, withHorizontalScrollbar = false) {
        Column {
            if (session.analyseLines.isEmpty()) {
                Text("(no suggestions)") { }
            }
            for (line in session.analyseLines) {
                if (line.path == null) {
                    Text(line.text) { modifier.margin(top = MyantsTheme.space.s2) }
                } else {
                    Button(line.text) {
                        modifier.margin(top = MyantsTheme.space.s2).onClick {
                            session.applyAnalyse(line.path, line.to)
                        }
                    }
                }
            }
        }
    }
}
