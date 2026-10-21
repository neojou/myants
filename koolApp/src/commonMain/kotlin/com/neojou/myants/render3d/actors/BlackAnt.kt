package com.neojou.myants.render3d.actors

import de.fabmax.kool.math.MutableMat4f
import de.fabmax.kool.math.MutableVec3f
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.rad
import de.fabmax.kool.scene.InstanceLayouts
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.MeshInstanceList
import com.neojou.myants.render3d.WorldPalette
import de.fabmax.kool.util.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

internal class AntTint(val chitin: Color, val eye: Color, val jaw: Color)

/** Colony 0 is black chitin. A rival colony stays dark, with a warm brown cast. */
internal fun outdoorAntTint(colonyId: Int): AntTint = if (colonyId == 0) {
    AntTint(WorldPalette.antChitin, WorldPalette.antEye, WorldPalette.antJaw)
} else {
    AntTint(WorldPalette.rivalChitin, WorldPalette.rivalEye, WorldPalette.rivalJaw)
}

internal fun uploadAntMeshes(
    ant: GltfAnt,
    body: Mesh<*>,
    eyes: Mesh<*>,
    jaws: Mesh<*>,
    legs: List<Mesh<*>>,
) {
    body.upload(ant.body)
    eyes.upload(ant.eyes)
    jaws.upload(ant.mandibles)
    for (i in ant.legs.indices) legs[i].upload(ant.legs[i].mesh)
}

/**
 * One outdoor ant. The body, eyes and jaws share one rigid pose.
 * Each leg pivots at the thoracic socket stored in the glTF node.
 * Tripod: L1, R2, L3 move together; the other three are half a step away.
 */
internal fun GltfAnt.draw(
    body: MeshInstanceList<InstanceLayouts.ModelMatColor>,
    eyes: MeshInstanceList<InstanceLayouts.ModelMatColor>,
    jaws: MeshInstanceList<InstanceLayouts.ModelMatColor>,
    legs: List<MeshInstanceList<InstanceLayouts.ModelMatColor>>,
    x: Float,
    y: Float,
    z: Float,
    yaw: Float,
    length: Float,
    phase: Float,
    moving: Boolean,
    tint: AntTint,
    mat: MutableMat4f,
    scale: MutableVec3f,
) {
    rigid(body, x, y, z, yaw, length, tint.chitin, mat, scale)
    rigid(eyes, x, y, z, yaw, length, tint.eye, mat, scale)
    rigid(jaws, x, y, z, yaw, length, tint.jaw, mat, scale)
    val layout = InstanceLayouts.ModelMatColor
    for (i in this.legs.indices) {
        val leg = this.legs[i]
        val tripod = i == 0 || i == 3 || i == 4
        val ang = ((phase + if (tripod) 0f else 0.5f) * (PI.toFloat() * 2f))
        val amp = if (moving) 0.42f else 0f
        val raw = sin(ang) * amp
        val angle = if (leg.footX >= leg.socketX) raw else -raw
        val lift = if (moving) max(0f, cos(ang)) * 0.03f else 0f
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
            set(layout.color, tint.chitin)
        }
    }
}

private fun rigid(
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

private fun Mesh<*>.upload(part: AntMesh) {
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
