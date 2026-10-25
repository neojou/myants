package com.neojou.myants.ui

import com.neojou.myants.sim.Ant
import com.neojou.myants.sim.AntKind
import com.neojou.myants.sim.AntState
import com.neojou.myants.sim.Brood
import com.neojou.myants.sim.BroodStage
import com.neojou.myants.sim.CarryKind
import com.neojou.myants.sim.ChamberRole
import com.neojou.myants.sim.Colony
import com.neojou.myants.sim.FoodKind
import com.neojou.myants.sim.InsectKind
import com.neojou.myants.sim.Pheromone
import kotlin.math.abs
import kotlin.math.round

/**
 * Inspect lines. English one-line text keeps the historical enum wording so existing tests stay valid.
 * The card ([InspectPanel]) uses translated labels and sentence-case names in every language.
 */
/** One label/value line of the floating inspector card (#61). */
internal data class InspectRow(val label: String, val value: String)

/** Structured inspect card: a title plus at most [MAX_ROWS] label/value rows. The one-line [InspectCopy] text stays for tests. */
internal data class InspectPanel(val title: String, val rows: List<InspectRow>) {
    companion object {
        const val MAX_ROWS = 6
        fun of(title: String, rows: List<InspectRow>) = InspectPanel(title, rows.take(MAX_ROWS))
    }
}

internal object InspectCopy {
    fun antLine(ant: Ant, lang: UiLanguage = L10n.language.value): String {
        if (lang == UiLanguage.EN) {
            return "Ant ${ant.type} ${ant.state} e=${ant.energy.toInt()} h=${ant.health.toInt()} ${ant.carrying}"
        }
        return line(
            lang,
            "${kind(ant.type, lang)} ${state(ant.state, lang)}",
            "${word(UiText.INSPECT_ENERGY, lang)}=${ant.energy.toInt()}",
            "${word(UiText.INSPECT_HEALTH, lang)}=${ant.health.toInt()}",
            carry(ant.carrying, lang),
        )
    }

    fun queenLine(energy: Int, age: Int, lang: UiLanguage = L10n.language.value): String {
        if (lang == UiLanguage.EN) return "Queen e=$energy age=$age"
        return "${word(UiText.INSPECT_QUEEN, lang)} ${word(UiText.INSPECT_ENERGY, lang)}=$energy ${word(UiText.INSPECT_AGE, lang)}=$age"
    }

    fun foodLine(type: FoodKind, amount: Int, lang: UiLanguage = L10n.language.value): String {
        if (lang == UiLanguage.EN) return "$type $amount"
        return "${food(type, lang)} $amount"
    }

    fun insectLine(type: InsectKind, health: Int, lang: UiLanguage = L10n.language.value): String {
        if (lang == UiLanguage.EN) return "$type hp $health"
        return "${insect(type, lang)} ${word(UiText.INSPECT_HP, lang)} $health"
    }

    fun antPanel(ant: Ant, lang: UiLanguage = L10n.language.value): InspectPanel = InspectPanel.of(
        "${label(UiText.INSPECT_ANT, lang)} ${kind(ant.type, lang)}",
        listOf(
            InspectRow(label(UiText.INSPECT_STATE, lang), state(ant.state, lang)),
            InspectRow(label(UiText.INSPECT_ENERGY, lang), ant.energy.toInt().toString()),
            InspectRow(label(UiText.INSPECT_HEALTH, lang), ant.health.toInt().toString()),
            InspectRow(label(UiText.INSPECT_CARRY, lang), carry(ant.carrying, lang)),
        ),
    )

    fun queenPanel(energy: Int, age: Int, lang: UiLanguage = L10n.language.value): InspectPanel = InspectPanel.of(
        label(UiText.INSPECT_QUEEN, lang),
        listOf(
            InspectRow(label(UiText.INSPECT_ENERGY, lang), energy.toString()),
            InspectRow(label(UiText.INSPECT_AGE, lang), age.toString()),
        ),
    )

    fun foodPanel(type: FoodKind, amount: Int, lang: UiLanguage = L10n.language.value): InspectPanel = InspectPanel.of(
        food(type, lang),
        listOf(InspectRow(label(UiText.INSPECT_AMOUNT, lang), amount.toString())),
    )

    fun insectPanel(type: InsectKind, health: Int, lang: UiLanguage = L10n.language.value): InspectPanel = InspectPanel.of(
        insect(type, lang),
        listOf(InspectRow(label(UiText.INSPECT_HP, lang), health.toString())),
    )

