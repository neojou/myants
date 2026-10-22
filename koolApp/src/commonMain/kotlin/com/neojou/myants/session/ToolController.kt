package com.neojou.myants.session

import com.neojou.myants.SimTool
import com.neojou.myants.render3d.LeafSpot
import com.neojou.myants.sim.Ant
import com.neojou.myants.sim.AntPlace
import com.neojou.myants.sim.FoodKind
import com.neojou.myants.sim.GrassTuft
import com.neojou.myants.sim.InsectKind
import com.neojou.myants.sim.Obstacle
import com.neojou.myants.sim.SimConfig
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
        const val GROUND_PICK = 8.0
    }

    private var leaves: List<LeafSpot> = emptyList()
    var inspectedAnt: Ant? = null
    /** Historical one-line text. Tests lock the English wording. The HUD shows [inspectPanel]. */
    val inspect = mutableStateOf("")
    internal val inspectPanel = mutableStateOf<InspectPanel?>(null)
    private var target: InspectTarget? = null

    /** Current pick for the selection ring. Null after [clear] or a miss. */
    internal fun inspectTarget(): InspectTarget? = target

    /** Applies the current tool at world position ([wx], [wy]). */
    internal fun apply(world: World, wx: Double, wy: Double, leaves: List<LeafSpot> = emptyList()) {
        when (tool) {
            SimTool.SUGAR -> world.placeFood(wx, wy, FoodKind.SUGAR)
            SimTool.PROTEIN -> world.placeFood(wx, wy, FoodKind.PROTEIN)
            SimTool.ENEMY -> world.spawnEnemyAt(wx, wy, InsectKind.PREDATOR)
            SimTool.INSPECT -> inspectNearest(world, wx, wy, leaves)
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
    internal fun refresh(world: World, leaves: List<LeafSpot> = emptyList()): Boolean {
        this.leaves = leaves
        val current = target ?: return false
        if (!stillThere(current, world)) {
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

    internal fun inspectNearest(world: World, wx: Double, wy: Double, leaves: List<LeafSpot> = emptyList()) {
        this.leaves = leaves
        var best = Double.MAX_VALUE
        var picked: InspectTarget? = null
        fun consider(x: Double, y: Double, candidate: InspectTarget) {
            val d = dist2(x, y, wx, wy)
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
        if (picked == null) picked = nearestStone(world, wx, wy)
        if (picked == null) picked = nearestGrass(world, wx, wy)
        if (picked == null) picked = nearestLeaf(leaves, wx, wy)
        if (picked == null) {
            picked = if (insideMap(wx, wy)) {
                if (target is InspectTarget.PheromoneSample) null else InspectTarget.PheromoneSample(wx, wy, world.colonies)
            } else {
                null
            }
        }
        show(picked)
    }

    private fun stillThere(current: InspectTarget, world: World): Boolean = when (current) {
        is InspectTarget.LeafItem -> leaves.any { it.x == current.x && it.y == current.y }
        else -> current.present(world)
    }

    private fun nearestStone(world: World, wx: Double, wy: Double): InspectTarget? {
        var best = Double.MAX_VALUE
        var picked: Obstacle? = null
        fun consider(stone: Obstacle) {
            val d = dist2(stone.x, stone.y, wx, wy)
            if (d <= stone.radius * stone.radius && d < best) {
                best = d
                picked = stone
            }
        }
        for (stone in world.terrain.obstacles) consider(stone)
        consider(world.terrain.clockStone)
        val stone = picked ?: return null
        return InspectTarget.StoneItem(stone, clock = stone === world.terrain.clockStone)
    }

    private fun nearestGrass(world: World, wx: Double, wy: Double): InspectTarget? {
        var best = Double.MAX_VALUE
        var picked: GrassTuft? = null
        for (tuft in world.grass) {
            val d = dist2(tuft.x, tuft.y, wx, wy)
            if (d <= GROUND_PICK * GROUND_PICK && d < best) {
                best = d
                picked = tuft
            }
        }
        val tuft = picked ?: return null
        return InspectTarget.GrassItem(tuft)
    }

    private fun nearestLeaf(spots: List<LeafSpot>, wx: Double, wy: Double): InspectTarget? {
        var best = Double.MAX_VALUE
        var picked: LeafSpot? = null
        for (leaf in spots) {
            val d = dist2(leaf.x.toDouble(), leaf.y.toDouble(), wx, wy)
            if (d <= GROUND_PICK * GROUND_PICK && d < best) {
                best = d
                picked = leaf
            }
        }
        val leaf = picked ?: return null
        return InspectTarget.LeafItem(leaf.x, leaf.y)
    }

    private fun insideMap(x: Double, y: Double): Boolean =
        x >= 0.0 && y >= 0.0 && x < SimConfig.width.toDouble() && y < SimConfig.height.toDouble()

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
