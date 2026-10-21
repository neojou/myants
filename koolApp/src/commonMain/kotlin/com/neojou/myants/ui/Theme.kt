package com.neojou.myants.ui

import de.fabmax.kool.modules.ui2.Dp
import de.fabmax.kool.modules.ui2.UiSurface
import de.fabmax.kool.util.Color
import kotlin.math.pow

/**
 * Single source of UI design tokens (M1-b1 spec, issue #3).
 * Only HUD/UI appearance; simulation data colors live in [Data] unchanged.
 */
object MyantsTheme {
    object colors {
        val surface = Color("#120F17").withAlpha(0.72f)
        val surfaceRaised = Color("#1C1822").withAlpha(0.86f)
        val surfaceVariant = Color("#2A2730")
        val outline = Color("#EADDFF").withAlpha(0.10f)
        val onSurface = Color("#EEEEEE")
        val onSurfaceVar = Color("#BDBDBD")
        val onSurfaceDisabled = Color("#EEEEEE").withAlpha(0.38f)
        val primary = Color("#6750A4")
        val primaryVariant = Color("#4F378B")
        val primaryContainer = Color("#EADDFF")
        val primaryHover = Color("#EADDFF").withAlpha(0.12f)
        val primaryPressed = Color("#EADDFF").withAlpha(0.20f)
        val onPrimary = Color("#FFFFFF")
        val danger = Color("#601410")
        val onDanger = Color("#F2B8B5")
        val success = Color("#6FDF6F")
    }

    /**
     * One own-colony color preset. Hex values stay fixed.
     * [name] follows the interface language so the swatch is not identified by color alone.
     */
    data class AntColorChoice(val hex: String, val en: String, val zh: String, val ja: String, val de: String) {
        fun name(lang: UiLanguage): String = when (lang) {
            UiLanguage.EN -> en
            UiLanguage.ZH_TW -> zh
            UiLanguage.JA -> ja
            UiLanguage.DE -> de
        }
    }

    /** Data colors: moved here, values must match AntSim and never change. */
    object Data {
        val antColorChoices = listOf(
            AntColorChoice("#c4703a", "Copper", "銅", "銅", "Kupfer"),
            AntColorChoice("#222222", "Black", "黑", "黒", "Schwarz"),
            AntColorChoice("#d9bf52", "Gold", "金", "金", "Gold"),
            AntColorChoice("#88c0a0", "Sage", "鼠尾草綠", "セージ", "Salbei"),
            AntColorChoice("#6fa8dc", "Blue", "藍", "青", "Blau"),
        )
        val antColors = antColorChoices.map { it.hex }

        /**
         * Colony 1 (the rival) keeps worker tint #d9bf52.
         * Name plus number, so that team is not identified by color alone.
         * Gold is preset index 2. Clicking that swatch still changes colony 0 only.
         */
        fun rivalColonyLabel(lang: UiLanguage): String =
            "${L10n.string(UiText.RIVAL_COLONY, lang)} 2 · ${antColorChoices[2].name(lang)} #d9bf52"
    }

    object radius {
        val sm: Dp = Dp(4f)
        val md: Dp = Dp(6f)
        val lg: Dp = Dp(10f)
    }

    object size {
        val button: Dp = Dp(28f)
        val icon: Dp = Dp(18f)
        /** One HUD digit slot. Fira stays as it is; the slot reserves the width. */
        val hudDigit: Dp = Dp(11f)
    }

    object motion {
        /** Tooltip delay; issue #6 requires < 0.5s. */
        const val tooltipDelaySec = 0.45
        /** Icon button content scale while pressed. Spec: 0.96, no bounce. */
        const val pressScale = 0.96f
        /** Matches the 120 ms hover fade in the HUD spec. */
        const val pressSeconds = 0.12f
    }

    object stroke {
        /** Selected-state outline width (non-color cue). */
        val selected: Dp = Dp(1f)
    }

    object space {
        val s1: Dp = Dp(4f)
        val s2: Dp = Dp(8f)
        val s3: Dp = Dp(12f)
        val s4: Dp = Dp(16f)
        val s5: Dp = Dp(20f)
        val s6: Dp = Dp(24f)
        val gapInGroup: Dp = Dp(2f)
        val gapGroup: Dp = Dp(6f)
        val panelPadding: Dp = s2
        val screenEdge: Dp = s3
    }

    object text {
        const val sm = 10f
        const val md = 12f
        const val lg = 14f
        const val title = 18f
    }

    /** WCAG relative luminance contrast ratio between two opaque colors. */
    fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    /** Alpha-composite [fg] over opaque [bg]. */
    fun over(fg: Color, bg: Color): Color = Color(
        fg.r * fg.a + bg.r * (1f - fg.a),
        fg.g * fg.a + bg.g * (1f - fg.a),
        fg.b * fg.a + bg.b * (1f - fg.a),
        1f,
    )

    private fun luminance(c: Color): Double {
        fun ch(v: Float): Double {
            val d = v.toDouble()
            return if (d <= 0.03928) d / 12.92 else ((d + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * ch(c.r) + 0.7152 * ch(c.g) + 0.0722 * ch(c.b)
    }
}

/**
 * kool draws Button and ComboBox corners with Sizes.smallGap.
 * Point that at [MyantsTheme.radius.md] so those corners match the 6dp selection outline.
 */
internal fun UiSurface.applyControlRadius() {
    sizes = sizes.copy(smallGap = MyantsTheme.radius.md)
}
