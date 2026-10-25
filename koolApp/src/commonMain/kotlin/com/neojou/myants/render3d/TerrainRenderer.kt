package com.neojou.myants.render3d

import com.neojou.myants.render3d.phero.PheromoneField
import com.neojou.myants.render3d.quality.QualityPreset
import com.neojou.myants.render3d.terrain.GroundHeight
import com.neojou.myants.render3d.terrain.GroundMesh
import com.neojou.myants.sim.SimConfig
import de.fabmax.kool.math.MutableVec3f
import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.modules.ksl.blocks.ColorBlockConfig
import de.fabmax.kool.pipeline.BlendMode
import de.fabmax.kool.pipeline.deferred.deferredKslPbrShader
import de.fabmax.kool.pipeline.ibl.SkyCubeIblSystem
import de.fabmax.kool.scene.Light
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.Node
import de.fabmax.kool.scene.VertexLayouts
import de.fabmax.kool.scene.addMesh
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.Time
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Ground mesh, pheromone emission, GroundHeight rebuild and sun / IBL (#47). */
internal class TerrainRenderer : SubRenderer {
    private val phero = PheromoneField("phero3d")
    private var groundShader: de.fabmax.kool.pipeline.deferred.DeferredKslPbrShader? = null
    private lateinit var ground: Mesh<*>
    private var serial = -1
    private var iblStamp = -1.0

    /** Set by [WorldRenderer] when the deferred pipeline mounts. */
    var sun: Light.Directional? = null
    var sky: SkyCubeIblSystem? = null

    override fun build(opaque: Node, forward: Node) {
        val gshader = deferredKslPbrShader {
            color {
                vertexColor()
                uniformColor(Color.WHITE, blendMode = ColorBlockConfig.BlendMode.Multiply)
            }
            emission {
                textureColor(gamma = 1f, blendMode = ColorBlockConfig.BlendMode.Set)
                constColor(Color.WHITE, blendMode = ColorBlockConfig.BlendMode.Multiply)
            }
            roughness(0.92f)
            metallic(0f)
        }
        groundShader = gshader
        ground = opaque.addMesh(VertexLayouts.PositionNormalTexCoordColor) {
            isFrustumChecked = false
            shader = gshader
        }
    }

    override fun update(state: RenderState, preset: QualityPreset, dt: Float) {
        if (serial != state.worldSerial) {
            serial = state.worldSerial
            GroundHeight.rebuild(state.seed, serial)
            // PropsRenderer 的 scatterKey 含 worldSerial，換世界時自動失效，故不需手動清除。
            rebuildGround()
        }
        updateSun(state)
        updatePheromone(state)
    }

    private fun rebuildGround() {
        val cols = 71
        val rows = 43
        val dx = SimConfig.width.toFloat() / (cols - 1)
        val dz = SimConfig.height.toFloat() / (rows - 1)
        ground.generate {
            for (iz in 0 until rows) {
                for (ix in 0 until cols) {
                    val x = ix * dx
                    val z = iz * dz
                    val y = GroundHeight.at(x, z)
                    val hL = GroundHeight.at(x - dx, z)
                    val hR = GroundHeight.at(x + dx, z)
                    val hD = GroundHeight.at(x, z - dz)
                    val hU = GroundHeight.at(x, z + dz)
                    val n = MutableVec3f(hL - hR, dx * 2f, hD - hU)
                    if (n.length() < 1e-4f) n.set(Vec3f.Y_AXIS) else n.norm()
                    color = soilColor(ix, iz)
                    vertex(Vec3f(x, y, z), n, Vec2f(x / SimConfig.width, z / SimConfig.height))
                }
            }
            for (iz in 0 until rows - 1) {
                for (ix in 0 until cols - 1) {
                    GroundMesh.emitCell(ix, iz, cols) { a, b, c -> addTriIndices(a, b, c) }
                }
            }
        }
    }

    private fun updateSun(state: RenderState) {
        val light = sun ?: return
        val world = state.world
        val bright = if (state.view.dayNight) world.dayBrightness().toFloat() else 1f
        val elev = if (state.view.dayNight) 6f + bright * 64f else 70f
        val az = (world.timeOfDay * 160.0 - 80.0).toFloat()
        val elevRad = elev * (PI.toFloat() / 180f)
        val azRad = az * (PI.toFloat() / 180f)
        val dir = Vec3f(
            sin(azRad) * cos(elevRad),
            -sin(elevRad),
            cos(azRad) * cos(elevRad),
        )
        val night = bright < 0.2f
        val color = if (night) WorldPalette.moonLight else WorldPalette.sunLight
        val intensity = if (night) 0.35f else 0.4f + bright * 3.2f
        light.setup(dir).setColor(color, intensity)
        val cube = sky
        if (cube != null) {
            cube.skyPass.elevation = elev
            cube.skyPass.azimuth = az + 180f
            val now = Time.gameTime
            if (iblStamp < 0.0 || now - iblStamp > 2.0) {
                iblStamp = now
                cube.updateIblMaps()
            }
        }
    }

    private fun updatePheromone(state: RenderState) {
        val shader = groundShader ?: return
        val show = state.view.showPheromones && state.view.pheromoneStrength > 0.0
        val tex = phero.update(state.world.colonies[0].outdoorField, state.view.pheromoneStrength, show)
        if (tex != null) shader.emissionMap = tex
        val bright = if (state.view.dayNight) state.world.dayBrightness().toFloat() else 1f
        val boost = if (!show) 0f else 1.6f + (1f - bright) * 2.4f
        shader.emission = WorldPalette.emission(boost)
        val wet = state.world.raining && state.view.weatherUser
        val k = 0.28f + 0.72f * bright
        shader.color = WorldPalette.groundTint(k, wet)
        shader.roughness = if (wet) 0.28f else 0.92f
    }
}
