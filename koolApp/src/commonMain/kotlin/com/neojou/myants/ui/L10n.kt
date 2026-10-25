package com.neojou.myants.ui

import de.fabmax.kool.modules.ui2.UiScope
import de.fabmax.kool.modules.ui2.mutableStateOf

/** Interface language. Missing or unknown saved values stay [EN]. */
enum class UiLanguage(val endonym: String) {
    EN("English"),
    ZH_TW("繁體中文"),
    JA("日本語"),
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
    CANCEL,
    RESTART_TITLE,
    RESTART_BODY,
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
    NO_SUGGESTIONS,
    SEV_INFO,
    SEV_CRITICAL,
    SEV_WARN,
    SEV_GOOD,
    QUALITY_ULTRA_LOW,
    QUALITY_LOW,
    QUALITY_MEDIUM,
    QUALITY_HIGH,
    QUALITY_ULTRA,
    PARAM_ANT_SPEED,
    PARAM_ENERGY_USE,
    PARAM_QUEEN_LAY,
    PARAM_LIFESPAN,
    PARAM_EGG_COST,
    PARAM_SUGAR_VALUE,
    PARAM_PROTEIN_VALUE,
    PARAM_SUGAR_ENERGY,
    PARAM_COLONY_UPKEEP,
    PARAM_BROOD_UPKEEP,
    PARAM_PHEROMONE_SPREAD,
    PARAM_PHEROMONE_DECAY,
    PARAM_FOOD_TRAIL_DECAY,
    PARAM_TRAIL_SENSOR,
    PARAM_DISPERSAL_RADIUS,
    PARAM_DISPERSAL_STRENGTH,
    PARAM_SITE_FIDELITY,
    PARAM_PROTEIN_FORAGER,
    PARAM_BROOD_UPKEEP_LIMIT,
    PARAM_SOLDIER_FOOD,
    PARAM_SOLDIER_PROTEIN,
    PARAM_SUGAR_SOURCES,
    PARAM_PREDATOR_SPAWN,
    INSPECT_ANT,
    INSPECT_QUEEN,
    INSPECT_ENERGY,
    INSPECT_HEALTH,
    INSPECT_AGE,
    INSPECT_HP,
    INSPECT_EGG,
    INSPECT_LARVA,
    INSPECT_PUPA,
    INSPECT_HUNGER,
    INSPECT_CHAMBER,
    INSPECT_ROLE_QUEEN,
    INSPECT_ROLE_BROOD,
    INSPECT_ROLE_STORAGE,
    INSPECT_ROLE_CEMETERY,
    INSPECT_RADIUS,
    INSPECT_STONE,
    INSPECT_CLOCK,
    INSPECT_GRASS,
    INSPECT_LEAF,
    INSPECT_SIZE,
    INSPECT_PHEROMONE,
    INSPECT_HOME,
    INSPECT_TRAIL_SUGAR,
    INSPECT_TRAIL_PROTEIN,
    INSPECT_DANGER,
    INSPECT_FOLLOW,
    INSPECT_FOLLOWING,
    INSPECT_STATE,
    INSPECT_CARRY,
    INSPECT_AMOUNT,
    INSPECT_CASTE,
    INSPECT_ROLE,
    AN_WAIT_M, AN_WAIT_S, AN_WAIT_E,
    AN_POP_FALL_M, AN_POP_FALL_S, AN_POP_FALL_E,
    AN_POP_DIP_M, AN_POP_DIP_S, AN_POP_DIP_E,
    AN_POP_BOOM_M, AN_POP_BOOM_S, AN_POP_BOOM_E,
    AN_POP_GOOD_M, AN_POP_GOOD_S, AN_POP_GOOD_E,
    AN_ENERGY_LOW_M, AN_ENERGY_LOW_S, AN_ENERGY_LOW_E,
    AN_ENERGY_MID_M, AN_ENERGY_MID_S, AN_ENERGY_MID_E,
    AN_ENERGY_HIGH_M, AN_ENERGY_HIGH_S, AN_ENERGY_HIGH_E,
    AN_FORAGE_HIGH_M, AN_FORAGE_HIGH_S, AN_FORAGE_HIGH_E,
    AN_FORAGE_LOW_M, AN_FORAGE_LOW_S, AN_FORAGE_LOW_E,
    AN_COMBAT_HIGH_M, AN_COMBAT_HIGH_S, AN_COMBAT_HIGH_E,
    AN_COMBAT_LOW_M, AN_COMBAT_LOW_S, AN_COMBAT_LOW_E,
    AN_QUEEN_ENERGY_M, AN_QUEEN_ENERGY_S, AN_QUEEN_ENERGY_E,
    AN_QUEEN_STRESS_M, AN_QUEEN_STRESS_S, AN_QUEEN_STRESS_E,
    AN_BROOD_LOW_M, AN_BROOD_LOW_S, AN_BROOD_LOW_E,
    AN_SOLDIERS_HIGH_M, AN_SOLDIERS_HIGH_S, AN_SOLDIERS_HIGH_E,
    AN_SOLDIERS_LOW_M, AN_SOLDIERS_LOW_S, AN_SOLDIERS_LOW_E,
    AN_SUGAR_LOW_M, AN_SUGAR_LOW_S, AN_SUGAR_LOW_E,
    AN_SUGAR_HIGH_M, AN_SUGAR_HIGH_S, AN_SUGAR_HIGH_E,
    AN_PROTEIN_HIGH_M, AN_PROTEIN_HIGH_S, AN_PROTEIN_HIGH_E,
    AN_OVERALL_M, AN_OVERALL_S, AN_OVERALL_E,
}

