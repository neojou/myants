package com.neojou.myants.render3d

import com.neojou.myants.render3d.actors.mergedTriangles
import com.neojou.myants.render3d.actors.parseGltfMerged
import com.neojou.myants.render3d.actors.uploadMerged
import com.neojou.myants.render3d.quality.QualityPreset
import com.neojou.tools.LogLevel
import com.neojou.tools.MyLog
import de.fabmax.kool.Assets
import de.fabmax.kool.math.MutableMat4f
import de.fabmax.kool.math.MutableVec3f
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.rad
import de.fabmax.kool.pipeline.CullMethod
import de.fabmax.kool.pipeline.deferred.deferredKslPbrShader
import de.fabmax.kool.scene.InstanceLayouts
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.MeshInstanceList
import de.fabmax.kool.scene.Node
import de.fabmax.kool.scene.VertexLayouts
import de.fabmax.kool.scene.addMesh
import de.fabmax.kool.util.Color
import kotlinx.coroutines.launch

private const val TAG = "RENDER"

internal typealias Batch = MeshInstanceList<InstanceLayouts.ModelMatColor>

/**
 * One world sub-renderer (#47). Reads [RenderState] only; it never gets a [RenderHost],
 * so every write outside render3d stays in [WorldRenderer] and the nest cutaway.
 */
internal interface SubRenderer {
    fun build(opaque: Node, forward: Node)
    fun update(state: RenderState, preset: QualityPreset, dt: Float)
}

/** Per-sub-renderer scratch matrix and scale, so no mutable temp is shared between renderers. */
internal class Instancer {
    val layout = InstanceLayouts.ModelMatColor
    val mat = MutableMat4f()
    val scale = MutableVec3f()

    fun pose(
        batch: Batch,
        x: Float, y: Float, z: Float, yaw: Float,
        lx: Float, ly: Float, lz: Float,
        sx: Float, sy: Float, sz: Float,
        color: Color,
    ) {
        mat.setIdentity().translate(x, y, z).rotate(yaw.rad, Vec3f.Y_AXIS).translate(lx, ly, lz).scale(scale.set(sx, sy, sz))
        batch.addInstance {
            set(layout.modelMat, mat)
            set(layout.color, color)
        }
    }

    fun batch(size: Int): Batch = MeshInstanceList(layout, size)

    fun shaded(parent: Node, instances: Batch, name: String, roughness: Float, metallic: Float): Mesh<*> {
        val shader = deferredKslPbrShader {
            vertices { instancedModelMatrix(layout.modelMat) }
            color { instanceColor(layout.color) }
            roughness(roughness)
            metallic(metallic)
        }
        return parent.addMesh(VertexLayouts.PositionNormal, instances = instances) {
            isFrustumChecked = false
            this.name = name
            generate { }
            this.shader = shader
        }
    }

    fun instanced(
        parent: Node,
        instances: Batch,
        name: String,
        roughness: Float = 0.8f,
        doubleSided: Boolean = false,
        build: de.fabmax.kool.scene.geometry.MeshBuilder<*>.() -> Unit,
    ): Mesh<*> {
        val shader = deferredKslPbrShader {
            vertices { instancedModelMatrix(layout.modelMat) }
            color { instanceColor(layout.color) }
            roughness(roughness)
            if (doubleSided) {
                pipeline { cullMethod = CullMethod.NO_CULLING }
            }
        }
        return parent.addMesh(VertexLayouts.PositionNormal, instances = instances) {
            isFrustumChecked = false
            this.name = name
            generate { build() }
            this.shader = shader
        }
    }
}

/** Loads a merged glb into [meshes]; on failure the built-in fallback shape stays. */
internal fun loadMergedGlb(
    scope: kotlinx.coroutines.CoroutineScope,
    path: String,
    meshes: List<Mesh<*>>,
    label: String,
    fallback: String = "sphere",
    onReady: () -> Unit,
) {
    scope.launch {
        val loaded = Assets.defaultLoader.loadBlob(path)
        loaded.onSuccess { buf ->
            try {
                val parsed = parseGltfMerged(buf.toArray())
                for (mesh in meshes) mesh.uploadMerged(parsed)
                onReady()
                MyLog.add(TAG, "$label gltf tris=${mergedTriangles(parsed)}", LogLevel.DEBUG)
            } catch (err: Throwable) {
                MyLog.add(TAG, "$label glTF failed: ${err.message}. Using the $fallback.", LogLevel.DEBUG)
            }
        }.onFailure { err ->
            MyLog.add(TAG, "$label glTF missing: ${err.message}. Using the $fallback.", LogLevel.DEBUG)
        }
    }
}
