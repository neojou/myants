package com.neojou.myants.session

import com.neojou.myants.AnalyseLine
import com.neojou.myants.sim.SimConfig
import com.neojou.myants.sim.World

/** `--shot` presets: time of day, rain, close-up zoom. Moved verbatim from AppSession (#7 step 3). */
object ShotPose {
    fun apply(shot: String?, world: World, camera: CameraState) {
        when (shot) {
            "day" -> {
                world.age = (world.dayLength * 0.45).toInt()
                world.timeOfDay = 0.45
            }
            "night" -> {
                world.age = (world.dayLength * 0.92).toInt()
                world.timeOfDay = (world.age % world.dayLength).toDouble() / world.dayLength.toDouble()
            }
            "rain" -> {
                world.age = (world.dayLength * 0.45).toInt()
                world.timeOfDay = 0.45
                world.raining = true
                world.rainTimer = 100000
            }
            "ant" -> {
                world.age = (world.dayLength * 0.45).toInt()
                world.timeOfDay = 0.45
                camera.zoom = 6.0
            }
            else -> Unit
        }
    }
}

/** Analyse panel lines from the sim observer. Moved verbatim from AppSession (#7 step 3). */
fun buildAnalyseLines(world: World): List<AnalyseLine> {
    val lines = ArrayList<AnalyseLine>()
    val suggestions = world.observer.analyze()
    if (suggestions.isEmpty()) {
        lines += AnalyseLine("empty", null, false, "", null, 0.0)
    }
    for (s in suggestions) {
        lines += AnalyseLine(s.id, s.severity, false, "${s.severity} | ${s.metric}: ${s.suggestion}", null, 0.0)
        lines += AnalyseLine(s.id, null, true, s.effect, null, 0.0)
        for (a in s.actions) {
            lines += AnalyseLine(null, null, false, a.label, a.path, a.to)
        }
    }
    return lines
}

/** Tuner overrides (`ov:<path>` settings) applied to [SimConfig]. */
class TunerOverrides {
    val values = HashMap<String, Double>()

    /** Applies saved `ov:` keys; unknown paths from older settings files are skipped. */
    fun load(saved: Map<String, String>) {
        for ((key, value) in saved) {
            if (!key.startsWith("ov:")) continue
            val number = value.toDoubleOrNull() ?: continue
            try {
                SimConfig.setNumber(key.removePrefix("ov:"), number)
                values[key.removePrefix("ov:")] = number
            } catch (_: Throwable) {
                // unknown path from an older settings file
            }
        }
    }

    /** @return false when [path] is not a tunable number. */
    fun set(path: String, value: Double): Boolean = try {
        SimConfig.setNumber(path, value)
        values[path] = value
        true
    } catch (_: Throwable) {
        false
    }

    fun reset() {
        values.clear()
        SimConfig.resetTunables()
    }
}

/** Frames-per-second over 1 s windows of game time. */
class FpsCounter {
    private var frames: Int = 0
    private var stamp: Double = 0.0
    var shown: Int = 0
        private set

    /** Counts one frame at [now]; returns true when a new 1 s value was published. */
    fun frame(now: Double): Boolean {
        frames++
        if (stamp == 0.0) stamp = now
        if (now - stamp >= 1.0) {
            shown = frames
            frames = 0
            stamp = now
            return true
        }
        return false
    }
}
