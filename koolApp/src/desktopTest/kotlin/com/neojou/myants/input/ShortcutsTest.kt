package com.neojou.myants.input

import com.neojou.myants.AppSession
import com.neojou.myants.LaunchConfig
import com.neojou.myants.SimTool
import com.neojou.myants.sim.SimConfig
import com.neojou.myants.ui.L10n
import com.neojou.myants.ui.UiLanguage
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Key mapping only. No window and no GPU. */
class ShortcutsTest {
    private lateinit var home: File
    private var oldHome: String? = null
    private lateinit var s: AppSession

    @BeforeTest
    fun setUp() {
        oldHome = System.getProperty("user.home")
        home = Files.createTempDirectory("myants-keys").toFile()
        System.setProperty("user.home", home.absolutePath)
        s = AppSession(LaunchConfig(seed = 1, colonies = 1))
    }

    @AfterTest
    fun tearDown() {
        System.setProperty("user.home", oldHome!!)
        home.deleteRecursively()
        SimConfig.resetTunables()
        L10n.language.value = UiLanguage.EN
    }

    @Test
    fun typedCharactersMapToActionsAndBracketsStayUnbound() {
        assertEquals(Shortcut.PAUSE, shortcutFor(' '))
        assertEquals(Shortcut.STEP, shortcutFor('.'))
        assertEquals(Shortcut.FASTER, shortcutFor('+'))
        assertEquals(Shortcut.SLOWER, shortcutFor('-'))
        assertEquals(Shortcut.ZOOM_IN, shortcutFor('='))
        assertEquals(Shortcut.ZOOM_OUT, shortcutFor('_'))
        assertEquals(Shortcut.TOOL_INSPECT, shortcutFor('1'))
        assertEquals(Shortcut.TOOL_SUGAR, shortcutFor('2'))
        assertEquals(Shortcut.TOOL_PROTEIN, shortcutFor('3'))
        assertEquals(Shortcut.TOOL_ENEMY, shortcutFor('4'))
        assertNull(shortcutFor('['))
        assertNull(shortcutFor(']'))
        assertNull(shortcutFor('a'))
    }

    @Test
    fun shortcutsDriveTheSessionWithoutAWindow() {
        assertFalse(s.paused)
        val age = s.world.age
        s.applyShortcut(Shortcut.STEP)
        assertEquals(age, s.world.age)
        s.applyShortcut(Shortcut.PAUSE)
        assertTrue(s.paused)
        s.applyShortcut(Shortcut.STEP)
        assertEquals(age + 1, s.world.age)

        assertEquals("1x", s.speedLabel())
        s.applyShortcut(Shortcut.FASTER)
        assertEquals("2x", s.speedLabel())
        s.applyShortcut(Shortcut.SLOWER)
        assertEquals("1x", s.speedLabel())

        val zoom = s.zoom
        s.applyShortcut(Shortcut.ZOOM_IN)
        assertTrue(s.zoom > zoom)
        s.applyShortcut(Shortcut.ZOOM_OUT)
        assertEquals(zoom, s.zoom, 1e-9)

        s.applyShortcut(Shortcut.TOOL_SUGAR)
        assertEquals(SimTool.SUGAR, s.tool)
        s.applyShortcut(Shortcut.TOOL_PROTEIN)
        assertEquals(SimTool.PROTEIN, s.tool)
        s.applyShortcut(Shortcut.TOOL_ENEMY)
        assertEquals(SimTool.ENEMY, s.tool)
        s.applyShortcut(Shortcut.TOOL_INSPECT)
        assertEquals(SimTool.INSPECT, s.tool)
    }
}
