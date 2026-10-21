package com.neojou.myants.ui

import com.neojou.myants.sim.Ant
import com.neojou.myants.sim.AntKind
import com.neojou.myants.sim.AntState
import com.neojou.myants.sim.CarryKind
import com.neojou.myants.sim.FoodKind
import com.neojou.myants.sim.InsectKind

/**
 * Inspect lines. English keeps the historical enum wording so existing tests stay valid.
 * Other languages use this table, including kind and state names.
 */
/** Structured inspect rows. The one-line [InspectCopy] text stays for tests. */
internal data class InspectPanel(val title: String, val rows: List<String>)

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

    fun antPanel(ant: Ant, lang: UiLanguage = L10n.language.value): InspectPanel {
        val title = if (lang == UiLanguage.EN) "Ant ${ant.type}" else "${word(UiText.INSPECT_ANT, lang)} ${kind(ant.type, lang)}"
        return InspectPanel(
            title,
            listOf(
                state(ant.state, lang),
                "${word(UiText.INSPECT_ENERGY, lang)}=${ant.energy.toInt()}",
                "${word(UiText.INSPECT_HEALTH, lang)}=${ant.health.toInt()}",
                carry(ant.carrying, lang),
            ),
        )
    }

    fun queenPanel(energy: Int, age: Int, lang: UiLanguage = L10n.language.value): InspectPanel = InspectPanel(
        word(UiText.INSPECT_QUEEN, lang),
        listOf(
            "${word(UiText.INSPECT_ENERGY, lang)}=$energy",
            "${word(UiText.INSPECT_AGE, lang)}=$age",
        ),
    )

    fun foodPanel(type: FoodKind, amount: Int, lang: UiLanguage = L10n.language.value): InspectPanel = InspectPanel(
        if (lang == UiLanguage.EN) type.name else food(type, lang),
        listOf(amount.toString()),
    )

    fun insectPanel(type: InsectKind, health: Int, lang: UiLanguage = L10n.language.value): InspectPanel = InspectPanel(
        if (lang == UiLanguage.EN) type.name else insect(type, lang),
        listOf("${word(UiText.INSPECT_HP, lang)} $health"),
    )

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

    private fun name(table: Map<String, Triple<String, String, String>>, key: String, lang: UiLanguage): String {
        val row = table[key] ?: return key
        return when (lang) {
            UiLanguage.EN -> key
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
