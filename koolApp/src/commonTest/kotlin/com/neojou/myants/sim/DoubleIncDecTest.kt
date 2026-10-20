package com.neojou.myants.sim

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * #31: the custom Double.inc/dec extensions were shadowed by Kotlin's built-in members and removed.
 * Queen.layTimer and other Double counters rely on the built-in `++`/`--` stepping by exactly 1.0.
 */
class DoubleIncDecTest {
    @Test
    fun builtInDoubleIncDecStepByOne() {
        var d = 1.5
        d--
        assertEquals(0.5, d)
        d++
        d++
        assertEquals(2.5, d)
    }
}
