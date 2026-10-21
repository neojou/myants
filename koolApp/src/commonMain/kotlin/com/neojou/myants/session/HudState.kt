package com.neojou.myants.session

import com.neojou.myants.GraphSample
import com.neojou.myants.sim.AntKind
import com.neojou.myants.sim.SimConfig
import com.neojou.myants.sim.World
import de.fabmax.kool.modules.ui2.mutableStateOf
import kotlin.math.floor

/** HUD strings and the Stats graph history. Moved verbatim from AppSession (#7 step 3). */
class HudState {
    val fps = mutableStateOf("FPS: 0")
    val pop = mutableStateOf("Population: 0")
    val food = mutableStateOf("Protein: 0 | Sugar: 0")
    val queen = mutableStateOf("Queen Age: 0m 0s")
    val history = ArrayDeque<GraphSample>()

    private var nextSyncAt: Double = Double.NEGATIVE_INFINITY

    /** Times [sync] actually wrote the HUD strings. Unit tests use this count. */
    internal var syncPublishes: Int = 0

    /**
     * Writes the HUD strings at about 4 Hz. [force] writes immediately (single step).
     * Graph samples stay on the 60-tick path in [recordSample].
     */
    fun sync(world: World, fpsShown: Int, nowSeconds: Double, force: Boolean = false) {
        if (!force && nowSeconds < nextSyncAt) return
        nextSyncAt = nowSeconds + HUD_PERIOD_SECONDS
        syncPublishes++
        fps.value = "FPS: $fpsShown"
        if (world.colonies.size > 1) {
            val split = world.colonies.joinToString(" vs ") { it.ants.size.toString() }
            pop.value = "Population: ${world.totalAntCount()} ($split)"
        } else {
            val ants = world.colonies[0].ants
            val workers = ants.count { it.type == AntKind.WORKER }
            val soldiers = ants.count { it.type == AntKind.SOLDIER }
            pop.value = "Population: ${ants.size} (W:$workers S:$soldiers)"
        }
        food.value = "Protein: ${floor(world.proteinStockpile).toInt()} | Sugar: ${floor(world.sugarStockpile).toInt()}"
        val ageSeconds = world.queen.age / 60
        queen.value = "Queen Age: ${ageSeconds / 60}m ${ageSeconds % 60}s"
    }

    /** Appends one graph sample; keeps the last 180. */
    fun recordSample(world: World) {
        val ants = world.ants
        val avg = if (ants.isEmpty()) 0.0 else ants.sumOf { it.energy } / ants.size
        history.addLast(
            GraphSample(
                population = ants.size,
                sugar = world.sugarStockpile,
                protein = world.proteinStockpile,
                energyPct = avg / SimConfig.antMaxEnergy,
            ),
        )
        while (history.size > 180) history.removeFirst()
    }

    companion object {
        /** About 4 Hz. Issue #9. */
        private const val HUD_PERIOD_SECONDS = 0.25

        fun speedLabel(s: Double): String = if (s == 0.5) "0.5x" else "${s.toInt()}x"
    }
}
