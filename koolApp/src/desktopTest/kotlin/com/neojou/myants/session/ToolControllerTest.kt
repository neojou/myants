package com.neojou.myants.session

import com.neojou.myants.SimTool
import com.neojou.myants.render3d.LeafSpot
import com.neojou.myants.sim.Ant
import com.neojou.myants.sim.AntKind
import com.neojou.myants.sim.AntPlace
import com.neojou.myants.sim.Brood
import com.neojou.myants.sim.BroodStage
import com.neojou.myants.sim.ChamberRole
import com.neojou.myants.sim.Food
import com.neojou.myants.sim.FoodKind
import com.neojou.myants.sim.GrassTuft
import com.neojou.myants.sim.InsectKind
import com.neojou.myants.sim.Obstacle
import com.neojou.myants.sim.Pheromone
import com.neojou.myants.sim.Rng
import com.neojou.myants.sim.SimConfig
import com.neojou.myants.sim.World
import com.neojou.myants.ui.InspectRow
import com.neojou.myants.ui.L10n
import com.neojou.myants.ui.UiLanguage
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ToolControllerTest {
    private lateinit var world: World
    private var px = 0.0
    private var py = 0.0

    @BeforeTest
    fun setUp() {
        L10n.language.value = UiLanguage.EN
        Rng.seed(1)
        SimConfig.colonyCount = 1
        world = World()
        for (c in world.colonies) c.ants.clear()
        world.foods.clear()
        world.insects.clear()
        val p = world.getSafePosition()
        px = p.x
        py = p.y
    }

    @Test
    fun defaultToolIsInspect() {
        assertEquals(SimTool.INSPECT, ToolController().tool)
    }

    @Test
    fun placementToolsAddToWorld() {
        val t = ToolController()
        t.tool = SimTool.SUGAR
        t.apply(world, px, py)
        t.tool = SimTool.PROTEIN
        t.apply(world, px, py)
        assertEquals(listOf(FoodKind.SUGAR, FoodKind.PROTEIN), world.foods.map { it.type })
        assertEquals(listOf(200.0, 150.0), world.foods.map { it.amount })
        t.tool = SimTool.ENEMY
        t.apply(world, px, py)
        assertEquals(InsectKind.PREDATOR, world.insects.single().type)
        assertEquals("", t.inspect.value)
    }

    @Test
    fun inspectPicksNearestWithin30() {
        world.foods.add(Food(px + 20, py, FoodKind.SUGAR, 42.0))
        val ant = Ant(px + 5, py, AntKind.WORKER).apply { location = AntPlace.WORLD; x = px + 5; y = py }
        world.colonies[0].ants.add(ant)
        val t = ToolController()
        t.apply(world, px, py)
        assertSame(ant, t.inspectedAnt)
        assertTrue(t.inspect.value.startsWith("Ant WORKER"), t.inspect.value)

        t.apply(world, px + 21, py)
        assertNull(t.inspectedAnt)
        assertTrue(t.inspectTarget() is InspectTarget.FoodItem)
        assertEquals("SUGAR 42", t.inspect.value)

        world.grass.clear()
        val (ex, ey) = emptyGround()
        t.apply(world, ex, ey)
        assertNull(t.inspectedAnt)
        assertTrue(t.inspectTarget() is InspectTarget.PheromoneSample)
        assertEquals(
            "PHEROMONE c0 home=0.00 sugar=0.00 protein=0.00 danger=0.00",
            t.inspect.value,
        )
        t.apply(world, ex, ey)
        assertNull(t.inspectTarget())
        assertEquals("", t.inspect.value)
    }

    @Test
    fun inspectIgnoresAntsInNest() {
        val ant = Ant(px, py, AntKind.WORKER).apply { location = AntPlace.NEST; x = px; y = py }
        world.colonies[0].ants.add(ant)
        val t = ToolController()
        t.inspectNearest(world, px, py)
        assertNull(t.inspectedAnt)
    }

    @Test
    fun inspectNestFindsNestAntAndQueen() {
        val colony = world.colonies[0]
        val q = colony.queen
        val ant = Ant(q.x + 100, q.y, AntKind.SOLDIER).apply { location = AntPlace.NEST; x = q.x + 100; y = q.y }
        colony.ants.add(ant)
        val t = ToolController()
        t.inspectNest(world, q.x + 100, q.y)
        assertSame(ant, t.inspectedAnt)
        assertTrue(t.inspectTarget() is InspectTarget.NestAnt)
        assertTrue(t.inspect.value.startsWith("Ant SOLDIER"))
        t.inspectNest(world, q.x, q.y)
        assertNull(t.inspectedAnt)
        assertTrue(t.inspectTarget() is InspectTarget.QueenTarget)
        assertTrue(t.inspect.value.startsWith("Queen"))
    }

    @Test
    fun inspectNestFindsBroodThenChamber() {
        val colony = world.colonies[0]
        val q = colony.queen
        colony.ants.clear()
        colony.brood.clear()
        val egg = Brood(q.x + 60, q.y).apply { stage = BroodStage.EGG; age = 3.0 }
        val larva = Brood(q.x, q.y + 70).apply { stage = BroodStage.LARVA; age = 4.0; hunger = 6.0 }
        val pupa = Brood(q.x - 60, q.y).apply {
            stage = BroodStage.PUPA
            age = 1.0
            destinedCaste = AntKind.SOLDIER
        }
        val carried = Brood(20.0, 20.0).apply {
            stage = BroodStage.EGG
            age = 1.0
            carrier = Ant(0.0, 0.0, AntKind.WORKER)
        }
        colony.brood += listOf(egg, larva, pupa, carried)
        val t = ToolController()
        t.inspectNest(world, egg.x, egg.y)
        assertEquals("EGG age=3", t.inspect.value)
        assertEquals("Egg", t.inspectPanel.value?.title)
        assertEquals(listOf(InspectRow("Age", "3")), t.inspectPanel.value?.rows)

        t.inspectNest(world, larva.x, larva.y)
        assertEquals("LARVA age=4 hunger=6", t.inspect.value)
        assertTrue(t.inspectPanel.value!!.rows.contains(InspectRow("Hunger", "6")))

        t.inspectNest(world, pupa.x, pupa.y)
        assertEquals("PUPA age=1 SOLDIER", t.inspect.value)

        // Queen stays ahead of a brood that is also inside the queen radius.
        val near = Brood(q.x + 15, q.y).apply { stage = BroodStage.EGG; age = 1.0 }
        colony.brood += near
        t.inspectNest(world, near.x, near.y)
        assertTrue(t.inspectTarget() is InspectTarget.QueenTarget)

        val room = colony.nest.addChamber(24.0, 24.0, 30.0, ChamberRole.STORAGE)
        t.inspectNest(world, carried.x, carried.y)
        assertTrue(t.inspectTarget() is InspectTarget.ChamberItem)
        assertEquals("CHAMBER STORAGE r=30", t.inspect.value)
        assertEquals(listOf(InspectRow("Role", "Storage"), InspectRow("Radius", "30")), t.inspectPanel.value?.rows)
        assertEquals(room.x, 24.0)

        L10n.language.value = UiLanguage.ZH_TW
        t.inspectNest(world, egg.x, egg.y)
        assertEquals("卵", t.inspectPanel.value?.title)
        assertEquals("卵 年齡=3", t.inspect.value)
        t.inspectNest(world, room.x, room.y)
        assertEquals("巢間", t.inspectPanel.value?.title)
        assertEquals(listOf(InspectRow("角色", "存放"), InspectRow("半徑", "30")), t.inspectPanel.value?.rows)
    }

    @Test
    fun refreshRewritesTheLineAndClearsWhenTheAntIsGone() {
        val ant = Ant(px, py, AntKind.WORKER).apply {
            location = AntPlace.WORLD
            x = px
            y = py
            energy = 11.0
            health = 7.0
        }
        world.colonies[0].ants.add(ant)
        val t = ToolController()
        t.inspectNearest(world, px, py)
        assertTrue(t.inspect.value.contains("e=11"), t.inspect.value)
        assertEquals("Ant Worker", t.inspectPanel.value?.title)
        ant.energy = 4.0
        assertTrue(t.refresh(world))
        assertTrue(t.inspect.value.contains("e=4"), t.inspect.value)
        assertTrue(t.inspectPanel.value!!.rows.contains(InspectRow("Energy", "4")))
        assertFalse(t.refresh(world))
        L10n.language.value = UiLanguage.ZH_TW
        assertTrue(t.refresh(world))
        assertEquals("螞蟻 工蟻", t.inspectPanel.value?.title)
        world.colonies[0].ants.remove(ant)
        assertTrue(t.refresh(world))
        assertEquals("", t.inspect.value)
        assertNull(t.inspectedAnt)
        assertNull(t.inspectTarget())
        assertNull(t.inspectPanel.value)
    }

    @Test
    fun clearResetsInspect() {
        val t = ToolController()
        t.inspect.value = "x"
        t.clear()
        assertEquals("", t.inspect.value)
        assertNull(t.inspectedAnt)
        assertNull(t.inspectTarget())
        assertNull(t.inspectPanel.value)
    }

    @Test
    fun inspectHitsStoneBeforeGrassLeafAndPheromone() {
        world.grass.clear()
        val t = ToolController()
        val clock = world.terrain.clockStone
        t.inspectNearest(world, clock.x, clock.y)
        assertEquals("CLOCK r=${clock.radius.toInt()}", t.inspect.value)
        assertEquals("Clock", t.inspectPanel.value?.title)

        val stone = world.terrain.obstacles.first()
        t.inspectNearest(world, stone.x, stone.y)
        assertEquals("STONE r=${stone.radius.toInt()}", t.inspect.value)

        world.foods.add(Food(clock.x, clock.y, FoodKind.SUGAR, 5.0))
        t.inspectNearest(world, clock.x, clock.y)
        assertTrue(t.inspectTarget() is InspectTarget.FoodItem)
        world.foods.clear()

        val (gx, gy) = emptyGround()
        world.grass.add(GrassTuft(gx, gy, 3.2, 0.0))
        val leaves = listOf(LeafSpot(gx.toFloat(), gy.toFloat()))
        t.inspectNearest(world, gx, gy, leaves)
        assertEquals("GRASS s=3", t.inspect.value)
        world.grass.clear()
        t.inspectNearest(world, gx, gy, leaves)
        assertEquals("LEAF", t.inspect.value)
        assertEquals("Leaf", t.inspectPanel.value?.title)
        assertFalse(t.refresh(world, leaves))
        assertTrue(t.refresh(world, emptyList()))
        assertNull(t.inspectTarget())

        val (ex, ey) = emptyGround()
        t.inspectNearest(world, ex, ey)
        assertEquals(emptyList(), t.inspectPanel.value?.rows, "zero channels are hidden")
        world.colonies[0].outdoorField.deposit(ex, ey, Pheromone.HOME, 0.5)
        assertTrue(t.refresh(world))
        assertEquals(listOf(InspectRow("Home", "0.50")), t.inspectPanel.value?.rows)
        assertTrue(t.inspect.value.contains("home=0.50"), t.inspect.value)
        t.inspectNearest(world, -1.0, ey)
        assertNull(t.inspectTarget())

        L10n.language.value = UiLanguage.ZH_TW
        t.inspectNearest(world, clock.x, clock.y)
        assertEquals("時鐘", t.inspectPanel.value?.title)
        t.inspectNearest(world, gx, gy, leaves)
        assertEquals("落葉", t.inspect.value)
    }

    @Test
    fun pheromoneSampleListsEveryColony() {
        val previous = SimConfig.colonyCount
        try {
            SimConfig.colonyCount = 2
            val both = World()
            both.grass.clear()
            both.foods.clear()
            both.insects.clear()
            for (c in both.colonies) c.ants.clear()
            val (x, y) = emptyGround(both)
            val t = ToolController()
            t.inspectNearest(both, x, y)
            assertTrue(t.inspect.value.contains("c0 "), t.inspect.value)
            assertTrue(t.inspect.value.contains("c1 "), t.inspect.value)
            assertEquals(0, t.inspectPanel.value?.rows?.size)
            both.colonies[1].outdoorField.deposit(x, y, Pheromone.DANGER, 0.25)
            t.refresh(both)
            assertEquals(listOf(InspectRow("B Danger", "0.25")), t.inspectPanel.value?.rows)
        } finally {
            SimConfig.colonyCount = previous
        }
    }

    private fun emptyGround(target: World = world): Pair<Double, Double> {
        val w = SimConfig.width.toDouble()
        val h = SimConfig.height.toDouble()
        var y = 2.0
        while (y < h - 2.0) {
            var x = 2.0
            while (x < w - 2.0) {
                if (!inStone(target, x, y) && !nearActor(target, x, y) && !nearGrass(target, x, y)) {
                    return x to y
                }
                x += 17.0
            }
            y += 17.0
        }
        error("no empty ground")
    }

    private fun inStone(target: World, x: Double, y: Double): Boolean {
        if (covers(target.terrain.clockStone, x, y)) return true
        return target.terrain.obstacles.any { covers(it, x, y) }
    }

    private fun covers(stone: Obstacle, x: Double, y: Double): Boolean {
        val dx = stone.x - x
        val dy = stone.y - y
        return dx * dx + dy * dy <= stone.radius * stone.radius
    }

    private fun nearActor(target: World, x: Double, y: Double): Boolean {
        fun near(ax: Double, ay: Double): Boolean {
            val dx = ax - x
            val dy = ay - y
            return dx * dx + dy * dy <= 40.0 * 40.0
        }
        for (colony in target.colonies) {
            for (ant in colony.ants) {
                if (ant.location == AntPlace.WORLD && near(ant.x, ant.y)) return true
            }
        }
        if (target.foods.any { near(it.x, it.y) }) return true
        return target.insects.any { near(it.x, it.y) }
    }

    private fun nearGrass(target: World, x: Double, y: Double): Boolean {
        for (tuft in target.grass) {
            val dx = tuft.x - x
            val dy = tuft.y - y
            if (dx * dx + dy * dy <= 8.0 * 8.0) return true
        }
        return false
    }
}
