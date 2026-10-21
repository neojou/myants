package com.neojou.myants.ui

/** UI language for tooltips. AntSim UI copy is German by default. */
enum class UiLang { EN, DE }

/**
 * HUD toolbar icons (Speed / Zoom / Tool rows), spec #5 §3.
 * [file] is the Material Symbols Rounded SVG under `assets/icons/`; [fallback] is the
 * ASCII label used when the asset is missing. Shortcuts are only listed when bound in AntSimScene.
 */
enum class HudIcon(
    val file: String,
    val fallback: String,
    val tooltipEn: String,
    val tooltipDe: String,
    val filledFile: String? = null,
) {
    SLOWER("remove", "-", "Slower", "Langsamer"),
    FASTER("add", "+", "Faster", "Schneller"),
    PAUSE("pause_fill", "||", "Pause/Resume (Space)", "Pause/Fortsetzen (Leertaste)"),
    RESUME("play_arrow_fill", ">", "Pause/Resume (Space)", "Pause/Fortsetzen (Leertaste)"),
    STEP("skip_next_fill", ">|", "Single Step", "Einzelschritt"),
    ZOOM_IN("zoom_in", "+", "Zoom in (+)", "Hineinzoomen (+)"),
    ZOOM_OUT("zoom_out", "-", "Zoom out (\u2212)", "Herauszoomen (\u2212)"),
    RESET_CAMERA("center_focus_strong", "o", "Reset Camera", "Kamera zurücksetzen"),
    SCREENSHOT("photo_camera", "PNG", "Screenshot PNG", "Bildschirmfoto (PNG)"),
    INSPECT("search", "?", "Inspect", "Untersuchen", "search_fill"),
    SUGAR("cookie", "S", "Place Sugar", "Zucker platzieren", "cookie_fill"),
    PROTEIN("set_meal", "P", "Place Protein", "Protein platzieren", "set_meal_fill"),
    ENEMY("skull", "E", "Spawn Enemy", "Feind erzeugen", "skull_fill"),
    RAIN("rainy", "Rain", "Rain on/off", "Regen an/aus", "rainy_fill");

    fun assetPath(filled: Boolean = false): String = "icons/${if (filled && filledFile != null) filledFile else file}.svg"

    fun tooltip(lang: UiLang, disabled: Boolean = false): String {
        val base = if (lang == UiLang.DE) tooltipDe else tooltipEn
        if (!disabled) return base
        return base + if (lang == UiLang.DE) " (nur pausiert)" else " (paused only)"
    }

    /** Text shown in the button: the ASCII fallback when the icon texture is not available. */
    fun label(iconLoaded: Boolean): String = if (iconLoaded) "" else fallback

    companion object {
        /** Every SVG file that must ship in assets/icons. */
        fun allAssetPaths(): Set<String> =
            entries.flatMap { listOfNotNull(it.assetPath(false), it.filledFile?.let { _ -> it.assetPath(true) }) }.toSet()
    }
}
