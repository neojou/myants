package com.neojou.myants.input

import com.neojou.myants.AppSession
import com.neojou.myants.SimTool
import de.fabmax.kool.input.KeyboardInput
import de.fabmax.kool.input.UniversalKeyCode

/**
 * Toolbar shortcuts. `[` and `]` are not here: AntSim has neither, and #11 only asked to evaluate them.
 *
 * `+` and `-` change speed (#11). AntSim used those characters to zoom. `=` and `_` still zoom.
 * Listeners use the typed character, not the physical key. On desktop a key-down code is the
 * unshifted GLFW key, so `+` and `_` never arrive as key-down. There is no text field, so typed
 * characters are not being entered anywhere else.
 */
enum class Shortcut {
    PAUSE,
    STEP,
    SLOWER,
    FASTER,
    ZOOM_IN,
    ZOOM_OUT,
    TOOL_INSPECT,
    TOOL_SUGAR,
    TOOL_PROTEIN,
    TOOL_ENEMY,
}

fun shortcutFor(ch: Char): Shortcut? = when (ch) {
    ' ' -> Shortcut.PAUSE
    '.' -> Shortcut.STEP
    '-' -> Shortcut.SLOWER
    '+' -> Shortcut.FASTER
    '=' -> Shortcut.ZOOM_IN
    '_' -> Shortcut.ZOOM_OUT
    '1' -> Shortcut.TOOL_INSPECT
    '2' -> Shortcut.TOOL_SUGAR
    '3' -> Shortcut.TOOL_PROTEIN
    '4' -> Shortcut.TOOL_ENEMY
    else -> null
}

fun AppSession.applyShortcut(shortcut: Shortcut) {
    when (shortcut) {
        Shortcut.PAUSE -> togglePause()
        Shortcut.STEP -> stepOnce()
        Shortcut.SLOWER -> stepSpeed(-1)
        Shortcut.FASTER -> stepSpeed(1)
        Shortcut.ZOOM_IN -> zoomBy(1.25)
        Shortcut.ZOOM_OUT -> zoomBy(1.0 / 1.25)
        Shortcut.TOOL_INSPECT -> selectTool(SimTool.INSPECT)
        Shortcut.TOOL_SUGAR -> selectTool(SimTool.SUGAR)
        Shortcut.TOOL_PROTEIN -> selectTool(SimTool.PROTEIN)
        Shortcut.TOOL_ENEMY -> selectTool(SimTool.ENEMY)
    }
}

private const val BOUND = " +-=_1234."

fun installShortcuts(session: AppSession) {
    for (ch in BOUND) {
        val action = shortcutFor(ch) ?: continue
        val name = "shortcut-$ch"
        KeyboardInput.addKeyListener(
            UniversalKeyCode(KeyboardInput.getKeyCodeForChar(ch), name),
            name,
            { ev ->
                ev.isCharTyped && !ev.isRepeated && !ev.isCtrlDown && !ev.isAltDown && !ev.isSuperDown
            },
        ) { session.applyShortcut(action) }
    }
}
