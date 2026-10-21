package com.neojou.myants

import com.neojou.myants.classic2d.addAntSimScene
import com.neojou.myants.classic2d.addLegacyNestOverlay
import com.neojou.myants.render3d.RenderMode
import com.neojou.myants.render3d.addWorldScene
import com.neojou.myants.render3d.nest.addNestPresent
import com.neojou.myants.render3d.saveScreenshotPng
import com.neojou.tools.LogLevel
import com.neojou.tools.MyLog
import com.neojou.tools.SystemSettings
import de.fabmax.kool.KoolApplication
import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.modules.ksl.KslUnlitShader
import de.fabmax.kool.pipeline.AttachmentConfig
import de.fabmax.kool.pipeline.ClearColorFill
import de.fabmax.kool.pipeline.CullMethod
import de.fabmax.kool.pipeline.DepthCompareOp
import de.fabmax.kool.pipeline.FrameCopy
import de.fabmax.kool.pipeline.OffscreenPass2d
import de.fabmax.kool.pipeline.TexFormat
import de.fabmax.kool.scene.Node
import de.fabmax.kool.scene.OrthographicCamera
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.scene.VertexLayouts
import de.fabmax.kool.scene.addMesh
import de.fabmax.kool.util.BackendScope
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.Viewport
import kotlinx.coroutines.launch

private const val TAG = "APP"

/** Shared app body. Desktop and the browser both call this. */
fun KoolApplication.myantsApp(launch: LaunchConfig = LaunchConfig()) {
    SystemSettings.initOnce()
    MyLog.add(TAG, "System Ready", LogLevel.DEBUG)
    val session = AppSession(launch)
    addAntSimScene(session)
    addWorldScene(session)
    addNestPresent(session)
    addLegacyNestOverlay(session)
    addAntSimUi(session)
    addAntSimInput(session)
    session.applyRendererVisibility()
}

/** Window and tab title, for example `myants 0.13.0`. */
val appTitle: String get() = "${AppVersion.APP_NAME} ${AppVersion.NAME}"

internal fun Scene.captureFrame(session: AppSession) {
    // The screen-copy image Vulkan builds is sampled, not transferable, so
    // Texture.download rejects it. The grab pass below blits that image into
    // an offscreen color target and registers a view copy, which makes kool
    // recreate the target with TRANSFER_SRC. Download that target one frame later.
    if (session.screenshotRequested && ShotGrab.phase == 0) {
        session.screenshotRequested = false
        ShotGrab.start(this, session)
    }
    ShotGrab.advance(this)
}

private object ShotGrab {
    var phase = 0
    private var path = ""
    private var copy: FrameCopy? = null
    private var copy2: FrameCopy? = null
    private var pass: OffscreenPass2d? = null
    private var shader: KslUnlitShader? = null
    private var armed = false
    private var keepCopy = false
    private var expectW = 0
    private var expectH = 0
    private var spins = 0
    private var settle = 0
    private var retries = 0

    fun start(scene: Scene, session: AppSession) {
        path = session.shotOut ?: "antsim_seed${session.seed}_t${session.world.age}.png"
        copy?.release()
        copy2?.release()
        copy2 = null
        copy = scene.mainRenderPass.defaultView.copyOutput(
            isCopyColor = true,
            isCopyDepth = false,
            drawGroupId = Int.MAX_VALUE,
            isSingleShot = true,
        )
        if (pass == null) {
            val sh = KslUnlitShader {
                color { textureColor() }
                pipeline {
                    cullMethod = CullMethod.NO_CULLING
                    depthTest = DepthCompareOp.ALWAYS
                    isWriteDepth = false
                }
            }
            shader = sh
            val node = Node("shot-grab")
            node.addMesh(VertexLayouts.PositionTexCoord) {
                isFrustumChecked = false
                generate { rect { size.set(2f, 2f) } }
                shader = sh
            }
            val grab = OffscreenPass2d(
                node,
                AttachmentConfig.singleColorDefaultDepth(
                    TexFormat.RGBA,
                    ClearColorFill(Color.BLACK),
                ),
                Vec2i(64, 64),
                "shot-grab",
                numSamples = 1,
            )
            val cam = OrthographicCamera()
            cam.isKeepAspectRatio = false
            cam.isClipToViewport = false
            cam.left = -1f
            cam.right = 1f
            cam.bottom = -1f
            cam.top = 1f
            cam.clipNear = 0.1f
            cam.clipFar = 10f
            cam.position.set(0f, 0f, 2f)
            cam.lookAt.set(0f, 0f, 0f)
            grab.camera = cam
            grab.isEnabled = false
            grab.onUpdate {
                // A single-shot copy clears isCopySource on the next frame, and Vulkan
                // then rebuilds the color image without TRANSFER_SRC before download runs.
                if (phase >= 2) {
                    if (!keepCopy) {
                        grab.defaultView.copyOutput(
                            isCopyColor = true,
                            isCopyDepth = false,
                            drawGroupId = Int.MAX_VALUE,
                            isSingleShot = false,
                        )
                        keepCopy = true
                    }
                    armed = true
                }
            }
            scene.addOffscreenPass(grab)
            pass = grab
        }
        pass?.isEnabled = false
        armed = false
        spins = 0
        settle = 0
        retries = 0
        phase = 1
    }

