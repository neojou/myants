package com.neojou.myants.session

import com.neojou.myants.sim.Ant
import com.neojou.myants.sim.AntPlace
import com.neojou.myants.sim.AntState
import com.neojou.myants.sim.CarryKind
import com.neojou.myants.sim.SimConfig
import com.neojou.myants.sim.World
import kotlin.math.cos
import kotlin.math.sin

/**
 * Cinematic auto-camera and user "follow inspected ant". Moved verbatim from AppSession (#7 step 3).
 * Only moves [CameraState]; never writes to the simulation.
 */
class CinemaDirector {
    var pausedUntil: Double = 0.0
    private var targetX: Double = SimConfig.width / 2.0
    private var targetY: Double = SimConfig.height / 2.0
    private var targetZoom: Double = 1.0
    var nextPick: Double = 0.0
    private var phase: Double = 0.0
    /** Ant currently tracked by the cinematic camera. */
    var follow: Ant? = null
    private val shotQueue = ArrayDeque<String>()
    private val random = kotlin.random.Random(1)

    /** User input pauses the cinematic camera for 25 s of game time. */
    fun notifyInput(now: Double) {
        pausedUntil = now + 25.0
    }

    /**
     * Moves the camera towards the inspected ant while the user follow is on.
     * @return false when the ant left the world or died, so the caller turns follow off.
     */
    fun updateUserFollow(camera: CameraState, ant: Ant): Boolean {
        if (ant.location != AntPlace.WORLD || ant.energy <= 0.0) return false
        val k = 0.12
        camera.x += (ant.x - camera.x) * k
        camera.y += (ant.y - camera.y) * k
        return true
    }

    fun update(world: World, camera: CameraState, now: Double) {
        if (now < pausedUntil) return
        val ants = ArrayList<Ant>()
        for (colony in world.colonies) {
            for (ant in colony.ants) {
                if (ant.location == AntPlace.WORLD) ants += ant
            }
        }
        val followed = follow
        val gone = followed != null && (followed.location != AntPlace.WORLD || followed.energy <= 0.0)
        if (nextPick == 0.0 || now >= nextPick || gone) pickShot(world, ants, now)
        follow?.let {
            targetX = it.x
            targetY = it.y
        }
        phase += 0.004
        val amp = if (follow != null) 2.0 else 7.0
        val dx = cos(phase) * amp
        val dy = sin(phase * 0.7) * amp * 0.7
        val k = if (follow != null) 0.08 else 0.014
        camera.zoom += (targetZoom - camera.zoom) * k
        camera.x += (targetX + dx - camera.x) * k
        camera.y += (targetY + dy - camera.y) * k
    }

    private fun pickShot(world: World, ants: List<Ant>, now: Double) {
        val events = ants.filter {
            it.state == AntState.RAIDING || it.state == AntState.ATTACKING ||
                it.state == AntState.FLEEING || it.state == AntState.MILKING
        }
        val kind = nextKind(events.isNotEmpty(), ants.isNotEmpty())
        follow = null
        var hold: Double
        when (kind) {
            "ENTRANCE" -> {
                val colony = world.colonies[random.nextInt(world.colonies.size)]
                targetX = colony.entranceWorld.x
                targetY = colony.entranceWorld.y
                targetZoom = 2.1 + random.nextDouble() * 0.4
                hold = 8.0 + random.nextDouble() * 3.0
            }
            "EVENT" -> {
                val ant = events[random.nextInt(events.size)]
                follow = ant
                targetX = ant.x
                targetY = ant.y
                targetZoom = if (ant.state == AntState.RAIDING) 2.6 else if (ant.state == AntState.MILKING) 3.2 else 3.0
                hold = 7.0 + random.nextDouble() * 4.0
            }
            "FOLLOW" -> {
                val carriers = ants.filter { it.carrying != CarryKind.NONE }
                val pool = when {
                    events.isNotEmpty() -> events
                    carriers.isNotEmpty() -> carriers
                    else -> ants
                }
                val ant = pickWithRunway(pool)
                follow = ant
                targetX = ant.x
                targetY = ant.y
                targetZoom = 2.8 + random.nextDouble() * 0.4
                hold = 8.0 + random.nextDouble() * 4.0
            }
            else -> {
                var cx = SimConfig.width / 2.0
                var cy = SimConfig.height / 2.0
                if (ants.isNotEmpty()) {
                    cx = ants.sumOf { it.x } / ants.size
                    cy = ants.sumOf { it.y } / ants.size
                }
                targetX = cx
                targetY = cy
                targetZoom = 1.05 + random.nextDouble() * 0.25
                hold = 11.0 + random.nextDouble() * 4.0
            }
        }
        nextPick = now + hold
    }

    internal fun nextKind(hasEvents: Boolean, hasAnts: Boolean): String {
        if (shotQueue.isEmpty()) {
            shotQueue.addAll(listOf("FOLLOW", "ENTRANCE", "FOLLOW", "WIDE", "EVENT"))
        }
        val iter = shotQueue.iterator()
        while (iter.hasNext()) {
            val kind = iter.next()
            if (kind == "EVENT" && !hasEvents) continue
            if (kind == "FOLLOW" && !hasAnts) continue
            iter.remove()
            return kind
        }
        return "WIDE"
    }

    private fun pickWithRunway(pool: List<Ant>): Ant {
        var best = pool[random.nextInt(pool.size)]
        var bestD = -1.0
        repeat(5) {
            val ant = pool[random.nextInt(pool.size)]
            val e = ant.colony.entranceWorld
            val dx = ant.x - e.x
            val dy = ant.y - e.y
            val d = dx * dx + dy * dy
            if (d > bestD) {
                bestD = d
                best = ant
            }
        }
        return best
    }
}
