package com.neojou.myants.sim

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// Ant state handlers: forage, harvest, return, milk.

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
