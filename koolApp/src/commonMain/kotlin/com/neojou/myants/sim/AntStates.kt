package com.neojou.myants.sim

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

fun handleNursing(ant: Ant, world: World) {
    if (ant.carrying != CarryKind.PROTEIN) {
        ant.state = AntState.IDLE
        return
    }
    var target: Any? = null
    if (ant.colony.queen.energy < SimConfig.ant.queenCriticalEnergy) {
        target = ant.colony.queen
    } else {
        val criticalLarva = ant.colony.brood.firstOrNull { it.stage == BroodStage.LARVA && it.hunger > 50.0 }
        if (criticalLarva != null) {
            target = criticalLarva
        } else if (ant.colony.queen.energy < SimConfig.ant.queenMaintainEnergy) {
            target = ant.colony.queen
        } else {
            val hungryLarva = ant.colony.brood.firstOrNull { it.stage == BroodStage.LARVA && it.hunger > 20.0 }
            if (hungryLarva != null) target = hungryLarva
        }
    }
    if (target == null) {
        ant.state = AntState.RETURNING
        return
    }
    val targetX = when (target) {
        is Queen -> target.x
        is Brood -> target.x
        else -> ant.x
    }
    val targetY = when (target) {
        is Queen -> target.y
        is Brood -> target.y
        else -> ant.y
    }
    val dx = targetX - ant.x
    val dy = targetY - ant.y
    val dist = sqrt(dx * dx + dy * dy)
    ant.steerThroughNest(targetX, targetY)
    if (dist < 10.0) {
        if (target === ant.colony.queen) {
            ant.colony.queen.energy += SimConfig.ant.queenFeedAmount
        } else if (target is Brood) {
            target.feed(SimConfig.ant.larvaFeedAmount)
        }
        ant.carryingAmount -= SimConfig.ant.larvaFeedAmount
        ant.carrying = CarryKind.NONE
        ant.carryingAmount = 0.0
        ant.state = AntState.IDLE
    }
}

fun handleResting(ant: Ant, world: World) {
    if (ant.location == AntPlace.WORLD) {
        ant.speedMultiplier = 1.0
        ant.patrolTarget = null
        val home = ant.colony.entranceWorld
        ant.angle = atan2(home.y - ant.y, home.x - ant.x)
    } else {
        if (ant.patrolTarget == null) {
            var currentChamber: Chamber? = null
            for (c in ant.colony.nest.chambers) {
                val dx = ant.x - c.x
                val dy = ant.y - c.y
                val limit = c.radius * 0.9
                if (dx * dx + dy * dy < limit * limit) {
                    currentChamber = c
                    break
                }
            }
            val chamber = currentChamber
            if (chamber != null) {
                if (chamber.type == ChamberRole.STORAGE && Rng.rand() < 0.8) {
                    val otherChambers = ant.colony.nest.chambers.filter { it !== chamber }
                    if (otherChambers.isNotEmpty()) {
                        ant.wander()
                        ant.applySeparation(world)
                        return
                    }
                }
                val angle = Rng.rand() * PI * 2.0
                val r = Rng.rand() * (chamber.radius * 0.7)
                ant.patrolTarget = Vec2(
                    chamber.x + cos(angle) * r,
                    chamber.y + sin(angle) * r,
                )
            } else {
                ant.wander()
                ant.applySeparation(world)
                return
            }
        }
        val bed = ant.patrolTarget ?: return
        val dx = bed.x - ant.x
        val dy = bed.y - ant.y
        val distSq = dx * dx + dy * dy
        if (distSq < 100.0) {
            ant.speedMultiplier = 0.0
            ant.restTimer -= 1.0
            if (ant.restTimer <= 0.0 || ant.energy < SimConfig.ant.restWakeThreshold) {
                ant.state = AntState.IDLE
                ant.patrolTarget = null
            }
        } else {
            ant.speedMultiplier = 0.5
            ant.angle = atan2(dy, dx)
            ant.applySeparation(world)
        }
    }
}