    fun broodLine(brood: Brood, lang: UiLanguage = L10n.language.value): String {
        val age = brood.age.toInt()
        val stage = word(stageKey(brood.stage), lang)
        val base = "$stage ${word(UiText.INSPECT_AGE, lang)}=$age"
        val withHunger = if (brood.stage == BroodStage.LARVA) {
            "$base ${word(UiText.INSPECT_HUNGER, lang)}=${brood.hunger.toInt()}"
        } else {
            base
        }
        val caste = brood.destinedCaste
        val casteText = caste?.let { if (lang == UiLanguage.EN) it.name else kind(it, lang) }
        return if (brood.stage == BroodStage.PUPA && casteText != null) "$withHunger $casteText" else withHunger
    }

    fun broodPanel(brood: Brood, lang: UiLanguage = L10n.language.value): InspectPanel {
        val rows = ArrayList<InspectRow>()
        rows += InspectRow(label(UiText.INSPECT_AGE, lang), brood.age.toInt().toString())
        if (brood.stage == BroodStage.LARVA) {
            rows += InspectRow(label(UiText.INSPECT_HUNGER, lang), brood.hunger.toInt().toString())
        }
        val caste = brood.destinedCaste
        if (brood.stage == BroodStage.PUPA && caste != null) rows += InspectRow(label(UiText.INSPECT_CASTE, lang), kind(caste, lang))
        return InspectPanel.of(label(stageKey(brood.stage), lang), rows)
    }

    fun chamberLine(role: ChamberRole, radius: Int, lang: UiLanguage = L10n.language.value): String =
        "${word(UiText.INSPECT_CHAMBER, lang)} ${word(roleKey(role), lang)} ${word(UiText.INSPECT_RADIUS, lang)}=$radius"

    fun chamberPanel(role: ChamberRole, radius: Int, lang: UiLanguage = L10n.language.value): InspectPanel = InspectPanel.of(
        label(UiText.INSPECT_CHAMBER, lang),
        listOf(
            InspectRow(label(UiText.INSPECT_ROLE, lang), label(roleKey(role), lang)),
            InspectRow(label(UiText.INSPECT_RADIUS, lang), radius.toString()),
        ),
    )

    fun stoneLine(radius: Int, clock: Boolean, lang: UiLanguage = L10n.language.value): String =
        "${word(if (clock) UiText.INSPECT_CLOCK else UiText.INSPECT_STONE, lang)} ${word(UiText.INSPECT_RADIUS, lang)}=$radius"

    fun stonePanel(radius: Int, clock: Boolean, lang: UiLanguage = L10n.language.value): InspectPanel = InspectPanel.of(
        label(if (clock) UiText.INSPECT_CLOCK else UiText.INSPECT_STONE, lang),
        listOf(InspectRow(label(UiText.INSPECT_RADIUS, lang), radius.toString())),
    )

    fun grassLine(size: Int, lang: UiLanguage = L10n.language.value): String =
        "${word(UiText.INSPECT_GRASS, lang)} ${word(UiText.INSPECT_SIZE, lang)}=$size"

    fun grassPanel(size: Int, lang: UiLanguage = L10n.language.value): InspectPanel = InspectPanel.of(
        label(UiText.INSPECT_GRASS, lang),
        listOf(InspectRow(label(UiText.INSPECT_SIZE, lang), size.toString())),
    )

    fun leafLine(lang: UiLanguage = L10n.language.value): String = word(UiText.INSPECT_LEAF, lang)

    fun leafPanel(lang: UiLanguage = L10n.language.value): InspectPanel =
        InspectPanel.of(label(UiText.INSPECT_LEAF, lang), emptyList())

    fun pheromoneLine(colonies: List<Colony>, x: Double, y: Double, lang: UiLanguage = L10n.language.value): String =
        "${word(UiText.INSPECT_PHEROMONE, lang)} " + colonies.joinToString(" ") { colonyChannels(it, x, y, lang) }

    /** Only non-zero channels (at two decimals); colonies are tagged A, B, ... when more than one is listed. */
    fun pheromonePanel(colonies: List<Colony>, x: Double, y: Double, lang: UiLanguage = L10n.language.value): InspectPanel {
        val rows = ArrayList<InspectRow>()
        val channels = listOf(
            UiText.INSPECT_HOME to Pheromone.HOME,
            UiText.INSPECT_TRAIL_SUGAR to Pheromone.SUGAR,
            UiText.INSPECT_TRAIL_PROTEIN to Pheromone.PROTEIN,
            UiText.INSPECT_DANGER to Pheromone.DANGER,
        )
        for (colony in colonies) {
            val field = colony.outdoorField
            val prefix = if (colonies.size > 1) "${colonyTag(colony.id)} " else ""
            for ((key, channel) in channels) {
                val text = fixed2(field.get(x, y, channel))
                if (text == "0.00") continue
                rows += InspectRow(prefix + label(key, lang), text)
            }
        }
        return InspectPanel.of(label(UiText.INSPECT_PHEROMONE, lang), rows)
    }

