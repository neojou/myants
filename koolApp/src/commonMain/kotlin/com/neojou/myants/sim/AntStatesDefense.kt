package com.neojou.myants.sim

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// Ant state handlers: patrol, raid, flee, combat.

fun handlePatrolling(ant: Ant, world: World) {
    if (ant.type == AntKind.SOLDIER) {
        val sightRange = sqrt(SimConfig.combat.soldierSightRangeSq)
        for (insect in world.insectGrid.getNearby(ant.x, ant.y, sightRange)) {
            if (insect.type == InsectKind.SPIDER || insect.type == InsectKind.PREDATOR || insect.type == InsectKind.BEETLE) {
                val dx = ant.x - insect.x
                val dy = ant.y - insect.y
                if (dx * dx + dy * dy < SimConfig.combat.soldierSightRangeSq) {
                    ant.state = AntState.ATTACKING
                    return
                }
            }
        }
        if (ant.location == AntPlace.WORLD) {
            val foes = world.spatialGrid.getNearby(ant.x, ant.y, 100.0)
            for (o in foes) {
                if (o.colony !== ant.colony && o.location == AntPlace.WORLD && o.type != AntKind.QUEEN) {
                    val dx = ant.x - o.x
                    val dy = ant.y - o.y
                    if (dx * dx + dy * dy < SimConfig.ant.detectEnemyRangeSq) {
                        ant.state = AntState.ATTACKING
                        return
                    }
                }
            }
        }
        if (ant.colony.outdoorField.get(ant.x, ant.y, Pheromone.DANGER) > SimConfig.combat.alarmThreshold) {
            ant.state = AntState.ATTACKING
            return
        }
    }
    if (ant.energy < SimConfig.ant.foragingHungerThreshold) {
        ant.state = AntState.HUNGRY
        return
    }
    if (ant.colony.proteinStockpile < 20.0) {
        ant.state = AntState.FORAGING
        return
    }
    if (Rng.rand() < 0.00005) {
        ant.state = AntState.RESTING
        ant.restTimer = 300.0 + Rng.rand() * 600.0
        return
    }
    if (ant.location == AntPlace.NEST) {
        val entrance = ant.colony.nest.getEntrance()
        val nextNode = ant.colony.nest.getNextNodeTowards(ant.x, ant.y, entrance.x, entrance.y)
        if (nextNode != null) {
            val ang = atan2(nextNode.y - ant.y, nextNode.x - ant.x)
            ant.angle = ang + (Rng.rand() - 0.5) * 0.5
        } else {
            val dx = entrance.x - ant.x
            val dy = entrance.y - ant.y
            ant.angle = atan2(dy, dx) + (Rng.rand() - 0.5) * 1.0
        }
        return
    }
    val eW = ant.colony.entranceWorld
    val entranceX = if (ant.colony.isLandscape) eW.x - 150.0 else eW.x
    val entranceY = if (ant.colony.isLandscape) eW.y else eW.y - 150.0
    var distSq = 0.0
    val existing = ant.patrolTarget
    if (existing != null) {
        val dx = existing.x - ant.x
        val dy = existing.y - ant.y
        distSq = dx * dx + dy * dy
    }
    if (ant.patrolTarget == null || distSq < 400.0 || Rng.rand() < 0.005) {
        val angle = Rng.rand() * PI * 2.0
        val r = Rng.rand()
        val longChance = SimConfig.combat.patrolLongChance
        val dist = if (r < longChance) {
            200.0 + (r / longChance) * max(SimConfig.width.toDouble(), SimConfig.height.toDouble()) * SimConfig.combat.patrolLongRangeFrac
        } else {
            50.0 + ((r - longChance) / (1.0 - longChance)) * 200.0
        }
        var tx = entranceX + cos(angle) * dist
        var ty = entranceY + sin(angle) * dist
        val width = SimConfig.width.toDouble()
        val height = SimConfig.height.toDouble()
        tx = max(10.0, min(width - 10.0, tx))
        ty = max(10.0, min(height - 10.0, ty))
        ant.patrolTarget = Vec2(tx, ty)
        val dx = tx - ant.x
        val dy = ty - ant.y
        distSq = dx * dx + dy * dy
    }
    val patrol = ant.patrolTarget ?: return
    val dx = patrol.x - ant.x
    val dy = patrol.y - ant.y
    val desired = atan2(dy, dx)
    var diff = desired - ant.angle
    while (diff < -PI) diff += PI * 2.0
    while (diff > PI) diff -= PI * 2.0
    ant.angle += diff * 0.2
    ant.angle += (Rng.rand() - 0.5) * 0.2
}

