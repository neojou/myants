package com.neojou.myants.render3d

import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.modules.ksl.blocks.cameraData
import de.fabmax.kool.pipeline.deferred.DeferredKslPbrShader
import de.fabmax.kool.scene.InstanceLayouts

/**
 * Shared chitin rim / fresnel (#84, night in #91), approach A: the deferred PBR material
 * adds `rimColor × strength × pow(1 − saturate(N·V), p)` to its emission output. There is
 * no side(N, L) base (Plan C, ART_DIRECTION §6.8): the term is zero wherever the surface faces
 * the camera, so the body interior keeps the #95 fill and only the silhouette band brightens. It
 * rides through the normal G-buffer and lighting pass. Only rimmed parts (see [RimParts]) use this
 * shader; everything else keeps the plain deferred shader. [profile] picks the strength curve
 * (ant / beetle chitin, or the night-only ladybug at 0.6×, #91).
 */
internal class RimShader(cfg: Config, private val profile: OutdoorLighting.RimProfile) : DeferredKslPbrShader(cfg) {
    /** rimColor × rimStrength, linear. */
    var rimColor: Vec3f by uniform3f(U_COLOR)
    var rimExp: Float by uniform1f(U_EXP, OutdoorLighting.RIM_EXP)
    fun apply(bright: Float) {
        val c = OutdoorLighting.rimColor(bright)
        val k = OutdoorLighting.rimStrength(bright, profile)
        rimColor = Vec3f(c.r * k, c.g * k, c.b * k)
        rimExp = OutdoorLighting.rimExp(bright)
    }

    companion object {
        private const val U_COLOR = "uRimColor"
        private const val U_EXP = "uRimExp"

        fun create(
            layout: InstanceLayouts.ModelMatColor,
            roughness: Float,
            metallic: Float,
            profile: OutdoorLighting.RimProfile = OutdoorLighting.RimProfile.CHITIN,
        ): RimShader {
            val builder = DeferredKslPbrShader.Config.Builder().apply {
                vertices { instancedModelMatrix(layout.modelMat) }
                color { instanceColor(layout.color) }
                roughness(roughness)
                metallic(metallic)
                modelCustomizer = { addRim() }
            }
            return RimShader(builder.build(), profile)
        }

        private fun KslProgram.addRim() {
            val camData = cameraData()
            val viewPos = interStageFloat3("rimViewPos")
            vertexStage {
                main {
                    val worldPos = getFloat3Port("worldPos").output
                    viewPos.input set (camData.viewMat * float4Value(worldPos, 1f.const)).xyz
                }
            }
            fragmentStage {
                val rimColor = uniformFloat3(U_COLOR)
                val rimExp = uniformFloat1(U_EXP)
                main {
                    val emission = getFloat4Port("emissionColor")
                    val base = float4Var(emission.input.input)
                    val n = float3Var(normalize(getFloat3Port("normal").output))
                    val v = float3Var(normalize(viewPos.output * (-1f).const))
                    val fres = float1Var(pow(1f.const - saturate(dot(n, v)), rimExp))
                    emission.input(float4Value(base.rgb + rimColor * fres, base.a))
                }
            }
        }
    }
}

/** Which parts get the shared rim (Artist table on #84). Legs, antennae, jaws, eyes: never. */
internal object RimParts {
    enum class AntPart { BODY, HEAD, JAW, ANTENNA, LEG, EYE }

    /** Critter batches. [APPENDAGE] holds antennae and mandibles (#91 Artist review). */
    enum class CritterPart { BODY, MARK, APPENDAGE, EYE, LEG }

    fun ant(part: AntPart): Boolean = part == AntPart.BODY || part == AntPart.HEAD

    /** Profile for an ant part: the shared chitin rim, or none. */
    fun antRim(part: AntPart): OutdoorLighting.RimProfile? = if (ant(part)) OutdoorLighting.RimProfile.CHITIN else null

    /**
     * Critter body and marks: beetle (elytra, pronotum, head) shares the chitin rim; the ladybug shell
     * (spots and pronotum included) gets the night-only 0.6× rim (#91), so its noon look is unchanged.
     * Everything else (aphid, prey, …) and all eyes / legs: no rim.
     */
    fun critter(kind: com.neojou.myants.sim.InsectKind): OutdoorLighting.RimProfile? = when (kind) {
        com.neojou.myants.sim.InsectKind.BEETLE -> OutdoorLighting.RimProfile.CHITIN
        com.neojou.myants.sim.InsectKind.LADYBUG -> OutdoorLighting.RimProfile.LADYBUG
        else -> null
    }

    /** Profile for one critter batch: body and marks follow [critter]; appendages, eyes and legs never rim. */
    fun critterRim(kind: com.neojou.myants.sim.InsectKind, part: CritterPart): OutdoorLighting.RimProfile? =
        if (part == CritterPart.BODY || part == CritterPart.MARK) critter(kind) else null
}
