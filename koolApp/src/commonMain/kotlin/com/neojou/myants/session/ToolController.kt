package com.neojou.myants.session

import com.neojou.myants.SimTool
import com.neojou.myants.sim.Ant
import com.neojou.myants.sim.AntPlace
import com.neojou.myants.sim.FoodKind
import com.neojou.myants.sim.InsectKind
import com.neojou.myants.sim.World
import com.neojou.myants.ui.InspectPanel
import de.fabmax.kool.modules.ui2.mutableStateOf

/**
 * Active map tool, placement and Inspect hit tests. Moved verbatim from AppSession (#7 step 2).
 * The world is passed in because AppSession replaces it on restart.
 */
class ToolController {
    var tool: SimTool = SimTool.INSPECT

    private companion object {
        const val BROOD_PICK = 12.0
    }
    var inspectedAnt: Ant? = null
    /** Historical one-line text. Tests lock the English wording. The HUD shows [inspectPanel]. */
    val inspect = mutableStateOf("")
    internal val inspectPanel = mutableStateOf<InspectPanel?>(null)
    private var target: InspectTarget? = null

    /** Current pick for the selection ring. Null after [clear] or a miss. */
    internal fun inspectTarget(): InspectTarget? = target

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
        target = null
        inspectedAnt = null
        inspect.value = ""
        inspectPanel.value = null
    }

    /**
     * Rewrites the line and panel from the stored pick.
     * Returns whether the shown text changed. Call about once a sim second, not every frame.
     */
    fun refresh(world: World): Boolean {
        val current = target ?: return false
        if (!current.present(world)) {
            clear()
            return true
        }
        val line = current.line()
        val next = current.panel()
        val changed = line != inspect.value || next != inspectPanel.value
        inspectedAnt = current.ant
        inspect.value = line
        inspectPanel.value = next
        return changed
    }

    fun inspectNearest(world: World, wx: Double, wy: Double) {
        var best = Double.MAX_VALUE
        var picked: InspectTarget? = null
        fun consider(x: Double, y: Double, candidate: InspectTarget) {
            val dx = x - wx
            val dy = y - wy
            val d = dx * dx + dy * dy
            if (d < best && d <= 30.0 * 30.0) {
                best = d
                picked = candidate
            }
        }
        for (colony in world.colonies) {
            for (ant in colony.ants) {
                if (ant.location != AntPlace.WORLD) continue
                consider(ant.x, ant.y, InspectTarget.WorldAnt(ant))
            }
        }
        for (food in world.foods) {
            consider(food.x, food.y, InspectTarget.FoodItem(food))
        }
        for (insect in world.insects) {
            consider(insect.x, insect.y, InspectTarget.InsectItem(insect))
        }
        show(picked)
    }

    /** Hit test for the 3D nest cutaway (colony 0, nest coordinates). */
    fun inspectNest(world: World, nx: Double, ny: Double) {
        var best = Double.MAX_VALUE
        var picked: InspectTarget? = null
        val colony = world.colonies[0]
        for (ant in colony.ants) {
            if (ant.location != AntPlace.NEST) continue
            val dx = ant.x - nx
            val dy = ant.y - ny
            val d = dx * dx + dy * dy
            if (d < best && d <= 30.0 * 30.0) {
                best = d
                picked = InspectTarget.NestAnt(ant)
            }
        }
        val queen = colony.queen
        val qd = dist2(queen.x, queen.y, nx, ny)
        if (qd < best && qd <= 40.0 * 40.0) picked = InspectTarget.QueenTarget(colony)
        if (picked == null) {
            var broodBest = Double.MAX_VALUE
            for (brood in colony.brood) {
                if (brood.carrier != null) continue
                val d = dist2(brood.x, brood.y, nx, ny)
                if (d < broodBest && d <= BROOD_PICK * BROOD_PICK) {
                    broodBest = d
                    picked = InspectTarget.BroodItem(brood, colony)
                }
            }
        }
        if (picked == null) {
            val chamber = colony.nest.chamberAt(nx, ny)
            if (chamber != null) picked = InspectTarget.ChamberItem(chamber, colony)
        }
        show(picked)
    }

    private fun dist2(x: Double, y: Double, px: Double, py: Double): Double {
        val dx = x - px
        val dy = y - py
        return dx * dx + dy * dy
    }

    private fun show(picked: InspectTarget?) {
        target = picked
        inspectedAnt = picked?.ant
        inspect.value = picked?.line() ?: ""
        inspectPanel.value = picked?.panel()
    }
}
