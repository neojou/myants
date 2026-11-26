package com.neojou.myants.session

import com.neojou.myants.AppSession
import com.neojou.myants.LaunchConfig
import com.neojou.myants.parseLaunch
import com.neojou.myants.sim.Ant
import com.neojou.myants.sim.AntKind
import com.neojou.myants.sim.AntPlace
import com.neojou.myants.sim.Rng
import com.neojou.myants.sim.SimConfig
import com.neojou.myants.sim.World
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** #109 R2: `specimenRival=1` puts a colony-1 worker on the specimen ant's spot and pose; nothing else changes. */
class ShotPoseSpecimenRivalTest {
    private fun world(colonies: Int = 2): World {
        Rng.seed(12345)
        SimConfig.colonyCount = colonies
        return World()
    }

    private fun allAnts(w: World): List<Ant> = w.colonies.flatMap { it.ants }

    private fun snap(w: World, cam: CameraState): List<Any> =
        listOf(cam.x, cam.y, cam.zoom, w.timeOfDay, w.age, w.raining) +
            w.insects.map { listOf(it.type, it.x, it.y, it.angle) } +
            allAnts(w).map { listOf(it.colony.id, it.type, it.x, it.y, it.angle, it.location) }

    @AfterTest
    fun tearDown() = SimConfig.resetTunables()

    @Test
    fun rivalWorkerTakesTheSpecimenAntSpotAndPose() {
        for (shot in listOf("specimen", "specimen-night")) {
            val base = world(); val baseCam = CameraState()
            ShotPose.apply(shot, base, baseCam)
            val ref = base.ants.single { it.x == ShotPose.SPECIMEN_X - ShotPose.SPECIMEN_GAP && it.y == ShotPose.SPECIMEN_Y }

            val w = world(); val cam = CameraState()
            assertTrue(ShotPose.apply(shot, w, cam, specimenRival = true), shot)
            val there = allAnts(w).filter { it.x == ref.x && it.y == ref.y }
            assertEquals(1, there.size, "$shot: one ant on the spot")
            val rival = there.single()
            assertEquals(1, rival.colony.id, shot)
            assertEquals(AntKind.WORKER, rival.type, shot)
            assertEquals(AntPlace.WORLD, rival.location, shot)
            assertEquals(ref.angle, rival.angle, shot)
            assertEquals(listOf(baseCam.x, baseCam.y, baseCam.zoom), listOf(cam.x, cam.y, cam.zoom), shot)
            assertEquals(base.timeOfDay, w.timeOfDay, shot)
            assertEquals(base.insects.map { listOf(it.type, it.x, it.y, it.angle) }, w.insects.map { listOf(it.type, it.x, it.y, it.angle) }, shot)
        }
    }

    @Test
    fun rivalSpecimenIsReproducible() {
        fun once(): List<Any> {
            val w = world(); val cam = CameraState()
            ShotPose.apply("specimen", w, cam, specimenRival = true)
            return snap(w, cam)
        }
        assertEquals(once(), once())
    }

    @Test
    fun withoutTheFlagSpecimenIsUnchanged() {
        val a = world(); val ca = CameraState(); ShotPose.apply("specimen", a, ca)
        val b = world(); val cb = CameraState(); ShotPose.apply("specimen", b, cb, specimenRival = false)
        assertEquals(snap(a, ca), snap(b, cb))
        assertTrue(allAnts(a).none { it.colony.id == 1 && it.x == ShotPose.SPECIMEN_X - ShotPose.SPECIMEN_GAP && it.y == ShotPose.SPECIMEN_Y })
    }

    @Test
    fun theFlagDoesNothingForOtherShots() {
        for (s in listOf(null, "day", "night", "rain", "ant")) {
            val a = world(); val ca = CameraState(); assertFalse(ShotPose.apply(s, a, ca))
            val b = world(); val cb = CameraState(); assertFalse(ShotPose.apply(s, b, cb, specimenRival = true))
            assertEquals(snap(a, ca), snap(b, cb), "$s")
        }
    }

    @Test
    fun specimenRivalParses() {
        assertFalse(parseLaunch(emptyList()).specimenRival)
        assertTrue(parseLaunch(listOf("specimenRival=1")).specimenRival)
        assertTrue(parseLaunch(listOf("--specimen-rival=true")).specimenRival)
        assertFalse(parseLaunch(listOf("specimenRival=0")).specimenRival)
    }

    @Test
    fun sessionForcesTwoColoniesForTheRivalSpecimen() {
        val s = AppSession(LaunchConfig(seed = 12345, colonies = 1, shot = "specimen", specimenRival = true))
        assertEquals(2, s.colonies)
        assertTrue(s.animFrozen)
        val rival = allAnts(s.world).single { it.x == ShotPose.SPECIMEN_X - ShotPose.SPECIMEN_GAP && it.y == ShotPose.SPECIMEN_Y }
        assertEquals(1, rival.colony.id)
    }

    @Test
    fun sessionKeepsTheColonyCountWithoutTheFlag() {
        assertEquals(1, AppSession(LaunchConfig(seed = 12345, colonies = 1, shot = "specimen")).colonies)
        assertEquals(1, AppSession(LaunchConfig(seed = 12345, colonies = 1, specimenRival = true)).colonies)
    }
}
