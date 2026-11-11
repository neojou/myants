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
 * the camera, so the body interior keeps the #95 fill and only the silhouette band brightens. It so it rides through the normal G-buffer and lighting pass. Only rimmed parts
 * (see [RimParts]) use this shader; everything else keeps the plain deferred shader.
 */
internal class RimShader(cfg: Config) : DeferredKslPbrShader(cfg) {
    /** rimColor × rimStrength, linear. */
    var rimColor: Vec3f by uniform3f(U_COLOR)
    var rimExp: Float by uniform1f(U_EXP, OutdoorLighting.RIM_EXP)
    fun apply(bright: Float) {
        val c = OutdoorLighting.rimColor(bright)
        val k = OutdoorLighting.rimStrength(bright)
        rimColor = Vec3f(c.r * k, c.g * k, c.b * k)
        rimExp = OutdoorLighting.rimExp(bright)
    }

    companion object {
        private const val U_COLOR = "uRimColor"
        private const val U_EXP = "uRimExp"

        fun create(layout: InstanceLayouts.ModelMatColor, roughness: Float, metallic: Float): RimShader {
            val builder = DeferredKslPbrShader.Config.Builder().apply {
                vertices { instancedModelMatrix(layout.modelMat) }
                color { instanceColor(layout.color) }
                roughness(roughness)
                metallic(metallic)
                modelCustomizer = { addRim() }
            }
            return RimShader(builder.build())
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
    enum class AntPart { BODY, HEAD, JAW, LEG, EYE }

    fun ant(part: AntPart): Boolean = part == AntPart.BODY || part == AntPart.HEAD

    /** Beetle body (elytra, pronotum, head) and marks; the ladybug keeps its glossy shell (#91 handles night). */
    fun critterBody(kind: com.neojou.myants.sim.InsectKind): Boolean = kind == com.neojou.myants.sim.InsectKind.BEETLE

    /** TDD stub (#91). */
    fun critter(kind: com.neojou.myants.sim.InsectKind): OutdoorLighting.RimProfile? =
        if (critterBody(kind)) OutdoorLighting.RimProfile.CHITIN else null
}
