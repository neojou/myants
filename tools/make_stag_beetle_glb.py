#!/usr/bin/env python3
"""Male European stag beetle silhouette for the outdoor beetle slot.

Blender is Z-up. This script models in glTF axes (head +X, up +Y, left +Z)
and converts when it creates objects. The glTF export then turns Z-up back
into Y-up, so the file matches the other insects.

Shape reference only: J.F. Gaffard, Lucanus cervus, June 2004 (CC BY-SA 3.0).
The photograph is not embedded and is not used as a texture.

Run:
  /Applications/Blender.app/Contents/MacOS/Blender --background --python tools/make_stag_beetle_glb.py

No armature. Feet on y = 0, length 1, about 700 to 2200 triangles.
"""

import bpy
import bmesh
from mathutils import Vector
from pathlib import Path

OUT = Path(__file__).resolve().parents[1] / "koolApp" / "assets" / "models" / "insects" / "beetle.glb"


def gltf_to_blender(x, y, z):
    """glTF +Y up, +Z left becomes Blender +Z up, -Y left."""
    return (x, -z, y)


def link(obj):
    bpy.context.scene.collection.objects.link(obj)
    return obj


def empty(name, parent, loc):
    obj = bpy.data.objects.new(name, None)
    obj.empty_display_size = 0.04
    obj.parent = parent
    obj.location = Vector(gltf_to_blender(*loc))
    return link(obj)


def finish_mesh(name, parent, bm, loc=(0.0, 0.0, 0.0)):
    for face in bm.faces:
        face.smooth = True
    me = bpy.data.meshes.new(name + "Data")
    bm.to_mesh(me)
    bm.free()
    obj = bpy.data.objects.new(name, me)
    obj.parent = parent
    obj.location = Vector(gltf_to_blender(*loc))
    return link(obj)


def ellipsoid(name, parent, center, radius, u_segments=12, v_segments=8):
    sx, sy, sz = radius
    bm = bmesh.new()
    bmesh.ops.create_uvsphere(
        bm, u_segments=u_segments, v_segments=v_segments, radius=1.0,
    )
    # Blender axes are (glTF X, glTF Z, glTF Y).
    bmesh.ops.scale(bm, vec=Vector((sx, sz, sy)), verts=bm.verts)
    return finish_mesh(name, parent, bm, center)


def rod(name, parent, start, end, radius):
    """A 6-sided shaft from start to end, both in glTF parent space."""
    a = Vector(start)
    b = Vector(end)
    delta = b - a
    length = delta.length
    if length < 1e-6:
        raise SystemExit("rod %s is degenerate" % name)
    direction = delta.normalized()
    up = Vector((0.0, 1.0, 0.0))
    if abs(direction.dot(up)) > 0.92:
        up = Vector((0.0, 0.0, 1.0))
    side = direction.cross(up).normalized()
    up = side.cross(direction).normalized()
    r0 = radius
    r1 = radius * 0.62

    def ring(origin, r):
        return [
            origin + side * r + up * (r * 0.72),
            origin + up * r,
            origin - side * r + up * (r * 0.72),
            origin - side * r - up * (r * 0.55),
            origin - up * (r * 0.7),
            origin + side * r - up * (r * 0.55),
        ]

    near = ring(a, r0)
    far = ring(b, r1)
    bm = bmesh.new()
    verts = [bm.verts.new(gltf_point(p)) for p in near + far]
    bm.verts.ensure_lookup_table()
    n = 6

    def face(indices):
        bm.faces.new([verts[i] for i in indices])

    face(list(range(n - 1, -1, -1)))
    face(list(range(n, n * 2)))
    for i in range(n):
        j = (i + 1) % n
        face((i, j, n + j, n + i))
    bmesh.ops.recalc_face_normals(bm, faces=list(bm.faces))
    return finish_mesh(name, parent, bm)


def gltf_point(p):
    x, y, z = gltf_to_blender(p.x, p.y, p.z)
    return (x, y, z)


def leg(thorax, index_name, socket, foot, radius):
    """socket and foot are glTF points local to the thorax. The pivot is the socket.

    The knee sits out and down, under the shell. Each shaft runs past the joint
    so the two pieces read as one leg.
    """
    node = empty(index_name, thorax, socket)
    drop = foot[1] - socket[1]
    out = foot[2] - socket[2]
    knee = (
        (socket[0] + foot[0]) * 0.5,
        socket[1] + drop * 0.42,
        socket[2] + out * 0.68,
    )
    local_knee = tuple(knee[i] - socket[i] for i in range(3))
    local_foot = tuple(foot[i] - socket[i] for i in range(3))
    past = tuple(local_knee[i] + (local_foot[i] - local_knee[i]) * 0.22 for i in range(3))
    before = tuple(local_knee[i] * 0.78 for i in range(3))
    rod("Femur" + index_name, node, (0.0, 0.0, 0.0), past, radius)
    rod("Tibia" + index_name, node, before, local_foot, radius * 0.78)