fun handleNurseIdle(ant: Ant, world: World) {
    if (ant.location == AntPlace.WORLD) {
        val home = ant.colony.entranceWorld
        ant.angle = atan2(home.y - ant.y, home.x - ant.x)
        return
    }
    if (ant.colony.digSite != null && ant.type == AntKind.WORKER && ant.energy > SimConfig.ant.hungryThreshold) {
        var diggers = 0
        for (a in ant.colony.ants) if (a.state == AntState.DIGGING) diggers++
        if (diggers < SimConfig.nest.maxDiggers) {
            ant.state = AntState.DIGGING
            return
        }
    }
    if (Rng.rand() < 0.001) {
        ant.state = AntState.RESTING
        ant.restTimer = 300.0 + Rng.rand() * 300.0
        return
    }
    if (ant.energy < SimConfig.ant.nurseEatThreshold) {
        val storage = ant.colony.nest.getChamber(ChamberRole.STORAGE)
        val dx = storage.x - ant.x
        val dy = storage.y - ant.y
        val distSq = dx * dx + dy * dy
        if (distSq < SimConfig.ant.arriveRangeSq) {
            ant.eatFromStockpile(world)
        } else {
            ant.steerThroughNest(storage.x, storage.y)
        }
        return
    }
    if (ant.carrying == CarryKind.NONE) {
        val broodChambers = ant.colony.nest.getChambers(ChamberRole.BROOD)
        val broodHomes = if (broodChambers.isNotEmpty()) broodChambers else listOf(ant.colony.nest.getChamber(ChamberRole.BROOD))
        if (broodHomes.isNotEmpty()) {
            val misplacedBrood = ant.colony.brood.firstOrNull { b ->
                if (b.carrier != null) return@firstOrNull false
                for (ch in broodHomes) {
                    val dx = b.x - ch.x
                    val dy = b.y - ch.y
                    if (dx * dx + dy * dy <= ch.radius * ch.radius) return@firstOrNull false
                }
                true
            }
            if (misplacedBrood != null) {
                val dx = misplacedBrood.x - ant.x
                val dy = misplacedBrood.y - ant.y
                val distSq = dx * dx + dy * dy
                if (distSq < SimConfig.ant.arriveRangeSq) {
                    ant.carrying = CarryKind.BROOD
                    ant.carryingInstance = misplacedBrood
                    misplacedBrood.carrier = ant
                    ant.state = AntState.TRANSPORTING
                    val home = ant.colony.nest.nearestChamber(ChamberRole.BROOD, misplacedBrood.x, misplacedBrood.y)
                        ?: ant.colony.nest.getChamber(ChamberRole.BROOD)
                    ant.carryTarget = Vec2(home.x, home.y)
                } else {
                    ant.steerThroughNest(misplacedBrood.x, misplacedBrood.y)
                }
                return
            }
        }
        if (ant.colony.proteinStockpile >= 10.0) {
            val queenHungry = ant.colony.queen.energy < SimConfig.ant.queenHungryEnergy
            val larvaHungry = ant.colony.brood.any { it.stage == BroodStage.LARVA && it.hunger > 20.0 }
            if (queenHungry || larvaHungry) {
                val storage = ant.colony.nest.getChamber(ChamberRole.STORAGE)
                val dx = storage.x - ant.x
                val dy = storage.y - ant.y
                if (dx * dx + dy * dy < SimConfig.ant.arriveRangeSq) {
                    ant.colony.proteinStockpile -= 10.0
                    ant.carrying = CarryKind.PROTEIN
                    ant.carryingAmount = 10.0
                    ant.state = AntState.NURSING
                } else {
                    ant.steerThroughNest(storage.x, storage.y)
                }
                return
            }
        }
    }
    if (ant.type == AntKind.WORKER) {
        val emergency = ant.colony.sugarStockpile < SimConfig.ant.forageEmergencySugar ||
            ant.colony.proteinStockpile < SimConfig.ant.forageEmergencyProtein
        if (emergency || Rng.rand() < ant.forageUrge()) {
            ant.state = AntState.FORAGING
            return
        }
    }
    if (Rng.rand() < 0.005) {
        ant.state = AntState.RESTING
        ant.restTimer = 300.0 + Rng.rand() * 300.0
    } else {
        ant.wander()
        ant.speedMultiplier = 0.3
        ant.applySeparation(world)
    }
}

