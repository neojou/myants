package com.neojou.myants.qe

import com.neojou.myants.AppSession
import com.neojou.myants.LaunchConfig
import com.neojou.myants.SimTool
import com.neojou.myants.sim.AntPlace
import com.neojou.myants.sim.FoodKind
import com.neojou.myants.sim.Rng
import com.neojou.myants.sim.SimConfig
import com.neojou.myants.sim.World
import com.neojou.myants.session.ToolController
import com.neojou.myants.ui.L10n
import com.neojou.myants.ui.UiLanguage
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import com.neojou.myants.inspectNest
import com.neojou.myants.toggleUserFollow

/**
 * QE acceptance for M4 Inspect (#12 / #49). Black-box: only the public session/tool
 * entry points a click goes through, and observable results (HUD line, sim state, camera).
 */
class InspectAcceptanceTest {
    private lateinit var home: File
    private var oldHome: String? = null

    @BeforeTest
    fun setUp() {
        oldHome = System.getProperty("user.home")
        home = Files.createTempDirectory("myants-qe49").toFile()
        System.setProperty("user.home", home.absolutePath)
    }

    @AfterTest
    fun tearDown() {
        System.setProperty("user.home", oldHome!!)
        home.deleteRecursively()
        SimConfig.resetTunables()
        L10n.language.value = UiLanguage.EN
    }

    /** Everything the sim owns that an Inspect bug could plausibly touch, plus the RNG stream. */
    private fun fingerprint(w: World): String = buildString {
        append("age=").append(w.age).append(" rng=").append(Rng.getState()).append('\n')
        for (c in w.colonies) {
            append("q=").append(c.queen.energy).append(',').append(c.queen.age).append(',').append(c.queen.x).append(',').append(c.queen.y).append('\n')
            for (a in c.ants) append("a=").append(a.x).append(',').append(a.y).append(',').append(a.energy).append(',').append(a.location).append('\n')
            for (b in c.brood) append("b=").append(b.x).append(',').append(b.y).append(',').append(b.carrier != null).append('\n')
            append("ph=").append(c.outdoorField.toSugar.contentHashCode()).append(',').append(c.outdoorField.toHome.contentHashCode())
                .append(',').append(c.nestGrid.toHome.contentHashCode()).append('\n')
        }
        for (f in w.foods) append("f=").append(f.x).append(',').append(f.y).append(',').append(f.amount).append('\n')
        for (i in w.insects) append("i=").append(i.x).append(',').append(i.y).append(',').append(i.health).append('\n')
    }

    private fun run(seed: Int, steps: Int, inspectEveryStep: Boolean): String {
        Rng.seed(seed)
        val w = World()
        val tools = ToolController()
        repeat(steps) { step ->
            if (inspectEveryStep) {
                // Click on every kind of target: actors, ground/pheromone, nest, then refresh.
                val ant = w.colonies[0].ants.firstOrNull { it.location == AntPlace.WORLD }
                if (ant != null) tools.apply(w, ant.x, ant.y)
                w.foods.firstOrNull()?.let { tools.apply(w, it.x, it.y) }
                tools.apply(w, (step * 37 % SimConfig.width).toDouble(), (step * 53 % SimConfig.height).toDouble())
                val q = w.colonies[0].queen
                tools.inspectNest(w, q.x, q.y)
                tools.inspectNest(w, q.x + 25.0, q.y + 5.0)
                w.colonies[0].brood.firstOrNull()?.let { tools.inspectNest(w, it.x, it.y) }
                tools.refresh(w)
            }
            w.update()
        }
        return fingerprint(w)
    }

    @Test
    fun inspectNeverWritesBackToTheSim() {
        for (seed in intArrayOf(1, 42)) {
            val plain = run(seed, 1500, inspectEveryStep = false)
            val inspected = run(seed, 1500, inspectEveryStep = true)
            assertEquals(plain, inspected, "seed $seed: Inspect changed the simulation")
        }
    }

    @Test
    fun hudLineRefreshesOncePerSimSecondNotEveryTick() {
        val s = AppSession(LaunchConfig(seed = 3, colonies = 1))
        s.paused = true
        s.cinema = false
        val p = s.world.getSafePosition()
        s.world.placeFood(p.x, p.y, FoodKind.SUGAR)
        val food = s.world.foods.last()
        s.tool = SimTool.INSPECT
        s.clickWorld(food.x, food.y)
        val first = s.inspect.value
        assertTrue(first.isNotEmpty(), "click on food shows a line")
        // advance to just past a 60-tick boundary so a full second is ahead
        while (s.world.age % 60 != 1) s.stepOnce()
        food.amount = 77.0
        var ticks = 0
        while (s.world.age % 60 != 0) {
            assertEquals(first, s.inspect.value, "line must not change mid-second (tick $ticks)")
            s.stepOnce(); ticks++
        }
        assertTrue(ticks in 55..60)
        assertNotEquals(first, s.inspect.value, "line updates at the sim-second boundary")
        assertTrue(s.inspect.value.contains("77"), s.inspect.value)
    }

    @Test
    fun nestPickDoesNotMoveTheWorldCamera() {
        val s = AppSession(LaunchConfig(seed = 3, colonies = 1))
        s.paused = true
        s.cinema = false
        s.camera.x = 300.0; s.camera.y = 200.0; s.camera.zoom = 2.0
        val q = s.world.colonies[0].queen
        s.inspectNest(q.x, q.y)
        assertTrue(s.inspect.value.isNotEmpty(), "queen pick shows a line")
        s.toggleUserFollow()
        repeat(5) { s.tick(0.016f) }
        assertEquals(300.0, s.camera.x, 1e-9)
        assertEquals(200.0, s.camera.y, 1e-9)
        assertEquals(2.0, s.camera.zoom, 1e-9)
    }
}
