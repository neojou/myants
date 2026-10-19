package com.neojou.myants.sim

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow

data class TunerAction(
    val path: String,
    val label: String,
    val from: Double,
    val to: Double,
)

data class TunerSuggestion(
    val metric: String,
    val observed: String,
    val target: String,
    val severity: String,
    val suggestion: String,
    val effect: String,
    val actions: List<TunerAction> = emptyList(),
)

data class GraphPoint(
    val population: Int,
    val sugar: Double,
    val protein: Double,
    val energyPct: Double,
)

private data class Snapshot(
    val age: Int,
    val antCount: Int,
    val workerCount: Int,
    val soldierCount: Int,
    val foragingCount: Int,
    val attackingCount: Int,
    val fleeingCount: Int,
    val avgEnergy: Double,
    val queenEnergy: Double,
    val queenStress: Double,
    val sugarStockpile: Double,
    val proteinStockpile: Double,
    val broodCount: Int,
    val larvaeCount: Int,
    val enemyCount: Int,
)

private const val SAMPLE_INTERVAL = 600
private const val WINDOW_SIZE = 12
private const val GRAPH_INTERVAL = 60
private const val GRAPH_SIZE = 180

private fun jsRound(x: Double): Double {
    if (!x.isFinite()) return x
    return floor(x + 0.5)
}

private fun jsIsInteger(n: Double): Boolean =
    n.isFinite() && abs(n) <= 9007199254740991.0 && n == floor(n)

private fun jsToPrecision(value: Double, precision: Int): Double {
    if (value == 0.0 || !value.isFinite()) return value
    val neg = value < 0.0
    val absV = abs(value)
    val exp = floor(kotlin.math.log10(absV)).toInt()
    val scale = 10.0.pow((precision - 1 - exp).toDouble())
    val rounded = jsRound(absV * scale) / scale
    return if (neg) -rounded else rounded
}

private fun nudgeValue(current: Double, factor: Double): Double {
    val v = current * factor
    if (jsIsInteger(current) && abs(current) >= 1.0) return jsRound(v)
    return jsToPrecision(v, 3)
}

private fun jsToFixed(value: Double, digits: Int): String {
    val sign = if (value < 0.0) "-" else ""
    val factor = 10.0.pow(digits.toDouble())
    val scaled = floor(abs(value) * factor + 0.5)
    val asLong = scaled.toLong()
    if (digits <= 0) return sign + asLong.toString()
    val div = factor.toLong()
    val whole = asLong / div
    val frac = (asLong % div).toString().padStart(digits, '0')
    return "$sign$whole.$frac"
}

private fun jsNum(n: Double): String {
    if (n.isNaN()) return "NaN"
    if (n == Double.POSITIVE_INFINITY) return "Infinity"
    if (n == Double.NEGATIVE_INFINITY) return "-Infinity"
    if (jsIsInteger(n) && abs(n) < 1e15) {
        val sign = if (n < 0.0) "-" else ""
        return sign + abs(n).toLong().toString()
    }
    val text = jsToFixed(n, 6)
    if ('.' !in text) return text
    return text.trimEnd('0').trimEnd('.')
}

private fun configNumber(path: String): Double = when (path) {
    "gracePeriod" -> SimConfig.gracePeriod.toDouble()
    "maxPredators" -> SimConfig.maxPredators.toDouble()
    "ant.queenFeedAmount" -> SimConfig.ant.queenFeedAmount
    "soldierHealth" -> SimConfig.soldierHealth
    "ant.queenCriticalEnergy" -> SimConfig.ant.queenCriticalEnergy
    "soldierUnlockThreshold" -> SimConfig.soldierUnlockThreshold.toDouble()
    else -> SimConfig.getNumber(path)
}

private fun setConfigNumber(path: String, value: Double) {
    when (path) {
        "gracePeriod" -> SimConfig.gracePeriod = jsRound(value).toInt()
        "maxPredators" -> SimConfig.maxPredators = jsRound(value).toInt()
        "ant.queenFeedAmount" -> SimConfig.ant.queenFeedAmount = value
        "soldierHealth" -> SimConfig.soldierHealth = value
        "ant.queenCriticalEnergy" -> SimConfig.ant.queenCriticalEnergy = value
        "soldierUnlockThreshold" -> SimConfig.soldierUnlockThreshold = jsRound(value).toInt()
        else -> SimConfig.setNumber(path, value)
    }
}