/**
 * UI copy, including analysis sentences and inspect labels.
 * The MIT attribution line stays English in every language.
 * Default is [UiLanguage.EN].
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

    private val CORE: Map<UiText, Map<UiLanguage, String>> = mapOf(
        UiText.SPEED to pack("Speed", "速度", "速度", "Tempo"),
        UiText.ZOOM to pack("Zoom", "縮放", "ズーム", "Zoom"),
        UiText.RESTART to pack("Restart", "重新開始", "再開", "Neustart"),
        UiText.GRAFIK to pack("Graphics", "圖形", "グラフィック", "Grafik"),
        UiText.PARAMS to pack("Params", "參數", "パラメータ", "Parameter"),
        UiText.STATS to pack("Stats", "統計", "統計", "Verlauf"),
        UiText.ANALYSE to pack("Analysis", "分析", "分析", "Analyse"),
        UiText.ABOUT to pack("About", "關於", "情報", "Info"),
        UiText.LANGUAGE to pack("Language", "語言", "言語", "Sprache"),
        UiText.OK to pack("OK", "確定", "OK", "OK"),
        UiText.RESET to pack("Reset", "重設", "リセット", "Zurücksetzen"),
        UiText.CANCEL to pack("Cancel", "取消", "キャンセル", "Abbrechen"),
        UiText.RESTART_TITLE to pack("Restart?", "重新開始？", "再開しますか？", "Neustart?"),
        UiText.RESTART_BODY to pack(
            "The current world is discarded.",
            "目前的世界會被捨棄。",
            "いまの世界は破棄されます。",
            "Die aktuelle Welt wird verworfen.",
        ),
        UiText.QUALITY to pack("Quality", "品質", "品質", "Qualität"),
        UiText.RENDERER to pack("Renderer", "渲染", "レンダラー", "Renderer"),
        UiText.CLASSIC_2D to pack("Classic 2D", "經典 2D", "クラシック2D", "Klassisch 2D"),
        UiText.PHEROMONES to pack("Pheromone trails", "費洛蒙軌跡", "フェロモンの跡", "Pheromon-Spuren"),
        UiText.ANT_COLOR to pack("Ant color (own colony)", "蟻色（自己的族群）", "アリの色（自分のコロニー）", "Ameisenfarbe (eigene Kolonie)"),
        UiText.DAY_NIGHT to pack("Day/night tint", "日夜色調", "昼夜の色", "Tag/Nacht-Tönung"),
        UiText.CINEMA to pack("Cinema camera", "電影相機", "シネマカメラ", "Kino-Kamera"),
        UiText.BLOOM to pack("Bloom", "光暈", "ブルーム", "Bloom / Glühen"),
        UiText.BLOOM_OFF to pack("Bloom (off)", "光暈（關閉）", "ブルーム（オフ）", "Bloom / Glühen (aus)"),
        UiText.SHADOWS to pack("Shadows", "陰影", "影", "Schatten"),
        UiText.WEATHER to pack("Weather", "天氣效果", "天候効果", "Wetter-Effekte"),
        UiText.GRASS to pack("Grass density", "草密度", "草の密度", "Gras-Dichte"),
        UiText.PITCH to pack("Camera pitch", "相機傾角", "カメラの傾き", "Kamera-Neigung"),
        UiText.VERSION to pack("Version", "版本", "バージョン", "Version"),
        UiText.HUD_FPS to pack("FPS", "幀率", "フレームレート", "FPS"),
        UiText.POPULATION to pack("Population", "族群", "個体数", "Bevölkerung"),
        UiText.PROTEIN to pack("Protein", "蛋白質", "タンパク質", "Protein"),
        UiText.SUGAR to pack("Sugar", "糖", "糖", "Zucker"),
        UiText.QUEEN_AGE to pack("Queen Age", "蟻后年齡", "女王の年齢", "Königinnenalter"),
        UiText.WORKERS to pack("W", "工", "働", "W"),
        UiText.SOLDIERS to pack("S", "兵", "兵", "S"),
        UiText.VERSUS to pack("vs", "對", "対", "gegen"),
        UiText.UNIT_MIN to pack("m", "分", "分", "min"),
        UiText.UNIT_SEC to pack("s", "秒", "秒", "s"),
        UiText.SERIES_POP to pack("Pop", "族群", "個体", "Pop"),
        UiText.SERIES_SUGAR to pack("Sugar", "糖", "糖", "Zucker"),
        UiText.SERIES_PROTEIN to pack("Protein", "蛋白質", "タンパク質", "Protein"),
        UiText.SERIES_ENERGY to pack("Energy", "能量", "エネルギー", "Energie"),
        UiText.RIVAL_COLONY to pack("Rival colony", "敵群", "敵コロニー", "Rivalenkolonie"),
        UiText.TIP_SLOWER to pack("Slower (-)", "減速（-）", "減速（-）", "Langsamer (-)"),
        UiText.TIP_FASTER to pack("Faster (+)", "加速（+）", "加速（+）", "Schneller (+)"),
        UiText.TIP_PAUSE to pack("Pause/Resume (Space)", "暫停／繼續（空白鍵）", "一時停止／再開（スペース）", "Pause/Fortsetzen (Leertaste)"),
        UiText.TIP_STEP to pack("Single Step (.)", "單步（.）", "シングルステップ（.）", "Einzelschritt (.)"),
        UiText.TIP_ZOOM_IN to pack("Zoom in (=)", "放大（=）", "拡大（=）", "Hineinzoomen (=)"),
        UiText.TIP_ZOOM_OUT to pack("Zoom out (_)", "縮小（_）", "縮小（_）", "Herauszoomen (_)"),
        UiText.TIP_RESET_CAMERA to pack("Reset Camera", "重設相機", "カメラを戻す", "Kamera zurücksetzen"),
        UiText.TIP_SCREENSHOT to pack("Screenshot PNG", "截圖 PNG", "スクリーンショット PNG", "Bildschirmfoto (PNG)"),
        UiText.TIP_INSPECT to pack("Inspect (1)", "檢查（1）", "調べる（1）", "Untersuchen (1)"),
        UiText.TIP_SUGAR to pack("Place Sugar (2)", "放置糖（2）", "糖を置く（2）", "Zucker platzieren (2)"),
        UiText.TIP_PROTEIN to pack("Place Protein (3)", "放置蛋白質（3）", "タンパク質を置く（3）", "Protein platzieren (3)"),
        UiText.TIP_ENEMY to pack("Spawn Enemy (4)", "產生敵人（4）", "敵を出す（4）", "Feind erzeugen (4)"),
        UiText.TIP_RAIN to pack("Rain on/off", "雨 開／關", "雨 オン／オフ", "Regen an/aus"),
        UiText.TIP_PAUSED_ONLY to pack("(paused only)", "（僅暫停時）", "（停止中のみ）", "(nur pausiert)"),
    )

    private val TABLE: Map<UiText, Map<UiLanguage, String>> = CORE + extraCopy()

    fun severity(token: String, lang: UiLanguage = language.value): String {
        val key = when (token) {
            "critical" -> UiText.SEV_CRITICAL
            "warn" -> UiText.SEV_WARN
            "good" -> UiText.SEV_GOOD
            "info" -> UiText.SEV_INFO
            else -> null
        }
        return if (key == null) token.ifBlank { string(UiText.SEV_INFO, lang) } else string(key, lang)
    }

    fun analyseHead(id: String, severityToken: String, lang: UiLanguage = language.value): String {
        val parts = analyseParts(id) ?: return ""
        return "${severity(severityToken, lang)} | ${string(parts.metric, lang)}: ${string(parts.suggestion, lang)}"
    }

    fun analyseEffect(id: String, lang: UiLanguage = language.value): String {
        val parts = analyseParts(id) ?: return ""
        return string(parts.effect, lang)
    }

    private fun pack(en: String, zh: String, ja: String, de: String) = mapOf(
        UiLanguage.EN to en,
        UiLanguage.ZH_TW to zh,
        UiLanguage.JA to ja,
        UiLanguage.DE to de,
    )
}

/** Reads [L10n.language] so the caller recomposes when the language changes. */
fun UiScope.tr(key: UiText): String {
    L10n.language.use()
    return L10n.string(key)
}