fun handleTransporting(ant: Ant, world: World) {
    val brood = ant.carryingInstance as? Brood
    if (brood == null) {
        ant.state = AntState.IDLE
        ant.carrying = CarryKind.NONE
        ant.carryTarget = null
        return
    }
    brood.x = ant.x
    brood.y = ant.y
    val target = ant.carryTarget
    val broodChamber = (if (target != null) ant.colony.nest.nearestChamber(ChamberRole.BROOD, target.x, target.y) else null)
        ?: ant.colony.nest.getChamber(ChamberRole.BROOD)
    val dx = broodChamber.x - ant.x
    val dy = broodChamber.y - ant.y
    val distSq = dx * dx + dy * dy
    val limit = broodChamber.radius * 0.8
    if (distSq < limit * limit) {
        ant.wander()
        if (Rng.rand() < 0.05) {
            brood.carrier = null
            ant.carryingInstance = null
            ant.carrying = CarryKind.NONE
            ant.carryTarget = null
            ant.state = AntState.IDLE
            ant.angle += PI
        }
    } else {
        ant.angle = atan2(dy, dx)
    }
}

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

fun handleForaging(ant: Ant, world: World) {
    if (ant.energy < SimConfig.ant.foragingHungerThreshold) {
        ant.state = AntState.HUNGRY
        return
    }
    if (ant.obstacleTimer > 0) return
    if (ant.obstacleTimer > 0) return
    if (world.raining) {
        if (ant.location == AntPlace.WORLD) {
            val home = ant.colony.entranceWorld
            ant.angle = atan2(home.y - ant.y, home.x - ant.x) + (Rng.rand() - 0.5) * 0.4
            ant.speedMultiplier = 1.2
        } else {
            ant.speedMultiplier = 0.2
            ant.angle += (Rng.rand() - 0.5) * 0.6
        }
        return
    }
    if (ant.location == AntPlace.NEST) {
        val entrance = ant.colony.nest.getEntrance()
        val targetX = entrance.x
        val targetY = entrance.y
        val nextNode = ant.colony.nest.getNextNodeTowards(ant.x, ant.y, targetX, targetY)
        if (nextNode != null) {
            val ang = atan2(nextNode.y - ant.y, nextNode.x - ant.x)
            ant.angle = ang + (Rng.rand() - 0.5) * 0.5
        } else {
            ant.angle = atan2(targetY - ant.y, targetX - ant.x) + (Rng.rand() - 0.5) * 1.0
        }
        return
    }
    if (ant.exitTimer > 0) {
        ant.exitTimer--
        ant.angle = ant.colony.worldExitAngle
        ant.angle += (Rng.rand() - 0.5) * 1.0
        return
    }
    val proteinLow = ant.colony.proteinStockpile < SimConfig.eggCost * 25.0
    val sugarLow = ant.colony.sugarStockpile < 1000.0
    var prioritizeProtein = false
    if (ant.type == AntKind.SOLDIER) {
        prioritizeProtein = true
    } else {
        val share = SimConfig.ant.proteinForagerShare
        prioritizeProtein = if (proteinLow && sugarLow) {
            ant.forageSeed < share
        } else if (proteinLow) {
            true
        } else if (sugarLow) {
            false
        } else {
            ant.forageSeed < share * 0.6
        }
    }
    val wantProtein = prioritizeProtein
    val candidates = world.foodGrid.getNearby(ant.x, ant.y, sqrt(SimConfig.ant.detectEnemyRangeSq)).sortedBy { it.seq }
    for (food in candidates) {
        if (food.amount <= 0.0) continue
        if (ant.type == AntKind.SOLDIER && food.type == FoodKind.SUGAR) continue
        if (ant.type == AntKind.WORKER && ant.energy > SimConfig.ant.foragingHungerThreshold) {
            val matches = if (wantProtein) {
                food.type == FoodKind.PROTEIN || food.type == FoodKind.CORPSE
            } else {
                food.type == FoodKind.SUGAR
            }
            if (!matches) continue
        }
        val dx = food.x - ant.x
        val dy = food.y - ant.y
        val distSq = dx * dx + dy * dy
        val foodRadius = max(8.0, sqrt(food.amount) * 0.35)
        val harvestRange = foodRadius + 5.0
        val harvestRangeSq = harvestRange * harvestRange
        if (distSq < harvestRangeSq) {
            ant.state = AntState.HARVESTING
            ant.harvestTimer = if (food.type == FoodKind.SUGAR) 60 else 120
            ant.carryingInstance = food
            return
        } else if (distSq < SimConfig.ant.detectEnemyRangeSq) {
            if (ant.stuckTimer > 20) {
                world.addParticle(ant.x, ant.y, "yellow")
                ant.angle += PI + (Rng.rand() - 0.5)
                ant.obstacleTimer = 45
                ant.stuckTimer = 0
                return
            }
            ant.angle = atan2(dy, dx)
            ant.angle += (Rng.rand() - 0.5) * 0.1
            return
        }
    }
    if (ant.thinkTimer > 0) {
        ant.thinkTimer--
    } else {
        ant.thinkTimer = 3 + floor(Rng.rand() * 3.0).toInt()
        val dangerLevel = ant.colony.outdoorField.get(ant.x, ant.y, Pheromone.DANGER)
        if (dangerLevel > SimConfig.combat.alarmThreshold) {
            val allies = ant.countNearbyAllies(world, 100.0)
            val enemies = ant.countNearbyEnemies(world, 100.0)
            if (allies >= SimConfig.combat.mobMinAllies &&
                allies.toDouble() >= max(1.0, enemies.toDouble()) * SimConfig.combat.mobSuperiority
            ) {
                ant.state = AntState.ATTACKING
            } else {
                ant.state = AntState.FLEEING
                ant.fleeTimer = 30
                ant.angle += PI
            }
            return
        }
        if (ant.location == AntPlace.WORLD) {
            var rivals = 0
            val near = world.spatialGrid.getNearby(ant.x, ant.y, 60.0)
            for (o in near) {
                if (o.colony !== ant.colony && o.location == AntPlace.WORLD && o.type != AntKind.QUEEN) {
                    val dx = o.x - ant.x
                    val dy = o.y - ant.y
                    if (dx * dx + dy * dy < 3600.0) rivals++
                }
            }
            if (rivals > 0) {
                val allies = ant.countNearbyAllies(world, 100.0)
                if (allies >= SimConfig.combat.mobMinAllies && allies.toDouble() >= rivals.toDouble() * SimConfig.combat.mobSuperiority) {
                    ant.state = AntState.ATTACKING
                } else {
                    ant.state = AntState.FLEEING
                    ant.fleeTimer = 30
                    ant.angle += PI
                }
                return
            }
        }
        val huntCandidates = world.insectGrid.getNearby(ant.x, ant.y, 70.0).sortedBy { it.seq }
        for (insect in huntCandidates) {
            val isPrey = insect.type == InsectKind.PREY || insect.type == InsectKind.BEETLE ||
                insect.type == InsectKind.LADYBUG || insect.type == InsectKind.SPIDER ||
                insect.type == InsectKind.PREDATOR
            if (!isPrey) continue
            val dx = ant.x - insect.x
            val dy = ant.y - insect.y
            val distSq = dx * dx + dy * dy
            if (ant.type == AntKind.SOLDIER || prioritizeProtein || distSq < 2500.0) {
                if (distSq < 4900.0) {
                    if (ant.type == AntKind.WORKER) {
                        if (insect.type == InsectKind.BEETLE) {
                            val allies = ant.countNearbyAllies(world, 80.0)
                            if (allies < 5) continue
                        }
                        if (insect.type == InsectKind.SPIDER || insect.type == InsectKind.PREDATOR) {
                            val allies = ant.countNearbyAllies(world, 100.0)
                            if (allies < 2) continue
                        }
                    }
                    ant.state = AntState.ATTACKING
                    return
                }
            }
        }
        if (ant.type == AntKind.WORKER) {
            val aphidCandidates = world.insectGrid.getNearby(ant.x, ant.y, sqrt(SimConfig.ant.detectEnemyRangeSq)).sortedBy { it.seq }
            for (insect in aphidCandidates) {
                if (insect.type != InsectKind.APHID) continue
                val dx = ant.x - insect.x
                val dy = ant.y - insect.y
                val distSq = dx * dx + dy * dy
                if (distSq < SimConfig.ant.attackRangeSq) {
                    ant.state = AntState.MILKING
                    ant.harvestTimer = 60
                    ant.carryingInstance = insect
                    return
                } else if (distSq < SimConfig.ant.detectEnemyRangeSq) {
                    ant.angle = atan2(dy, dx)
                    return
                }
            }
        }
    }
    val targetPheromone = if (prioritizeProtein) Pheromone.PROTEIN else Pheromone.SUGAR
    val foundFood = ant.senseAndSteer(world, targetPheromone)
    if (!foundFood) {
        if (!ant.steerToMemory() && !ant.disperseFromNest(world)) ant.wander()
    }
    ant.colony.outdoorField.depositCircle(ant.x, ant.y, Pheromone.HOME, SimConfig.pheromone.depositTrail, SimConfig.pheromone.trailRadius)
}

