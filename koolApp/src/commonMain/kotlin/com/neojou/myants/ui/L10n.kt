package com.neojou.myants.ui

import de.fabmax.kool.modules.ui2.UiScope
import de.fabmax.kool.modules.ui2.mutableStateOf

/** Interface language. Missing or unknown saved values stay [EN]. */
enum class UiLanguage(val endonym: String) {
    EN("English"),
    ZH_TW("繁體中文"),
    DE("Deutsch"),
}

enum class UiText {
    SPEED,
    ZOOM,
    RESTART,
    GRAFIK,
    PARAMS,
    STATS,
    ANALYSE,
    ABOUT,
    LANGUAGE,
    OK,
    RESET,
}

/**
 * UI copy. Simulation text, colony names, and the MIT line are not in this table.
 * Default is [UiLanguage.EN]. AntSim's own UI is mixed English and German; that difference is recorded in deviations.
 */
object L10n {
    val language = mutableStateOf(UiLanguage.EN)

    fun string(key: UiText, lang: UiLanguage = language.value): String = TABLE[key]!!.getValue(lang)

    private val TABLE: Map<UiText, Map<UiLanguage, String>> = mapOf(
        UiText.SPEED to pack("Speed", "速度", "Tempo"),
        UiText.ZOOM to pack("Zoom", "縮放", "Zoom"),
        UiText.RESTART to pack("Restart", "重新開始", "Neustart"),
        UiText.GRAFIK to pack("Graphics", "圖形", "Grafik"),
        UiText.PARAMS to pack("Params", "參數", "Parameter"),
        UiText.STATS to pack("Stats", "統計", "Verlauf"),
        UiText.ANALYSE to pack("Analysis", "分析", "Analyse"),
        UiText.ABOUT to pack("About", "關於", "Info"),
        UiText.LANGUAGE to pack("Language", "語言", "Sprache"),
        UiText.OK to pack("OK", "確定", "OK"),
        UiText.RESET to pack("Reset", "重設", "Zurücksetzen"),
    )

    private fun pack(en: String, zh: String, de: String) = mapOf(
        UiLanguage.EN to en,
        UiLanguage.ZH_TW to zh,
        UiLanguage.DE to de,
    )
}

/** Reads [L10n.language] so the caller recomposes when the language changes. */
fun UiScope.tr(key: UiText): String {
    L10n.language.use()
    return L10n.string(key)
}
