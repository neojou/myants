package com.neojou.myants

import com.neojou.myants.input.PointerRouter
import de.fabmax.kool.KoolApplication
import de.fabmax.kool.addScene
import de.fabmax.kool.pipeline.ClearColorLoad

/**
 * Runs after the UI scene. Button clicks consume the pointer during the UI update,
 * so this pass no longer treats them as world clicks.
 */
fun KoolApplication.addAntSimInput(session: AppSession) = addScene {
    clearColor = ClearColorLoad
    var shotFrames = 0
    onUpdate {
        val winW = it.ctx.window.size.x.toFloat().coerceAtLeast(1f)
        val winH = it.ctx.window.size.y.toFloat().coerceAtLeast(1f)
        PointerRouter.handle(session, winW, winH)
        shotFrames = shotFrame(session, shotFrames)
        captureFrame(session)
    }
}