fun handleRaiding(ant: Ant, world: World) {
    if (ant.location == AntPlace.WORLD) {
        val foes = world.spatialGrid.getNearby(ant.x, ant.y, 100.0)
        for (o in foes) {
            if (o.colony !== ant.colony && o.location == AntPlace.WORLD && o.type != AntKind.QUEEN && o.health > 0.0) {
                val dx = ant.x - o.x
                val dy = ant.y - o.y
                if (dx * dx + dy * dy < SimConfig.ant.detectEnemyRangeSq) {
                    ant.state = AntState.ATTACKING
                    return
                }
            }
        }
    }
    val t = ant.raidTarget
    if (t == null) {
        ant.state = AntState.PATROLLING
        return
    }
    val dx = t.x - ant.x
    val dy = t.y - ant.y
    if (dx * dx + dy * dy < SimConfig.combat.raidArriveRangeSq) {
        val enemy = world.colonies.firstOrNull { it !== ant.colony }
        if (enemy != null) {
            var snatched = false
            var i = enemy.brood.size - 1
            while (i >= 0) {
                if (enemy.brood[i].carrier == null) {
                    enemy.brood.removeAt(i)
                    snatched = true
                    break
                }
                i--
            }
            if (snatched) {
                ant.carrying = CarryKind.BROOD
                ant.carryingInstance = null
                ant.carryingAmount = 1.0
            } else if (enemy.proteinStockpile >= SimConfig.proteinValue) {
                enemy.proteinStockpile -= SimConfig.proteinValue
                ant.carrying = CarryKind.PROTEIN
                ant.carryingAmount = SimConfig.proteinValue
                ant.carryingQuality = 1.0
            } else if (enemy.sugarStockpile >= SimConfig.sugarValue) {
                enemy.sugarStockpile -= SimConfig.sugarValue
                ant.carrying = CarryKind.SUGAR
                ant.carryingAmount = SimConfig.sugarValue
                ant.carryingQuality = 1.0
            }
        }
        ant.raidTarget = null
        ant.state = AntState.RETURNING
        return
    }
    ant.angle = atan2(dy, dx)
}

fun handleFleeing(ant: Ant, world: World) {
    ant.speedMultiplier = 2.5
    ant.fleeTimer--
    if (ant.obstacleTimer > 0) return
    val homeX: Double
    val homeY: Double
    if (ant.location == AntPlace.WORLD) {
        homeX = ant.colony.entranceWorld.x
        homeY = ant.colony.entranceWorld.y
    } else if (SimConfig.nestWidth < SimConfig.nestHeight) {
        homeX = 0.0
        homeY = SimConfig.nestHeight / 2.0
    } else {
        homeX = SimConfig.nestWidth / 2.0
        homeY = 0.0
    }
    val dx = homeX - ant.x
    val dy = homeY - ant.y
    val homeAngle = atan2(dy, dx)
    var diff = homeAngle - ant.angle
    while (diff < -PI) diff += PI * 2.0
    while (diff > PI) diff -= PI * 2.0
    ant.angle += diff * 0.5
    ant.angle += (Rng.rand() - 0.5) * 0.5
    if (ant.fleeTimer % 5 == 0) {
        val grid = if (ant.location == AntPlace.NEST) ant.colony.nestGrid else ant.colony.outdoorField
        grid.depositCircle(ant.x, ant.y, Pheromone.DANGER, SimConfig.pheromone.depositTrail, 10.0)
    }
    if (ant.fleeTimer <= 0) ant.state = AntState.FORAGING
}

