package com.neojou.myants.sim

import kotlin.math.ceil
import kotlin.math.floor

/**
 * Four float32 fields. Arithmetic is float64, then rounded on store,
 * matching JS `Float32Array` assignment.
 */
class PheromoneGrid(
    worldWidth: Double,
    worldHeight: Double,
    scale: Double = SimConfig.pheromone.resolutionScale,
) {
    var scale: Double = scale
        private set
    val width: Int = ceil(worldWidth * this.scale).toInt()
    val height: Int = ceil(worldHeight * this.scale).toInt()

    val toHome: FloatArray
    val toSugar: FloatArray
    val toProtein: FloatArray
    val toDanger: FloatArray
    private val temp: FloatArray

    var diffusionEnabled: Boolean = true

    init {
        val size = width * height
        toHome = FloatArray(size)
        toSugar = FloatArray(size)
        toProtein = FloatArray(size)
        toDanger = FloatArray(size)
        temp = FloatArray(size)
    }

    constructor(worldWidth: Int, worldHeight: Int, scale: Double = SimConfig.pheromone.resolutionScale) :
        this(worldWidth.toDouble(), worldHeight.toDouble(), scale)

    fun scaleAll(factor: Double) {
        for (i in toHome.indices) {
            toHome[i] = f32mul(toHome[i], factor)
            toSugar[i] = f32mul(toSugar[i], factor)
            toProtein[i] = f32mul(toProtein[i], factor)
            toDanger[i] = f32mul(toDanger[i], factor)
        }
    }

    fun update() {
        val decay = SimConfig.pheromone.decay
        val foodDecay = SimConfig.pheromone.foodDecay
        val dangerDecay = SimConfig.pheromone.dangerDecay
        val min = SimConfig.pheromone.minThreshold
        for (i in toHome.indices) {
            toHome[i] = f32mul(toHome[i], decay)
            toSugar[i] = f32mul(toSugar[i], foodDecay)
            toProtein[i] = f32mul(toProtein[i], foodDecay)
            toDanger[i] = f32mul(toDanger[i], dangerDecay)
            if (toHome[i].toDouble() < min) toHome[i] = 0f
            if (toSugar[i].toDouble() < min) toSugar[i] = 0f
            if (toProtein[i].toDouble() < min) toProtein[i] = 0f
            if (toDanger[i].toDouble() < min) toDanger[i] = 0f
        }
        if (diffusionEnabled && SimConfig.pheromone.diffusionEnabled) {
            val rate = SimConfig.pheromone.diffusionRate
            diffuse(toHome, rate)
            if (SimConfig.pheromone.diffuseFood) {
                diffuse(toSugar, rate)
                diffuse(toProtein, rate)
            }
        }
    }

    private fun diffuse(grid: FloatArray, rate: Double) {
        val w = width
        val h = height
        val keep = 1.0 - rate
        val min = SimConfig.pheromone.minThreshold
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                val i = row + x
                val c = grid[i].toDouble()
                val l = if (x > 0) grid[i - 1].toDouble() else c
                val r = if (x < w - 1) grid[i + 1].toDouble() else c
                temp[i] = (keep * c + rate * (l + r) * 0.5).toFloat()
            }
        }
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                val i = row + x
                val c = temp[i].toDouble()
                val u = if (y > 0) temp[i - w].toDouble() else c
                val d = if (y < h - 1) temp[i + w].toDouble() else c
                var v = keep * c + rate * (u + d) * 0.5
                if (v < min) v = 0.0
                grid[i] = v.toFloat()
            }
        }
    }

    private fun channel(type: Pheromone): FloatArray = when (type) {
        Pheromone.HOME -> toHome
        Pheromone.SUGAR -> toSugar
        Pheromone.PROTEIN -> toProtein
        Pheromone.DANGER -> toDanger
    }

    fun deposit(x: Double, y: Double, type: Pheromone, amount: Double) {
        val ix = floor(x * scale).toInt()
        val iy = floor(y * scale).toInt()
        if (ix in 0 until width && iy in 0 until height) {
            val idx = iy * width + ix
            val grid = channel(type)
            grid[idx] = f32addCap1(grid[idx], amount)
        }
    }

    fun depositCircle(x: Double, y: Double, type: Pheromone, amount: Double, radius: Double) {
        val scaledX = x * scale
        val scaledY = y * scale
        val scaledRadius = radius * scale
        val startX = floor(scaledX - scaledRadius).toInt()
        val endX = floor(scaledX + scaledRadius).toInt()
        val startY = floor(scaledY - scaledRadius).toInt()
        val endY = floor(scaledY + scaledRadius).toInt()
        val r2 = scaledRadius * scaledRadius
        val grid = channel(type)
        for (iy in startY..endY) {
            for (ix in startX..endX) {
                if (ix in 0 until width && iy in 0 until height) {
                    val dx = ix - scaledX
                    val dy = iy - scaledY
                    if (dx * dx + dy * dy <= r2) {
                        val idx = iy * width + ix
                        grid[idx] = f32addCap1(grid[idx], amount)
                    }
                }
            }
        }
    }

    fun get(x: Double, y: Double, type: Pheromone): Double {
        val ix = floor(x * scale).toInt()
        val iy = floor(y * scale).toInt()
        if (ix in 0 until width && iy in 0 until height) {
            return channel(type)[iy * width + ix].toDouble()
        }
        return 0.0
    }
}
