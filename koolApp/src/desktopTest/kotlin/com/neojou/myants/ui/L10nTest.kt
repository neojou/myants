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
    }
}
