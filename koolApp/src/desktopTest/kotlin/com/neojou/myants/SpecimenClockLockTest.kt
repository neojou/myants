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
    fun animationClockRunsOnlyWhileUnpaused() {
        val c = SimClock()
        c.advance(0.5) { }
        assertEquals(0.5, c.animTime, 1e-9)
        c.paused = true
        c.advance(3.0) { }
        assertEquals(0.5, c.animTime, 1e-9)
        c.paused = false
        c.advance(0.25) { }
        assertEquals(0.75, c.animTime, 1e-9)
    }
}
