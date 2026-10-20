package com.neojou.myants.render3d.camera

import de.fabmax.kool.math.Vec3f
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OrthoGroundTest {
    @Test
    fun windowCenterHitsLookAtWhenTheFrustumIsSymmetric() {
        val hit = OrthoGround.hit(
            px = 100f,
            py = 50f,
            vpX = 0f,
            vpY = 0f,
            vpW = 200f,
            vpH = 100f,
            left = -100f,
            right = 100f,
            bottom = -50f,
            top = 50f,
            eye = Vec3f(700f, 500f, 420f),
            lookAt = Vec3f(700f, 0f, 420f),
            up = Vec3f(0f, 0f, -1f),
            groundY = 0f,
        )
        assertNotNull(hit)
        assertTrue(abs(hit.x - 700f) < 1e-3f, "x=${hit.x}")
        assertTrue(abs(hit.y - 420f) < 1e-3f, "z=${hit.y}")
    }

    @Test
    fun screenBottomIsPositiveZWhenLookingStraightDown() {
        val hit = OrthoGround.hit(
            px = 100f,
            py = 100f,
            vpX = 0f,
            vpY = 0f,
            vpW = 200f,
            vpH = 100f,
            left = -100f,
            right = 100f,
            bottom = -50f,
            top = 50f,
            eye = Vec3f(700f, 500f, 420f),
            lookAt = Vec3f(700f, 0f, 420f),
            up = Vec3f(0f, 0f, -1f),
            groundY = 0f,
        )
        assertNotNull(hit)
        assertTrue(hit.y > 420f, "z=${hit.y}")
    }

    @Test
    fun leftPaneCenterHitsTheFocusOfAnAsymmetricFrustum() {
        val viewW = 1000f
        val fx = 0.37f
        val focus = OrthoGround.hit(
            px = 370f,
            py = 400f,
            vpX = 0f,
            vpY = 0f,
            vpW = 1000f,
            vpH = 800f,
            left = -viewW * fx,
            right = viewW * (1f - fx),
            bottom = -400f,
            top = 400f,
            eye = Vec3f(700f, 400f, 420f),
            lookAt = Vec3f(700f, 0f, 420f),
            up = Vec3f(0f, 0f, -1f),
            groundY = 0f,
        )
        assertNotNull(focus)
        assertTrue(abs(focus.x - 700f) < 1e-2f, "focus x=${focus.x}")
        assertTrue(abs(focus.y - 420f) < 1e-2f, "focus z=${focus.y}")

        val windowCenter = OrthoGround.hit(
            px = 500f,
            py = 400f,
            vpX = 0f,
            vpY = 0f,
            vpW = 1000f,
            vpH = 800f,
            left = -viewW * fx,
            right = viewW * (1f - fx),
            bottom = -400f,
            top = 400f,
            eye = Vec3f(700f, 400f, 420f),
            lookAt = Vec3f(700f, 0f, 420f),
            up = Vec3f(0f, 0f, -1f),
            groundY = 0f,
        )
        assertNotNull(windowCenter)
        assertTrue(abs(windowCenter.x - 700f) > 50f, "window center x=${windowCenter.x}")
    }

    @Test
    fun parallelRayAndPlaneBehindTheCameraAreMisses() {
        val level = OrthoGround.hit(
            px = 50f,
            py = 50f,
            vpX = 0f,
            vpY = 0f,
            vpW = 100f,
            vpH = 100f,
            left = -10f,
            right = 10f,
            bottom = -10f,
            top = 10f,
            eye = Vec3f(0f, 5f, 0f),
            lookAt = Vec3f(10f, 5f, 0f),
            up = Vec3f(0f, 1f, 0f),
            groundY = 0f,
        )
        assertNull(level)

        val behind = OrthoGround.hit(
            px = 50f,
            py = 50f,
            vpX = 0f,
            vpY = 0f,
            vpW = 100f,
            vpH = 100f,
            left = -10f,
            right = 10f,
            bottom = -10f,
            top = 10f,
            eye = Vec3f(0f, 10f, 0f),
            lookAt = Vec3f(0f, 0f, 0f),
            up = Vec3f(0f, 0f, -1f),
            groundY = 20f,
        )
        assertNull(behind)
    }
}
