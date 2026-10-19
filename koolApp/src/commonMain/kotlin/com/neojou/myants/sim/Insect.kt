package com.neojou.myants.sim

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

enum class InsectAi { IDLE, WANDER, FLEE, HUNT }

class Insect(
    override var x: Double,
    override var y: Double,
    val type: InsectKind,
) : SpatialItem {
    val seq: Int = nextSeq++
    var health: Double = 0.0
    var angle: Double = 0.0
    var speed: Double = 0.0
    var attackCooldown: Int = 0
    var state: InsectAi = InsectAi.WANDER
    var stateTimer: Double = 0.0
    var thinkTimer: Int = 0
    var targetX: Double = x
    var targetY: Double = y

    init {
        angle = Rng.rand() * PI * 2.0
        when (type) {
            InsectKind.PREY -> {
                health = 20.0
                speed = 1.0
            }
            InsectKind.PREDATOR -> {
                health = SimConfig.enemy.predatorHealth
                speed = 1.3
            }
            InsectKind.SPIDER -> {
                health = SimConfig.enemy.spiderHealth
                speed = 1.8
            }
            InsectKind.BEETLE -> {
                health = SimConfig.enemy.beetleHealth
                speed = 0.6
            }
            InsectKind.LADYBUG -> {
                health = 80.0
                speed = 1.0
            }
            InsectKind.APHID -> {
                health = 10.0
                speed = 0.2
            }
        }
    }

    fun update(world: World) {
        if (health <= 0.0) return
        if (attackCooldown > 0) attackCooldown--
        if (thinkTimer > 0) {
            thinkTimer--
        } else {
            thinkTimer = 3 + floor(Rng.rand() * 3.0).toInt()
            when (type) {
                InsectKind.PREY -> updatePrey(world)
                InsectKind.PREDATOR -> updatePredator(world)
                InsectKind.SPIDER -> updateSpider(world)
                InsectKind.BEETLE -> updateBeetle(world)
                InsectKind.LADYBUG -> updateLadybug(world)
                InsectKind.APHID -> updateAphid(world)
            }
        }
        executeMovement(world)
    }

    fun executeMovement(world: World) {
        if (state == InsectAi.IDLE) {
            stateTimer -= 1.0
            if (stateTimer <= 0.0) {
                state = InsectAi.WANDER
                pickRandomTarget()
            }
            return
        }
        val dx = targetX - x
        val dy = targetY - y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist < 5.0) {
            if (state == InsectAi.WANDER) {
                state = InsectAi.IDLE
                stateTimer = 30.0 + Rng.rand() * 60.0
            }
            return
        }
        val targetAngle = atan2(dy, dx)
        var diff = targetAngle - angle
        while (diff < -PI) diff += PI * 2.0
        while (diff > PI) diff -= PI * 2.0
        val turnSpeed = 0.15
        angle += maxOf(-turnSpeed, minOf(turnSpeed, diff))
        val step = speed * (1.0 - grappleSlow(world))
        val nextX = x + cos(angle) * step
        val nextY = y + sin(angle) * step
        if (!world.terrain.isBlocked(nextX, nextY)) {
            x = nextX
            y = nextY
        } else {
            angle += PI
            pickRandomTarget()
        }
    }

    fun grappleSlow(world: World): Double {
        if (type == InsectKind.APHID) return 0.0
        val r = SimConfig.combat.grappleRadius
        val r2 = r * r
        var holders = 0
        for (ant in world.spatialGrid.getNearby(x, y, r)) {
            if (ant.location != AntPlace.WORLD) continue
            val dx = ant.x - x
            val dy = ant.y - y
            if (dx * dx + dy * dy < r2) holders++
        }
        return min(SimConfig.combat.grappleMaxSlow, holders.toDouble() * SimConfig.combat.grappleSlowPerAnt)
    }

    fun pickRandomTarget() {
        val range = 100.0
        targetX = x + (Rng.rand() - 0.5) * range * 2.0
        targetY = y + (Rng.rand() - 0.5) * range * 2.0
    }

    fun updatePrey(world: World) {
        var nearestAnt: Ant? = null
        var minDst = 10000.0
        for (ant in world.spatialGrid.getNearby(x, y, 116.0)) {
            if (ant.colony !== world.colonies[0]) continue
            val dx = x - ant.x
            val dy = y - ant.y
            val d2 = dx * dx + dy * dy
            if (d2 < minDst) {
                minDst = d2
                nearestAnt = ant
            }
        }
        val nearest = nearestAnt
        if (nearest != null && minDst < 3600.0) {
            state = InsectAi.FLEE
            val dx = x - nearest.x
            val dy = y - nearest.y
            targetX = x + dx * 5.0
            targetY = y + dy * 5.0
            speed = 3.5
        } else {
            speed = 1.0
            if (state == InsectAi.FLEE) {
                state = InsectAi.WANDER
                pickRandomTarget()
            }
        }
    }

    fun updatePredator(world: World) {
        huntAnts(world, 15000.0, SimConfig.enemy.predatorDamage)
    }

    fun updateSpider(world: World) {
        huntAnts(world, 20000.0, SimConfig.enemy.spiderDamage)
    }

    fun updateBeetle(world: World) {
        huntAnts(world, 3000.0, SimConfig.enemy.beetleDamage)
        speed = 0.6
    }

    fun updateLadybug(world: World) {
        var nearestAphid: Insect? = null
        var minDst = Double.POSITIVE_INFINITY
        for (insect in world.insects) {
            if (insect.type == InsectKind.APHID) {
                val dx = x - insect.x
                val dy = y - insect.y
                val d2 = dx * dx + dy * dy
                if (d2 < minDst) {
                    minDst = d2
                    nearestAphid = insect
                }
            }
        }
        val aphid = nearestAphid
        if (aphid != null && minDst < 20000.0) {
            state = InsectAi.HUNT
            targetX = aphid.x
            targetY = aphid.y
            if (minDst < 100.0) {
                if (attackCooldown <= 0) {
                    aphid.health -= 10.0
                    world.addParticle(aphid.x, aphid.y, "red", ParticleKind.BLOOD)
                    attackCooldown = 60
                }
            }
        }
    }

    fun updateAphid(world: World) {
        if (Rng.rand() < 0.01 && state != InsectAi.WANDER) {
            state = InsectAi.WANDER
            targetX = x + (Rng.rand() - 0.5) * 24.0
            targetY = y + (Rng.rand() - 0.5) * 24.0
        }
        speed = 0.15
    }

    fun huntAnts(world: World, rangeSq: Double, damage: Double) {
        var nearestAnt: Ant? = null
        var minDst = Double.POSITIVE_INFINITY
        val r = sqrt(rangeSq) + 16.0
        for (ant in world.spatialGrid.getNearby(x, y, r)) {
            if (ant.colony !== world.colonies[0]) continue
            val dx = x - ant.x
            val dy = y - ant.y
            val d2 = dx * dx + dy * dy
            if (d2 < minDst) {
                minDst = d2
                nearestAnt = ant
            }
        }
        val nearest = nearestAnt
        if (nearest != null && minDst < rangeSq) {
            state = InsectAi.HUNT
            targetX = nearest.x
            targetY = nearest.y
            if (minDst < 100.0) {
                if (attackCooldown <= 0) {
                    nearest.health -= damage
                    world.addParticle(nearest.x, nearest.y, "red", ParticleKind.BLOOD)
                    attackCooldown = 30
                }
            }
        } else if (state == InsectAi.HUNT) {
            state = InsectAi.WANDER
            pickRandomTarget()
        }
    }

    companion object {
        private var nextSeq: Int = 0
    }
}
