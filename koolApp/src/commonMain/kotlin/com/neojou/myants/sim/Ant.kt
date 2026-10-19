package com.neojou.myants.sim

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class Ant(
    x0: Double,
    y0: Double,
    val type: AntKind,
) : SpatialItem {
    override var x: Double = x0
    override var y: Double = y0
    var angle: Double = 0.0
    var state: AntState = AntState.IDLE
    var carrying: CarryKind = CarryKind.NONE
    var carryingAmount: Double = 0.0
    var carryingInstance: Any? = null
    var energy: Double = SimConfig.antMaxEnergy
    var health: Double = 0.0
    var location: AntPlace = AntPlace.WORLD
    lateinit var colony: Colony

    var age: Int = 0
    var maxAge: Double = 0.0
    var foodMemoryX: Double = -1.0
    var foodMemoryY: Double = -1.0
    var carryingQuality: Double = 1.0
    var cropSugar: Double = 0.0
    var trophTimer: Int = 0

    // Field initializers run before the constructor body. TS order is forageSeed, shade, then body.
    var forageSeed: Double = Rng.rand()
    var sizeVar: Double = 1.0
    var attackDamage: Double = SimConfig.workerDamage
    var sizeSpeed: Double = 1.0
    var sizeUpkeep: Double = 1.0
    var shade: Int = floor(Rng.rand() * 4.0).toInt()

    var obstacleTimer: Int = 0
    var exitTimer: Int = 0
    var fleeTimer: Int = 0
    var attackCooldown: Int = 0
    var sprintTimer: Int = 0
    var sprintCooldown: Int = 0
    var speedMultiplier: Double = 1.0
    var thinkTimer: Int = 0
    var harvestTimer: Int = 0
    var restTimer: Double = 0.0

    var patrolAngle: Double = 0.0
    var patrolRadius: Double = 100.0
    var patrolTarget: Vec2? = null
    var raidTarget: Vec2? = null
    var carryTarget: Vec2? = null
    var stuckTimer: Int = 0
    var lastX: Double = 0.0
    var lastY: Double = 0.0
    var nestEscapeTimer: Int = 0
    var nestEscapeX: Double = 0.0
    var nestEscapeY: Double = 0.0
    var progressX: Double = 0.0
    var progressY: Double = 0.0
    var progressTimer: Int = 0
    var wallSlidInWindow: Boolean = false

    init {
        angle = Rng.rand() * PI * 2.0
        energy = SimConfig.antMaxEnergy
        val poly = SimConfig.ant.poly
        val sizeMin: Double
        val sizeRange: Double
        if (type == AntKind.SOLDIER) {
            sizeMin = poly.soldierSizeMin
            sizeRange = poly.soldierSizeRange
        } else if (type == AntKind.WORKER) {
            sizeMin = poly.workerSizeMin
            sizeRange = poly.workerSizeRange
        } else {
            sizeMin = 1.55
            sizeRange = 0.1
        }
        sizeVar = sizeMin + Rng.rand() * sizeRange
        val baseHealth = if (type == AntKind.SOLDIER) SimConfig.soldierHealth else SimConfig.workerHealth
        val baseDamage = if (type == AntKind.SOLDIER) SimConfig.soldierDamage else SimConfig.workerDamage
        val sizeRel = sizeVar / (sizeMin + sizeRange / 2.0)
        health = floor(baseHealth * sizeRel + 0.5)
        attackDamage = baseDamage * sizeRel
        sizeSpeed = 1.0 / (poly.speedBias + (1.0 - poly.speedBias) * sizeVar)
        sizeUpkeep = poly.upkeepBase + (1.0 - poly.upkeepBase) * sizeVar
        maxAge = SimConfig.ant.lifespan + Rng.rand() * SimConfig.ant.lifespanJitter
        carrying = CarryKind.NONE
        location = AntPlace.WORLD
        if (type == AntKind.QUEEN) {
            state = AntState.IDLE
            location = AntPlace.NEST
        } else if (type == AntKind.SOLDIER) {
            state = AntState.PATROLLING
        } else {
            state = if (Rng.rand() > 0.3) AntState.FORAGING else AntState.NURSING
            if (state == AntState.NURSING) location = AntPlace.NEST
        }
    }

    fun update(world: World) {
        age++
        if (type != AntKind.QUEEN && age.toDouble() > maxAge) {
            health = 0.0
            return
        }
        if (state == AntState.RESTING) {
            energy -= SimConfig.antEnergyDecay * 0.1 * sizeUpkeep
        } else {
            energy -= SimConfig.antEnergyDecay * sizeUpkeep
        }
        if (energy <= 0.0) {
            health -= 1.0
            if (health <= 0.0) {
                val index = world.ants.indexOf(this)
                if (index >= 0) world.ants.removeAt(index)
                return
            }
        }
        if (energy < SimConfig.ant.hungryThreshold &&
            state != AntState.HUNGRY &&
            state != AntState.ATTACKING &&
            state != AntState.FORAGING &&
            state != AntState.HARVESTING
        ) {
            state = AntState.HUNGRY
        }
        if (attackCooldown > 0) attackCooldown--
        if (obstacleTimer > 0) obstacleTimer--
        speedMultiplier = 1.0
        when (state) {
            AntState.FORAGING -> handleForaging(this, world)
            AntState.RETURNING -> handleReturning(this, world)
            AntState.NURSING -> handleNursing(this, world)
            AntState.PATROLLING -> handlePatrolling(this, world)
            AntState.FLEEING -> handleFleeing(this, world)
            AntState.ATTACKING -> handleCombat(this, world)
            AntState.RAIDING -> handleRaiding(this, world)
            AntState.TRANSPORTING -> handleTransporting(this, world)
            AntState.HARVESTING -> handleHarvesting(this, world)
            AntState.MILKING -> handleMilking(this, world)
            AntState.HUNGRY -> handleHungry(this, world)
            AntState.RESTING -> handleResting(this, world)
            AntState.DIGGING -> handleDigging(this, world)
            AntState.IDLE -> {
                if (type == AntKind.QUEEN) {
                    // Queen idle logic lives on Queen.
                } else if (type == AntKind.WORKER) {
                    handleNurseIdle(this, world)
                } else if (type == AntKind.SOLDIER) {
                    state = AntState.PATROLLING
                }
            }
        }
        if (location == AntPlace.NEST && type != AntKind.QUEEN) {
            if (trophTimer > 0) trophTimer--
            else {
                trophTimer = SimConfig.ant.troph.interval
                trophallaxis(world)
            }
        }
        move(world)
    }

    fun senseAndSteer(world: World, pheromoneType: Pheromone): Boolean {
        val grid = if (location == AntPlace.NEST) colony.nestGrid else colony.outdoorField
        val sensorDist = SimConfig.antSensorDist * (if (location == AntPlace.WORLD) world.activityFactor() else 1.0)
        val sensorAngle = SimConfig.antSensorAngle
        val turnSpeed = SimConfig.antTurnSpeed
        fun level(offset: Double): Double {
            val sx = x + cos(angle + offset) * sensorDist
            val sy = y + sin(angle + offset) * sensorDist
            return grid.get(sx, sy, pheromoneType)
        }
        val center = level(0.0)
        val left = level(-sensorAngle)
        val right = level(sensorAngle)
        if (center < 0.05 && left < 0.05 && right < 0.05) return false
        if (center > left && center > right) {
            angle += (Rng.rand() - 0.5) * 0.05
        } else if (center < left && center < right) {
            if (Rng.rand() < 0.5) angle -= turnSpeed
            else angle += turnSpeed
        } else if (left > right) {
            angle -= turnSpeed
        } else if (right > left) {
            angle += turnSpeed
        }
        return true
    }

    fun senseAndAvoid(world: World, pheromoneType: Pheromone) {
        val grid = if (location == AntPlace.NEST) colony.nestGrid else colony.outdoorField
        val sensorDist = SimConfig.antSensorDist
        val sensorAngle = SimConfig.antSensorAngle
        val turnSpeed = SimConfig.antTurnSpeed
        fun level(offset: Double): Double {
            val sx = x + cos(angle + offset) * sensorDist
            val sy = y + sin(angle + offset) * sensorDist
            return grid.get(sx, sy, pheromoneType)
        }
        val center = level(0.0)
        val left = level(-sensorAngle)
        val right = level(sensorAngle)
        if (center < 0.05 && left < 0.05 && right < 0.05) {
            wander()
            return
        }
        if (center > left && center > right) {
            if (Rng.rand() < 0.5) angle -= turnSpeed
            else angle += turnSpeed
        } else if (left > right) {
            angle += turnSpeed
        } else if (right > left) {
            angle -= turnSpeed
        } else {
            wander()
        }
    }

    fun disperseFromNest(world: World): Boolean {
        val e = colony.entranceWorld
        val dx = x - e.x
        val dy = y - e.y
        val distSq = dx * dx + dy * dy
        val r = SimConfig.ant.dispersalRadius
        if (distSq > r * r || distSq < 1.0) return false
        val outward = atan2(dy, dx)
        var diff = outward - angle
        while (diff < -PI) diff += PI * 2.0
        while (diff > PI) diff -= PI * 2.0
        angle += diff * SimConfig.ant.dispersalStrength + (Rng.rand() - 0.5) * 0.3
        return true
    }

    fun steerThroughNest(targetX: Double, targetY: Double) {
        val wp = colony.nest.nestWaypoint(x, y, targetX, targetY)
        val tx = if (wp != null) wp.x else targetX
        val ty = if (wp != null) wp.y else targetY
        angle = atan2(ty - y, tx - x)
    }

    fun steerToMemory(): Boolean {
        if (foodMemoryX < 0.0) return false
        val dx = foodMemoryX - x
        val dy = foodMemoryY - y
        if (dx * dx + dy * dy < SimConfig.ant.arriveRangeSq) {
            foodMemoryX = -1.0
            foodMemoryY = -1.0
            return false
        }
        val target = atan2(dy, dx)
        var diff = target - angle
        while (diff < -PI) diff += PI * 2.0
        while (diff > PI) diff -= PI * 2.0
        angle += diff * SimConfig.ant.memoryBias + (Rng.rand() - 0.5) * 0.2
        return true
    }

    fun wander() {
        angle += (Rng.rand() - 0.5) * 0.2
    }

    fun forageUrge(): Double {
        val f = if (maxAge > 0.0) age.toDouble() / maxAge else 0.0
        val young = SimConfig.ant.nurseAgeFraction
        val old = SimConfig.ant.forageAgeFraction
        val lo = SimConfig.ant.forageUrgeYoung
        val hi = SimConfig.ant.forageUrgeOld
        if (f <= young) return lo
        if (f >= old) return hi
        val t = (f - young) / (old - young)
        return lo + t * (hi - lo)
    }

    fun eatFromStockpile(world: World) {
        val deficit = SimConfig.antMaxEnergy - energy
        if (deficit <= 0.0 || colony.sugarStockpile <= 0.0) return
        val sugarNeeded = deficit / SimConfig.sugarEnergyValue
        val sugarEaten = min(colony.sugarStockpile, sugarNeeded)
        colony.sugarStockpile -= sugarEaten
        energy += sugarEaten * SimConfig.sugarEnergyValue
    }

    fun move(world: World) {
        applySeparation(world)
        var speedMul = speedMultiplier
        if (location == AntPlace.NEST && nestEscapeTimer > 0) {
            nestEscapeTimer--
            angle = atan2(nestEscapeY - y, nestEscapeX - x)
            speedMul = max(speedMul, 0.6)
        }
        val night = if (location == AntPlace.WORLD) world.activityFactor() else 1.0
        val rain = if (location == AntPlace.WORLD && world.raining) SimConfig.environment.rainSpeed else 1.0
        val speed = SimConfig.antSpeed * speedMul * sizeSpeed * night * rain
        val nextX = x + cos(angle) * speed
        val nextY = y + sin(angle) * speed
        if (location == AntPlace.WORLD) {
            val e0 = colony.entranceWorld
            val width = SimConfig.width.toDouble()
            val height = SimConfig.height.toDouble()
            val hitEntrance = when (colony.entranceSide) {
                EntranceSide.RIGHT -> nextX > width - 10.0 && abs(nextY - e0.y) < 50.0
                EntranceSide.LEFT -> nextX < 10.0 && abs(nextY - e0.y) < 50.0
                EntranceSide.BOTTOM -> nextY > height - 10.0 && abs(nextX - e0.x) < 50.0
                EntranceSide.TOP -> nextY < 10.0 && abs(nextX - e0.x) < 50.0
            }
            if (hitEntrance) {
                location = AntPlace.NEST
                val e = colony.entranceNestLocal
                x = e.x
                y = e.y
                angle = colony.entranceNestAngle
                return
            }
            if (!world.terrain.isBlocked(nextX, nextY) && nextX > 0.0 && nextX < width && nextY > 0.0 && nextY < height) {
                x = nextX
                y = nextY
            } else {
                angle = world.terrain.getCollisionAngle(x, y, angle)
                obstacleTimer = 10 + floor(Rng.rand() * 10.0).toInt()
                x += cos(angle) * speed
                y += sin(angle) * speed
            }
        } else {
            val nest = colony.nest
            val isLandscape = nest.height > nest.width
            val hitExit = if (isLandscape) {
                nextX < 5.0 && abs(nextY - nest.height / 2.0) < 50.0
            } else {
                nextY < 5.0 && abs(nextX - nest.width / 2.0) < 50.0
            }
            if (hitExit) {
                location = AntPlace.WORLD
                val exit = colony.worldExitPoint
                x = exit.x
                y = exit.y
                angle = colony.worldExitAngle
                exitTimer = 45
                return
            }
            val nestBuffer = 5.0
            if (nest.isInside(nextX, nextY, nestBuffer)) {
                x = nextX
                y = nextY
            } else {
                wallSlidInWindow = true
                val probe = SimConfig.antSpeed * max(0.5, speedMultiplier) * sizeSpeed
                var slid = false
                var k = 1
                while (k <= 6 && !slid) {
                    val off = k * 0.4
                    for (dir in intArrayOf(1, -1)) {
                        val a = angle + dir * off
                        val tx = x + cos(a) * probe
                        val ty = y + sin(a) * probe
                        if (nest.isInside(tx, ty, nestBuffer)) {
                            angle = a
                            x = tx
                            y = ty
                            slid = true
                            break
                        }
                    }
                    k++
                }
                if (!slid) {
                    val nearest = nest.getNearestNode(x, y)
                    if (nearest != null) {
                        val angleToCenter = atan2(nearest.y - y, nearest.x - x)
                        x += cos(angleToCenter) * 2.0
                        y += sin(angleToCenter) * 2.0
                        angle = angleToCenter + (Rng.rand() - 0.5) * 0.6
                    }
                }
            }
            if (stuckTimer > 25 && nestEscapeTimer == 0) {
                startNestEscape()
                stuckTimer = 0
            }
        }
        val movedDist = (x - lastX) * (x - lastX) + (y - lastY) * (y - lastY)
        if (speedMultiplier > 0.0 && movedDist < 0.25) stuckTimer++
        else stuckTimer = 0
        lastX = x
        lastY = y
        if (location == AntPlace.NEST && speedMul > 0.0 && nestEscapeTimer == 0) {
            progressTimer++
            if (progressTimer >= 20) {
                val pdx = x - progressX
                val pdy = y - progressY
                if (pdx * pdx + pdy * pdy < 36.0 && wallSlidInWindow) startNestEscape()
                progressX = x
                progressY = y
                progressTimer = 0
                wallSlidInWindow = false
            }
        } else {
            progressX = x
            progressY = y
            progressTimer = 0
            wallSlidInWindow = false
        }
    }

    fun startNestEscape() {
        val node = colony.nest.getNearestNode(x, y) ?: return
        nestEscapeX = node.x
        nestEscapeY = node.y
        nestEscapeTimer = 25
    }

    fun trophallaxis(world: World) {
        val t = SimConfig.ant.troph
        if (cropSugar <= 0.0 || energy < t.donorMinEnergy) return
        val nearby = world.spatialGrid.getNearby(x, y, t.donateRadius)
        for (other in nearby) {
            if (other === this || other.location != AntPlace.NEST || other.type == AntKind.QUEEN) continue
            if (other.colony !== colony) continue
            if (other.energy >= t.recipientHungry) continue
            val dx = other.x - x
            val dy = other.y - y
            if (dx * dx + dy * dy > t.donateRadius * t.donateRadius) continue
            val give = min(t.donateChunk, min(cropSugar, SimConfig.antMaxEnergy - other.energy))
            if (give <= 0.0) continue
            other.energy += give
            cropSugar -= give
            colony.trophallaxisCount++
            feedParticle(world, other.x, other.y)
            return
        }
        if (cropSugar < t.larvaCropCost) return
        for (b in colony.brood) {
            if (b.stage != BroodStage.LARVA || b.carrier != null || b.hunger <= t.larvaHungry) continue
            val dx = b.x - x
            val dy = b.y - y
            if (dx * dx + dy * dy > t.donateRadius * t.donateRadius) continue
            b.feed(t.larvaRelief)
            cropSugar -= t.larvaCropCost
            colony.trophallaxisCount++
            feedParticle(world, b.x, b.y)
            return
        }
    }

    private fun feedParticle(world: World, px: Double, py: Double) {
        if (colony.id != 0) return
        world.nestParticles.add(NestParticle(px, py, 0.0, -0.15, 0.6, "#ffd24a"))
    }

    fun applySeparation(world: World) {
        if (location == AntPlace.NEST) return
        val separationRadius = 15.0
        val nearby = world.spatialGrid.getNearby(x, y, separationRadius)
        var dx = 0.0
        var dy = 0.0
        var count = 0
        for (other in nearby) {
            if (other !== this && other.location == location && other.colony === colony) {
                val distSq = (x - other.x) * (x - other.x) + (y - other.y) * (y - other.y)
                if (distSq < separationRadius * separationRadius && distSq > 0.0) {
                    val dist = sqrt(distSq)
                    val force = (separationRadius - dist) / separationRadius
                    dx += (x - other.x) / dist * force
                    dy += (y - other.y) / dist * force
                    count++
                }
            }
        }
        if (count > 0) {
            x += dx * 0.5
            y += dy * 0.5
        }
    }

    fun countNearbyAllies(world: World, radius: Double): Int {
        val nearby = world.spatialGrid.getNearby(x, y, radius)
        var count = 0
        val r2 = radius * radius
        for (ant in nearby) {
            if (ant !== this && ant.type != AntKind.QUEEN && ant.location == location && ant.colony === colony) {
                val dx = ant.x - x
                val dy = ant.y - y
                if (dx * dx + dy * dy < r2) count++
            }
        }
        return count
    }

    fun countNearbyEnemies(world: World, radius: Double): Int {
        if (location == AntPlace.NEST) return 0
        val r2 = radius * radius
        var count = 0
        for (ins in world.insectGrid.getNearby(x, y, radius)) {
            if (ins.type == InsectKind.PREDATOR || ins.type == InsectKind.SPIDER || ins.type == InsectKind.BEETLE) {
                val dx = ins.x - x
                val dy = ins.y - y
                if (dx * dx + dy * dy < r2) count++
            }
        }
        for (other in world.spatialGrid.getNearby(x, y, radius)) {
            if (other.colony !== colony && other.location == AntPlace.WORLD && other.type != AntKind.QUEEN) {
                val dx = other.x - x
                val dy = other.y - y
                if (dx * dx + dy * dy < r2) count++
            }
        }
        return count
    }
}