fun handleCombat(ant: Ant, world: World) {
    ant.speedMultiplier = 1.5
    if (ant.type != AntKind.SOLDIER) {
        val enemies = ant.countNearbyEnemies(world, 100.0)
        if (enemies > 0) {
            val allies = ant.countNearbyAllies(world, 100.0)
            val outmatched = allies.toDouble() < max(
                SimConfig.combat.mobMinAllies.toDouble(),
                enemies.toDouble() * SimConfig.combat.mobSuperiority,
            )
            if (outmatched) {
                ant.state = AntState.FLEEING
                ant.fleeTimer = 60
                ant.angle += PI + (Rng.rand() - 0.5)
                return
            }
        }
    }
    var nearestX = 0.0
    var nearestY = 0.0
    var nearestInsect: Insect? = null
    var nearestAnt: Ant? = null
    var minDist = Double.POSITIVE_INFINITY
    val chaseRangeSq = if (ant.type == AntKind.SOLDIER) SimConfig.combat.soldierSightRangeSq else SimConfig.ant.detectEnemyRangeSq
    for (insect in world.insectGrid.getNearby(ant.x, ant.y, sqrt(chaseRangeSq))) {
        if (insect.type != InsectKind.APHID) {
            val dx = ant.x - insect.x
            val dy = ant.y - insect.y
            val d2 = dx * dx + dy * dy
            if (d2 < minDist) {
                minDist = d2
                nearestX = insect.x
                nearestY = insect.y
                nearestInsect = insect
                nearestAnt = null
            }
        }
    }
    if (ant.location == AntPlace.WORLD) {
        val foes = world.spatialGrid.getNearby(ant.x, ant.y, 110.0)
        for (other in foes) {
            if (other.colony !== ant.colony && other.location == AntPlace.WORLD && other.type != AntKind.QUEEN && other.health > 0.0) {
                val dx = ant.x - other.x
                val dy = ant.y - other.y
                val d2 = dx * dx + dy * dy
                if (d2 < minDist) {
                    minDist = d2
                    nearestX = other.x
                    nearestY = other.y
                    nearestInsect = null
                    nearestAnt = other
                }
            }
        }
    }
    val bugNow = nearestInsect
    val foeNow = nearestAnt
    val hasEnemy = bugNow != null || foeNow != null
    if (hasEnemy && minDist < chaseRangeSq) {
        val majorThreat = bugNow != null && (
            bugNow.type == InsectKind.SPIDER ||
                bugNow.type == InsectKind.PREDATOR ||
                bugNow.type == InsectKind.BEETLE
            )
        if (majorThreat) {
            val allies = ant.countNearbyAllies(world, 90.0)
            val enemies = ant.countNearbyEnemies(world, 90.0)
            val rushAllies = if (ant.type == AntKind.SOLDIER) SimConfig.combat.soldierRushAllies else SimConfig.combat.mobRushAllies
            val rushReady = allies >= rushAllies && allies.toDouble() >= enemies.toDouble() * SimConfig.combat.mobSuperiority
            if (world.age % 6 == 0) {
                ant.colony.outdoorField.depositCircle(ant.x, ant.y, Pheromone.DANGER, SimConfig.pheromone.depositTrail, 10.0)
            }
            if (!rushReady) {
                val toEnemy = atan2(nearestY - ant.y, nearestX - ant.x)
                if (minDist < SimConfig.combat.rallyStandoffSq) {
                    ant.angle = toEnemy + PI + (Rng.rand() - 0.5) * 0.9
                } else {
                    ant.angle = toEnemy + PI / 2.0 + (Rng.rand() - 0.5) * 1.4
                }
                ant.speedMultiplier = 1.3
                return
            }
        }
        val dx = nearestX - ant.x
        val dy = nearestY - ant.y
        ant.angle = atan2(dy, dx)
        ant.angle += (Rng.rand() - 0.5) * 0.1
        if (ant.sprintTimer > 0) {
            ant.speedMultiplier = 2.5
            ant.sprintTimer--
        } else if (ant.sprintCooldown > 0) {
            ant.sprintCooldown--
        } else if (minDist > SimConfig.ant.attackRangeSq) {
            ant.sprintTimer = 30
            ant.sprintCooldown = 180
        }
        if (minDist < SimConfig.ant.attackRangeSq) {
            ant.speedMultiplier = 0.0
            if (ant.attackCooldown <= 0) {
                val dmg = ant.attackDamage
                val bug = nearestInsect
                val foe = nearestAnt
                if (bug != null) bug.health -= dmg
                else if (foe != null) foe.health -= dmg
                world.addParticle(nearestX, nearestY, "red", ParticleKind.BLOOD)
                ant.attackCooldown = 20
            }
        }
    } else {
        var brave = false
        if (ant.type == AntKind.SOLDIER) {
            brave = true
        } else {
            val allies = ant.countNearbyAllies(world, 150.0)
            if (allies >= 4) brave = true
        }
        if (brave) {
            ant.speedMultiplier = if (ant.type == AntKind.SOLDIER) 2.0 else 1.3
            val sensorDist = SimConfig.antSensorDist
            val sensorAngle = SimConfig.antSensorAngle
            fun danger(a: Double): Double {
                return ant.colony.outdoorField.get(
                    ant.x + cos(ant.angle + a) * sensorDist,
                    ant.y + sin(ant.angle + a) * sensorDist,
                    Pheromone.DANGER,
                )
            }
            val l = danger(-sensorAngle)
            val c = danger(0.0)
            val r = danger(sensorAngle)
            if (c < 0.01 && l < 0.01 && r < 0.01) {
                ant.state = AntState.FORAGING
                ant.speedMultiplier = 1.0
                return
            }
            if (c > l && c > r) {
                // straight
            } else if (l > r) ant.angle -= SimConfig.antTurnSpeed * 2.0
            else ant.angle += SimConfig.antTurnSpeed * 2.0
            ant.move(world)
            return
        } else {
            ant.speedMultiplier = 1.0
            ant.state = AntState.FORAGING
        }
    }
}
