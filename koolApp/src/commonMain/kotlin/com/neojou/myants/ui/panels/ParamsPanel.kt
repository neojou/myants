package com.neojou.myants.ui.panels

import com.neojou.myants.sim.SimConfig
import kotlin.math.roundToInt
import com.neojou.myants.AppSession
import com.neojou.myants.ui.MyantsTheme
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.scene.Scene

private data class SliderSpec(val path: String, val label: String, val min: Double, val max: Double, val step: Double)

private val SLIDERS = listOf(
    SliderSpec("antSpeed", "antSpeed", 0.5, 6.0, 0.1),
    SliderSpec("antEnergyDecay", "antEnergyDecay", 0.05, 1.0, 0.01),
    SliderSpec("queenLayInterval", "queenLayInterval (Pop)", 50.0, 800.0, 10.0),
    SliderSpec("ant.lifespan", "ant.lifespan (Pop)", 6000.0, 80000.0, 2000.0),
    SliderSpec("eggCost", "eggCost", 5.0, 60.0, 1.0),
    SliderSpec("sugarValue", "sugarValue", 1.0, 40.0, 1.0),
    SliderSpec("proteinValue", "proteinValue", 1.0, 30.0, 1.0),
    SliderSpec("sugarEnergyValue", "sugarEnergyValue", 20.0, 300.0, 10.0),
    SliderSpec("colonyUpkeep", "colonyUpkeep", 0.0, 0.01, 0.0005),
    SliderSpec("broodProteinUpkeep", "broodProteinUpkeep", 0.0, 0.01, 0.0005),
    SliderSpec("pheromone.diffusionRate", "pheromone.diffusionRate", 0.0, 0.5, 0.01),
    SliderSpec("pheromone.decay", "pheromone.decay", 0.9, 0.999, 0.001),
    SliderSpec("pheromone.foodDecay", "pheromone.foodDecay (Straßen)", 0.9, 0.999, 0.001),
    SliderSpec("antSensorDist", "antSensorDist (Spur-Reichweite)", 10.0, 120.0, 5.0),
    SliderSpec("ant.dispersalRadius", "ant.dispersalRadius (Streuung)", 0.0, 600.0, 20.0),
    SliderSpec("ant.dispersalStrength", "ant.dispersalStrength", 0.0, 0.4, 0.02),
    SliderSpec("ant.memoryBias", "ant.memoryBias (Site Fidelity)", 0.0, 1.0, 0.05),
    SliderSpec("ant.proteinForagerShare", "ant.proteinForagerShare", 0.0, 0.8, 0.05),
    SliderSpec("broodProteinUpkeep", "broodProteinUpkeep (Pop-Limit)", 0.0, 0.005, 0.0005),
    SliderSpec("brood.soldierFoodThreshold", "brood.soldierFoodThreshold", 100.0, 2000.0, 100.0),
    SliderSpec("brood.soldierProteinLevel", "brood.soldierProteinLevel", 0.0, 500.0, 25.0),
    SliderSpec("sugarSourceCount", "sugarSourceCount", 1.0, 10.0, 1.0),
    SliderSpec("predatorSpawnRate", "predatorSpawnRate", 0.0, 0.005, 0.0001),
)

internal fun UiScope.params(session: AppSession) {
    Button("Reset") {
        modifier.margin(top = MyantsTheme.space.s2).onClick { session.resetTunables() }
    }
    ScrollArea(height = 360.dp, withHorizontalScrollbar = false) {
        Column {
            for (spec in SLIDERS) {
                val value = SimConfig.getNumber(spec.path)
                Text("${spec.label} ${fmt(value)}") { modifier.margin(top = MyantsTheme.space.s2) }
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
