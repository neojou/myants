package com.neojou.myants.session

import com.neojou.myants.AnalyseLine
import com.neojou.myants.sim.AntPlace
import com.neojou.myants.sim.Insect
import com.neojou.myants.sim.InsectKind
import com.neojou.myants.sim.SimConfig
import com.neojou.myants.sim.World

/** `--shot` presets: time of day, rain, close-up zoom. Moved verbatim from AppSession (#7 step 3). */
object ShotPose {
    /**
     * `specimen` (#76): fixed composition for headless measurement. Noon light, zoom 6, one ant and one
     * ladybug at fixed world points around the camera centre. Returns true when the sim must start paused
     * so the composition stays frozen. `specimenCritter=beetle` (#81) swaps the ladybug for a beetle at the same spot. Only runs when `shot=specimen`; normal runs never reach it.
     */
    const val SPECIMEN_X = 420.0
    const val SPECIMEN_Y = 300.0
    const val SPECIMEN_GAP = 9.0

    fun apply(shot: String?, world: World, camera: CameraState, critter: InsectKind = InsectKind.LADYBUG): Boolean {
        if (shot == "specimen") {
            world.age = (world.dayLength * 0.45).toInt()
            world.timeOfDay = 0.45
            world.raining = false
            camera.x = SPECIMEN_X
            camera.y = SPECIMEN_Y
            camera.zoom = 6.0
            val ant = world.ants.firstOrNull { it.location == AntPlace.WORLD } ?: world.ants.firstOrNull()
            if (ant != null) {
                ant.location = AntPlace.WORLD
                ant.x = SPECIMEN_X - SPECIMEN_GAP
                ant.y = SPECIMEN_Y
                ant.angle = 0.0
            }
            val bug = Insect(SPECIMEN_X + SPECIMEN_GAP, SPECIMEN_Y, critter)
            bug.angle = kotlin.math.PI
            world.insects.add(bug)
            return true
        }
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
        return false
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
