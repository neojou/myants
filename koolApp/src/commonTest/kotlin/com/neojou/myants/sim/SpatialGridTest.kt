package com.neojou.myants.sim

import kotlin.test.Test
import kotlin.test.assertTrue

class SpatialGridTest {
    private class Pt(override val x: Double, override val y: Double) : SpatialItem

    @Test
    fun returnsEntitiesInNearbyCells() {
        val grid = SpatialGrid<Pt>(1000, 600, 50.0)
        val near = Pt(100.0, 100.0)
        grid.add(near)
        assertTrue(near in grid.getNearby(100.0, 100.0, 30.0))
    }

    @Test
    fun excludesFarEntities() {
        val grid = SpatialGrid<Pt>(1000, 600, 50.0)
        val far = Pt(900.0, 500.0)
        grid.add(far)
        assertTrue(far !in grid.getNearby(100.0, 100.0, 30.0))
    }

    @Test
    fun clearEmptiesTheGrid() {
        val grid = SpatialGrid<Pt>(1000, 600, 50.0)
        grid.add(Pt(100.0, 100.0))
        grid.clear()
        assertTrue(grid.getNearby(100.0, 100.0, 50.0).isEmpty())
    }

    @Test
    fun removeDropsOneItem() {
        val grid = SpatialGrid<Pt>(1000, 600, 50.0)
        val a = Pt(100.0, 100.0)
        val b = Pt(102.0, 98.0)
        grid.add(a)
        grid.add(b)
        grid.remove(a)
        val found = grid.getNearby(100.0, 100.0, 30.0)
        assertTrue(a !in found)
        assertTrue(b in found)
    }

    @Test
    fun edgeCellDoesNotCrash() {
        val grid = SpatialGrid<Pt>(1000, 600, 50.0)
        val corner = Pt(0.0, 0.0)
        grid.add(corner)
        assertTrue(corner in grid.getNearby(0.0, 0.0, 60.0))
    }
}
