package com.neojou.myants.ui

import com.neojou.tools.LogLevel
import com.neojou.tools.MyLog
import de.fabmax.kool.modules.ui2.Sizes
import de.fabmax.kool.modules.ui2.UiScope
import de.fabmax.kool.modules.ui2.mutableStateOf
import de.fabmax.kool.util.MsdfFont
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Latin UI stays on Fira. Traditional Chinese and Japanese use a small Noto Sans CJK atlas.
 * The atlas is only the glyphs the interface actually draws.
 */
object UiFonts {
    const val CJK_PATH = "fonts/noto-sans-cjk"

    val cjk = mutableStateOf<MsdfFont?>(null)

    private var digits: MsdfFont? = null
    private var latinSizes: Sizes? = null
    private var cjkSizes: Sizes? = null
    private var cjkFace: MsdfFont? = null

    /** Fira at the medium body size, so HUD digit slots keep their 11dp width. */
    fun firaDigits(): MsdfFont = digits ?: MsdfFont(sizePts = 15f).also { digits = it }

    fun load(scope: CoroutineScope) {
        scope.launch {
            val loaded = MsdfFont(CJK_PATH).getOrNull()
            if (loaded == null) {
                MyLog.add("UI", "CJK font missing: $CJK_PATH", LogLevel.WARN)
            } else {
                cjk.value = loaded
            }
        }
    }

    internal fun sizesFor(face: MsdfFont?, useCjk: Boolean): Sizes {
        if (useCjk && face != null) {
            if (cjkSizes == null || cjkFace != face) {
                cjkFace = face
                cjkSizes = Sizes.medium(
                    smallText = face.derive(12f),
                    normalText = face.derive(15f),
                    largeText = face.derive(20f),
                ).copy(smallGap = MyantsTheme.radius.md)
            }
            return cjkSizes!!
        }
        return latinSizes ?: Sizes.medium.copy(smallGap = MyantsTheme.radius.md).also { latinSizes = it }
    }
}

/**
 * Call at the start of a surface compose, before children read [de.fabmax.kool.modules.ui2.UiScope.sizes].
 * [forceCjk] keeps the language menu's endonyms readable while the rest of the UI is still English.
 */
internal fun UiScope.installLanguageFont(forceCjk: Boolean = false) {
    val lang = L10n.language.use()
    val face = UiFonts.cjk.use()
    val useCjk = face != null && (forceCjk || lang == UiLanguage.ZH_TW || lang == UiLanguage.JA)
    val next = UiFonts.sizesFor(face, useCjk)
    if (surface.sizes != next) surface.sizes = next
}

/** Every character the interface can draw, including ASCII that stays on screen in Chinese and Japanese. */
internal fun interfaceGlyphs(): String {
    val chars = LinkedHashSet<Char>()
    fun add(text: String) {
        text.forEach { chars += it }
    }
    for (key in UiText.entries) {
        for (lang in UiLanguage.entries) add(L10n.string(key, lang))
    }
    add(InspectCopy.glyphChars())
    for (choice in MyantsTheme.Data.antColorChoices) {
        for (lang in UiLanguage.entries) add(choice.name(lang))
    }
    for (lang in UiLanguage.entries) add(lang.endonym)
    add("Based on AntSim_V2 by nilsgollub (MIT)")
    add("3D")
    add("SSAO")
    for (cp in 0x20..0x7E) chars += cp.toChar()
    chars += '→'
    chars += '·'
    return chars.joinToString("")
}
