package com.neojou.myants.session

import com.neojou.myants.sim.Ant
import com.neojou.myants.sim.AntKind
import com.neojou.myants.sim.AntPlace
import com.neojou.myants.sim.Rng
import com.neojou.myants.sim.SimConfig
import com.neojou.myants.sim.World
import com.neojou.myants.ui.HudSlots
import com.neojou.myants.ui.L10n
import com.neojou.myants.ui.MyantsTheme
import com.neojou.myants.ui.UiLanguage
import com.neojou.myants.ui.hudNumberWidth
import de.fabmax.kool.modules.ui2.Dp
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionPartsTest {
    private fun world(): World {
        Rng.seed(1)
        SimConfig.colonyCount = 1
        return World()
    }

    @AfterTest
    fun tearDown() {
        SimConfig.resetTunables()
        L10n.language.value = UiLanguage.EN
    }

    @Test
    fun hudStringsMatchLegacyFormat() {
        val w = world()
        val h = HudState()
        h.sync(w, 42, 0.0)
        assertEquals("FPS: 42", h.fps.value)
        val ants = w.colonies[0].ants
        assertEquals(
            "Population: ${ants.size} (W:${ants.count { it.type == AntKind.WORKER }} S:${ants.count { it.type == AntKind.SOLDIER }})",
            h.pop.value,
        )
        assertTrue(h.food.value.startsWith("Protein: ") && " | Sugar: " in h.food.value)
        assertEquals("Queen Age: 0m 0s", h.queen.value)
        assertEquals("42", h.fpsDigits.value)
        assertFalse(h.popSplit.value)
        assertEquals(ants.size.toString(), h.popDigits.value)
        assertEquals(ants.count { it.type == AntKind.WORKER }.toString(), h.workers.value)
        assertEquals(ants.count { it.type == AntKind.SOLDIER }.toString(), h.soldiers.value)
        assertEquals("0", h.queenMin.value)
        assertEquals("0", h.queenSec.value)
        assertTrue(h.proteinDigits.value.isNotEmpty() && h.sugarDigits.value.isNotEmpty())
        assertEquals("0.5x", HudState.speedLabel(0.5))
        assertEquals("8x", HudState.speedLabel(8.0))
    }

    @Test
    fun hudStringsFollowLanguage() {
        val w = world()
        val h = HudState()
        L10n.language.value = UiLanguage.DE
        h.sync(w, 42, 0.0, force = true)
        assertTrue("Zucker" in h.food.value, h.food.value)
        assertEquals("Königinnenalter: 0min 0s", h.queen.value)
        assertTrue(h.pop.value.startsWith("Bevölkerung:"), h.pop.value)
        L10n.language.value = UiLanguage.ZH_TW
        h.sync(w, 42, 0.0, force = true)
        assertTrue(h.pop.value.startsWith("族群:"), h.pop.value)
        assertTrue("糖" in h.food.value, h.food.value)
        L10n.language.value = UiLanguage.EN
        h.sync(w, 42, 0.0, force = true)
        assertTrue("Sugar" in h.food.value, h.food.value)
        assertEquals("Queen Age: 0m 0s", h.queen.value)
        assertTrue(h.pop.value.startsWith("Population:"), h.pop.value)
    }

    @Test
    fun hudSplitPopulationKeepsLegacyStringAndDigitFields() {
        SimConfig.colonyCount = 2
        try {
            Rng.seed(1)
            val w = World()
            val h = HudState()
            h.sync(w, 1, 0.0)
            assertTrue(h.popSplit.value)
            assertFalse(h.popMany.value)
            assertEquals(w.colonies[0].ants.size.toString(), h.colonyA.value)
            assertEquals(w.colonies[1].ants.size.toString(), h.colonyB.value)
            val split = w.colonies.joinToString(" vs ") { it.ants.size.toString() }
            assertEquals("Population: ${w.totalAntCount()} ($split)", h.pop.value)
        } finally {
            SimConfig.colonyCount = 1
        }
    }

    @Test
    fun hudNumberSlotsStayFixedWidth() {
        assertEquals(Dp(11f), MyantsTheme.size.hudDigit)
        assertEquals(Dp(44f), hudNumberWidth(HudSlots.FPS))
        assertEquals(Dp(55f), hudNumberWidth(HudSlots.POP))
        assertEquals(Dp(66f), hudNumberWidth(HudSlots.STOCK))
        assertEquals(Dp(44f), hudNumberWidth(HudSlots.QUEEN_MIN))
        assertEquals(Dp(22f), hudNumberWidth(HudSlots.QUEEN_SEC))
    }

    @Test
    fun historyKeepsLast180() {
        val w = world()
        val h = HudState()
        repeat(200) { h.recordSample(w) }
        assertEquals(180, h.history.size)
    }

    @Test
    fun cinemaShotQueueOrderAndSkips() {
        val c = CinemaDirector()
        assertEquals(listOf("FOLLOW", "ENTRANCE", "FOLLOW", "WIDE", "EVENT"), List(5) { c.nextKind(true, true) })
        // no events: EVENT is skipped, no ants: FOLLOW is skipped
        assertEquals("ENTRANCE", c.nextKind(false, false))
        assertEquals("WIDE", c.nextKind(false, false))
    }

    @Test
    fun cinemaPausedAfterInputDoesNotMoveCamera() {
        val w = world()
        val cam = CameraState()
        val c = CinemaDirector()
        c.notifyInput(10.0)
        c.update(w, cam, 30.0)
        assertEquals(SimConfig.width / 2.0, cam.x)
        c.update(w, cam, 36.0)
        assertTrue(c.nextPick > 36.0)
    }

    @Test
    fun userFollowMovesTowardsAntAndStopsWhenGone() {
        val cam = CameraState()
        val ant = Ant(0.0, 0.0, AntKind.WORKER).apply { location = AntPlace.WORLD; x = cam.x + 100; y = cam.y; energy = 10.0 }
        val c = CinemaDirector()
        val x0 = cam.x
        assertTrue(c.updateUserFollow(cam, ant))
        assertEquals(x0 + 12.0, cam.x, 1e-9)
        ant.location = AntPlace.NEST
        assertFalse(c.updateUserFollow(cam, ant))
    }

    @Test
    fun tunerOverridesLoadSetReset() {
        val t = TunerOverrides()
        val base = SimConfig.antSpeed
        t.load(mapOf("ov:antSpeed" to "3.5", "ov:no.such.path" to "1", "ov:antEnergyDecay" to "x", "speed" to "2"))
        assertEquals(mapOf("antSpeed" to 3.5), t.values)
        assertEquals(3.5, SimConfig.antSpeed)
        assertFalse(t.set("no.such.path", 1.0))
        t.reset()
        assertTrue(t.values.isEmpty())
        assertEquals(base, SimConfig.antSpeed)
    }

    @Test
    fun fpsCounterPublishesOncePerSecond() {
        val f = FpsCounter()
        var published = 0
        var t = 1.0
        repeat(120) { if (f.frame(t)) published++; t += 1.0 / 60.0 }
        assertEquals(1, published)
        assertTrue(f.shown in 60..62, "shown=${f.shown}")
    }

    @Test
    fun shotPosePresets() {
        val w = world()
        val cam = CameraState()
        ShotPose.apply("ant", w, cam)
        assertEquals(6.0, cam.zoom)
        assertEquals(0.45, w.timeOfDay)
        ShotPose.apply("rain", w, cam)
        assertTrue(w.raining)
        val w2 = world()
        val age = w2.age
        ShotPose.apply(null, w2, cam)
        assertEquals(age, w2.age)
    }
}
