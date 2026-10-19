package com.neojou.myants.sim

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

class Obstacle(val x: Double, val y: Double, val radius: Double)

class Terrain {
    val obstacles = ArrayList<Obstacle>()
    val clockStone: Obstacle

    init {
        clockStone = Obstacle(SimConfig.width / 2.0, SimConfig.height / 2.0, SimConfig.clock.radius)
        generate()
    }

    fun generate() {
        val count = 8
        for (i in 0 until count) {
            var x = 0.0
            var y = 0.0
            var r = 0.0
            var valid = false
            var attempts = 0
            while (!valid && attempts < 100) {
                x = Rng.rand() * SimConfig.width
                y = Rng.rand() * SimConfig.height
                r = 20 + Rng.rand() * 30
                val entranceX = SimConfig.width.toDouble()
                val entranceY = SimConfig.height / 2.0
                val dx = x - entranceX
                val dy = y - entranceY
                val clearEntrance = dx * dx + dy * dy > 40000
                val minDist = SimConfig.clock.radius + r + 15
                val cx = x - SimConfig.width / 2.0
                val cy = y - SimConfig.height / 2.0
                val clearClock = cx * cx + cy * cy > minDist * minDist
                if (clearEntrance && clearClock) valid = true
                attempts++
            }
            if (valid) obstacles.add(Obstacle(x, y, r))
        }
    }

    fun isBlocked(x: Double, y: Double, buffer: Double = 0.0): Boolean {
        if (x < buffer || x >= SimConfig.width - buffer || y < buffer || y >= SimConfig.height - buffer) return true
        val cdx = x - clockStone.x
        val cdy = y - clockStone.y
        val cr = clockStone.radius + buffer
        if (cdx * cdx + cdy * cdy < cr * cr) return true
        for (obs in obstacles) {
            val dx = x - obs.x
            val dy = y - obs.y
            val rad = obs.radius + buffer
            if (dx * dx + dy * dy < rad * rad) return true
        }
        return false
    }

    fun getCollisionAngle(x: Double, y: Double, currentAngle: Double): Double {
        run {
            val dx = x - clockStone.x
            val dy = y - clockStone.y
            val distSq = dx * dx + dy * dy
            val checkRad = clockStone.radius + 2
            if (distSq < checkRad * checkRad) {
                return tangent(dx, dy, currentAngle)
            }
        }
        for (obs in obstacles) {
            val dx = x - obs.x
            val dy = y - obs.y
            val distSq = dx * dx + dy * dy
            val checkRad = obs.radius + 2
            if (distSq < checkRad * checkRad) {
                return tangent(dx, dy, currentAngle)
            }
        }
        return currentAngle + PI + (Rng.rand() - 0.5) * 1.5
    }

    private fun tangent(dx: Double, dy: Double, currentAngle: Double): Double {
        val normalAngle = atan2(dy, dx)
        val t1 = normalAngle + PI / 2
        val t2 = normalAngle - PI / 2
        val diff1 = abs(normalizeAngle(t1 - currentAngle))
        val diff2 = abs(normalizeAngle(t2 - currentAngle))
        return if (diff1 < diff2) t1 else t2
    }

    fun normalizeAngle(a: Double): Double {
        var v = a
        while (v > PI) v -= PI * 2
        while (v < -PI) v += PI * 2
        return v
    }
}
