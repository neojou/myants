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
    QUALITY,
    RENDERER,
    CLASSIC_2D,
    PHEROMONES,
    ANT_COLOR,
    DAY_NIGHT,
    CINEMA,
    BLOOM,
    BLOOM_OFF,
    SHADOWS,
    WEATHER,
    GRASS,
    PITCH,
    VERSION,
    HUD_FPS,
    POPULATION,
    PROTEIN,
    SUGAR,
    QUEEN_AGE,
    WORKERS,
    SOLDIERS,
    VERSUS,
    UNIT_MIN,
    UNIT_SEC,
    SERIES_POP,
    SERIES_SUGAR,
    SERIES_PROTEIN,
    SERIES_ENERGY,
    RIVAL_COLONY,
    TIP_SLOWER,
    TIP_FASTER,
    TIP_PAUSE,
    TIP_STEP,
    TIP_ZOOM_IN,
    TIP_ZOOM_OUT,
    TIP_RESET_CAMERA,
    TIP_SCREENSHOT,
    TIP_INSPECT,
    TIP_SUGAR,
    TIP_PROTEIN,
    TIP_ENEMY,
    TIP_RAIN,
    TIP_PAUSED_ONLY,
}

/**
 * UI copy. Simulation text, colony names, and the MIT line are not in this table.
 * Default is [UiLanguage.EN]. AntSim's own UI is mixed English and German; that difference is recorded in deviations.
 */
object L10n {
    val language = mutableStateOf(UiLanguage.EN)

    fun string(key: UiText, lang: UiLanguage = language.value): String = TABLE[key]!!.getValue(lang)

    fun fpsLine(value: String, lang: UiLanguage = language.value): String =
        "${string(UiText.HUD_FPS, lang)}: $value"

    fun foodLine(protein: Int, sugar: Int, lang: UiLanguage = language.value): String =
        "${string(UiText.PROTEIN, lang)}: $protein | ${string(UiText.SUGAR, lang)}: $sugar"

    fun queenLine(minutes: Int, seconds: Int, lang: UiLanguage = language.value): String =
        "${string(UiText.QUEEN_AGE, lang)}: $minutes${string(UiText.UNIT_MIN, lang)} $seconds${string(UiText.UNIT_SEC, lang)}"

    fun populationLine(total: String, detail: String, lang: UiLanguage = language.value): String =
        "${string(UiText.POPULATION, lang)}: $total ($detail)"

    fun populationSingleDetail(workers: String, soldiers: String, lang: UiLanguage = language.value): String =
        "${string(UiText.WORKERS, lang)}:$workers ${string(UiText.SOLDIERS, lang)}:$soldiers"

    fun populationSplitDetail(counts: List<String>, lang: UiLanguage = language.value): String =
        counts.joinToString(" ${string(UiText.VERSUS, lang)} ")

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
        UiText.QUALITY to pack("Quality", "品質", "Qualität"),
        UiText.RENDERER to pack("Renderer", "渲染", "Renderer"),
        UiText.CLASSIC_2D to pack("Classic 2D", "經典 2D", "Klassisch 2D"),
        UiText.PHEROMONES to pack("Pheromone trails", "費洛蒙軌跡", "Pheromon-Spuren"),
        UiText.ANT_COLOR to pack("Ant color (own colony)", "蟻色（自己的族群）", "Ameisenfarbe (eigene Kolonie)"),
        UiText.DAY_NIGHT to pack("Day/night tint", "日夜色調", "Tag/Nacht-Tönung"),
        UiText.CINEMA to pack("Cinema camera", "電影相機", "Kino-Kamera"),
        UiText.BLOOM to pack("Bloom", "光暈", "Bloom / Glühen"),
        UiText.BLOOM_OFF to pack("Bloom (off)", "光暈（關閉）", "Bloom / Glühen (aus)"),
        UiText.SHADOWS to pack("Shadows", "陰影", "Schatten"),
        UiText.WEATHER to pack("Weather", "天氣效果", "Wetter-Effekte"),
        UiText.GRASS to pack("Grass density", "草密度", "Gras-Dichte"),
        UiText.PITCH to pack("Camera pitch", "相機傾角", "Kamera-Neigung"),
        UiText.VERSION to pack("Version", "版本", "Version"),
        UiText.HUD_FPS to pack("FPS", "幀率", "FPS"),
        UiText.POPULATION to pack("Population", "族群", "Bevölkerung"),
        UiText.PROTEIN to pack("Protein", "蛋白質", "Protein"),
        UiText.SUGAR to pack("Sugar", "糖", "Zucker"),
        UiText.QUEEN_AGE to pack("Queen Age", "蟻后年齡", "Königinnenalter"),
        UiText.WORKERS to pack("W", "工", "W"),
        UiText.SOLDIERS to pack("S", "兵", "S"),
        UiText.VERSUS to pack("vs", "對", "gegen"),
        UiText.UNIT_MIN to pack("m", "分", "min"),
        UiText.UNIT_SEC to pack("s", "秒", "s"),
        UiText.SERIES_POP to pack("Pop", "族群", "Pop"),
        UiText.SERIES_SUGAR to pack("Sugar", "糖", "Zucker"),
        UiText.SERIES_PROTEIN to pack("Protein", "蛋白質", "Protein"),
        UiText.SERIES_ENERGY to pack("Energy", "能量", "Energie"),
        UiText.RIVAL_COLONY to pack("Rival colony", "敵群", "Rivalenkolonie"),
        UiText.TIP_SLOWER to pack("Slower", "減速", "Langsamer"),
        UiText.TIP_FASTER to pack("Faster", "加速", "Schneller"),
        UiText.TIP_PAUSE to pack("Pause/Resume (Space)", "暫停／繼續（空白鍵）", "Pause/Fortsetzen (Leertaste)"),
        UiText.TIP_STEP to pack("Single Step", "單步", "Einzelschritt"),
        UiText.TIP_ZOOM_IN to pack("Zoom in (+)", "放大（+）", "Hineinzoomen (+)"),
        UiText.TIP_ZOOM_OUT to pack("Zoom out (\u2212)", "縮小（\u2212）", "Herauszoomen (\u2212)"),
        UiText.TIP_RESET_CAMERA to pack("Reset Camera", "重設相機", "Kamera zurücksetzen"),
        UiText.TIP_SCREENSHOT to pack("Screenshot PNG", "截圖 PNG", "Bildschirmfoto (PNG)"),
        UiText.TIP_INSPECT to pack("Inspect", "檢查", "Untersuchen"),
        UiText.TIP_SUGAR to pack("Place Sugar", "放置糖", "Zucker platzieren"),
        UiText.TIP_PROTEIN to pack("Place Protein", "放置蛋白質", "Protein platzieren"),
        UiText.TIP_ENEMY to pack("Spawn Enemy", "產生敵人", "Feind erzeugen"),
        UiText.TIP_RAIN to pack("Rain on/off", "雨 開／關", "Regen an/aus"),
        UiText.TIP_PAUSED_ONLY to pack("(paused only)", "（僅暫停時）", "(nur pausiert)"),
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
