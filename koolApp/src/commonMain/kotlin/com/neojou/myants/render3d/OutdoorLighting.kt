package com.neojou.myants.render3d

import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.util.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

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

    /** Night sky gradient suggested by the Artist on #74: cool moonlit, not saturated blue. */
    val nightZenith = Color(0.30f, 0.36f, 0.50f)
    val nightHorizon = Color(0.34f, 0.38f, 0.48f)

    /**
     * The irradiance map is baked once from the day gradient, so night is a per-channel
     * tint on the ambient factor: night horizon / day horizon, normalised to luma 1.
     * Day gradient x tint is roughly the night gradient.
     */
    val nightTint: Color = run {
        val r = nightHorizon.r / ambientHorizon.r
        val g = nightHorizon.g / ambientHorizon.g
        val b = nightHorizon.b / ambientHorizon.b
        val l = 0.2126f * r + 0.7152f * g + 0.0722f * b
        Color(r / l, g / l, b / l, 1f)
    }

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

    /** Ambient factor: cool tinted night, blending to the neutral day factor as the sun rises. */
    fun ambientColor(bright: Float): Color {
        val k = ambientStrength(bright)
        val t = if (isNight(bright)) 0f else ((bright - NIGHT_BELOW) / (1f - NIGHT_BELOW)).coerceIn(0f, 1f)
        fun ch(n: Float) = k * (n + (1f - n) * t)
        return Color(ch(nightTint.r), ch(nightTint.g), ch(nightTint.b), 1f)
    }

    /** Sun / moon direction (pointing down from the light); shared by the light and the rim (#84). */
    fun lightDirection(bright: Float, timeOfDay: Double): Vec3f {
        val elevRad = sunElevation(bright) * (PI.toFloat() / 180f)
        val azRad = (timeOfDay * 160.0 - 80.0).toFloat() * (PI.toFloat() / 180f)
        return Vec3f(sin(azRad) * cos(elevRad), -sin(elevRad), cos(azRad) * cos(elevRad))
    }

    // ---- Shared chitin rim / fresnel (#84 day, #91 night) ----

    /** Day rim strength (linear emission), Artist range 0.06–0.15. */
    const val RIM_DAY = 0.10f

    /** Night rim strength; 0 until #91 sets it. */
    const val RIM_NIGHT = 0.0f

    /** Fresnel exponent p, Artist range 3–5 for day (narrow edge). */
    const val RIM_EXP = 4.0f

    /** side(N, L) = RIM_SIDE_BASE + (1 - RIM_SIDE_BASE) * (1 - saturate(N·L)). */
    const val RIM_SIDE_BASE = 0.35f

    /** Rim colour: warm sun by day, moon at night. */
    fun rimColor(bright: Float): Color = lightColor(bright)

    /** Night value under the moon, then linear to [RIM_DAY] with the same t as [ambientStrength]. */
    fun rimStrength(bright: Float): Float {
        if (isNight(bright)) return RIM_NIGHT
        val t = ((bright - NIGHT_BELOW) / (1f - NIGHT_BELOW)).coerceIn(0f, 1f)
        return RIM_NIGHT + (RIM_DAY - RIM_NIGHT) * t
    }

    /** #91 may interpolate a night exponent here; day is fixed. */
    @Suppress("UNUSED_PARAMETER")
    fun rimExp(bright: Float): Float = RIM_EXP

    /** CPU mirror of the shader term (per channel factor before rimColor), for tests. */
    fun rimFactor(bright: Float, nDotV: Float, nDotL: Float): Float {
        val f = (1f - nDotV.coerceIn(0f, 1f)).pow(rimExp(bright))
        val side = RIM_SIDE_BASE + (1f - RIM_SIDE_BASE) * (1f - nDotL.coerceIn(0f, 1f))
        return rimStrength(bright) * f * side
    }
}
