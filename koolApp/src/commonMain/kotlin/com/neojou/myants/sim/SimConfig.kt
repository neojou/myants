package com.neojou.myants.sim

import kotlin.math.PI
import kotlin.math.roundToInt

/**
 * Mutable defaults from AntSim_V2 `src/config.ts` at the headless aspect
 * (1000×600, no DOM) which yields a 1400×840 world.
 * Slider and tuner writes go through [setNumber].
 */
object SimConfig {
    const val WORLD_SCALE: Double = 1.4
    const val NEST_FRACTION: Double = 0.28

    var width: Int = 1400
    var height: Int = 840
    var nestWidth: Int = 392
    var nestHeight: Int = 840

    val areaFactor: Double get() = (width.toDouble() * height.toDouble()) / 600_000.0

    var initialWorkers: Int = 15
    var soldierUnlockThreshold: Int = 30
    var colonyCount: Int = 1

    var sugarValue: Double = (10 * WORLD_SCALE).roundToInt().toDouble()
    var proteinValue: Double = (7 * WORLD_SCALE).roundToInt().toDouble()
    var startSugar: Double = 600.0
    var startProtein: Double = 300.0
    var sugarEnergyValue: Double = 100.0
    var colonyUpkeep: Double = 0.001
    var queenSugarRegen: Double = 0.3
    var broodProteinUpkeep: Double = 0.0006

    var antSpeed: Double = 2.5
    var antSensorAngle: Double = PI / 4
    var antSensorDist: Double = 40.0
    var antTurnSpeed: Double = 0.15
    var workerHealth: Double = 20.0
    var soldierHealth: Double = 60.0
    var soldierDamage: Double = 8.0
    var workerDamage: Double = 3.0
    var antMaxEnergy: Double = 2000.0
    var antEnergyDecay: Double = 0.30

    var queenPositionX: Double = width / 2.0
    var queenPositionY: Double = height / 2.0
    var eggCost: Double = 20.0
    var queenLayInterval: Double = 180.0
    var queenStarveDeathTicks: Int = 1200

    var pheromoneDecay: Double = 0.990
    var evaporationRate: Double = 0.02

    object pheromone {
        var resolutionScale: Double = 0.3
        var updateSkip: Int = 4
        var decay: Double = 0.990
        var foodDecay: Double = 0.994
        var dangerDecay: Double = 0.95
        var minThreshold: Double = 0.001
        var diffusionEnabled: Boolean = true
        var diffuseFood: Boolean = false
        var diffusionRate: Double = 0.12
        var depositTrail: Double = 0.5
        var depositFood: Double = 1.0
        var trailRadius: Double = 3.0
        var qualityRef: Double = 300.0
        var minQuality: Double = 0.2
    }

    object brood {
        var eggDuration: Int = 1000
        var larvaDuration: Int = 2000
        var pupaDuration: Int = 1500
        var larvaStarveLimit: Double = 200.0
        var hungerRate: Double = 0.005
        var soldierProteinLevel: Double = 150.0
        var soldierFoodThreshold: Double = 1700.0
        var maxSoldierFraction: Double = 0.20
    }

    object ant {
        var lifespan: Double = 38000.0
        var lifespanJitter: Double = 12000.0
        var detectEnemyRangeSq: Double = 10000.0
        var attackRangeSq: Double = 900.0
        var arriveRangeSq: Double = 400.0
        var queenFeedAmount: Double = 500.0
        var larvaFeedAmount: Double = 50.0
        var hungryThreshold: Double = 200.0
        var foragingHungerThreshold: Double = 600.0
        var nurseEatThreshold: Double = 1000.0
        var restWakeThreshold: Double = 500.0
        var queenCriticalEnergy: Double = 1500.0
        var queenMaintainEnergy: Double = 1900.0
        var queenHungryEnergy: Double = 1800.0
        var dispersalRadius: Double = (280 * WORLD_SCALE).roundToInt().toDouble()
        var dispersalStrength: Double = 0.10
        var nurseAgeFraction: Double = 0.2
        var forageAgeFraction: Double = 0.6
        var forageUrgeYoung: Double = 0.001
        var forageUrgeOld: Double = 0.02
        var memoryBias: Double = 0.3
        var forageEmergencySugar: Double = 200.0
        var forageEmergencyProtein: Double = 40.0
        var proteinForagerShare: Double = 0.4

