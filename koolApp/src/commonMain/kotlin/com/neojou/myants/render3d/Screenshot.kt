package com.neojou.myants.render3d

import de.fabmax.kool.pipeline.BufferedImageData2d

/** Desktop writes a PNG. JS logs and returns false. */
expect fun saveScreenshotPng(image: BufferedImageData2d, path: String): Boolean
