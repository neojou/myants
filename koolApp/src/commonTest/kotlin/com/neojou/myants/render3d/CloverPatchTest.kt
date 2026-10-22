package com.neojou.myants.render3d

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CloverPatchTest {
    @Test
    fun theSamePatchAlwaysBloomsOrNot() {
        assertEquals(cloverBlooms(12.5f, 40f), cloverBlooms(12.5f, 40f))
        assertEquals(cloverBlooms(3f, 8f), cloverBlooms(3f, 8f))
    }

    @Test
    fun somePatchesBloomAndSomeDoNot() {
        var blooms = 0
        val samples = 400
        for (i in 0 until samples) {
            if (cloverBlooms(i * 3.7f, i * 1.9f + 4f)) blooms++
        }
        val share = blooms.toFloat() / samples
        assertTrue(share > 0.25f, "share=$share")
        assertTrue(share < 0.55f, "share=$share")
    }
}
