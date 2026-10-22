package com.neojou.myants.session

import com.neojou.myants.SimTool
import com.neojou.myants.sim.Ant
import com.neojou.myants.sim.AntKind
import com.neojou.myants.sim.AntPlace
import com.neojou.myants.sim.Brood
import com.neojou.myants.sim.BroodStage
import com.neojou.myants.sim.ChamberRole
import com.neojou.myants.sim.Food
import com.neojou.myants.sim.FoodKind
import com.neojou.myants.sim.InsectKind
import com.neojou.myants.sim.Rng
import com.neojou.myants.sim.SimConfig
import com.neojou.myants.sim.World
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

        t.apply(world, px + 200, py + 200)
        assertNull(t.inspectedAnt)
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
        assertEquals("EGG", t.inspectPanel.value?.title)
        assertEquals(listOf("age=3"), t.inspectPanel.value?.rows)

        t.inspectNest(world, larva.x, larva.y)
        assertEquals("LARVA age=4 hunger=6", t.inspect.value)
        assertTrue(t.inspectPanel.value!!.rows.contains("hunger=6"))

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
        assertEquals(listOf("STORAGE", "r=30"), t.inspectPanel.value?.rows)
        assertEquals(room.x, 24.0)

        L10n.language.value = UiLanguage.ZH_TW
        t.inspectNest(world, egg.x, egg.y)
        assertEquals("卵", t.inspectPanel.value?.title)
        assertEquals("卵 年齡=3", t.inspect.value)
        t.inspectNest(world, room.x, room.y)
        assertEquals("巢間", t.inspectPanel.value?.title)
        assertEquals(listOf("存放", "半徑=30"), t.inspectPanel.value?.rows)
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
        assertEquals("Ant WORKER", t.inspectPanel.value?.title)
        ant.energy = 4.0
        assertTrue(t.refresh(world))
        assertTrue(t.inspect.value.contains("e=4"), t.inspect.value)
        assertTrue(t.inspectPanel.value!!.rows.any { it.contains("e=4") })
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
}
