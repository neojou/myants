package com.neojou.myants.sim

data class PerformanceProfile(
    val maxAnts: Int,
    val particleLimit: Int,
    val simpleInsects: Boolean,
    val simpleAnts: Boolean,
    val pheromoneUpdateSkip: Int,
    val legAnimation: Boolean,
    val grassAnimation: Boolean,
)

object PerformanceManager {
    var level: QualityLevel = QualityLevel.MEDIUM
    var settings: PerformanceProfile = profile(QualityLevel.MEDIUM)

    fun profile(level: QualityLevel): PerformanceProfile = when (level) {
        QualityLevel.ULTRA_LOW -> PerformanceProfile(80, 20, true, true, 6, false, false)
        QualityLevel.LOW -> PerformanceProfile(120, 40, true, true, 6, false, false)
        QualityLevel.MEDIUM -> PerformanceProfile(300, 100, false, false, 4, false, false)
        QualityLevel.HIGH -> PerformanceProfile(600, 200, false, false, 2, true, false)
        QualityLevel.ULTRA -> PerformanceProfile(1200, 2000, false, false, 2, true, true)
    }

    fun setQuality(next: QualityLevel) {
        level = next
        settings = profile(next)
    }

    private var lowFpsFrames: Int = 0
    private var lastDowngradeMs: Long = 0

    fun monitorFps(fps: Double, nowMs: Long) {
        if (fps < 20.0) {
            lowFpsFrames++
            if (lowFpsFrames > 60 && nowMs - lastDowngradeMs > 10_000) {
                downgrade()
                lastDowngradeMs = nowMs
                lowFpsFrames = 0
            }
        } else {
            lowFpsFrames = 0
        }
    }

    fun downgrade() {
        val next = when (level) {
            QualityLevel.ULTRA -> QualityLevel.HIGH
            QualityLevel.HIGH -> QualityLevel.MEDIUM
            QualityLevel.MEDIUM -> QualityLevel.LOW
            QualityLevel.LOW, QualityLevel.ULTRA_LOW -> return
        }
        setQuality(next)
    }
}
