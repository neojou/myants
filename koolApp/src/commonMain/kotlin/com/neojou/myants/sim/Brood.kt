package com.neojou.myants.sim

class Brood(
    override var x: Double,
    override var y: Double,
) : SpatialItem {
    var stage: BroodStage = BroodStage.EGG
    var age: Double = 0.0
    var hunger: Double = 0.0
    var carrier: Ant? = null
    var cumulativeFood: Double = 0.0
    var destinedCaste: AntKind? = null

    /** @return true when a pupa is ready to hatch. */
    fun update(): Boolean {
        age += 1.0
        if (stage == BroodStage.EGG) {
            if (age > SimConfig.brood.eggDuration) {
                stage = BroodStage.LARVA
                age = 0.0
                hunger = 0.0
            }
        } else if (stage == BroodStage.LARVA) {
            hunger += SimConfig.brood.hungerRate
            if (hunger > SimConfig.brood.larvaStarveLimit) return false
            if (age > SimConfig.brood.larvaDuration && hunger < 20) {
                destinedCaste = if (cumulativeFood >= SimConfig.brood.soldierFoodThreshold) {
                    AntKind.SOLDIER
                } else {
                    AntKind.WORKER
                }
                stage = BroodStage.PUPA
                age = 0.0
            }
        } else if (stage == BroodStage.PUPA) {
            if (age > SimConfig.brood.pupaDuration) return true
        }
        return false
    }

    fun feed(amount: Double) {
        if (stage == BroodStage.LARVA) {
            val next = hunger - amount
            hunger = if (next < 0.0) 0.0 else next
        }
    }

    fun provision() {
        if (stage == BroodStage.LARVA) cumulativeFood++
    }
}