def world_leg(thorax, origin, name, socket, foot, radius):
    local_socket = tuple(socket[i] - origin[i] for i in range(3))
    local_foot = tuple(foot[i] - origin[i] for i in range(3))
    leg(thorax, name, local_socket, local_foot, radius)


def pair(thorax, origin, index, socket_y, socket_z, foot_x, foot_z, radius):
    world_leg(
        thorax, origin, "LegL" + str(index),
        (foot_x, socket_y, socket_z), (foot_x, 0.0, foot_z), radius,
    )
    world_leg(
        thorax, origin, "LegR" + str(index),
        (foot_x, socket_y, -socket_z), (foot_x, 0.0, -foot_z), radius,
    )


def mandible(head, side):
    """side is +1 for glTF +Z (left). Segments overlap so the antler reads as one piece."""
    s = float(side)
    tag = "L" if side > 0 else "R"
    rod(
        "MandibleBase" + tag, head,
        (0.10, -0.04, 0.08 * s), (0.42, 0.06, 0.18 * s), 0.075,
    )
    rod(
        "MandibleShaft" + tag, head,
        (0.36, 0.04, 0.16 * s), (0.88, 0.22, 0.14 * s), 0.055,
    )
    rod(
        "MandibleTip" + tag, head,
        (0.80, 0.20, 0.13 * s), (1.16, 0.04, 0.04 * s), 0.038,
    )
    rod(
        "MandibleTooth" + tag, head,
        (0.55, 0.12, 0.14 * s), (0.62, -0.02, 0.02 * s), 0.032,
    )


def build():
    root = empty("Beetle", None, (0.0, 0.0, 0.0))
    # Chestnut shells. The node name Elytra* is the mark bucket in the renderer.
    ellipsoid("ElytraL", root, (0.92, 0.50, 0.16), (0.62, 0.24, 0.20), 14, 8)
    ellipsoid("ElytraR", root, (0.92, 0.50, -0.16), (0.62, 0.24, 0.20), 14, 8)
    thorax_at = (1.62, 0.42, 0.0)
    thorax = empty("Thorax", root, thorax_at)
    ellipsoid("Pronotum", thorax, (0.0, 0.08, 0.0), (0.34, 0.22, 0.42), 12, 8)
    head = empty("Head", root, (2.08, 0.46, 0.0))
    ellipsoid("HeadShell", head, (0.0, 0.0, 0.0), (0.26, 0.18, 0.34), 12, 8)
    ellipsoid("EyeL", head, (0.06, 0.06, 0.16), (0.06, 0.05, 0.045), 8, 6)
    ellipsoid("EyeR", head, (0.06, 0.06, -0.16), (0.06, 0.05, 0.045), 8, 6)
    rod("AntennaL", head, (-0.02, 0.08, 0.14), (0.10, 0.16, 0.28), 0.018)
    rod("AntennaR", head, (-0.02, 0.08, -0.14), (0.10, 0.16, -0.28), 0.018)
    mandible(head, 1)
    mandible(head, -1)
    # socket_y sits inside the underside of the pronotum (bottom is about 0.28).
    pair(thorax, thorax_at, 1, 0.34, 0.22, 1.95, 0.62, 0.048)
    pair(thorax, thorax_at, 2, 0.32, 0.26, 1.50, 0.72, 0.046)
    pair(thorax, thorax_at, 3, 0.32, 0.22, 1.05, 0.60, 0.044)
    return root


def fit(root):
    """Match tools/make_black_ant_glb.py fit_root, in Blender Z-up space."""
    bpy.context.view_layer.update()
    points = []
    for obj in bpy.context.scene.objects:
        if obj.type != "MESH":
            continue
        for vert in obj.data.vertices:
            points.append(obj.matrix_world @ vert.co)
    xs = [p.x for p in points]
    ys = [p.y for p in points]
    zs = [p.z for p in points]
    span = max(xs) - min(xs)
    scale = 1.0 / span
    center_x = (max(xs) + min(xs)) * 0.5
    center_y = (max(ys) + min(ys)) * 0.5
    min_z = min(zs)
    root.scale = (scale, scale, scale)
    root.location = (-scale * center_x, -scale * center_y, -scale * min_z)
    bpy.context.view_layer.update()


def export(path):
    path.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.export_scene.gltf(
        filepath=str(path),
        export_format="GLB",
        export_yup=True,
        export_apply=False,
        export_animations=False,
        export_skins=False,
        export_morph=False,
        export_materials="NONE",
        export_extras=False,
    )


def main():
    bpy.ops.wm.read_factory_settings(use_empty=True)
    root = build()
    fit(root)
    export(OUT)
    print("wrote", OUT)


if __name__ == "__main__":
    main()
