package com.neojou.myants.render3d.quality

import com.neojou.myants.sim.QualityLevel

enum class PipelineKind { FORWARD, DEFERRED }

enum class ShadowKind { OFF, MAP_1024, MAP_2048, MAP_4096 }

enum class IblKind { GRADIENT, BAKED, SKY_SLOW, SKY_FAST }

/**
 * Render-only knobs. [QualityLevel] still owns the sim caps in [com.neojou.myants.sim.PerformanceManager].
 * [tierBias] drops grass, shadows, bloom and resolution without changing the sim level.
 */
data class QualityPreset(
    val level: QualityLevel,
    val pipeline: PipelineKind,
    val renderResolution: Float,
    val shadow: ShadowKind,
    val ssaoScale: Float?,
    val bloomRequested: Boolean,
    val ibl: IblKind,
    val iblSeconds: Float,
    val foodLights: Int,
    val fineGrass: Int,
    val grassSway: Boolean,
    val grassCastsShadow: Boolean,
    val pebbles: Int,
    val leaves: Int,
    val flowers: Int,
    val poseBuckets: Int,
    val legAnimation: Boolean,
    val rainStreaks: Int,
    val nestScale: Float,
    val nestAo: Boolean,
    val nestMsaa: Int,
) {
    val pipeKey: String
        get() = "$pipeline|$shadow|$renderResolution|$ssaoScale|$bloomRequested|$ibl"

    companion object {
        fun from(level: QualityLevel, tierBias: Int = 0): QualityPreset {
            val shifted = shift(level, tierBias)
            return table(shifted)
        }

        fun shift(level: QualityLevel, bias: Int): QualityLevel {
            if (bias <= 0) return level
            val entries = QualityLevel.entries
            val index = (level.ordinal - bias).coerceAtLeast(0)
            return entries[index]
        }

        private fun table(level: QualityLevel): QualityPreset = when (level) {
            QualityLevel.ULTRA_LOW -> QualityPreset(
                level, PipelineKind.FORWARD, 0.6f, ShadowKind.OFF, null, false,
                IblKind.GRADIENT, 0f, 0, 0, false, false,
                300, 0, 30, 1, false, 500, 0.5f, false, 1,
            )
            QualityLevel.LOW -> QualityPreset(
                level, PipelineKind.FORWARD, 0.75f, ShadowKind.MAP_1024, null, false,
                IblKind.GRADIENT, 0f, 0, 2000, false, false,
                800, 100, 60, 1, true, 1000, 0.75f, false, 2,
            )
            QualityLevel.MEDIUM -> QualityPreset(
                level, PipelineKind.DEFERRED, 1f, ShadowKind.MAP_2048, 0.5f, true,
                IblKind.SKY_SLOW, 2f, 16, 6000, true, false,
                2000, 300, 120, 4, true, 3000, 1f, false, 4,
            )
            QualityLevel.HIGH -> QualityPreset(
                level, PipelineKind.DEFERRED, 1f, ShadowKind.MAP_2048, 0.75f, true,
                IblKind.SKY_SLOW, 2f, 32, 12000, true, false,
                4000, 600, 200, 6, true, 5000, 1f, false, 4,
            )
            QualityLevel.ULTRA -> QualityPreset(
                level, PipelineKind.DEFERRED, 1f, ShadowKind.MAP_4096, 1f, true,
                IblKind.SKY_FAST, 0.5f, 64, 20000, true, true,
                5000, 1000, 300, 9, true, 8000, 1f, true, 4,
            )
        }
    }
}
