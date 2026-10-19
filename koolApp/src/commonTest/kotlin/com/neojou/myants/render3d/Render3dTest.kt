package com.neojou.myants.render3d

import com.neojou.myants.render3d.actors.PoseBuckets
import com.neojou.myants.render3d.phero.PheromoneColor
import com.neojou.myants.render3d.terrain.GroundHeight
import com.neojou.myants.render3d.terrain.GroundMesh
import de.fabmax.kool.math.MutableMat4f
import de.fabmax.kool.math.MutableVec3f
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.rad
import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PoseBucketTest {
    @Test
    fun singleBucketIsZero() {
        assertEquals(0, PoseBuckets.index(0.2f, 3f, 1))
    }

    @Test
    fun indexStaysInsideTheBucketCount() {
        val count = 9
        for (speed in floatArrayOf(0f, 0.2f, 0.8f, 2f)) {
            val index = PoseBuckets.index(0.37f, speed, count)
            assertTrue(index in 0 until count)
        }
    }

    @Test
    fun phaseStaysInUnitInterval() {
        val moving = PoseBuckets.phase(2, 6, 1.25f, true)
        val still = PoseBuckets.phase(2, 6, 1.25f, false)
        assertTrue(moving in 0f..1f)
        assertTrue(still in 0f..1f)
        assertEquals(2f / 6f, still)
    }
}

class GroundHeightTest {
    @Test
    fun sameSeedAndSerialMatch() {
        val a = GroundHeight.sampleForTest(12345, 1, 100f, 40f)
        val b = GroundHeight.sampleForTest(12345, 1, 100f, 40f)
        assertEquals(a, b)
    }

    @Test
    fun heightStaysWithinSix() {
        val h = GroundHeight.sampleForTest(7, 3, 700f, 400f)
        assertTrue(h in 0f..6f)
    }

    @Test
    fun cellWindingPointsUp() {
        val y = GroundMesh.normalY(
            0f, 0f, 0f,
            0f, 0f, 1f,
            1f, 0f, 0f,
        )
        assertTrue(y > 0f)
    }
}

class PheromoneColorTest {
    @Test
    fun faintChannelsAreClear() {
        val px = PheromoneColor.rgba(0.01, 0.01, 0.01, 0.01, 1.0)
        assertEquals(0, px[0])
        assertEquals(0, px[3])
    }

    @Test
    fun sugarLightsGreenAndRed() {
        val px = PheromoneColor.rgba(0.0, 1.0, 0.0, 0.0, 1.0)
        assertTrue(px[0] > 0)
        assertTrue(px[1] > 0)
        assertEquals(0, px[2])
        assertEquals(255, px[3])
    }
}

class WorldFrameTest {
    @Test
    fun yawSendsLocalXAlongTravel() {
        val angle = (PI / 2.0).toFloat()
        val yaw = WorldFrames.yawRad(angle)
        val mat = MutableMat4f().rotate(yaw.rad, Vec3f.Y_AXIS)
        val out = MutableVec3f(1f, 0f, 0f)
        mat.transform(out)
        val travel = WorldFrames.travel(angle)
        assertTrue(abs(out.x - travel.x) < 1e-4f)
        assertTrue(abs(out.z - travel.z) < 1e-4f)
    }

    @Test
    fun straightDownUpKeepsSimYAtTheBottom() {
        val look = WorldFrames.lookDir(-90f)
        val up = WorldFrames.screenUp(-90f)
        assertTrue(abs(look.y + 1f) < 1e-4f)
        assertEquals(0f, up.x)
        assertEquals(0f, up.y)
        assertTrue(up.z < 0f)
    }
}
