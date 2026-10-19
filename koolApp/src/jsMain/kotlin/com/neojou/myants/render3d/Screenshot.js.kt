package com.neojou.myants.render3d

import com.neojou.tools.LogLevel
import com.neojou.tools.MyLog
import de.fabmax.kool.pipeline.BufferedImageData2d

actual fun saveScreenshotPng(image: BufferedImageData2d, path: String): Boolean {
    MyLog.add("APP", "PNG encoder is not available in this build ($path ${image.width}x${image.height})", LogLevel.DEBUG)
    return false
}
