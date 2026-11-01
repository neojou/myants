package com.neojou.myants.render3d

import de.fabmax.kool.pipeline.BufferedImageData2d
import de.fabmax.kool.pipeline.MipMapping
import de.fabmax.kool.pipeline.SamplerSettings
import de.fabmax.kool.pipeline.TexFormat
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.util.Uint8Buffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Seeded SSAO rotation noise (#83). kool 0.19's AmbientOcclusionPass builds its 4x4 noise texture with an
 * unseeded `shuffled()`, so every page load got different AO grain (QE saw it on the clover at the bottom).
 * Same layout as kool's, only the shuffle is fixed, so the same seed and view give the same pixels.
 */
internal object AoNoise {
    const val SIZE = 4

    /** Rotation angle per texel: kool's 16 even steps, shuffled with a fixed seed. */
    fun angles(): FloatArray {
        val n = SIZE * SIZE
        return (0 until n).map { 2f * PI.toFloat() * it / n }.shuffled(Random(0x83A0)).toFloatArray()
    }

    fun texture(): Texture2d {
        val ang = angles()
        val buf = Uint8Buffer(4 * ang.size)
        for (i in ang.indices) {
            buf[i * 4 + 0] = ((cos(ang[i]) * 0.5f + 0.5f) * 255).toInt().toUByte()
            buf[i * 4 + 1] = ((sin(ang[i]) * 0.5f + 0.5f) * 255).toInt().toUByte()
            buf[i * 4 + 2] = 0u
            buf[i * 4 + 3] = 1u
        }
        val data = BufferedImageData2d(buf, SIZE, SIZE, TexFormat.RGBA)
        return Texture2d(TexFormat.RGBA, MipMapping.Off, SamplerSettings().nearest(), "myants_ao_noise") { data }
    }
}
