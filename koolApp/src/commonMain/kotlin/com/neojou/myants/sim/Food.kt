package com.neojou.myants.sim

class Food(
    override var x: Double,
    override var y: Double,
    val type: FoodKind,
    amount: Double,
) : SpatialItem {
    var amount: Double = amount
    var maxAmount: Double = amount
    var corpseType: String? = null
    var corpseAngle: Double = 0.0
    var colonyId: Int? = null
    val seq: Int = nextSeq++

    fun harvest(take: Double): Double {
        val taken = if (amount < take) amount else take
        amount -= taken
        return taken
    }

    fun update() {
        if (type == FoodKind.PROTEIN) {
            amount -= 0.02
        } else if (type == FoodKind.CORPSE) {
            amount -= maxAmount * 0.0005
        }
    }

    companion object {
        private var nextSeq: Int = 0
    }
}
