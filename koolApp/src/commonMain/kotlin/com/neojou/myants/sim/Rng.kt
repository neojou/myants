package com.neojou.myants.sim

/**
 * mulberry32, bit-compatible with AntSim_V2 `src/rng.ts` (commit cefa083).
 * One global stream. Call order is part of the simulation.
 */
object Rng {
    private var state: Int = 0x9e3779b9.toInt()

    fun seed(seed: Int) {
        state = seed
        if (state == 0) state = 0x9e3779b9.toInt()
    }

    fun getState(): Int = state

    fun setState(value: Int) {
        state = value
    }

    /** Uniform in [0, 1). Same uint32 step as JS `Math.imul` + `>>> 0`. */
    fun rand(): Double {
        state += 0x6D2B79F5
        var t = (state xor (state ushr 15)) * (1 or state)
        t = (t + ((t xor (t ushr 7)) * (61 or t))) xor t
        val bits = (t xor (t ushr 14)).toLong() and 0xFFFFFFFFL
        return bits.toDouble() / 4294967296.0
    }

    /** The uint32 numerator of [rand], for tests. Advances state. */
    fun nextUint32(): Long {
        state += 0x6D2B79F5
        var t = (state xor (state ushr 15)) * (1 or state)
        t = (t + ((t xor (t ushr 7)) * (61 or t))) xor t
        return (t xor (t ushr 14)).toLong() and 0xFFFFFFFFL
    }
}
