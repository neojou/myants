package com.neojou.myants.render3d

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClockFaceTest {
    @Test
    fun colonIsLitOnEvenSeconds() {
        assertTrue(clockColonLit(0))
        assertFalse(clockColonLit(1))
        assertTrue(clockColonLit(2))
        assertFalse(clockColonLit(59))
    }
}
