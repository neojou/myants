package com.neojou.myants

import java.io.File

actual fun wallClockSeed(): Int = (System.currentTimeMillis() and 0xffffffffL).toInt()

actual fun wallClockHm(): Pair<Int, Int> {
    val t = java.time.LocalTime.now()
    return t.hour to t.minute
}

actual fun wallClockHms(): Triple<Int, Int, Int> {
    val t = java.time.LocalTime.now()
    return Triple(t.hour, t.minute, t.second)
}

actual class SettingsStore actual constructor() {
    private val file = File(System.getProperty("user.home"), ".myants/antsim.ui.v1.txt")

    actual fun read(): String? = if (file.isFile) file.readText() else null

    actual fun write(text: String) {
        file.parentFile?.mkdirs()
        file.writeText(text)
    }
}
