package com.neojou.myants.session

import com.neojou.myants.sim.AntPlace
import com.neojou.myants.sim.InsectKind
import com.neojou.myants.sim.Rng
import com.neojou.myants.sim.SimConfig
import com.neojou.myants.sim.World
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** #76: `shot=specimen` gives the same frozen composition every launch with the same seed. */
class ShotPoseSpecimenTest {
    private fun world(seed: Int = 12345): World {
        Rng.seed(seed)
        SimConfig.colonyCount = 1
        return World()
    }

    @AfterTest
    fun tearDown() = SimConfig.resetTunables()

    @Test
    fun specimenPutsAntAndLadybugAtCameraCentreAndAsksForPause() {
        val w = world()
        val cam = CameraState()
        assertTrue(ShotPose.apply("specimen", w, cam))
        assertEquals(6.0, cam.zoom)
        assertEquals(0.45, w.timeOfDay)
        val bugs = w.insects.filter { it.type == InsectKind.LADYBUG }
        assertEquals(1, bugs.size)
        val bug = bugs[0]
        assertTrue(abs(bug.x - cam.x) < 20 && abs(bug.y - cam.y) < 20)
        val ant = w.ants.first { abs(it.x - cam.x) < 20 && abs(it.y - cam.y) < 20 }
        assertEquals(AntPlace.WORLD, ant.location)
    }

    @Test
    fun specimenIsReproducible() {
        fun snap(): List<Any> {
            val w = world()
            ShotPose.apply("specimen", w, CameraState())
            return w.insects.map { listOf(it.type, it.x, it.y, it.angle) } +
                w.ants.map { listOf(it.x, it.y, it.angle, it.location) }
        }
        assertEquals(snap(), snap())
    }

    @Test
    fun otherShotsDoNotPauseOrSpawn() {
        for (s in listOf(null, "day", "night", "rain", "ant")) {
            val w = world()
            val before = w.insects.size
            assertFalse(ShotPose.apply(s, w, CameraState()))
            assertEquals(before, w.insects.size)
        }
    }

    @Test
    fun specimenCritterBeetleTakesTheLadybugSpot() {
        val lady = world().also { ShotPose.apply("specimen", it, CameraState()) }.insects.last()
        val w = world()
        assertTrue(ShotPose.apply("specimen", w, CameraState(), InsectKind.BEETLE))
        val beetle = w.insects.last()
        assertEquals(InsectKind.BEETLE, beetle.type)
        assertEquals(listOf(lady.x, lady.y, lady.angle), listOf(beetle.x, beetle.y, beetle.angle))
        assertTrue(w.insects.none { it.type == InsectKind.LADYBUG && it.x == lady.x && it.y == lady.y })
    }

    @Test
    fun specimenCritterParsesOnlyKnownNames() {
        assertEquals(null, com.neojou.myants.parseLaunch(emptyList()).specimenCritter)
        assertEquals(InsectKind.BEETLE, com.neojou.myants.parseLaunch(listOf("specimenCritter=beetle")).specimenCritter)
        assertEquals(InsectKind.BEETLE, com.neojou.myants.parseLaunch(listOf("--specimen-critter=BEETLE")).specimenCritter)
        assertEquals(null, com.neojou.myants.parseLaunch(listOf("specimenCritter=spider")).specimenCritter)
    }

    @Test
    fun specimenNightIsSameCompositionAtNightAndAsksForPause() {
        val day = world(); val dayCam = CameraState()
        ShotPose.apply("specimen", day, dayCam, InsectKind.BEETLE)
        val night = world(); val cam = CameraState()
        assertTrue(ShotPose.apply("specimen-night", night, cam, InsectKind.BEETLE))
        assertEquals(0.92, night.timeOfDay, 0.005)
        assertEquals(0.0, night.dayBrightness(), 1e-9)
        assertFalse(night.raining)
        assertEquals(dayCam.x, cam.x); assertEquals(dayCam.y, cam.y); assertEquals(dayCam.zoom, cam.zoom)
        val b = night.insects.single { it.type == InsectKind.BEETLE }
        val bd = day.insects.single { it.type == InsectKind.BEETLE }
        assertEquals(bd.x, b.x); assertEquals(bd.y, b.y)
        assertEquals(0.45, day.timeOfDay)
    }
}
