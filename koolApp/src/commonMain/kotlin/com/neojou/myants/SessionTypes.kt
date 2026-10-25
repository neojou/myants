package com.neojou.myants

enum class SimTool { INSPECT, SUGAR, PROTEIN, ENEMY }

/**
 * One analysis row. [copyId] is resolved from the string table at draw time,
 * so a language change updates an open panel. [literal] is the action label,
 * or the fallback when [copyId] is unknown.
 */
class AnalyseLine(
    val copyId: String?,
    val severity: String?,
    val effect: Boolean,
    val literal: String,
    val path: String?,
    val to: Double,
)

class GraphSample(val population: Int, val sugar: Double, val protein: Double, val energyPct: Double)
