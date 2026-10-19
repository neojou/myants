package com.neojou.myants.sim

class Queen {
    var x: Double = 0.0
    var y: Double = 0.0
    var state: QueenState = QueenState.IDLE
    var energy: Double = 1000.0
    var maxEnergy: Double = 2000.0
    var stress: Double = 0.0
    var layTimer: Double = 0.0
    var age: Int = 0
    var dead: Boolean = false
    var starveTimer: Int = 0
    lateinit var colony: Colony

    fun update(world: World) {
        if (dead) return
        val danger = colony.outdoorField.get(x, y, Pheromone.DANGER)
        if (danger > 0.1) {
            stress += 1.0
            state = QueenState.STRESSED
        } else {
            stress = if (stress - 0.05 < 0.0) 0.0 else stress - 0.05
        }
        when (state) {
            QueenState.STRESSED -> {
                if (stress < 10) state = QueenState.IDLE
            }
            QueenState.IDLE -> {
                if (energy < maxEnergy && colony.sugarStockpile > 0) {
                    val regen = SimConfig.queenSugarRegen
                    val cost = regen / SimConfig.sugarEnergyValue
                    val spent = if (colony.sugarStockpile < cost) colony.sugarStockpile else cost
                    colony.sugarStockpile -= spent
                    val next = energy + spent * SimConfig.sugarEnergyValue
                    energy = if (next < maxEnergy) next else maxEnergy
                }
                if (energy > 500 && stress < 20 && colony.brood.size < 200) {
                    state = QueenState.LAYING
                    layTimer = SimConfig.queenLayInterval
                }
            }
            QueenState.LAYING -> {
                layTimer--
                if (layTimer <= 0) {
                    if (energy >= SimConfig.eggCost && colony.proteinStockpile >= 5) {
                        energy -= SimConfig.eggCost
                        colony.proteinStockpile -= 5
                        val eggX = x + (Rng.rand() - 0.5) * 8
                        val eggY = y + 20 + (Rng.rand() * 4)
                        colony.brood.add(Brood(eggX, eggY))
                        state = QueenState.IDLE
                    } else {
                        state = QueenState.IDLE
                    }
                }
            }
        }
        energy -= 0.2
        if (energy < 0) energy = 0.0
        if (colony.ants.isEmpty() || energy <= 0) {
            starveTimer++
            if (starveTimer > SimConfig.queenStarveDeathTicks) dead = true
        } else {
            starveTimer = 0
        }
        age++
    }

    fun feed(amount: Double) {
        val next = energy + amount
        energy = if (next < maxEnergy) next else maxEnergy
    }
}
