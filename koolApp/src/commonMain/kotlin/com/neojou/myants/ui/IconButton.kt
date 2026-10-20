package com.neojou.myants.ui

import com.neojou.tools.LogLevel
import com.neojou.tools.MyLog
import de.fabmax.kool.Assets
import de.fabmax.kool.loadTexture2d
import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.pipeline.Texture2d
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Async-loaded HUD icon textures. Missing/failed assets stay absent and the button falls back to text. */
object HudIcons {
    private const val TAG = "HudIcons"
    private const val RASTER_PX = 54 // 18dp x 3
    private val textures = mutableMapOf<String, Texture2d>()
    /** Bumped whenever a texture finishes loading, so UI recomposes. */
    val loadedCount = mutableStateOf(0)
    var lang: UiLang = UiLang.DE

    fun texture(path: String): Texture2d? = textures[path]

    fun loadAll(scope: CoroutineScope) {
        for (path in HudIcon.allAssetPaths()) {
            scope.launch {
                Assets.defaultLoader.loadTexture2d(path, resolveSize = Vec2i(RASTER_PX, RASTER_PX))
                    .onSuccess {
                        textures[path] = it
                        loadedCount.set(loadedCount.value + 1)
                    }
                    .onFailure { err ->
                        MyLog.add(TAG, "Icon $path missing: ${err.message}. Using text label.", LogLevel.WARN)
                    }
            }
        }
    }
}

/**
 * 28x28dp icon button with tooltip (spec #5 §4/§5). Colors come from [MyantsTheme].
 * Falls back to the ASCII label when the icon texture is not loaded.
 */
fun UiScope.IconButton(
    icon: HudIcon,
    selected: Boolean = false,
    enabled: Boolean = true,
    startMargin: Dp = Dp(0f),
    onClick: () -> Unit,
) {
    val c = MyantsTheme.colors
    HudIcons.loadedCount.use()
    val tex = HudIcons.texture(icon.assetPath(filled = selected))
    Box {
        val hovered = remember { mutableStateOf(false) }
        val bg = when {
            selected -> c.primary
            enabled && hovered.use() -> c.primaryHover
            else -> null
        }
        val fg = when {
            !enabled -> c.onSurfaceDisabled
            selected -> c.primaryContainer
            else -> c.onSurface
        }
        modifier
            .size(MyantsTheme.size.button, MyantsTheme.size.button)
            .margin(start = startMargin)
            .alignY(AlignmentY.Center)
            .onEnter { hovered.set(true) }
            .onExit { hovered.set(false) }
            .onClick { if (enabled) onClick() }
        modifier.background(bg?.let { RoundRectBackground(it, MyantsTheme.radius.md) })
        if (selected) modifier.border(RoundRectBorder(c.primaryContainer, MyantsTheme.radius.md, MyantsTheme.stroke.selected))
        if (tex != null) {
            Image(tex) {
                modifier
                    .size(MyantsTheme.size.icon, MyantsTheme.size.icon)
                    .align(AlignmentX.Center, AlignmentY.Center)
                    .imageSize(ImageSize.Stretch)
                    .tint(fg)
            }
        } else {
            Text(icon.label(false)) {
                modifier.align(AlignmentX.Center, AlignmentY.Center).textColor(fg)
            }
        }
        Tooltip(
            icon.tooltip(HudIcons.lang, disabled = !enabled),
            tooltipState = remember { TooltipState(MyantsTheme.motion.tooltipDelaySec) },
            backgroundColor = c.surfaceRaised,
            borderColor = c.outline,
        )
    }
}
