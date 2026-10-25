package com.neojou.myants.ui.panels

import com.neojou.myants.applyAnalyse
import com.neojou.myants.AnalyseLine
import com.neojou.myants.AppSession
import com.neojou.myants.ui.L10n
import com.neojou.myants.ui.MyantsTheme
import com.neojou.myants.ui.UiText
import com.neojou.myants.ui.tr
import de.fabmax.kool.modules.ui2.*

internal fun UiScope.analyse(session: AppSession) {
    ScrollArea(height = 280.dp, withHorizontalScrollbar = false) {
        Column {
            if (session.analyseLines.isEmpty()) {
                Text(tr(UiText.NO_SUGGESTIONS)) { }
            }
            for (line in session.analyseLines) {
                val label = analyseLabel(line)
                if (line.path == null) {
                    Text(label) { modifier.margin(top = MyantsTheme.space.s2) }
                } else {
                    Button(label) {
                        modifier.margin(top = MyantsTheme.space.s2).onClick {
                            session.applyAnalyse(line.path, line.to)
                        }
                    }
                }
            }
        }
    }
}

/** Resolves the string table on each compose, so an open panel follows the language. */
private fun UiScope.analyseLabel(line: AnalyseLine): String {
    L10n.language.use()
    val id = line.copyId
    if (id == null) return line.literal
    if (id == "empty") return L10n.string(UiText.NO_SUGGESTIONS)
    val resolved = if (line.effect) L10n.analyseEffect(id) else L10n.analyseHead(id, line.severity ?: "info")
    return resolved.ifBlank { line.literal.ifBlank { id } }
}
