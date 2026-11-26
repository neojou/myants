package com.neojou.myants.ui

import de.fabmax.kool.math.Easing
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color

/** Resolved look of a [TextButton] for a given state (pure, unit-tested). Opaque fills, per ART_DIRECTION §3. */
data class TextButtonVisual(
    /** Opaque fill, or null when disabled (no fill). */
    val background: Color?,
    val foreground: Color,
    /** 1dp border. May carry alpha: it is drawn over [background]. */
    val border: Color,
    val clickable: Boolean,
)

/** Danger border: [MyantsTheme.colors.onDanger] at α0.55 (#B06E6B over the danger fill, 3.30:1). */
private const val DANGER_BORDER_ALPHA = 0.55f

fun textButtonVisual(
    selected: Boolean,
    enabled: Boolean,
    danger: Boolean,
    hovered: Boolean,
    pressed: Boolean,
): TextButtonVisual {
    val c = MyantsTheme.colors
    val rest = if (danger) c.danger else c.surfaceVariant
    val bg = when {
        selected && pressed && enabled -> c.primaryVariant
        selected -> c.primary
        !enabled -> null
        pressed -> MyantsTheme.over(c.primaryPressed, rest)
        hovered -> MyantsTheme.over(c.primaryHover, rest)
        else -> rest
    }
    val fg = when {
        !enabled -> c.onSurfaceDisabled
        selected -> c.primaryContainer
        danger -> c.onDanger
        else -> c.onSurface
    }
    val border = when {
        !enabled -> c.outline
        selected -> c.primaryContainer
        danger -> c.onDanger.withAlpha(DANGER_BORDER_ALPHA)
        else -> c.outlineStrong
    }
    return TextButtonVisual(bg, fg, border, clickable = enabled)
}

/**
 * Text button for HUD, panels and dialogs (#87). Same state rules as [IconButton]: no kool [Button] cyan, no click ripple.
 *
 * kool 0.19 has no layout scale, so the press animation shrinks the button face (fill + border) to 0.96 over 120 ms.
 * The outer box keeps the measured size so neighbours do not move. The label itself keeps its size.
 *
 * @param layout runs on the outer box: margins, alignment, `width(Grow.Std)`.
 * @param fillWidth the face follows a `Grow` outer width instead of hugging the label.
 */
fun UiScope.TextButton(
    text: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    danger: Boolean = false,
    fillWidth: Boolean = false,
    layout: UiScope.() -> Unit = {},
    onClick: () -> Unit,
) {
    Box {
        layout()
        val hovered = remember { mutableStateOf(false) }
        val pressed = remember { mutableStateOf(false) }
        val pressedNow = pressed.use()
        val v = textButtonVisual(selected, enabled, danger, hovered.use(), pressedNow)
        val pressAnim = remember { FloatAnimator(MyantsTheme.motion.pressSeconds, Easing.smooth, initial = 1f) }
        val lastTarget = remember { floatArrayOf(1f) }
        val target = iconPressScale(pressedNow, enabled)
        if (lastTarget[0] != target) {
            lastTarget[0] = target
            pressAnim.start(target)
        }
        pressAnim.animatable.use()
        val scale = pressAnim.update()
        // Face size at rest, taken from the last layout on the first composition of a press, kept until the release finishes.
        val held = remember { floatArrayOf(0f, 0f, 0f) }
        if (scale == 1f && !pressedNow) {
            held[0] = 0f
        } else if (held[0] == 0f) {
            held[0] = 1f
            held[1] = uiNode.widthPx
            held[2] = uiNode.heightPx
        }
        val scaling = held[0] == 1f && held[1] > 0f && held[2] > 0f
        if (scaling) modifier.size(Dp.fromPx(held[1]), Dp.fromPx(held[2]))
        modifier
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
            if (scaling) {
                modifier
                    .size(Dp.fromPx(held[1] * scale), Dp.fromPx(held[2] * scale))
                    .align(AlignmentX.Center, AlignmentY.Center)
            } else if (fillWidth) {
                modifier.width(Grow.Std)
            }
            modifier
                .padding(horizontal = sizes.gap, vertical = sizes.smallGap)
                .background(v.background?.let { RoundRectBackground(it, MyantsTheme.radius.md) })
                .border(RoundRectBorder(v.border, MyantsTheme.radius.md, MyantsTheme.stroke.selected))
            Text(text) {
                modifier.align(AlignmentX.Center, AlignmentY.Center).textColor(v.foreground)
            }
        }
    }
}
