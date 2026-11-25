package com.neojou.myants.ui

import de.fabmax.kool.util.Color

/** Resolved look of a [TextButton] for a given state (pure, unit-tested). STUB (red commit). */
data class TextButtonVisual(
    val background: Color?,
    val foreground: Color,
    val border: Color,
    val clickable: Boolean,
)

fun textButtonVisual(
    selected: Boolean,
    enabled: Boolean,
    danger: Boolean,
    hovered: Boolean,
    pressed: Boolean,
): TextButtonVisual = TextButtonVisual(null, Color("#000000"), Color("#000000"), true)
