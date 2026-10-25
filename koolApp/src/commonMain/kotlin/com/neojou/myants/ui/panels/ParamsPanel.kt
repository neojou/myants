package com.neojou.myants.ui.panels

import com.neojou.myants.setTunable
import com.neojou.myants.resetTunables
import com.neojou.myants.sim.SimConfig
import kotlin.math.roundToInt
import com.neojou.myants.AppSession
import com.neojou.myants.ui.MyantsTheme
import com.neojou.myants.ui.UiText
import com.neojou.myants.ui.tr
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.scene.Scene

private data class SliderSpec(val path: String, val label: UiText, val min: Double, val max: Double, val step: Double)

private val SLIDERS = listOf(
    SliderSpec("antSpeed", UiText.PARAM_ANT_SPEED, 0.5, 6.0, 0.1),
    SliderSpec("antEnergyDecay", UiText.PARAM_ENERGY_USE, 0.05, 1.0, 0.01),
    SliderSpec("queenLayInterval", UiText.PARAM_QUEEN_LAY, 50.0, 800.0, 10.0),
    SliderSpec("ant.lifespan", UiText.PARAM_LIFESPAN, 6000.0, 80000.0, 2000.0),
    SliderSpec("eggCost", UiText.PARAM_EGG_COST, 5.0, 60.0, 1.0),
    SliderSpec("sugarValue", UiText.PARAM_SUGAR_VALUE, 1.0, 40.0, 1.0),
    SliderSpec("proteinValue", UiText.PARAM_PROTEIN_VALUE, 1.0, 30.0, 1.0),
    SliderSpec("sugarEnergyValue", UiText.PARAM_SUGAR_ENERGY, 20.0, 300.0, 10.0),
    SliderSpec("colonyUpkeep", UiText.PARAM_COLONY_UPKEEP, 0.0, 0.01, 0.0005),
    SliderSpec("broodProteinUpkeep", UiText.PARAM_BROOD_UPKEEP, 0.0, 0.01, 0.0005),
    SliderSpec("pheromone.diffusionRate", UiText.PARAM_PHEROMONE_SPREAD, 0.0, 0.5, 0.01),
    SliderSpec("pheromone.decay", UiText.PARAM_PHEROMONE_DECAY, 0.9, 0.999, 0.001),
    SliderSpec("pheromone.foodDecay", UiText.PARAM_FOOD_TRAIL_DECAY, 0.9, 0.999, 0.001),
    SliderSpec("antSensorDist", UiText.PARAM_TRAIL_SENSOR, 10.0, 120.0, 5.0),
    SliderSpec("ant.dispersalRadius", UiText.PARAM_DISPERSAL_RADIUS, 0.0, 600.0, 20.0),
    SliderSpec("ant.dispersalStrength", UiText.PARAM_DISPERSAL_STRENGTH, 0.0, 0.4, 0.02),
    SliderSpec("ant.memoryBias", UiText.PARAM_SITE_FIDELITY, 0.0, 1.0, 0.05),
    SliderSpec("ant.proteinForagerShare", UiText.PARAM_PROTEIN_FORAGER, 0.0, 0.8, 0.05),
    SliderSpec("broodProteinUpkeep", UiText.PARAM_BROOD_UPKEEP_LIMIT, 0.0, 0.005, 0.0005),
    SliderSpec("brood.soldierFoodThreshold", UiText.PARAM_SOLDIER_FOOD, 100.0, 2000.0, 100.0),
    SliderSpec("brood.soldierProteinLevel", UiText.PARAM_SOLDIER_PROTEIN, 0.0, 500.0, 25.0),
    SliderSpec("sugarSourceCount", UiText.PARAM_SUGAR_SOURCES, 1.0, 10.0, 1.0),
    SliderSpec("predatorSpawnRate", UiText.PARAM_PREDATOR_SPAWN, 0.0, 0.005, 0.0001),
)

internal fun UiScope.params(session: AppSession) {
    Button(tr(UiText.RESET)) {
        modifier.margin(top = MyantsTheme.space.s2).onClick { session.resetTunables() }
    }
    ScrollArea(height = 360.dp, withHorizontalScrollbar = false) {
        Column {
            for (spec in SLIDERS) {
                val value = SimConfig.getNumber(spec.path)
                Text("${tr(spec.label)} ${fmt(value)}") { modifier.margin(top = MyantsTheme.space.s2) }
                Slider(value.toFloat(), spec.min.toFloat(), spec.max.toFloat()) {
                    modifier.width(Grow.Std).onChange {
                        session.setTunable(spec.path, quantize(it.toDouble(), spec.min, spec.max, spec.step))
                    }
                }
            }
        }
    }
}

private fun fmt(value: Double): String {
    val scaled = (value * 10000.0).roundToInt() / 10000.0
    return scaled.toString()
}

private fun quantize(value: Double, min: Double, max: Double, step: Double): Double {
    if (step <= 0.0) return value.coerceIn(min, max)
    val steps = ((value - min) / step).roundToInt()
    return (min + steps * step).coerceIn(min, max)
}
