package com.neojou.myants.ui

import com.neojou.tools.LogLevel
import com.neojou.tools.MyLog
import de.fabmax.kool.Assets
import de.fabmax.kool.loadTexture2d
import de.fabmax.kool.math.Easing
import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.util.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Async-loaded HUD icon textures. Missing/failed assets stay absent and the button falls back to text. */
object HudIcons {
    private const val TAG = "HudIcons"
    private const val RASTER_PX = 54 // 18dp x 3
    private val textures = mutableMapOf<String, Texture2d>()
    /** Bumped whenever a texture finishes loading, so UI recomposes. */
    val loadedCount = mutableStateOf(0)

    fun texture(path: String): Texture2d? = textures[path]

    fun loadAll(scope: CoroutineScope) {
        for (path in HudIcon.allAssetPaths()) {
            scope.launch {
                loadIcon(
                    path,
                    loader = { Assets.defaultLoader.loadTexture2d(it, resolveSize = Vec2i(RASTER_PX, RASTER_PX)) },
                    onLoaded = { p, tex ->
                        textures[p] = tex
                        loadedCount.set(loadedCount.value + 1)
                    },
                )
            }
        }
    }

    /**
     * Loads one icon; on failure writes a WARN via [warn] and never throws, so the button keeps its text fallback.
     * @return true when the icon was loaded.
     */
    suspend fun <T> loadIcon(
        path: String,
        loader: suspend (String) -> Result<T>,
        onLoaded: (String, T) -> Unit,
        warn: (String) -> Unit = { MyLog.add(TAG, it, LogLevel.WARN) },
    ): Boolean {
        val result = try { loader(path) } catch (err: Throwable) { Result.failure(err) }
        result.onSuccess { onLoaded(path, it) }
            .onFailure { err -> warn("Icon $path missing: ${err.message}. Using text label.") }
        return result.isSuccess
    }
}

/** Resolved look of an [IconButton] for a given state (pure, unit-tested). */
data class IconButtonVisual(
    /** Icon asset to draw, or null when the texture is not loaded and [text] is shown instead. */
    val assetPath: String?,
    val text: String,
    val background: Color?,
    val foreground: Color,
    val outlined: Boolean,
    val clickable: Boolean,
)

fun iconButtonVisual(
    icon: HudIcon,
    selected: Boolean,
    enabled: Boolean,
    hovered: Boolean,
    pressed: Boolean,
    isLoaded: (String) -> Boolean,
): IconButtonVisual {
    val c = MyantsTheme.colors
    val path = icon.assetPath(filled = selected)
    val loaded = isLoaded(path)
    val bg = when {
        selected && pressed && enabled -> c.primaryVariant
        selected -> c.primary
        !enabled -> null
        pressed -> c.primaryPressed
        hovered -> c.primaryHover
        else -> null
    }
    val fg = when {
        !enabled -> c.onSurfaceDisabled
        selected -> c.primaryContainer
        else -> c.onSurface
    }
    return IconButtonVisual(
        assetPath = if (loaded) path else null,
        text = icon.label(loaded),
        background = bg,
        foreground = fg,
        outlined = selected,
        clickable = enabled,
    )
}

/** Target content scale. Disabled buttons stay at 1 so a dead control does not dip. */
fun iconPressScale(pressed: Boolean, enabled: Boolean): Float =
    if (pressed && enabled) MyantsTheme.motion.pressScale else 1f

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
    Box {
        val hovered = remember { mutableStateOf(false) }
        val pressed = remember { mutableStateOf(false) }
        val pressedNow = pressed.use()
        val v = iconButtonVisual(icon, selected, enabled, hovered.use(), pressedNow) { HudIcons.texture(it) != null }
        // kool 0.19 has no layout scale. The inner box animates; this 28dp box stays the hit target.
        val pressAnim = remember { FloatAnimator(MyantsTheme.motion.pressSeconds, Easing.smooth, initial = 1f) }
        val lastTarget = remember { floatArrayOf(1f) }
        val target = iconPressScale(pressedNow, enabled)
        if (lastTarget[0] != target) {
            lastTarget[0] = target
            pressAnim.start(target)
        }
        pressAnim.animatable.use()
        val scale = pressAnim.update()
        modifier
            .size(MyantsTheme.size.button, MyantsTheme.size.button)
            .margin(start = startMargin)
            .alignY(AlignmentY.Center)
            .onEnter { hovered.set(true) }
            .onHover { pressed.set(it.pointer.isLeftButtonDown) }
            .onExit {
                hovered.set(false)
                pressed.set(false)
            }
            .onClick {
                pressed.set(false)
                if (v.clickable) onClick()
            }
        Box {
            modifier
                .size(MyantsTheme.size.button * scale, MyantsTheme.size.button * scale)
                .align(AlignmentX.Center, AlignmentY.Center)
                .background(v.background?.let { RoundRectBackground(it, MyantsTheme.radius.md) })
            if (v.outlined) modifier.border(RoundRectBorder(c.primaryContainer, MyantsTheme.radius.md, MyantsTheme.stroke.selected))
            val tex = v.assetPath?.let { HudIcons.texture(it) }
            if (tex != null) {
                Image(tex) {
                    modifier
                        .size(MyantsTheme.size.icon * scale, MyantsTheme.size.icon * scale)
                        .align(AlignmentX.Center, AlignmentY.Center)
                        .imageSize(ImageSize.Stretch)
                        .tint(v.foreground)
                }
            } else {
                Text(v.text) {
                    modifier.align(AlignmentX.Center, AlignmentY.Center).textColor(v.foreground)
                }
            }
        }
        Tooltip(
            icon.tooltip(L10n.language.use(), disabled = !enabled),
            tooltipState = remember { TooltipState(MyantsTheme.motion.tooltipDelaySec) },
            backgroundColor = c.surfaceRaised,
            borderColor = c.outline,
        )
    }
}
