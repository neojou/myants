package com.neojou.myants

import com.neojou.myants.session.Panel
import com.neojou.myants.sim.FoodKind
import com.neojou.myants.sim.SimConfig
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Guards the AppSession to session package delegation layer (#7). */
class AppSessionTest {
    private lateinit var home: File
    private var oldHome: String? = null
    private lateinit var s: AppSession

    @BeforeTest
    fun setUp() {
        oldHome = System.getProperty("user.home")
        home = Files.createTempDirectory("myants-home").toFile()
        System.setProperty("user.home", home.absolutePath) // keep the real settings file untouched
        s = AppSession(LaunchConfig(seed = 1, colonies = 1))
        for (c in s.world.colonies) c.ants.clear()
        s.world.foods.clear()
        s.world.insects.clear()
    }

    @AfterTest
    fun tearDown() {
        System.setProperty("user.home", oldHome!!)
        home.deleteRecursively()
        SimConfig.resetTunables()
    }

    @Test
    fun toolPropertiesDelegateToToolController() {
        s.tool = SimTool.ENEMY
        assertEquals(SimTool.ENEMY, s.tools.tool)
        assertSame(s.tools.inspect, s.inspect)
    }

    @Test
    fun clickWorldAppliesToolAndBumps() {
        val p = s.world.getSafePosition()
        val rev = s.revision.value
        s.tool = SimTool.SUGAR
        s.clickWorld(p.x, p.y)
        assertEquals(FoodKind.SUGAR, s.world.foods.single().type)
        assertEquals(rev + 1, s.revision.value)
        s.tool = SimTool.INSPECT
        s.clickWorld(p.x, p.y)
        assertEquals("SUGAR 200", s.inspect.value)
    }

    @Test
    fun restartClearsInspectThroughToolController() {
        val p = s.world.getSafePosition()
        s.world.placeFood(p.x, p.y, FoodKind.PROTEIN)
        s.clickWorld(p.x, p.y)
        assertTrue(s.inspect.value.isNotEmpty())
        s.restart(fromUser = false)
        assertEquals("", s.inspect.value)
        assertNull(s.inspectedAnt)
    }

    @Test
    fun chromeStatesTrackToolbarActions() {
        assertFalse(s.pausedState.value)
        assertEquals("1x", s.speedLabelState.value)
        assertEquals(SimTool.INSPECT, s.toolState.value)
        assertFalse(s.rainingState.value)
        assertEquals(1, s.colonyCountState.value)

        s.togglePause()
        assertTrue(s.pausedState.value)
        s.stepSpeed(1)
        assertEquals("2x", s.speedLabelState.value)
        s.tool = SimTool.ENEMY
        s.bump()
        assertEquals(SimTool.ENEMY, s.toolState.value)
        s.toggleRain()
        assertTrue(s.rainingState.value)
        assertTrue(s.world.raining)
        s.setColonies(2)
        assertEquals(2, s.colonyCountState.value)
        assertFalse(s.rainingState.value)
        s.setColonies(1)
        assertEquals(1, s.colonyCountState.value)
    }

    @Test
    fun clockCameraAndViewDelegation() {
        s.togglePause()
        assertTrue(s.clock.paused)
        s.camX = 12.0
        assertEquals(12.0, s.camera.x)
        s.openPanel(Panel.PARAMS)
        assertTrue(s.view.showParams && s.showParams)
        s.refreshAnalyse()
        assertFalse(s.showParams)
        assertTrue(s.showAnalyse && s.analyseLines.isNotEmpty())
        s.stepSpeed(1)
        assertEquals("2x", s.speedLabel())
        assertTrue(File(home, ".myants/antsim.ui.v1.txt").readText().startsWith("speed=2.0\n"))
    }
}
