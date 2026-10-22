package com.neojou.myants.render3d.nest

import com.neojou.myants.sim.BroodStage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NestMarksTest {
    @Test
    fun chamberRimIsWiderThanTheFloor() {
        assertEquals(118f, chamberRimDiameter(100f), 0.01f)
        assertTrue(chamberRimDiameter(40f) > 40f)
    }

    @Test
    fun tunnelDiskIsNarrowerThanTheNode() {
        assertEquals(78f, tunnelDrawDiameter(100f), 0.01f)
        assertTrue(tunnelDrawDiameter(40f) < 40f)
    }

    @Test
    fun deepSoilCoversTheLowerPartOfThePanel() {
        assertTrue(SOIL_BAND_FRACTION > 0.5f)
        assertTrue(SOIL_BAND_FRACTION < 0.85f)
    }

    @Test
    fun broodShapeFollowsTheStage() {
        val egg = broodSpan(BroodStage.EGG, 1f, 0f)
        val larva = broodSpan(BroodStage.LARVA, 1f, 0f)
        val pupa = broodSpan(BroodStage.PUPA, 1f, 0f)
        assertEquals(egg.length, egg.width, 0.001f)
        assertTrue(larva.length / larva.width > 2f)
        assertTrue(pupa.length > pupa.width)
        assertTrue(pupa.width > egg.width)
        assertTrue(pupa.length / pupa.width < larva.length / larva.width)
        assertTrue(broodSpan(BroodStage.EGG, 1f, 1f).length > egg.length)
    }

    @Test
    fun broodJitterStaysInRangeAndIsStable() {
        val a = broodJitter(12.5f, 40f)
        assertEquals(a, broodJitter(12.5f, 40f))
        assertTrue(a in 0f..1f)
        assertTrue(broodJitter(3f, 8f) != a)
    }

    @Test
    fun nestAntHasARearGasterAndASoldierGrowsTheHead() {
        val worker = nestAntParts(false)
        val soldier = nestAntParts(true)
        assertTrue(worker.gaster.along < 0f)
        assertTrue(worker.head.along > worker.thorax.along)
        assertTrue(soldier.head.length > worker.head.length)
        assertTrue(soldier.head.width > worker.head.width)
        assertEquals(worker.gaster.along, soldier.gaster.along)
        val soldierFront = soldier.head.along + soldier.head.length * 0.5f
        assertTrue(NEST_CARRY_ALONG > soldierFront)
    }

    @Test
    fun queenGasterIsLargerAndSitsBehindTheBody() {
        val marks = queenMarks(1f)
        assertEquals(2, marks.size)
        val gaster = marks[0]
        val body = marks[1]
        assertTrue(gaster.dx < body.dx)
        assertTrue(gaster.h > body.h)
        assertTrue(gaster.w + body.w > 22f)
    }
}