private fun mkAction(path: String, factor: Double): TunerAction {
    val from = configNumber(path)
    val to = nudgeValue(from, factor)
    return TunerAction(path, "$path: ${jsNum(from)} → ${jsNum(to)}", from, to)
}

/** Apply one tuner action onto live [SimConfig]. Paths missing from setNumber are written directly. */
fun applyTunerAction(a: TunerAction) {
    setConfigNumber(a.path, a.to)
}

class SimObserver {
    private val snapshots = ArrayList<Snapshot>()
    private var lastSampleAge = -1
    val history = ArrayList<GraphPoint>()
    private var lastGraphAge = -1
    val suggestions = ArrayList<TunerSuggestion>()

    fun getHistory(): List<GraphPoint> = history

    val snapshotCount: Int get() = snapshots.size

    fun reset() {
        snapshots.clear()
        history.clear()
        suggestions.clear()
        lastSampleAge = -1
        lastGraphAge = -1
    }

    fun observe(world: World) {
        if (world.age - lastGraphAge >= GRAPH_INTERVAL) {
            lastGraphAge = world.age
            val n = world.ants.size
            var sum = 0.0
            for (a in world.ants) sum += a.energy
            val avg = if (n > 0) sum / n.toDouble() else 0.0
            history.add(GraphPoint(
                population = n,
                sugar = world.sugarStockpile,
                protein = world.proteinStockpile,
                energyPct = avg / SimConfig.antMaxEnergy,
            ))
            if (history.size > GRAPH_SIZE) history.removeAt(0)
        }
        if (world.age - lastSampleAge < SAMPLE_INTERVAL) return
        lastSampleAge = world.age
        val ants = world.ants
        var workerCount = 0
        var soldierCount = 0
        var foragingCount = 0
        var attackingCount = 0
        var fleeingCount = 0
        var totalEnergy = 0.0
        for (a in ants) {
            totalEnergy += a.energy
            if (a.type == AntKind.WORKER) {
                workerCount++
                if (a.state == AntState.FORAGING || a.state == AntState.HARVESTING || a.state == AntState.RETURNING) {
                    foragingCount++
                }
            } else if (a.type == AntKind.SOLDIER) {
                soldierCount++
            }
            if (a.state == AntState.ATTACKING) attackingCount++
            if (a.state == AntState.FLEEING) fleeingCount++
        }
        val avgEnergy = if (ants.isNotEmpty()) totalEnergy / ants.size.toDouble() else 0.0
        var larvaeCount = 0
        for (b in world.brood) if (b.stage == BroodStage.LARVA) larvaeCount++
        var enemyCount = 0
        for (ins in world.insects) {
            if (ins.type == InsectKind.PREDATOR || ins.type == InsectKind.SPIDER || ins.type == InsectKind.BEETLE) {
                enemyCount++
            }
        }
        snapshots.add(Snapshot(
            age = world.age,
            antCount = ants.size,
            workerCount = workerCount,
            soldierCount = soldierCount,
            foragingCount = foragingCount,
            attackingCount = attackingCount,
            fleeingCount = fleeingCount,
            avgEnergy = avgEnergy,
            queenEnergy = world.queen.energy,
            queenStress = world.queen.stress,
            sugarStockpile = world.sugarStockpile,
            proteinStockpile = world.proteinStockpile,
            broodCount = world.brood.size,
            larvaeCount = larvaeCount,
            enemyCount = enemyCount,
        ))
        if (snapshots.size > WINDOW_SIZE) snapshots.removeAt(0)
    }

