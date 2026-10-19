package com.neojou.myants.render3d.actors

import kotlin.math.abs

/**
 * Pose buckets keep skinned instances from sharing one phase.
 * Procedural legs use the same index: each bucket is a fixed gait phase.
 */
object PoseBuckets {
    private const val GOLDEN = 0.618034f

    fun index(phaseKey: Float, speed: Float, bucketCount: Int): Int {
        val count = bucketCount.coerceAtLeast(1)
        if (count == 1) return 0
        val phase = fract(abs(phaseKey) * GOLDEN)
        val bands = 3.coerceAtMost(count)
        val phases = (count / bands).coerceAtLeast(1)
        val band = when {
            speed < 0.35f -> 0
            speed < 1.2f -> 1.coerceAtMost(bands - 1)
            else -> (bands - 1)
        }
        val slot = (phase * phases).toInt().coerceIn(0, phases - 1)
        return (band * phases + slot).coerceIn(0, count - 1)
    }

    /** 0..1 gait phase for a bucket, plus a little time so the legs keep moving. */
    fun phase(bucket: Int, bucketCount: Int, time: Float, moving: Boolean): Float {
        val count = bucketCount.coerceAtLeast(1)
        val base = (bucket % count).toFloat() / count
        if (!moving) return base
        return fract(base + time * 1.4f)
    }

    private fun fract(v: Float): Float {
        val i = v.toInt()
        val f = v - i
        return if (f < 0f) f + 1f else f
    }
}
