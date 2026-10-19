package com.neojou.myants.sim

import kotlin.math.ceil
import kotlin.math.floor

class SpatialGrid<T : SpatialItem>(width: Double, height: Double, val cellSize: Double) {
    val cols: Int = ceil(width / cellSize).toInt().coerceAtLeast(1)
    val rows: Int = ceil(height / cellSize).toInt().coerceAtLeast(1)
    private val buckets = HashMap<Int, MutableList<T>>()

    constructor(width: Int, height: Int, cellSize: Double) : this(width.toDouble(), height.toDouble(), cellSize)

    fun clear() {
        buckets.clear()
    }

    fun add(item: T) {
        val col = floor(item.x / cellSize).toInt().coerceIn(0, cols - 1)
        val row = floor(item.y / cellSize).toInt().coerceIn(0, rows - 1)
        val index = row * cols + col
        val bucket = buckets.getOrPut(index) { ArrayList() }
        bucket.add(item)
    }

    fun remove(item: T) {
        val col = floor(item.x / cellSize).toInt().coerceIn(0, cols - 1)
        val row = floor(item.y / cellSize).toInt().coerceIn(0, rows - 1)
        val bucket = buckets[row * cols + col] ?: return
        bucket.remove(item)
    }

    fun getNearby(x: Double, y: Double, radius: Double): List<T> {
        val nearby = ArrayList<T>()
        val cellRadius = ceil(radius / cellSize).toInt()
        val centerCol = floor(x / cellSize).toInt()
        val centerRow = floor(y / cellSize).toInt()
        for (r in (centerRow - cellRadius)..(centerRow + cellRadius)) {
            for (c in (centerCol - cellRadius)..(centerCol + cellRadius)) {
                if (c in 0 until cols && r in 0 until rows) {
                    val bucket = buckets[r * cols + c]
                    if (bucket != null) nearby.addAll(bucket)
                }
            }
        }
        return nearby
    }
}
