package com.neojou.myants.render3d

import de.fabmax.kool.util.Color

/**
 * Outdoor light targets from the Artist spec in #68. Pure numbers, so tests can
 * pin them without a GPU. [bright] is World.dayBrightness() (1 when day/night is off).
 */
internal object OutdoorLighting {
    /** Below this the moon replaces the sun. */
    const val NIGHT_BELOW = 0.2f

    /** About 5000 K. Noon may not be bluer than this. */
    val sunColor = Color(1f, 0.93f, 0.82f, 1f)

    /** About 7500 K: cool, not saturated blue. */
    val moonColor = Color(0.62f, 0.66f, 0.78f, 1f)
    const val MOON_INTENSITY = 0.7f

    /** Ambient (sky) light gradient with B/R <= 1.10 (zenith red raised from the suggested 0.62 so it passes); replaces the blue physical-sky irradiance. */
    val ambientZenith = Color(0.66f, 0.68f, 0.72f)
    val ambientHorizon = Color(0.80f, 0.78f, 0.74f)
    val ambientGround = Color(0.55f, 0.45f, 0.35f)

    const val AMBIENT_DAY = 0.40f
    const val AMBIENT_NIGHT = 0.22f

    /** kool 0.19.0's deferred output quad is fixed to ACES (approximated) at exposure 1.0; same day and night. */
    const val EXPOSURE = 1.0f

    const val BLOOM_STRENGTH = 0.35f
    const val BLOOM_THRESHOLD = 1.2f
    const val BLOOM_RADIUS = 2f


    fun isNight(bright: Float): Boolean = bright < NIGHT_BELOW

    /** Elevation in degrees: 10 deg at dawn, 58 deg at noon, so stones and leaves keep side shadows. */
    fun sunElevation(bright: Float): Float = 10f + bright.coerceIn(0f, 1f) * 48f

    /** 1.0 at dawn, 3.2 at noon. */
    fun sunIntensity(bright: Float): Float = 1.0f + bright.coerceIn(0f, 1f) * 2.2f

    fun lightColor(bright: Float): Color = if (isNight(bright)) moonColor else sunColor

    fun lightIntensity(bright: Float): Float = if (isNight(bright)) MOON_INTENSITY else sunIntensity(bright)

    /** Scalar ambient weight: night value under the moon, then up to the day value at noon. */
    fun ambientStrength(bright: Float): Float {
        if (isNight(bright)) return AMBIENT_NIGHT
        val t = ((bright - NIGHT_BELOW) / (1f - NIGHT_BELOW)).coerceIn(0f, 1f)
        return AMBIENT_NIGHT + (AMBIENT_DAY - AMBIENT_NIGHT) * t
    }
}
