package com.neojou.myants

import de.fabmax.kool.KoolConfigJvm
import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.pipeline.backend.gl.RenderBackendGl
import de.fabmax.kool.pipeline.backend.vk.RenderBackendVk
import de.fabmax.kool.util.GeneratorGlyphSet
import de.fabmax.kool.util.MsdfFontGenerator

/** Feasibility trial only. */
fun trialConfig(useGl: Boolean) = KoolConfigJvm(
    windowTitle = "trial",
    windowSize = Vec2i(1600, 900),
    renderBackend = if (useGl) RenderBackendGl else RenderBackendVk,
)

suspend fun trialWriteCjkFont(ttf: String, out: String) = MsdfFontGenerator.writeMsdfFont(
    inputTtfPath = ttf,
    outputMsdfPath = out,
    fontName = "noto-sans-tc",
    glyphsToGenerate = GeneratorGlyphSet.FromString("族群蛋白質糖蟻后年齡速度圖形重新開始"),
    atlasDim = 1024,
)