    private fun colonyTag(id: Int): String = ('A' + (id % 26)).toString()

    private fun colonyChannels(colony: Colony, x: Double, y: Double, lang: UiLanguage): String {
        val field = colony.outdoorField
        return "c${colony.id} " + listOf(
            channelPair(UiText.INSPECT_HOME, field.get(x, y, Pheromone.HOME), lang),
            channelPair(UiText.INSPECT_TRAIL_SUGAR, field.get(x, y, Pheromone.SUGAR), lang),
            channelPair(UiText.INSPECT_TRAIL_PROTEIN, field.get(x, y, Pheromone.PROTEIN), lang),
            channelPair(UiText.INSPECT_DANGER, field.get(x, y, Pheromone.DANGER), lang),
        ).joinToString(" ")
    }

    private fun channelPair(key: UiText, value: Double, lang: UiLanguage): String =
        "${word(key, lang)}=${fixed2(value)}"

    private fun stageKey(stage: BroodStage): UiText = when (stage) {
        BroodStage.EGG -> UiText.INSPECT_EGG
        BroodStage.LARVA -> UiText.INSPECT_LARVA
        BroodStage.PUPA -> UiText.INSPECT_PUPA
    }

    private fun roleKey(role: ChamberRole): UiText = when (role) {
        ChamberRole.QUEEN -> UiText.INSPECT_ROLE_QUEEN
        ChamberRole.BROOD -> UiText.INSPECT_ROLE_BROOD
        ChamberRole.STORAGE -> UiText.INSPECT_ROLE_STORAGE
        ChamberRole.CEMETERY -> UiText.INSPECT_ROLE_CEMETERY
    }

    fun kind(kind: AntKind, lang: UiLanguage): String = name(KIND, kind.name, lang)
    fun state(state: AntState, lang: UiLanguage): String = name(STATE, state.name, lang)
    fun carry(carry: CarryKind, lang: UiLanguage): String = name(CARRY, carry.name, lang)
    fun food(kind: FoodKind, lang: UiLanguage): String = name(FOOD, kind.name, lang)
    fun insect(kind: InsectKind, lang: UiLanguage): String = name(INSECT, kind.name, lang)

    private fun line(lang: UiLanguage, body: String, energy: String, health: String, carry: String): String {
        val ant = word(UiText.INSPECT_ANT, lang)
        return "$ant $body $energy $health $carry"
    }

    private fun word(key: UiText, lang: UiLanguage) = L10n.string(key, lang)

    /** Card labels: readable words instead of the debug keys (e, h, r, s) and upper-case enum names. */
    private fun label(key: UiText, lang: UiLanguage): String = when (lang) {
        UiLanguage.EN -> EN_LABEL[key]
        UiLanguage.DE -> DE_LABEL[key]
        else -> null
    } ?: word(key, lang)

    private val EN_LABEL = mapOf(
        UiText.INSPECT_ENERGY to "Energy",
        UiText.INSPECT_HEALTH to "Health",
        UiText.INSPECT_AGE to "Age",
        UiText.INSPECT_HP to "Health",
        UiText.INSPECT_EGG to "Egg",
        UiText.INSPECT_LARVA to "Larva",
        UiText.INSPECT_PUPA to "Pupa",
        UiText.INSPECT_HUNGER to "Hunger",
        UiText.INSPECT_CHAMBER to "Chamber",
        UiText.INSPECT_ROLE_QUEEN to "Queen",
        UiText.INSPECT_ROLE_BROOD to "Brood",
        UiText.INSPECT_ROLE_STORAGE to "Storage",
        UiText.INSPECT_ROLE_CEMETERY to "Cemetery",
        UiText.INSPECT_RADIUS to "Radius",
        UiText.INSPECT_STONE to "Stone",
        UiText.INSPECT_CLOCK to "Clock",
        UiText.INSPECT_GRASS to "Grass",
        UiText.INSPECT_LEAF to "Leaf",
        UiText.INSPECT_SIZE to "Size",
        UiText.INSPECT_PHEROMONE to "Pheromone",
        UiText.INSPECT_HOME to "Home",
        UiText.INSPECT_TRAIL_SUGAR to "Sugar trail",
        UiText.INSPECT_TRAIL_PROTEIN to "Protein trail",
        UiText.INSPECT_DANGER to "Danger",
    )
    private val DE_LABEL = mapOf(
        UiText.INSPECT_ENERGY to "Energie",
        UiText.INSPECT_HEALTH to "Gesundheit",
        UiText.INSPECT_RADIUS to "Radius",
        UiText.INSPECT_SIZE to "Größe",
    )

