package com.neojou.myants.sim

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// Ant state handlers: nest work (nursing, resting, transporting, hunger, digging).

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
