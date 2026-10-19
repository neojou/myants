package com.neojou.myants

import kotlinx.browser.localStorage
import kotlinx.browser.window

actual fun wallClockSeed(): Int = kotlin.js.Date.now().toInt()

actual fun wallClockHm(): Pair<Int, Int> {
    val d = kotlin.js.Date()
    return d.getHours() to d.getMinutes()
}

actual class SettingsStore actual constructor() {
    actual fun read(): String? = try {
        localStorage.getItem(KEY)
    } catch (_: Throwable) {
        null
    }

    actual fun write(text: String) {
        try {
            localStorage.setItem(KEY, text)
        } catch (_: Throwable) {
            // private mode
        }
    }

    private companion object {
        const val KEY = "antsim.ui.v1"
    }
}

fun browserLaunch(): LaunchConfig {
    val search = window.location.search.removePrefix("?")
    if (search.isEmpty()) return LaunchConfig()
    return parseLaunch(search.split("&"))
}