fun handleHarvesting(ant: Ant, world: World) {
    ant.harvestTimer--
    ant.speedMultiplier = 0.0
    if (ant.harvestTimer <= 0) {
        val food = ant.carryingInstance as? Food
        if (food != null && food.amount > 0.0) {
            if (food.type == FoodKind.CORPSE && food.corpseType == "ANT" &&
                ant.colony.nest.getChambers(ChamberRole.CEMETERY).isNotEmpty()
            ) {
                ant.carrying = CarryKind.CORPSE
                ant.carryingInstance = food
                val idx = world.foods.indexOf(food)
                if (idx >= 0) world.foods.removeAt(idx)
                world.foodGrid.remove(food)
                ant.state = AntState.RETURNING
                ant.energy = SimConfig.antMaxEnergy
                ant.angle += PI
                return
            }
            food.harvest(1.0)
            ant.carrying = if (food.type == FoodKind.CORPSE) CarryKind.PROTEIN else when (food.type) {
                FoodKind.SUGAR -> CarryKind.SUGAR
                FoodKind.PROTEIN -> CarryKind.PROTEIN
                FoodKind.CORPSE -> CarryKind.PROTEIN
            }
            ant.state = AntState.RETURNING
            ant.energy = SimConfig.antMaxEnergy
            ant.cropSugar = SimConfig.ant.troph.cropCapacity
            ant.angle += PI
            ant.foodMemoryX = food.x
            ant.foodMemoryY = food.y
            ant.carryingQuality = max(
                SimConfig.pheromone.minQuality,
                min(1.0, food.amount / SimConfig.pheromone.qualityRef),
            )
            ant.carryingInstance = null
        } else {
            ant.state = AntState.FORAGING
            ant.carryingInstance = null
        }
    }
}

