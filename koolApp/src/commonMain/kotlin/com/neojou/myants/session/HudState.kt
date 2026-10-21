package com.neojou.myants.session

import com.neojou.myants.GraphSample
import com.neojou.myants.ui.L10n
import com.neojou.myants.ui.UiText
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

    /** Digit fields for the fixed-width HUD. The lines above stay the legacy sentences. */
    val fpsDigits = mutableStateOf("0")
    val popDigits = mutableStateOf("0")
    val popSplit = mutableStateOf(false)
    val popMany = mutableStateOf(false)
    val workers = mutableStateOf("0")
    val soldiers = mutableStateOf("0")
    val colonyA = mutableStateOf("0")
    val colonyB = mutableStateOf("0")
    val proteinDigits = mutableStateOf("0")
    val sugarDigits = mutableStateOf("0")
    val queenMin = mutableStateOf("0")
    val queenSec = mutableStateOf("0")

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
        val fpsText = fpsShown.toString()
        fpsDigits.value = fpsText
        fps.value = L10n.fpsLine(fpsText)
        if (world.colonies.size > 1) {
            val counts = world.colonies.map { it.ants.size.toString() }
            val total = world.totalAntCount().toString()
            popSplit.value = true
            popMany.value = counts.size != 2
            popDigits.value = total
            colonyA.value = counts[0]
            colonyB.value = if (counts.size == 2) counts[1] else counts.drop(1).joinToString(" ${L10n.string(UiText.VERSUS)} ")
            pop.value = L10n.populationLine(total, L10n.populationSplitDetail(counts))
        } else {
            val ants = world.colonies[0].ants
            val workerCount = ants.count { it.type == AntKind.WORKER }
            val soldierCount = ants.count { it.type == AntKind.SOLDIER }
            popSplit.value = false
            popMany.value = false
            popDigits.value = ants.size.toString()
            workers.value = workerCount.toString()
            soldiers.value = soldierCount.toString()
            pop.value = L10n.populationLine(
                ants.size.toString(),
                L10n.populationSingleDetail(workers.value, soldiers.value),
            )
        }
        val protein = floor(world.proteinStockpile).toInt()
        val sugar = floor(world.sugarStockpile).toInt()
        proteinDigits.value = protein.toString()
        sugarDigits.value = sugar.toString()
        food.value = L10n.foodLine(protein, sugar)
        val ageSeconds = world.queen.age / 60
        val minutes = ageSeconds / 60
        val seconds = ageSeconds % 60
        queenMin.value = minutes.toString()
        queenSec.value = seconds.toString()
        queen.value = L10n.queenLine(minutes, seconds)
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
