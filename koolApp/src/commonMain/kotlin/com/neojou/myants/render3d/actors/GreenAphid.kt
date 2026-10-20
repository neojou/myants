package com.neojou.myants.render3d.actors

import de.fabmax.kool.math.MutableMat4f
import de.fabmax.kool.math.MutableVec3f
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.rad
import de.fabmax.kool.scene.InstanceLayouts
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.MeshInstanceList
import de.fabmax.kool.util.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

internal class AphidTint(val cuticle: Color, val eye: Color, val leg: Color)

/** Pale jade cuticle of a pea aphid, with dark red eyes and olive legs. */
internal fun greenAphidTint(): AphidTint = AphidTint(
    Color(0.50f, 0.78f, 0.26f, 1f),
    Color(0.42f, 0.09f, 0.06f, 1f),
    Color(0.36f, 0.50f, 0.20f, 1f),
)

internal fun uploadAphidMeshes(
    aphid: GltfAphid,
    body: Mesh<*>,
    eyes: Mesh<*>,
    legs: List<Mesh<*>>,
) {
    body.uploadAphid(aphid.body)
    eyes.uploadAphid(aphid.eyes)
    for (i in aphid.legs.indices) legs[i].uploadAphid(aphid.legs[i].mesh)
}

/**
 * One outdoor aphid. The body and eyes share one rigid pose.
 * Each leg pivots at the thoracic socket. The step is small: aphids barely walk.
 * Tripod: L1, R2, L3 move together; the other three are half a step away.
 */
internal fun drawAphid(
    aphid: GltfAphid,
    body: MeshInstanceList<InstanceLayouts.ModelMatColor>,
    eyes: MeshInstanceList<InstanceLayouts.ModelMatColor>,
    legs: List<MeshInstanceList<InstanceLayouts.ModelMatColor>>,
    x: Float,
    y: Float,
    z: Float,
    yaw: Float,
    length: Float,
    phase: Float,
    moving: Boolean,
    mat: MutableMat4f,
    scale: MutableVec3f,
) {
    val tint = greenAphidTint()
    rigidAphid(body, x, y, z, yaw, length, tint.cuticle, mat, scale)
    rigidAphid(eyes, x, y, z, yaw, length, tint.eye, mat, scale)
    val layout = InstanceLayouts.ModelMatColor
    for (i in aphid.legs.indices) {
        val leg = aphid.legs[i]
        val tripod = i == 0 || i == 3 || i == 4
        val ang = ((phase + if (tripod) 0f else 0.5f) * (PI.toFloat() * 2f))
        val amp = if (moving) 0.12f else 0f
        val raw = sin(ang) * amp
        val angle = if (leg.footX >= leg.socketX) raw else -raw
        val lift = if (moving) max(0f, cos(ang)) * 0.012f else 0f
        mat.setIdentity()
            .translate(x, y, z)
            .rotate(yaw.rad, Vec3f.Y_AXIS)
            .scale(scale.set(length, length, length))
            .translate(leg.socketX, leg.socketY, leg.socketZ)
            .rotate(angle.rad, Vec3f.Z_AXIS)
            .translate(0f, lift, 0f)
            .translate(-leg.socketX, -leg.socketY, -leg.socketZ)
        legs[i].addInstance {
            set(layout.modelMat, mat)
            set(layout.color, tint.leg)
        }
    }
}

private fun rigidAphid(
    batch: MeshInstanceList<InstanceLayouts.ModelMatColor>,
    x: Float,
    y: Float,
    z: Float,
    yaw: Float,
    length: Float,
    color: Color,
    mat: MutableMat4f,
    scale: MutableVec3f,
) {
    val layout = InstanceLayouts.ModelMatColor
    mat.setIdentity()
        .translate(x, y, z)
        .rotate(yaw.rad, Vec3f.Y_AXIS)
        .scale(scale.set(length, length, length))
    batch.addInstance {
        set(layout.modelMat, mat)
        set(layout.color, color)
    }
}

private fun Mesh<*>.uploadAphid(part: AntMesh) {
    generate {
        var i = 0
        while (i < part.positions.size) {
            vertex(
                Vec3f(part.positions[i], part.positions[i + 1], part.positions[i + 2]),
                Vec3f(part.normals[i], part.normals[i + 1], part.normals[i + 2]),
            )
            i += 3
        }
        var t = 0
        while (t < part.indices.size) {
            addTriIndices(part.indices[t], part.indices[t + 1], part.indices[t + 2])
            t += 3
        }
    }
}
