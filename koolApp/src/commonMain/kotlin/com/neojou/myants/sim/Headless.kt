package com.neojou.myants.sim

import kotlin.math.floor

data class HeadlessMetrics(
    val ticks: Int,
    val population: Int,
    val workers: Int,
    val soldiers: Int,
    val brood: Int,
    val larvae: Int,
    val sugar: Int,
    val protein: Int,
    val queenEnergy: Int,
    val extraChambers: Int,
    val peakPopulation: Int,
    val minPopulation: Int,
)

/**
 * Seed, build a world, tick, and restore [SimConfig.colonyCount] to 1.
 * Population metrics follow TS `world.ants` (colony 0 only).
 */
fun runHeadless(seed: Int, ticks: Int, colonies: Int = 1): HeadlessMetrics {
    Rng.seed(seed)
    SimConfig.colonyCount = colonies
    try {
        val world = World()
        var peak = world.ants.size
        var minPop = world.ants.size
        for (i in 0 until ticks) {
            world.update()
            val n = world.ants.size
            if (n > peak) peak = n
            if (n < minPop) minPop = n
        }
        var workers = 0
        var soldiers = 0
        for (ant in world.ants) {
            if (ant.type == AntKind.WORKER) workers++
            else if (ant.type == AntKind.SOLDIER) soldiers++
        }
        return HeadlessMetrics(
            ticks = ticks,
            population = world.ants.size,
            workers = workers,
            soldiers = soldiers,
            brood = world.brood.size,
            larvae = world.larvae,
            sugar = floor(world.sugarStockpile).toInt(),
            protein = floor(world.proteinStockpile).toInt(),
            queenEnergy = floor(world.queen.energy).toInt(),
            extraChambers = world.nest.extraChambers,
            peakPopulation = peak,
            minPopulation = minPop,
        )
    } finally {
        SimConfig.colonyCount = 1
    }
}