    fun advance(scene: Scene) {
        val grab = pass ?: return
        val frame = copy ?: return
        when (phase) {
            1 -> {
                val first = frame.colorCopy2d
                if (!first.isLoaded || first.width <= 0 || first.height <= 0) return
                val second = copy2 ?: scene.mainRenderPass.defaultView.copyOutput(
                    isCopyColor = true,
                    isCopyDepth = false,
                    drawGroupId = Int.MAX_VALUE,
                    isSingleShot = true,
                ).also { copy2 = it }
                val tex = second.colorCopy2d
                if (!tex.isLoaded || tex.width <= 0 || tex.height <= 0) return
                shader?.colorMap = tex
                expectW = tex.width
                expectH = tex.height
                grab.setSize(tex.width, tex.height)
                grab.viewport = Viewport(0, 0, tex.width, tex.height)
                grab.isEnabled = true
                phase = 2
            }
            2 -> if (armed) {
                armed = false
                spins = 0
                settle = 0
                phase = 3
            }
            3 -> {
                val tex = grab.colorTexture
                val ready = tex != null && tex.isLoaded && tex.width == expectW && tex.height == expectH
                if (!ready) {
                    spins++
                    if (spins > 45) retryCopy(scene, grab, "color target ${tex?.width}x${tex?.height} never reached $expectW x $expectH")
                    return
                }
                // Width updates when the backend draw resizes the target. That same draw
                // is what adds TRANSFER_SRC. Two more updates, then the backend task below.
                settle++
                if (settle < 2) return
                phase = 4
                grab.isEnabled = false
                val held = frame
                val held2 = copy2
                copy = null
                copy2 = null
                // asyncSceneUpdate runs the frontend beside the backend. Queuing the
                // readback here let renderFrame clear it before the copy, and await never
                // resumed. Backend tasks run on the render thread immediately before
                // renderFrame, so the readback is still in the list when it is copied.
                BackendScope.launch {
                    try {
                        MyLog.add(TAG, "shot readback $expectW x $expectH", LogLevel.DEBUG)
                        val image = tex.download()
                        val ok = saveScreenshotPng(image, path)
                        if (ok) {
                            MyLog.add(TAG, "screenshot $path", LogLevel.DEBUG)
                        } else {
                            MyLog.add(TAG, "PNG encoder is not available in this build", LogLevel.DEBUG)
                        }
                    } catch (err: Throwable) {
                        MyLog.add(TAG, "PNG encoder is not available in this build (${err.message})", LogLevel.DEBUG)
                    } finally {
                        held.release()
                        held2?.release()
                    }
                }
            }
        }
    }

    private fun retryCopy(scene: Scene, grab: OffscreenPass2d, why: String) {
        retries++
        grab.isEnabled = false
        armed = false
        spins = 0
        settle = 0
        copy?.release()
        copy2?.release()
        copy = null
        copy2 = null
        if (retries > 3) {
            phase = 0
            MyLog.add(TAG, "PNG encoder is not available in this build ($why)", LogLevel.DEBUG)
            return
        }
        MyLog.add(TAG, "shot retry $retries ($why)", LogLevel.DEBUG)
        copy = scene.mainRenderPass.defaultView.copyOutput(
            isCopyColor = true,
            isCopyDepth = false,
            drawGroupId = Int.MAX_VALUE,
            isSingleShot = true,
        )
        phase = 1
    }
}

internal fun shotFrame(session: AppSession, frames: Int): Int {
    val next = frames + 1
    if (session.shot != null && next == 90) session.requestScreenshot()
    return next
}