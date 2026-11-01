package com.neojou.myants.session

/**
 * Simulation timing: pause, speed steps and the fixed 60 Hz accumulator.
 * Moved verbatim from AppSession (#7); behaviour must stay identical.
 */
class SimClock {
    val speeds = doubleArrayOf(0.0, 0.5, 1.0, 2.0, 4.0, 8.0)
    var speedIndex: Int = 2
    var paused: Boolean = false
    /**
     * Seconds of real time for render animation (leg cycles, grass sway, rain) (#83).
     * Keeps running while paused, like kool's wall clock did; only [animFrozen] stops it.
     */
    var animTime: Double = 0.0
        private set
    /** Set by specimen shots (#83) so the frame stays the same no matter how much wall time passes. */
    var animFrozen: Boolean = false
    private var accum: Double = 0.0
    private var halfTick: Boolean = false

    val speed: Double get() = speeds[speedIndex]

    /** Restores a saved speed value; unknown values are ignored. */
    fun restoreSpeed(spd: Double) {
        val idx = speeds.indexOfFirst { kotlin.math.abs(it - spd) < 1e-6 }
        if (idx >= 0) speedIndex = idx
    }

    fun togglePause() {
        paused = !paused
    }

    fun stepSpeed(dir: Int) {
        speedIndex = (speedIndex + dir).coerceIn(0, speeds.lastIndex)
    }

    /**
     * Advances real time by [dt] seconds and calls [stepSim] for every simulation tick due.
     * Ticks nothing while paused (animation time still runs unless [animFrozen]). At most 64 fixed frames per call; leftover time is capped at one frame.
     */
    fun advance(dt: Double, stepSim: () -> Unit) {
        if (!animFrozen) animTime += dt
        if (paused) return
        accum += dt
        val step = 1.0 / 60.0
        var frames = 0
        while (accum >= step && frames < 64) {
            accum -= step
            frames++
            val spd = speeds[speedIndex]
            if (spd == 0.0) continue
            if (spd < 1.0) {
                halfTick = !halfTick
                if (halfTick) stepSim()
            } else {
                repeat(spd.toInt()) { stepSim() }
            }
        }
        if (accum > step) accum = step
    }
}
