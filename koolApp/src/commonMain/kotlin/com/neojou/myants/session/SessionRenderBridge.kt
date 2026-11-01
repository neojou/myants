package com.neojou.myants.session

import com.neojou.myants.AppSession
import com.neojou.myants.chooseRenderer
import com.neojou.myants.render3d.CameraView
import com.neojou.myants.render3d.InspectRing
import com.neojou.myants.render3d.RenderHost
import com.neojou.myants.render3d.RenderMode
import com.neojou.myants.render3d.RenderState
import com.neojou.myants.render3d.ViewFlags
import com.neojou.myants.sim.World
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.util.Color

/** Boundary adapter: render3d reads and writes [AppSession] only through this (#47). Pure delegation. */
class SessionRenderBridge(private val s: AppSession) : RenderState, RenderHost, ViewFlags, CameraView {
    override val world: World get() = s.world
    override val seed: Int get() = s.seed
    override val worldSerial: Int get() = s.worldSerial
    override val renderer: RenderMode get() = s.renderer
    override val renderTierBias: Int get() = s.renderTierBias
    override val fpsShown: Int get() = s.fpsShownPublic
    override val view: ViewFlags get() = this
    override val camera: CameraView get() = this
    override val inspectRing: InspectRing? get() = s.inspectRingNow
    override val animTime: Float get() = s.animTime.toFloat()

    override val showPheromones: Boolean get() = s.showPheromones
    override val pheromoneStrength: Double get() = s.pheromoneStrength
    override val antColor: Color get() = s.antColor
    override val dayNight: Boolean get() = s.dayNight
    override val bloomUser: Boolean get() = s.bloomUser
    override val noBloom: Boolean get() = s.noBloom
    override val ssaoUser: Boolean get() = s.ssaoUser
    override val shadowsUser: Boolean get() = s.shadowsUser
    override val weatherUser: Boolean get() = s.weatherUser
    override val grassDensity: Float get() = s.grassDensity

    override val x: Double get() = s.camX
    override val y: Double get() = s.camY
    override val zoom: Double get() = s.zoom
    override val pitchDeg: Float get() = s.cameraPitchDeg
    override fun fitScale(leftW: Float, winH: Float): Double = s.fitScale(leftW, winH)

    override fun onWorldSceneReady(scene: Scene) { s.worldScene = scene }
    override fun onNestPresentReady(scene: Scene) { s.nestPresent = scene }
    override fun onNestCutawayMounted() { s.nest3dActive = true }
    override fun onFrame(dt: Float) = s.tick(dt)
    override fun requestRenderer(mode: RenderMode) = s.chooseRenderer(mode)
    override fun applyRendererVisibility() = s.applyRendererVisibility()
    override fun bumpRenderTierBias() { s.renderTierBias++ }
    override fun clampCamera(leftW: Float, winH: Float) = s.clampCamera(leftW, winH)
}