        object poly {
            var workerSizeMin: Double = 0.80
            var workerSizeRange: Double = 0.25
            var soldierSizeMin: Double = 1.25
            var soldierSizeRange: Double = 0.25
            var speedBias: Double = 0.45
            var upkeepBase: Double = 0.60
        }

        object troph {
            var cropCapacity: Double = 600.0
            var donorMinEnergy: Double = 1400.0
            var recipientHungry: Double = 1700.0
            var donateRadius: Double = 16.0
            var donateChunk: Double = 200.0
            var interval: Int = 6
            var larvaHungry: Double = 25.0
            var larvaRelief: Double = 30.0
            var larvaCropCost: Double = 60.0
        }
    }

    object combat {
        var alarmThreshold: Double = 0.04
        var mobMinAllies: Int = 2
        var mobSuperiority: Double = 1.5
        var grappleRadius: Double = 22.0
        var grappleSlowPerAnt: Double = 0.15
        var grappleMaxSlow: Double = 0.8
        var patrolLongChance: Double = 0.2
        var patrolLongRangeFrac: Double = 0.35
        var mobRushAllies: Int = 5
        var soldierRushAllies: Int = 2
        var rallyStandoffSq: Double = 48.0 * 48.0
        var soldierSightRangeSq: Double = 170.0 * 170.0
        var raidPopEdge: Double = 1.05
        var raidMercyRatio: Double = 1.25
        var raidMinSoldiers: Int = 3
        var raidPartySize: Int = 6
        var raidCooldown: Int = 1500
        var raidArriveRangeSq: Double = 70.0 * 70.0
    }

    object enemy {
        var predatorHealth: Double = 42.0
        var predatorDamage: Double = 6.0
        var spiderHealth: Double = 32.0
        var spiderDamage: Double = 7.0
        var beetleHealth: Double = 88.0
        var beetleDamage: Double = 12.0
    }

    object environment {
        var nightActivityMin: Double = 0.5
        var rainChance: Double = 0.00015
        var rainMinDuration: Int = 400
        var rainMaxDuration: Int = 1000
        var rainWashout: Double = 0.97
        var rainSpeed: Double = 0.7
    }

    var nestScaleRef: Double = 300 * WORLD_SCALE * (NEST_FRACTION / 0.20)
    var nestRouteClearance: Double = 6.0

    object nest {
        var excavateEvery: Int = 10
        var maxExtraChambers: Int = scaleCount(14)
        var storageBaseCapacity: Double = 1200.0
        var storagePerGranary: Double = 600.0
        var digWorkPerChamber: Double = 600.0
        var maxDiggers: Int = 4
    }

    object clock {
        var radius: Double = 150.0
        var fontSize: Double = 68.0
    }

    var obstacleCount: Int = 12
    var sugarSourceCount: Double = scaleCount(4).toDouble()
    var maxPrey: Int = scaleCount(7)
    var preySpawnRate: Double = 0.005 * ((1400.0 * 840.0) / 600_000.0)
    var gracePeriod: Int = 4000
    var predatorSpawnRate: Double = 0.0005
    var spiderSpawnRate: Double = 0.0003
    var beetleSpawnRate: Double = 0.0003
    var ladybugSpawnRate: Double = 0.0005
    var maxSpiders: Int = 1
    var maxBeetles: Int = 1
    var maxLadybugs: Int = 2
    var maxPredators: Int = 2

    fun scaleCount(base: Int): Int {
        val scaled = (base * areaFactor).roundToInt()
        return if (base > scaled) base else scaled
    }