fun handleReturning(ant: Ant, world: World) {
    if (ant.obstacleTimer > 0) return
    if (ant.stuckTimer > 20) {
        ant.angle += PI + (Rng.rand() - 0.5)
        ant.stuckTimer = 0
        ant.obstacleTimer = 10
        return
    }
    if (ant.carrying == CarryKind.CORPSE) {
        val corpse = ant.carryingInstance as? Food
        if (ant.location == AntPlace.WORLD) {
            val home = ant.colony.entranceWorld
            if (corpse != null) {
                corpse.x = ant.x
                corpse.y = ant.y
            }
            ant.angle = atan2(home.y - ant.y, home.x - ant.x)
            return
        }
        val grave = ant.colony.nest.nearestChamber(ChamberRole.CEMETERY, ant.x, ant.y)
        if (grave == null) {
            ant.colony.proteinStockpile = min(ant.colony.storageCapacity(), ant.colony.proteinStockpile + SimConfig.proteinValue)
            ant.carrying = CarryKind.NONE
            ant.carryingInstance = null
            ant.state = AntState.IDLE
            return
        }
        val dx = grave.x - ant.x
        val dy = grave.y - ant.y
        val limit = grave.radius * 0.7
        if (dx * dx + dy * dy < limit * limit) {
            if (corpse != null) {
                corpse.x = grave.x + (Rng.rand() - 0.5) * grave.radius
                corpse.y = grave.y + (Rng.rand() - 0.5) * grave.radius
                corpse.colonyId = ant.colony.id
                world.graveyard.add(corpse)
            }
            ant.carrying = CarryKind.NONE
            ant.carryingInstance = null
            ant.state = AntState.IDLE
            ant.angle += PI
            return
        }
        val nextNode = ant.colony.nest.getNextNodeTowards(ant.x, ant.y, grave.x, grave.y)
        ant.angle = if (nextNode != null) atan2(nextNode.y - ant.y, nextNode.x - ant.x) else atan2(dy, dx)
        return
    }
    val grid = if (ant.location == AntPlace.NEST) ant.colony.nestGrid else ant.colony.outdoorField
    val trail = SimConfig.pheromone.depositFood * ant.carryingQuality
    if (ant.carrying == CarryKind.SUGAR) {
        grid.depositCircle(ant.x, ant.y, Pheromone.SUGAR, trail, SimConfig.pheromone.trailRadius)
    } else if (ant.carrying == CarryKind.PROTEIN) {
        grid.depositCircle(ant.x, ant.y, Pheromone.PROTEIN, trail, SimConfig.pheromone.trailRadius)
    }
    if (ant.location == AntPlace.WORLD) {
        val home = ant.colony.entranceWorld
        val angleToHome = atan2(home.y - ant.y, home.x - ant.x)
        var threat: Insect? = null
        var threatD2 = SimConfig.ant.detectEnemyRangeSq
        for (ins in world.insectGrid.getNearby(ant.x, ant.y, sqrt(SimConfig.ant.detectEnemyRangeSq))) {
            if (ins.type == InsectKind.PREDATOR || ins.type == InsectKind.SPIDER || ins.type == InsectKind.BEETLE) {
                val d2 = (ins.x - ant.x) * (ins.x - ant.x) + (ins.y - ant.y) * (ins.y - ant.y)
                if (d2 < threatD2) {
                    threatD2 = d2
                    threat = ins
                }
            }
        }
        val threatIns = threat
        if (threatIns != null) {
            ant.colony.outdoorField.depositCircle(ant.x, ant.y, Pheromone.DANGER, SimConfig.pheromone.depositTrail, 10.0)
            ant.angle = atan2(ant.y - threatIns.y, ant.x - threatIns.x)
            var dh = angleToHome - ant.angle
            while (dh < -PI) dh += PI * 2.0
            while (dh > PI) dh -= PI * 2.0
            ant.angle += dh * 0.4
            ant.speedMultiplier = 2.0
            return
        }
        val foundTrail = ant.senseAndSteer(world, Pheromone.HOME)
        val biasStrength = if (foundTrail) 0.15 else 0.3
        var diff = angleToHome - ant.angle
        while (diff < -PI) diff += PI * 2.0
        while (diff > PI) diff -= PI * 2.0
        ant.angle += diff * biasStrength
    } else {
        val storage = ant.colony.nest.getChamber(ChamberRole.STORAGE)
        val dx = storage.x - ant.x
        val dy = storage.y - ant.y
        val distSq = dx * dx + dy * dy
        if (distSq < 2500.0) {
            val cap = ant.colony.storageCapacity()
            when (ant.carrying) {
                CarryKind.SUGAR -> ant.colony.sugarStockpile = min(cap, ant.colony.sugarStockpile + SimConfig.sugarValue)
                CarryKind.PROTEIN -> ant.colony.proteinStockpile = min(cap, ant.colony.proteinStockpile + SimConfig.proteinValue)
                CarryKind.BROOD -> ant.colony.brood.add(Brood(ant.colony.queen.x, ant.colony.queen.y))
                else -> {}
            }
            ant.carrying = CarryKind.NONE
            if (ant.energy < SimConfig.ant.nurseEatThreshold) ant.state = AntState.HUNGRY
            else ant.state = AntState.IDLE
            ant.angle += PI
            return
        } else {
            val nextNode = ant.colony.nest.getNextNodeTowards(ant.x, ant.y, storage.x, storage.y)
            if (nextNode != null) {
                ant.angle = atan2(nextNode.y - ant.y, nextNode.x - ant.x)
            } else {
                ant.angle = atan2(dy, dx)
            }
        }
    }
}

