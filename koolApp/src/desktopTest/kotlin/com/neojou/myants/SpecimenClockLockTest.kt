package com.neojou.myants

import com.neojou.myants.session.SimClock
import com.neojou.myants.sim.SimConfig
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * #83: shot_lock let wall time run before the screenshot and the frame still changed, because render
 * animation (critter legs, grass sway, rain) read kool's wall clock even while the sim was paused.
 * Specimen shots must keep world.age, timeOfDay and the animation clock fixed through any number of frames.
 */
class SpecimenClockLockTest {
    private lateinit var home: File
    private var oldHome: String? = null

    @BeforeTest
    fun setUp() {
        oldHome = System.getProperty("user.home")
        home = Files.createTempDirectory("myants-home").toFile()
        System.setProperty("user.home", home.absolutePath)
    }

    @AfterTest
    fun tearDown() {
        System.setProperty("user.home", oldHome!!)
        home.deleteRecursively()
        SimConfig.resetTunables()
    }

    private fun assertFrozen(shot: String, timeOfDay: Double) {
        val s = AppSession(LaunchConfig(seed = 12345, colonies = 1, shot = shot))
        assertTrue(s.paused, "$shot starts paused")
        val age = s.world.age
        val tod = s.world.timeOfDay
        val anim = s.renderBridge.animTime
        assertEquals(timeOfDay, tod, 0.005)
        repeat(600) { s.tick(1f / 60f) } // ten seconds of frames
        assertEquals(age, s.world.age, "$shot age")
        assertEquals(tod, s.world.timeOfDay, "$shot timeOfDay")
        assertEquals(anim, s.renderBridge.animTime, "$shot animation clock")
    }

    @Test
    fun specimenStaysFrozenThroughFrames() = assertFrozen("specimen", 0.45)

    @Test
    fun specimenNightStaysFrozenThroughFrames() = assertFrozen("specimen-night", 0.92)

    @Test
    fun animationClockKeepsRunningInNormalPause() {
        val c = SimClock()
        c.advance(0.5) { }
        c.paused = true
        c.advance(3.0) { }
        assertEquals(3.5, c.animTime, 1e-9)
    }

    @Test
    fun animationClockStopsOnlyWhenFrozen() {
        val c = SimClock()
        c.advance(0.5) { }
        c.animFrozen = true
        c.advance(3.0) { }
        assertEquals(0.5, c.animTime, 1e-9)
    }

    @Test
    fun normalSessionPauseKeepsAnimating() {
        val s = AppSession(LaunchConfig(seed = 12345, colonies = 1))
        s.paused = true
        val age = s.world.age
        val anim = s.renderBridge.animTime
        repeat(60) { s.tick(1f / 60f) }
        assertEquals(age, s.world.age, "paused sim does not tick")
        assertTrue(s.renderBridge.animTime > anim, "animation still runs in a normal pause")
    }

    @Test
    fun userResumeLiftsSpecimenFreeze() {
        val s = AppSession(LaunchConfig(seed = 12345, colonies = 1, shot = "specimen"))
        assertTrue(s.animFrozen, "specimen starts frozen")
        s.tick(1f / 60f)
        assertEquals(0.0, s.renderDt, "frozen frames get no render delta")
        s.paused = false
        assertTrue(!s.animFrozen, "user resume lifts the freeze")
        val anim = s.renderBridge.animTime
        repeat(30) { s.tick(1f / 60f) }
        assertTrue(s.renderBridge.animTime > anim, "animation runs again")
        assertEquals(1.0 / 60.0, s.renderDt, 1e-6)
    }

    @Test
    fun togglePauseAlsoLiftsFreeze() {
        val c = SimClock()
        c.paused = true
        c.animFrozen = true
        c.togglePause()
        assertTrue(!c.animFrozen)
        assertEquals(0.25, c.renderDt(0.25), 1e-9)
    }

    @Test
    fun normalPauseKeepsRenderDelta() {
        val s = AppSession(LaunchConfig(seed = 12345, colonies = 1))
        s.paused = true
        s.tick(1f / 60f)
        assertEquals(1.0 / 60.0, s.renderDt, 1e-6)
    }
}