    fun getNumber(path: String): Double = when (path) {
        "antSpeed" -> antSpeed
        "antEnergyDecay" -> antEnergyDecay
        "queenLayInterval" -> queenLayInterval
        "ant.lifespan" -> ant.lifespan
        "eggCost" -> eggCost
        "sugarValue" -> sugarValue
        "proteinValue" -> proteinValue
        "sugarEnergyValue" -> sugarEnergyValue
        "colonyUpkeep" -> colonyUpkeep
        "broodProteinUpkeep" -> broodProteinUpkeep
        "pheromone.diffusionRate" -> pheromone.diffusionRate
        "pheromone.decay" -> pheromone.decay
        "pheromone.foodDecay" -> pheromone.foodDecay
        "antSensorDist" -> antSensorDist
        "ant.dispersalRadius" -> ant.dispersalRadius
        "ant.dispersalStrength" -> ant.dispersalStrength
        "ant.memoryBias" -> ant.memoryBias
        "ant.proteinForagerShare" -> ant.proteinForagerShare
        "brood.soldierFoodThreshold" -> brood.soldierFoodThreshold
        "brood.soldierProteinLevel" -> brood.soldierProteinLevel
        "sugarSourceCount" -> sugarSourceCount
        "predatorSpawnRate" -> predatorSpawnRate
        "initialWorkers" -> initialWorkers.toDouble()
        "colonyCount" -> colonyCount.toDouble()
        "ant.lifespanJitter" -> ant.lifespanJitter
        "ant.forageUrgeYoung" -> ant.forageUrgeYoung
        "ant.forageUrgeOld" -> ant.forageUrgeOld
        "queenSugarRegen" -> queenSugarRegen
        "pheromone.depositTrail" -> pheromone.depositTrail
        "pheromone.depositFood" -> pheromone.depositFood
        "environment.rainChance" -> environment.rainChance
        "environment.nightActivityMin" -> environment.nightActivityMin
        "combat.raidCooldown" -> combat.raidCooldown.toDouble()
        else -> error("Unknown config path: $path")
    }

    fun setNumber(path: String, value: Double) {
        when (path) {
            "antSpeed" -> antSpeed = value
            "antEnergyDecay" -> antEnergyDecay = value
            "queenLayInterval" -> queenLayInterval = value
            "ant.lifespan" -> ant.lifespan = value
            "eggCost" -> eggCost = value
            "sugarValue" -> sugarValue = value
            "proteinValue" -> proteinValue = value
            "sugarEnergyValue" -> sugarEnergyValue = value
            "colonyUpkeep" -> colonyUpkeep = value
            "broodProteinUpkeep" -> broodProteinUpkeep = value
            "pheromone.diffusionRate" -> pheromone.diffusionRate = value
            "pheromone.decay" -> pheromone.decay = value
            "pheromone.foodDecay" -> pheromone.foodDecay = value
            "antSensorDist" -> antSensorDist = value
            "ant.dispersalRadius" -> ant.dispersalRadius = value
            "ant.dispersalStrength" -> ant.dispersalStrength = value
            "ant.memoryBias" -> ant.memoryBias = value
            "ant.proteinForagerShare" -> ant.proteinForagerShare = value
            "brood.soldierFoodThreshold" -> brood.soldierFoodThreshold = value
            "brood.soldierProteinLevel" -> brood.soldierProteinLevel = value
            "sugarSourceCount" -> sugarSourceCount = value
            "predatorSpawnRate" -> predatorSpawnRate = value
            "initialWorkers" -> initialWorkers = value.toInt()
            "colonyCount" -> colonyCount = value.toInt()
            "ant.lifespanJitter" -> ant.lifespanJitter = value
            "ant.forageUrgeYoung" -> ant.forageUrgeYoung = value
            "ant.forageUrgeOld" -> ant.forageUrgeOld = value
            "queenSugarRegen" -> queenSugarRegen = value
            "pheromone.depositTrail" -> pheromone.depositTrail = value
            "pheromone.depositFood" -> pheromone.depositFood = value
            "environment.rainChance" -> environment.rainChance = value
            "environment.nightActivityMin" -> environment.nightActivityMin = value
            "combat.raidCooldown" -> combat.raidCooldown = value.toInt()
            else -> error("Unknown config path: $path")
        }
    }

    /** Live-Parameter Reset. Does not rebuild the world. */
    fun resetTunables() {
        antSpeed = 2.5
        antEnergyDecay = 0.30
        queenLayInterval = 180.0
        ant.lifespan = 38000.0
        eggCost = 20.0
        sugarValue = (10 * WORLD_SCALE).roundToInt().toDouble()
        proteinValue = (7 * WORLD_SCALE).roundToInt().toDouble()
        sugarEnergyValue = 100.0
        colonyUpkeep = 0.001
        broodProteinUpkeep = 0.0006
        pheromone.diffusionRate = 0.12
        pheromone.decay = 0.990
        pheromone.foodDecay = 0.994
        antSensorDist = 40.0
        ant.dispersalRadius = (280 * WORLD_SCALE).roundToInt().toDouble()
        ant.dispersalStrength = 0.10
        ant.memoryBias = 0.3
        ant.proteinForagerShare = 0.4
        brood.soldierFoodThreshold = 1700.0
        brood.soldierProteinLevel = 150.0
        sugarSourceCount = scaleCount(4).toDouble()
        predatorSpawnRate = 0.0005
    }
}