fun handleHungry(ant: Ant, world: World) {
    if (ant.location == AntPlace.WORLD) {
        ant.state = AntState.RETURNING
        return
    }
    val storage = ant.colony.nest.getChamber(ChamberRole.STORAGE)
    val dx = storage.x - ant.x
    val dy = storage.y - ant.y
    val distSq = dx * dx + dy * dy
    if (distSq < SimConfig.ant.arriveRangeSq) {
        ant.eatFromStockpile(world)
        ant.state = if (ant.type == AntKind.SOLDIER) AntState.PATROLLING else AntState.FORAGING
    } else {
        val nextNode = ant.colony.nest.getNextNodeTowards(ant.x, ant.y, storage.x, storage.y)
        if (nextNode != null) ant.angle = atan2(nextNode.y - ant.y, nextNode.x - ant.x)
        else ant.angle = atan2(dy, dx)
    }
}

fun handleMilking(ant: Ant, world: World) {
    ant.harvestTimer--
    ant.speedMultiplier = 0.0
    val aphid = ant.carryingInstance as? Insect
    if (aphid != null) {
        val dx = aphid.x - ant.x
        val dy = aphid.y - ant.y
        ant.angle = atan2(dy, dx)
    }
    if (ant.harvestTimer <= 0) {
        ant.carrying = CarryKind.SUGAR
        ant.carryingAmount = 200.0
        ant.carryingQuality = 0.7
        ant.state = AntState.RETURNING
        ant.energy = SimConfig.antMaxEnergy
        ant.angle += PI
        ant.carryingInstance = null
        world.addParticle(ant.x, ant.y, "#FFD700", ParticleKind.DEFAULT)
    }
}

fun handleDigging(ant: Ant, world: World) {
    val col = ant.colony
    val site = col.digSite
    if (site == null || ant.location != AntPlace.NEST) {
        ant.state = AntState.IDLE
        return
    }
    if (ant.energy < SimConfig.ant.hungryThreshold) {
        ant.state = AntState.HUNGRY
        return
    }
    val dx = site.x - ant.x
    val dy = site.y - ant.y
    if (dx * dx + dy * dy > SimConfig.ant.arriveRangeSq * 4.0) {
        ant.steerThroughNest(site.x, site.y)
        return
    }
    ant.speedMultiplier = 0.15
    ant.angle += 0.35
    col.digProgress++
    if (col.id == 0 && col.digProgress % 40 == 0) {
        world.nestParticles.add(NestParticle(ant.x, ant.y, 0.0, -0.2, 0.8, null))
    }
}
