package com.neojou.myants

import com.neojou.myants.session.Panel
import de.fabmax.kool.util.Time
import com.neojou.myants.ui.L10n
import com.neojou.myants.ui.UiLanguage
import com.neojou.myants.sim.Ant
import com.neojou.myants.sim.AntKind
import com.neojou.myants.sim.AntPlace
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
        L10n.language.value = UiLanguage.EN
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
    fun restartPromptLeavesTheWorldUntilConfirmed() {
        val p = s.world.getSafePosition()
        s.world.placeFood(p.x, p.y, FoodKind.PROTEIN)
        val before = s.world
        val serial = s.worldSerial
        s.askRestart()
        assertTrue(s.restartPrompt)
        assertSame(before, s.world)
        assertEquals(serial, s.worldSerial)
        assertEquals(1, s.world.foods.count { it.type == FoodKind.PROTEIN })
        s.dismissRestart()
        assertFalse(s.restartPrompt)
        assertSame(before, s.world)
        assertEquals(1, before.foods.count { it.type == FoodKind.PROTEIN })
        s.askRestart()
        s.confirmRestart()
        assertFalse(s.restartPrompt)
        assertTrue(s.world !== before)
        assertEquals(serial + 1, s.worldSerial)
        assertEquals(0, s.world.foods.count { it.type == FoodKind.PROTEIN })
    }

    @Test
    fun aboutOpensThroughOpenPanelAndNotifiesInput() {
        assertFalse(s.showAbout)
        assertEquals(0.0, s.cine.pausedUntil)
        val rev = s.revision.value
        s.openPanel(Panel.ABOUT)
        assertTrue(s.showAbout)
        assertEquals(rev + 1, s.revision.value)
        assertEquals(Time.gameTime + 25.0, s.cine.pausedUntil, 1e-9)
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
    fun hudStringsPublishAtAboutFourHertz() {
        val hud = s.hud
        hud.sync(s.world, 30, 0.0)
        assertEquals(1, hud.syncPublishes)
        assertEquals("FPS: 30", s.hudFps.value)
        hud.sync(s.world, 40, 0.1)
        hud.sync(s.world, 50, 0.24)
        assertEquals(1, hud.syncPublishes)
        assertEquals("FPS: 30", s.hudFps.value)
        hud.sync(s.world, 60, 0.25)
        assertEquals(2, hud.syncPublishes)
        assertEquals("FPS: 60", s.hudFps.value)

        s.togglePause()
        val before = hud.syncPublishes
        s.stepOnce()
        assertEquals(before + 1, hud.syncPublishes)
    }

    @Test
    fun chromeOnlyActionsDoNotBumpPanelRevision() {
        val rev = s.revision.value
        s.stepSpeed(1)
        s.togglePause()
        s.toggleRain()
        s.tool = SimTool.SUGAR
        s.publishChrome()
        assertEquals(rev, s.revision.value)
        assertEquals("2x", s.speedLabelState.value)
        assertTrue(s.pausedState.value)
        assertTrue(s.rainingState.value)
        assertEquals(SimTool.SUGAR, s.toolState.value)
        s.setPheromones(!s.showPheromones)
        assertEquals(rev + 1, s.revision.value)
    }

    @Test
    fun grassAndPitchSlidersRefreshThePanel() {
        val rev = s.revision.value
        s.chooseGrassDensity(0.4f)
        assertEquals(0.4f, s.grassDensity)
        assertEquals(rev + 1, s.revision.value)
        s.choosePitch(-75f)
        assertEquals(-75f, s.cameraPitchDeg)
        assertEquals(rev + 2, s.revision.value)
        val text = File(home, ".myants/antsim.ui.v1.txt").readText()
        assertTrue("grassDensity=0.4" in text)
        assertTrue("cameraPitch=-75.0" in text)
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

    @Test
    fun languagePersistsAndReloads() {
        assertEquals(UiLanguage.EN, s.view.language)
        val rev = s.revision.value
        s.setLanguage(UiLanguage.ZH_TW)
        assertEquals(UiLanguage.ZH_TW, L10n.language.value)
        assertEquals(rev + 1, s.revision.value)
        val text = File(home, ".myants/antsim.ui.v1.txt").readText()
        assertTrue("language=ZH_TW" in text)
        val again = AppSession(LaunchConfig(seed = 1, colonies = 1))
        assertEquals(UiLanguage.ZH_TW, again.view.language)
        again.setLanguage(UiLanguage.EN)
    }

    @Test
    fun followTracksWorldTargetsAndStopsForNestOrAMiss() {
        s.paused = true
        s.cinema = false
        val p = s.world.getSafePosition()
        s.world.placeFood(p.x, p.y, FoodKind.SUGAR)
        s.clickWorld(p.x, p.y)
        assertEquals(p.x, s.followAnchor()?.first)
        assertEquals(p.y, s.followAnchor()?.second)
        s.camera.x = p.x - 100.0
        s.camera.y = p.y
        s.toggleUserFollow()
        assertTrue(s.userFollow)
        assertTrue(s.following.value)
        val x0 = s.camera.x
        s.tick(0f)
        assertEquals(x0 + 12.0, s.camera.x, 1e-6)
        assertTrue(s.userFollow)

        s.clickWorld(p.x, p.y)
        assertTrue(s.userFollow)

        s.clickWorld(-1.0, p.y)
        assertFalse(s.userFollow)
        assertFalse(s.following.value)
        assertNull(s.followAnchor())

        s.clickWorld(p.x, p.y)
        s.toggleUserFollow()
        s.inspectNest(0.0, 0.0)
        assertFalse(s.userFollow)
        assertNull(s.followAnchor())
        val held = s.camera.x
        s.tick(0f)
        assertEquals(held, s.camera.x, 1e-9)
    }

    @Test
    fun followStopsWhenTheWorldAntDiesOrTheUserDrags() {
        s.paused = true
        s.cinema = false
        val ant = Ant(100.0, 80.0, AntKind.WORKER).apply {
            location = AntPlace.WORLD
            x = 100.0
            y = 80.0
            energy = 10.0
        }
        s.world.colonies[0].ants.add(ant)
        s.clickWorld(ant.x, ant.y)
        assertEquals(ant.x, s.followAnchor()?.first)
        s.camera.x = ant.x - 100.0
        s.camera.y = ant.y
        s.toggleUserFollow()
        ant.energy = 0.0
        val x0 = s.camera.x
        s.tick(0f)
        assertFalse(s.userFollow)
        assertEquals(x0, s.camera.x, 1e-9)

        s.userFollow = true
        s.noteDrag()
        assertFalse(s.userFollow)
        assertFalse(s.following.value)
    }
}
