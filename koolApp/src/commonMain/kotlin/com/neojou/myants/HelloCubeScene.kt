package com.neojou.myants

import de.fabmax.kool.KoolApplication
import de.fabmax.kool.addScene
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.deg
import de.fabmax.kool.modules.ksl.KslPbrShader
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.scene.addColorMesh
import de.fabmax.kool.scene.defaultOrbitCamera
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.Time

/**
 * kool README 的 Hello World 場景：旋轉的彩色立方體。
 *
 * 內容與 README 的 `addScene { ... }` 區塊逐行相同，只是包成函式，
 * 讓 desktop 與 js 的 main 共用。
 */
fun KoolApplication.addHelloCubeScene(): Scene = addScene {
    defaultOrbitCamera()
    addColorMesh {
        generate { cube { colored() } }
        shader = KslPbrShader {
            color { vertexColor() }
            metallic(0f)
            roughness(0.25f)
        }
        onUpdate { transform.rotate(45f.deg * Time.deltaT, Vec3f.X_AXIS) }
    }
    lighting.singleDirectionalLight {
        setup(Vec3f(-1f, -1f, -1f))
        setColor(Color.WHITE, 5f)
    }
}
