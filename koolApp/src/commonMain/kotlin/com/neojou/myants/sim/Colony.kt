package com.neojou.myants.sim

import kotlin.math.PI

class Colony(
    val id: Int,
    val nest: Nest,
    val queen: Queen,
    var nestGrid: PheromoneGrid,
    val entranceSide: EntranceSide,
) {
    var outdoorField: PheromoneGrid = PheromoneGrid(SimConfig.width, SimConfig.height)
    val ants = ArrayList<Ant>()
    val brood = ArrayList<Brood>()
    var sugarStockpile: Double = SimConfig.startSugar
    var proteinStockpile: Double = SimConfig.startProtein
    var trophallaxisCount: Int = 0
    var raidCooldown: Int = 0
    var digSite: Vec2? = null
    var digProgress: Int = 0

    val isLandscape: Boolean
    val workerTint: Int
    val soldierTint: Int
    val teamColor: String
    val workerColor2D: String
    val soldierColor2D: String
    val entranceWorld: Vec2
    val worldExitPoint: Vec2
    val worldExitAngle: Double
    val entranceNestLocal: Vec2
    val entranceNestAngle: Double

    init {
        queen.colony = this
        val workerTints = intArrayOf(0xc4703a, 0xd9bf52, 0x88c0a0, 0xd0a0d0)
        val soldierTints = intArrayOf(0xffffff, 0xffffff, 0xffffff, 0xffffff)
        val teamColors = arrayOf("", "#d9bf52", "#88c0a0", "#d0a0d0")
        val worker2d = arrayOf("#bd6a38", "#c9a83e", "#7ab090", "#c090c0")
        val soldier2d = arrayOf("", "#2b271f", "", "")
        workerTint = workerTints[id % workerTints.size]
        soldierTint = soldierTints[id % soldierTints.size]
        teamColor = teamColors[id % teamColors.size]
        workerColor2D = worker2d[id % worker2d.size]
        soldierColor2D = soldier2d[id % soldier2d.size]

        isLandscape = nest.height > nest.width
        val w = SimConfig.width.toDouble()
        val h = SimConfig.height.toDouble()
        when (entranceSide) {
            EntranceSide.RIGHT -> {
                entranceWorld = Vec2(w, h / 2.0)
                worldExitPoint = Vec2(w - 25.0, h / 2.0)
                worldExitAngle = PI
                entranceNestLocal = Vec2(25.0, nest.height / 2.0)
                entranceNestAngle = 0.0
            }
            EntranceSide.LEFT -> {
                entranceWorld = Vec2(0.0, h / 2.0)
                worldExitPoint = Vec2(25.0, h / 2.0)
                worldExitAngle = 0.0
                entranceNestLocal = Vec2(25.0, nest.height / 2.0)
                entranceNestAngle = 0.0
            }
            EntranceSide.BOTTOM -> {
                entranceWorld = Vec2(w / 2.0, h)
                worldExitPoint = Vec2(w / 2.0, h - 25.0)
                worldExitAngle = -PI / 2.0
                entranceNestLocal = Vec2(nest.width / 2.0, 25.0)
                entranceNestAngle = PI / 2.0
            }
            EntranceSide.TOP -> {
                entranceWorld = Vec2(w / 2.0, 0.0)
                worldExitPoint = Vec2(w / 2.0, 25.0)
                worldExitAngle = PI / 2.0
                entranceNestLocal = Vec2(nest.width / 2.0, 25.0)
                entranceNestAngle = PI / 2.0
            }
        }
    }

    val eggs: Int get() = brood.count { it.stage == BroodStage.EGG }
    val larvae: Int get() = brood.count { it.stage == BroodStage.LARVA }
    val pupae: Int get() = brood.count { it.stage == BroodStage.PUPA }

    fun storageCapacity(): Double {
        val granaries = nest.getChambers(ChamberRole.STORAGE).size
        return SimConfig.nest.storageBaseCapacity + granaries * SimConfig.nest.storagePerGranary
    }

    fun spawnAnt(kind: AntKind) {
        val ant = Ant(queen.x, queen.y, kind)
        ant.location = AntPlace.NEST
        ant.colony = this
        ants.add(ant)
    }
}
