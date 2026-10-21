package com.neojou.myants.ui

/**
 * HUD toolbar icons (Speed / Zoom / Tool rows), spec #5 §3.
 * [file] is the Material Symbols Rounded SVG under `assets/icons/`; [fallback] is the
 * ASCII label used when the asset is missing. Shortcuts are only listed when bound in AntSimScene.
 */
enum class HudIcon(
    val file: String,
    val fallback: String,
    val tip: UiText,
    val filledFile: String? = null,
) {
    SLOWER("remove", "-", UiText.TIP_SLOWER),
    FASTER("add", "+", UiText.TIP_FASTER),
    PAUSE("pause_fill", "||", UiText.TIP_PAUSE),
    RESUME("play_arrow_fill", ">", UiText.TIP_PAUSE),
    STEP("skip_next_fill", ">|", UiText.TIP_STEP),
    ZOOM_IN("zoom_in", "+", UiText.TIP_ZOOM_IN),
    ZOOM_OUT("zoom_out", "-", UiText.TIP_ZOOM_OUT),
    RESET_CAMERA("center_focus_strong", "o", UiText.TIP_RESET_CAMERA),
    SCREENSHOT("photo_camera", "PNG", UiText.TIP_SCREENSHOT),
    INSPECT("search", "?", UiText.TIP_INSPECT, "search_fill"),
    SUGAR("cookie", "S", UiText.TIP_SUGAR, "cookie_fill"),
    PROTEIN("set_meal", "P", UiText.TIP_PROTEIN, "set_meal_fill"),
    ENEMY("skull", "E", UiText.TIP_ENEMY, "skull_fill"),
    RAIN("rainy", "Rain", UiText.TIP_RAIN, "rainy_fill");

    fun assetPath(filled: Boolean = false): String = "icons/${if (filled && filledFile != null) filledFile else file}.svg"

    fun tooltip(lang: UiLanguage, disabled: Boolean = false): String {
        val base = L10n.string(tip, lang)
        if (!disabled) return base
        return "$base ${L10n.string(UiText.TIP_PAUSED_ONLY, lang)}"
    }

    /** Text shown in the button: the ASCII fallback when the icon texture is not available. */
    fun label(iconLoaded: Boolean): String = if (iconLoaded) "" else fallback

    companion object {
        /** Every SVG file that must ship in assets/icons. */
        fun allAssetPaths(): Set<String> =
            entries.flatMap { listOfNotNull(it.assetPath(false), it.filledFile?.let { _ -> it.assetPath(true) }) }.toSet()
    }
}
