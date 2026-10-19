package com.neojou.myants.render3d

import com.neojou.tools.LogLevel
import com.neojou.tools.MyLog
import de.fabmax.kool.pipeline.BufferedImageData2d
import de.fabmax.kool.util.Uint8Buffer
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

actual fun saveScreenshotPng(image: BufferedImageData2d, path: String): Boolean {
    val data = image.data as? Uint8Buffer ?: run {
        MyLog.add("APP", "PNG encoder is not available in this build", LogLevel.DEBUG)
        return false
    }
    return try {
        val out = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until image.height) {
            val src = y * image.width * 4
            for (x in 0 until image.width) {
                val i = src + x * 4
                val r = data[i].toInt() and 255
                val g = data[i + 1].toInt() and 255
                val b = data[i + 2].toInt() and 255
                val a = data[i + 3].toInt() and 255
                out.setRGB(x, y, (a shl 24) or (r shl 16) or (g shl 8) or b)
            }
        }
        val file = File(path)
        file.parentFile?.mkdirs()
        ImageIO.write(out, "png", file)
        true
    } catch (err: Throwable) {
        MyLog.add("APP", "PNG encoder is not available in this build (${err.message})", LogLevel.DEBUG)
        false
    }
}
