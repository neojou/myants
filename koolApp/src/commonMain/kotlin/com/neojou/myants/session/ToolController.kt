package com.neojou.myants.session

import com.neojou.myants.SimTool
import com.neojou.myants.ui.InspectCopy
import com.neojou.myants.sim.Ant
import com.neojou.myants.sim.AntPlace
import com.neojou.myants.sim.FoodKind
import com.neojou.myants.sim.InsectKind
import com.neojou.myants.sim.World
import de.fabmax.kool.modules.ui2.mutableStateOf

/**
 * Active map tool, placement and Inspect hit tests. Moved verbatim from AppSession (#7 step 2).
 * The world is passed in because AppSession replaces it on restart.
 */
class ToolController {
    var tool: SimTool = SimTool.INSPECT
    var inspectedAnt: Ant? = null
    val inspect = mutableStateOf("")

    /** Applies the current tool at world position ([wx], [wy]). */
    fun apply(world: World, wx: Double, wy: Double) {
        when (tool) {
            SimTool.SUGAR -> world.placeFood(wx, wy, FoodKind.SUGAR)
            SimTool.PROTEIN -> world.placeFood(wx, wy, FoodKind.PROTEIN)
            SimTool.ENEMY -> world.spawnEnemyAt(wx, wy, InsectKind.PREDATOR)
            SimTool.INSPECT -> inspectNearest(world, wx, wy)
        }
    }

    fun clear() {
        inspectedAnt = null
        inspect.value = ""
    }

    fun inspectNearest(world: World, wx: Double, wy: Double) {
        var best = Double.MAX_VALUE
        var text = ""
        inspectedAnt = null
        fun consider(x: Double, y: Double, label: String, ant: Ant? = null) {
            val dx = x - wx
            val dy = y - wy
            val d = dx * dx + dy * dy
            if (d < best && d <= 30.0 * 30.0) {
                best = d
                text = label
                inspectedAnt = ant
            }
        }
        for (colony in world.colonies) {
            for (ant in colony.ants) {
                if (ant.location != AntPlace.WORLD) continue
                consider(ant.x, ant.y, antLine(ant), ant)
            }
        }
        for (food in world.foods) {
            consider(food.x, food.y, InspectCopy.foodLine(food.type, food.amount.toInt()))
        }
        for (insect in world.insects) {
            consider(insect.x, insect.y, InspectCopy.insectLine(insect.type, insect.health.toInt()))
        }
        inspect.value = text
    }

    /** Hit test for the 3D nest cutaway (colony 0, nest coordinates). */
    fun inspectNest(world: World, nx: Double, ny: Double) {
        var best = Double.MAX_VALUE
        var text = ""
        inspectedAnt = null
        val colony = world.colonies[0]
        for (ant in colony.ants) {
            if (ant.location != AntPlace.NEST) continue
            val dx = ant.x - nx
            val dy = ant.y - ny
            val d = dx * dx + dy * dy
            if (d < best && d <= 30.0 * 30.0) {
                best = d
                text = antLine(ant)
                inspectedAnt = ant
            }
        }
        val queen = colony.queen
        val qdx = queen.x - nx
        val qdy = queen.y - ny
        val qd = qdx * qdx + qdy * qdy
        if (qd < best && qd <= 40.0 * 40.0) {
            text = InspectCopy.queenLine(queen.energy.toInt(), queen.age)
            inspectedAnt = null
        }
        inspect.value = text
    }

    fun antLine(ant: Ant): String = InspectCopy.antLine(ant)
}
