package com.neojou.myants.render3d.actors

import com.neojou.myants.render3d.WorldPalette
import com.neojou.myants.sim.InsectKind
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

internal class CritterTint(val body: Color, val eye: Color, val leg: Color, val mark: Color)

/**
 * Palette body, except two shells that the deferred light would shift.
 * The beetle stays dark chestnut. The ladybug stays scarlet: the palette red's
 * green and blue survive the noon sun and come out pink. Classic 2D stays on the palette.
 */
internal fun critterTint(kind: InsectKind): CritterTint {
    val body = when (kind) {
        InsectKind.BEETLE -> WorldPalette.beetleShell3d
        InsectKind.LADYBUG -> WorldPalette.ladybugShell3d
        else -> WorldPalette.insect(kind)
    }
    val eye = WorldPalette.critterEye
    val leg = when (kind) {
        InsectKind.PREY -> WorldPalette.preyLeg
        InsectKind.PREDATOR -> WorldPalette.predatorLeg
        InsectKind.SPIDER -> WorldPalette.spiderLeg
        InsectKind.BEETLE -> WorldPalette.beetleLeg
        InsectKind.LADYBUG -> WorldPalette.ladybugLeg
        InsectKind.APHID -> WorldPalette.aphidLeg
    }
    val mark = when (kind) {
        InsectKind.LADYBUG -> WorldPalette.ladybugSpot
        InsectKind.BEETLE -> WorldPalette.beetleMark
        else -> body
    }
    return CritterTint(body, eye, leg, mark)
}

internal fun uploadCritterMeshes(
    critter: GltfCritter,
    body: Mesh<*>,
    eyes: Mesh<*>,
    marks: Mesh<*>,
    legs: List<Mesh<*>>,
) {
    body.uploadCritter(critter.body)
    eyes.uploadCritter(critter.eyes)
    marks.uploadCritter(critter.marks)
    for (i in critter.legs.indices) legs[i].uploadCritter(critter.legs[i].mesh)
}

/**
 * One outdoor insect other than the aphid. The body, eyes, and spots share a rigid pose.
 * Six-leg kinds use the ant tripod. Eight-leg kinds alternate two sets of four.
 */
internal fun drawCritter(
    critter: GltfCritter,
    kind: InsectKind,
    body: MeshInstanceList<InstanceLayouts.ModelMatColor>,
    eyes: MeshInstanceList<InstanceLayouts.ModelMatColor>,
    marks: MeshInstanceList<InstanceLayouts.ModelMatColor>,
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
    val tint = critterTint(kind)
    rigidCritter(body, x, y, z, yaw, length, tint.body, mat, scale)
    rigidCritter(eyes, x, y, z, yaw, length, tint.eye, mat, scale)
    if (critter.marks.indices.isNotEmpty()) {
        rigidCritter(marks, x, y, z, yaw, length, tint.mark, mat, scale)
    }
    val layout = InstanceLayouts.ModelMatColor
    val amp = if (moving) critterStep(kind) else 0f
    for (i in critter.legs.indices) {
        val leg = critter.legs[i]
        val lead = if (critter.legs.size >= 8) {
            i == 0 || i == 3 || i == 4 || i == 7
        } else {
            i == 0 || i == 3 || i == 4
        }
        val ang = ((phase + if (lead) 0f else 0.5f) * (PI.toFloat() * 2f))
        val raw = sin(ang) * amp
        val angle = if (leg.footX >= leg.socketX) raw else -raw
        val lift = if (moving) max(0f, cos(ang)) * amp * 0.08f else 0f
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

private fun critterStep(kind: InsectKind): Float = when (kind) {
    InsectKind.PREDATOR -> 0.38f
    InsectKind.SPIDER -> 0.32f
    InsectKind.PREY -> 0.22f
    InsectKind.LADYBUG -> 0.16f
    InsectKind.BEETLE -> 0.14f
    InsectKind.APHID -> 0.12f
}

private fun rigidCritter(
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

private fun Mesh<*>.uploadCritter(part: AntMesh) {
    if (part.indices.isEmpty()) return
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
