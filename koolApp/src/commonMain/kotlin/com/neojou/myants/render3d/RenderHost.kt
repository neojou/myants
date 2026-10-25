package com.neojou.myants.render3d

import de.fabmax.kool.scene.Scene

/**
 * Every write render3d makes outside itself (#47). One method per old write site;
 * call sites stay where the old writes were, so the order does not change.
 */
interface RenderHost {
    fun onWorldSceneReady(scene: Scene)
    fun onNestPresentReady(scene: Scene)
    fun onNestCutawayMounted()
    /** Advances the simulation one frame. */
    fun onFrame(dt: Float)
    fun requestRenderer(mode: RenderMode)
    fun applyRendererVisibility()
    fun bumpRenderTierBias()
    fun clampCamera(leftW: Float, winH: Float)
}
