package com.neojou.myants.render3d

/** Colon is lit on even local seconds and dark on odd seconds. */
internal fun clockColonLit(second: Int): Boolean = second % 2 == 0
