package com.neojou.myants.ui

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
        assertEquals("Tempo", L10n.string(UiText.SPEED, UiLanguage.DE))
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
    }
}