    /** English enum names in sentence case, e.g. WORKER -> Worker. */
    private fun enName(key: String): String = key.lowercase().replaceFirstChar { it.uppercase() }

    private fun name(table: Map<String, Triple<String, String, String>>, key: String, lang: UiLanguage): String {
        val row = table[key] ?: return key
        return when (lang) {
            UiLanguage.EN -> enName(key)
            UiLanguage.ZH_TW -> row.first
            UiLanguage.JA -> row.second
            UiLanguage.DE -> row.third
        }
    }

    private val KIND = mapOf(
        "WORKER" to Triple("工蟻", "働きアリ", "Arbeiter"),
        "SOLDIER" to Triple("兵蟻", "兵アリ", "Soldat"),
        "QUEEN" to Triple("蟻后", "女王", "Königin"),
    )
    private val STATE = mapOf(
        "FORAGING" to Triple("覓食", "採餌", "Sammelt"),
        "RETURNING" to Triple("返回", "帰還", "Kehrt zurück"),
        "IDLE" to Triple("閒置", "待機", "Untätig"),
        "NURSING" to Triple("照顧幼體", "育児", "Pflegt"),
        "PATROLLING" to Triple("巡邏", "巡回", "Patrouilliert"),
        "FLEEING" to Triple("逃跑", "逃走", "Flieht"),
        "ATTACKING" to Triple("攻擊", "攻撃", "Greift an"),
        "TRANSPORTING" to Triple("搬運", "運搬", "Transportiert"),
        "HARVESTING" to Triple("採集", "収穫", "Erntet"),
        "HUNGRY" to Triple("飢餓", "空腹", "Hungrig"),
        "MILKING" to Triple("擠蚜", "蚜虫から採取", "Melkt"),
        "RESTING" to Triple("休息", "休息", "Ruht"),
        "RAIDING" to Triple("突襲", "襲撃", "Überfällt"),
        "DIGGING" to Triple("挖掘", "掘削", "Gräbt"),
    )
    private val CARRY = mapOf(
        "NONE" to Triple("無", "なし", "Nichts"),
        "SUGAR" to Triple("糖", "糖", "Zucker"),
        "PROTEIN" to Triple("蛋白質", "タンパク質", "Protein"),
        "BROOD" to Triple("幼體", "幼虫", "Brut"),
        "CORPSE" to Triple("屍體", "死骸", "Kadaver"),
    )
    private val FOOD = mapOf(
        "SUGAR" to Triple("糖", "糖", "Zucker"),
        "PROTEIN" to Triple("蛋白質", "タンパク質", "Protein"),
        "CORPSE" to Triple("屍體", "死骸", "Kadaver"),
    )
    private val INSECT = mapOf(
        "PREY" to Triple("獵物", "獲物", "Beute"),
        "PREDATOR" to Triple("掠食者", "捕食者", "Räuber"),
        "APHID" to Triple("蚜蟲", "アブラムシ", "Blattlaus"),
        "SPIDER" to Triple("蜘蛛", "クモ", "Spinne"),
        "BEETLE" to Triple("甲蟲", "甲虫", "Käfer"),
        "LADYBUG" to Triple("瓢蟲", "テントウムシ", "Marienkäfer"),
    )

    fun glyphChars(): String {
        val chars = StringBuilder()
        fun add(row: Triple<String, String, String>) {
            chars.append(row.first).append(row.second).append(row.third)
        }
        KIND.values.forEach(::add)
        STATE.values.forEach(::add)
        CARRY.values.forEach(::add)
        FOOD.values.forEach(::add)
        INSECT.values.forEach(::add)
        return chars.toString()
    }

    fun coversEveryKind(): Boolean {
        if (KIND.keys != AntKind.entries.map { it.name }.toSet()) return false
        if (STATE.keys != AntState.entries.map { it.name }.toSet()) return false
        if (CARRY.keys != CarryKind.entries.map { it.name }.toSet()) return false
        if (FOOD.keys != FoodKind.entries.map { it.name }.toSet()) return false
        if (INSECT.keys != InsectKind.entries.map { it.name }.toSet()) return false
        val langs = listOf(UiLanguage.ZH_TW, UiLanguage.JA, UiLanguage.DE)
        val rows = KIND.values + STATE.values + CARRY.values + FOOD.values + INSECT.values
        return rows.all { row -> langs.indices.all { row.toList()[it].isNotBlank() } }
    }
}

/** Two digits after the point, without using the default locale. */
internal fun fixed2(value: Double): String {
    val negative = value < 0.0
    val rounded = round(abs(value) * 100.0).toLong()
    val whole = rounded / 100
    val frac = (rounded % 100).toString().padStart(2, '0')
    val text = "$whole.$frac"
    return if (negative && rounded != 0L) "-$text" else text
}
