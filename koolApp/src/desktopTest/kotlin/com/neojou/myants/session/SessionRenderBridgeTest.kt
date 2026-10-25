package com.neojou.myants.session

import com.neojou.myants.AppSession
import com.neojou.myants.LaunchConfig
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** The bridge is pure delegation: reads are live and writes land on the session (#47). */
class SessionRenderBridgeTest {
    private lateinit var home: File
    private var oldHome: String? = null
    private lateinit var s: AppSession

    @BeforeTest
    fun setUp() {
        oldHome = System.getProperty("user.home")
        home = Files.createTempDirectory("myants-home").toFile()
        System.setProperty("user.home", home.absolutePath)
        s = AppSession(LaunchConfig(seed = 1, colonies = 1))
    }

    @AfterTest
    fun tearDown() {
        oldHome?.let { System.setProperty("user.home", it) }
        home.deleteRecursively()
    }

    @Test
    fun readsAreLive() {
        val b = s.renderBridge
        assertSame(s.world, b.world)
        assertEquals(s.seed, b.seed)
        s.camX = 123.0
        s.camY = 45.0
        assertEquals(123.0, b.camera.x)
        assertEquals(45.0, b.camera.y)
        assertEquals(s.zoom, b.camera.zoom)
        assertEquals(s.grassDensity, b.view.grassDensity)
        assertEquals(s.antColor, b.view.antColor)
    }

    @Test
    fun writesLandOnSession() {
        val b = s.renderBridge
        val before = s.renderTierBias
        b.bumpRenderTierBias()
        assertEquals(before + 1, s.renderTierBias)
        assertEquals(s.renderTierBias, b.renderTierBias)
        b.onNestCutawayMounted()
        assertTrue(s.nest3dActive)
    }
}
