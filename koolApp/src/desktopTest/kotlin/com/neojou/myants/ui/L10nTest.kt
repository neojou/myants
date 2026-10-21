package com.neojou.myants.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class L10nTest {
    @Test
    fun everyKeyHasEnglishChineseAndGerman() {
        for (key in UiText.entries) {
            for (lang in UiLanguage.entries) {
                assertTrue(L10n.string(key, lang).isNotBlank(), "$key $lang")
            }
        }
        assertEquals("Speed", L10n.string(UiText.SPEED, UiLanguage.EN))
        assertEquals("速度", L10n.string(UiText.SPEED, UiLanguage.ZH_TW))
        assertEquals("速度", L10n.string(UiText.SPEED, UiLanguage.JA))
        assertEquals("Tempo", L10n.string(UiText.SPEED, UiLanguage.DE))
        assertEquals("言語", L10n.string(UiText.LANGUAGE, UiLanguage.JA))
        assertEquals("Verlauf", L10n.string(UiText.STATS, UiLanguage.DE))
        assertEquals("Protein: 3 | Sugar: 4", L10n.foodLine(3, 4, UiLanguage.EN))
        assertEquals("Protein: 3 | Zucker: 4", L10n.foodLine(3, 4, UiLanguage.DE))
        assertEquals("Queen Age: 1m 2s", L10n.queenLine(1, 2, UiLanguage.EN))
        assertEquals("Königinnenalter: 1min 2s", L10n.queenLine(1, 2, UiLanguage.DE))
        assertEquals("Sugar", L10n.string(UiText.SERIES_SUGAR, UiLanguage.EN))
        assertEquals("Zucker", L10n.string(UiText.SERIES_SUGAR, UiLanguage.DE))
        assertEquals("Cancel", L10n.string(UiText.CANCEL, UiLanguage.EN))
        assertEquals("取消", L10n.string(UiText.CANCEL, UiLanguage.ZH_TW))
        assertEquals("Abbrechen", L10n.string(UiText.CANCEL, UiLanguage.DE))
        assertEquals("(no suggestions)", L10n.string(UiText.NO_SUGGESTIONS, UiLanguage.EN))
        assertEquals("（沒有建議）", L10n.string(UiText.NO_SUGGESTIONS, UiLanguage.ZH_TW))
        assertEquals("（提案はありません）", L10n.string(UiText.NO_SUGGESTIONS, UiLanguage.JA))
        assertEquals("(keine Vorschläge)", L10n.string(UiText.NO_SUGGESTIONS, UiLanguage.DE))
        assertTrue(InspectCopy.coversEveryKind())
    }

    @Test
    fun analyseIdsMatchTheObserver() {
        val root = listOf(File("src/commonMain"), File("koolApp/src/commonMain")).first { it.isDirectory }
        val src = File(root, "kotlin/com/neojou/myants/sim/SimObserver.kt").readText()
        val ids = Regex("""id = "([^"]+)"""").findAll(src).map { it.groupValues[1] }.toList()
        assertEquals(21, ids.size)
        assertEquals(ids.size, ids.toSet().size)
        for (id in ids) {
            for (lang in UiLanguage.entries) {
                assertTrue(L10n.analyseHead(id, "info", lang).isNotBlank(), "$id head $lang")
                assertTrue(L10n.analyseEffect(id, lang).isNotBlank(), "$id effect $lang")
            }
        }
        assertTrue("info" in L10n.analyseHead("wait", "info", UiLanguage.EN))
        assertTrue("Hinweis" in L10n.analyseHead("wait", "info", UiLanguage.DE))
        assertTrue("Straßen" !in L10n.string(UiText.PARAM_FOOD_TRAIL_DECAY, UiLanguage.EN))
        assertEquals("Food trail decay", L10n.string(UiText.PARAM_FOOD_TRAIL_DECAY, UiLanguage.EN))
        assertEquals("食物軌跡衰退", L10n.string(UiText.PARAM_FOOD_TRAIL_DECAY, UiLanguage.ZH_TW))
        assertEquals("Futterspur-Abfall", L10n.string(UiText.PARAM_FOOD_TRAIL_DECAY, UiLanguage.DE))
    }
}