    fun analyze(): List<TunerSuggestion> {
        val results = ArrayList<TunerSuggestion>()
        if (snapshots.size < 2) {
            results.add(TunerSuggestion(
                metric = "Datenmenge",
                observed = "${snapshots.size} / $WINDOW_SIZE Snapshots",
                target = "≥ 2 Snapshots",
                severity = "info",
                suggestion = "Bitte noch etwas länger warten (ca. 20 Sekunden)",
                effect = "Der Observer braucht mindestens 2 Messungen für Trendanalyse.",
            ))
            suggestions.clear()
            suggestions.addAll(results)
            return results
        }
        val latest = snapshots[snapshots.size - 1]
        val first = snapshots[0]
        val span = snapshots.size
        val growthRate = (latest.antCount - first.antCount).toDouble() / maxOf(1, first.antCount).toDouble()
        if (growthRate < -0.25) {
            results.add(TunerSuggestion(
                metric = "Populationstrend",
                observed = "${jsToFixed(growthRate * 100.0, 0)} % Rückgang",
                target = "≥ 0 % (stabil)",
                severity = "critical",
                suggestion = "Schneller legen + länger leben (Pop ≈ lifespan/layInterval); bei Hunger Energieverbrauch senken",
                effect = "Kürzeres Lege-Intervall und höhere Lebensspanne heben das Populations-Gleichgewicht. Geringerer Energieverbrauch hilft nur, wenn die Vorräte knapp sind.",
                actions = listOf(mkAction("queenLayInterval", 0.7), mkAction("ant.lifespan", 1.3), mkAction("antEnergyDecay", 0.85)),
            ))
        } else if (growthRate < -0.05) {
            results.add(TunerSuggestion(
                metric = "Populationstrend",
                observed = "${jsToFixed(growthRate * 100.0, 0)} % Rückgang",
                target = "> −5 %",
                severity = "warn",
                suggestion = "Etwas schneller legen oder Ameisen länger leben lassen",
                effect = "Kürzeres Lege-Intervall oder höhere Lebensspanne hebt das Populations-Gleichgewicht (≈ lifespan/layInterval).",
                actions = listOf(mkAction("queenLayInterval", 0.85), mkAction("ant.lifespan", 1.2)),
            ))
        } else if (growthRate > 0.5) {
            results.add(TunerSuggestion(
                metric = "Populationstrend",
                observed = "+${jsToFixed(growthRate * 100.0, 0)} % Wachstum (sehr schnell)",
                target = "< 50 % pro Fenster",
                severity = "info",
                suggestion = "Lege-Intervall der Königin erhöhen oder Ei-Kosten anheben",
                effect = "Die Kolonie wächst sehr schnell. Ein größeres Lege-Intervall (oder teurere Eier) bremst die Geburtenrate auf ein angenehmeres Tempo.",
                actions = listOf(mkAction("queenLayInterval", 1.5), mkAction("eggCost", 1.3)),
            ))
        } else if (growthRate > 0.05) {
            results.add(TunerSuggestion(
                metric = "Populationstrend",
                observed = "+${jsToFixed(growthRate * 100.0, 0)} % Wachstum",
                target = "0–5 %",
                severity = "good",
                suggestion = "– kein Handlungsbedarf –",
                effect = "Kolonie wächst gesund.",
            ))
        }
        val avgEnergyRatio = latest.avgEnergy / SimConfig.antMaxEnergy
        if (avgEnergyRatio < 0.3) {
            results.add(TunerSuggestion(
                metric = "Durchschnittliche Energie",
                observed = "${jsToFixed(avgEnergyRatio * 100.0, 0)} % von Max",
                target = "> 50 %",
                severity = "critical",
                suggestion = "Energieverbrauch senken oder mehr Nahrungsquellen",
                effect = "Ameisen verbrennen Energie schneller als sie tanken können. Mehr Nahrungsquellen oder geringerer Verbrauch hält die Kolonie lebensfähig.",
                actions = listOf(mkAction("antEnergyDecay", 0.8), mkAction("sugarSourceCount", 1.5)),
            ))
        } else if (avgEnergyRatio < 0.5) {
            results.add(TunerSuggestion(
                metric = "Durchschnittliche Energie",
                observed = "${jsToFixed(avgEnergyRatio * 100.0, 0)} % von Max",
                target = "> 50 %",
                severity = "warn",
                suggestion = "Zuckerertrag erhöhen oder Verbrauch leicht senken",
                effect = "Energiepuffer der Ameisen ist knapp. Etwas mehr Nahrungsertrag verhindert Massensterben bei plötzlichem Angriff.",
                actions = listOf(mkAction("sugarValue", 1.3), mkAction("antEnergyDecay", 0.9)),
            ))
        } else if (avgEnergyRatio > 0.85) {
            results.add(TunerSuggestion(
                metric = "Durchschnittliche Energie",
                observed = "${jsToFixed(avgEnergyRatio * 100.0, 0)} % von Max",
                target = "50–85 %",
                severity = "info",
                suggestion = "Energieverbrauch erhöhen für mehr Herausforderung",
                effect = "Ameisen haben zu viel Reserve — das Spiel verliert Spannung. Mehr Verbrauch erzwingt aktiveres Sammeln.",
                actions = listOf(mkAction("antEnergyDecay", 1.15)),
            ))
        }
        val activeFrac = if (latest.workerCount > 0) latest.foragingCount.toDouble() / latest.workerCount.toDouble() else 0.0
        if (activeFrac > 0.85) {
            results.add(TunerSuggestion(
                metric = "Sammelquote",
                observed = "${jsToFixed(activeFrac * 100.0, 0)} % der Worker sammeln",
                target = "40–75 %",
                severity = "warn",
                suggestion = "Zuckerertrag oder Anzahl Nahrungsquellen erhöhen",
                effect = "Fast alle Worker müssen sammeln — ein Zeichen für Nahrungsknappheit. Mehr Ertragsquellen reduzieren den Stress der Kolonie.",
                actions = listOf(mkAction("sugarValue", 1.3), mkAction("sugarSourceCount", 1.5)),
            ))
        } else if (activeFrac < 0.2 && latest.antCount > 20) {
            results.add(TunerSuggestion(
                metric = "Sammelquote",
                observed = "${jsToFixed(activeFrac * 100.0, 0)} % der Worker sammeln",
                target = "40–75 %",
                severity = "info",
                suggestion = "Energieverbrauch erhöhen, damit Worker aktiver werden",
                effect = "Kolonie ist zu gut versorgt, Worker rasten zu viel. Ein höherer Energieverbrauch belebt die Simulation.",
                actions = listOf(mkAction("antEnergyDecay", 1.15)),
            ))
        }
        val combatRatio = if (latest.antCount > 0) {
            (latest.attackingCount + latest.fleeingCount).toDouble() / latest.antCount.toDouble()
        } else 0.0
        if (combatRatio > 0.35) {
            results.add(TunerSuggestion(
                metric = "Kampfbelastung",
                observed = "${jsToFixed(combatRatio * 100.0, 0)} % kämpfen/fliehen",
                target = "< 25 %",
                severity = "critical",
                suggestion = "Feind-Spawnrate senken oder Schonzeit verlängern",
                effect = "Zu viele Feinde überlasten die Kolonie. Längere Schonzeit oder langsamere Spawn-Rate gibt der Kolonie Zeit sich zu entwickeln.",
                actions = listOf(mkAction("predatorSpawnRate", 0.6), mkAction("gracePeriod", 1.5)),
            ))
        } else if (combatRatio < 0.02 && latest.antCount > 30 && span >= 6) {
            results.add(TunerSuggestion(
                metric = "Kampfbelastung",
                observed = "${jsToFixed(combatRatio * 100.0, 0)} % kämpfen/fliehen",
                target = "2–25 %",
                severity = "info",
                suggestion = "Feind-Spawnrate oder Feind-Obergrenze erhöhen",
                effect = "Keine Bedrohung — die Simulation wirkt langweilig. Mehr Feinde erzeugen spannende Verteidigungsreaktionen.",
                actions = listOf(mkAction("predatorSpawnRate", 1.5), mkAction("maxPredators", 1.5)),
            ))
        }
        val queenRatio = latest.queenEnergy / SimConfig.antMaxEnergy
        if (queenRatio < 0.3) {
            results.add(TunerSuggestion(
                metric = "Königinnen-Energie",
                observed = "${jsToFixed(queenRatio * 100.0, 0)} % von Max",
                target = "> 50 %",
                severity = "critical",
                suggestion = "Pflege-Protein-Menge oder Proteinwert erhöhen",
                effect = "Königin ist unterernährt und kann keine Eier legen. Mehr Protein pro Pflegeakt oder höherer Proteinwert pro Nahrungsquelle hilft.",
                actions = listOf(mkAction("ant.queenFeedAmount", 1.3), mkAction("proteinValue", 1.4)),
            ))
        }
        if (latest.queenStress > 20.0) {
            results.add(TunerSuggestion(
                metric = "Königinnen-Stress",
                observed = "Stress: ${jsToFixed(latest.queenStress, 1)}",
                target = "< 15",
                severity = "warn",
                suggestion = "Feind-Spawnrate senken oder Soldaten stärken",
                effect = "Feinde dringen nah an das Nest heran. Stärkere Soldaten oder weniger Feinde schützen die Königin besser.",
                actions = listOf(mkAction("predatorSpawnRate", 0.7), mkAction("soldierHealth", 1.25)),
            ))
        }
        val broodRatio = if (latest.antCount > 0) latest.broodCount.toDouble() / latest.antCount.toDouble() else 0.0
        if (broodRatio < 0.2 && latest.antCount > 10) {
            results.add(TunerSuggestion(
                metric = "Brut-Pipeline",
                observed = "${jsToFixed(broodRatio, 2)}× Koloniengröße",
                target = "> 0.3×",
                severity = "warn",
                suggestion = "Eiproduktion verbilligen oder Königin früher füttern",
                effect = "Königin legt zu wenige Eier. Günstigere Eiproduktion oder niedrigerer Hungerschwellenwert der Königin füllt die Pipeline.",
                actions = listOf(mkAction("eggCost", 0.8), mkAction("ant.queenCriticalEnergy", 0.9)),
            ))
        }
        val soldierFrac = if (latest.antCount > 0) latest.soldierCount.toDouble() / latest.antCount.toDouble() else 0.0
        if (soldierFrac > 0.40) {
            results.add(TunerSuggestion(
                metric = "Soldatenanteil",
                observed = "${jsToFixed(soldierFrac * 100.0, 0)} %",
                target = "10–30 %",
                severity = "info",
                suggestion = "Soldaten-Schwellenwert erhöhen",
                effect = "Zu viele Soldaten reduzieren die Sammelkapazität. Höherer Schwellenwert für Soldaten-Produktion balanciert die Kolonie.",
                actions = listOf(mkAction("soldierUnlockThreshold", 1.3)),
            ))
        } else if (soldierFrac < 0.05 && latest.antCount > 40) {
            results.add(TunerSuggestion(
                metric = "Soldatenanteil",
                observed = "${jsToFixed(soldierFrac * 100.0, 0)} %",
                target = "10–30 %",
                severity = "info",
                suggestion = "Soldaten-Schwellenwert senken",
                effect = "Keine Soldaten → Kolonie ist schutzlos. Niedrigerer Schwellenwert für frühere Soldaten-Produktion.",
                actions = listOf(mkAction("soldierUnlockThreshold", 0.7)),
            ))
        }
        val sugarPerAnt = if (latest.antCount > 0) latest.sugarStockpile / latest.antCount.toDouble() else 0.0
        if (sugarPerAnt < 5.0 && latest.antCount > 15) {
            results.add(TunerSuggestion(
                metric = "Zucker-Vorrat pro Ameise",
                observed = "${jsToFixed(sugarPerAnt, 1)} Einheiten",
                target = "> 10",
                severity = "warn",
                suggestion = "Zuckerertrag oder Anzahl Nahrungsquellen erhöhen",
                effect = "Vorrat reicht kaum für Notfälle. Mehr Ernte pro Quelle oder mehrere Quellen schafft einen Puffer.",
                actions = listOf(mkAction("sugarValue", 1.3), mkAction("sugarSourceCount", 1.5)),
            ))
        }
        val sugarGrowth = latest.sugarStockpile - first.sugarStockpile
        if (sugarGrowth > 200.0 && latest.sugarStockpile > 1500.0) {
            results.add(TunerSuggestion(
                metric = "Zucker-Überschuss",
                observed = "+${jsToFixed(sugarGrowth, 0)} (jetzt ${jsToFixed(latest.sugarStockpile, 0)})",
                target = "stabil",
                severity = "info",
                suggestion = "Kolonie-Unterhalt erhöhen oder Zuckerertrag senken",
                effect = "Der Vorrat wächst ungenutzt. Höherer Unterhalt pro Ameise oder weniger Ertrag pro Lieferung bringt Angebot und Verbrauch ins Gleichgewicht.",
                actions = listOf(mkAction("colonyUpkeep", 1.5), mkAction("sugarValue", 0.8)),
            ))
        }
        val proteinGrowth = latest.proteinStockpile - first.proteinStockpile
        if (proteinGrowth > 100.0 && latest.proteinStockpile > 800.0) {
            results.add(TunerSuggestion(
                metric = "Protein-Überschuss",
                observed = "+${jsToFixed(proteinGrowth, 0)} (jetzt ${jsToFixed(latest.proteinStockpile, 0)})",
                target = "stabil",
                severity = "info",
                suggestion = "Brut-Unterhalt erhöhen oder Proteinertrag senken",
                effect = "Protein staut sich an. Mehr Verbrauch pro Larve oder weniger Ertrag pro Lieferung verhindert das Horten.",
                actions = listOf(mkAction("broodProteinUpkeep", 1.5), mkAction("proteinValue", 0.8)),
            ))
        }
        if (results.isEmpty() || results.all { it.severity == "good" }) {
            results.add(TunerSuggestion(
                metric = "Gesamtzustand",
                observed = "Alles im grünen Bereich",
                target = "–",
                severity = "good",
                suggestion = "– Keine Anpassungen nötig –",
                effect = "Die Simulation läuft im Gleichgewicht. Probier den Auto-Tuner in 2 Minuten nochmal nach einem Angriff.",
            ))
        }
        suggestions.clear()
        suggestions.addAll(results)
        return results
    }
}
