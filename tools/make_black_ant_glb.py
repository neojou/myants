#!/usr/bin/env python3
"""Write a glTF 2.0 black worker ant.

The mesh is original. Local +X is the head, +Y is up, and the root node
scales the design so the body is 1 unit long with the feet on y = 0.
Run from anywhere: the .glb lands in koolApp/assets/models/ants/.
"""

import json
import math
import struct
from pathlib import Path

TAU = math.pi * 2.0
STACKS = 10
SLICES = 14
SIDES = 8

CHITIN = 0
EYE = 1
MANDIBLE = 2


def norm(v):
    x, y, z = v
    length = math.sqrt(x * x + y * y + z * z)
    if length < 1e-8:
        return (0.0, 1.0, 0.0)
    return (x / length, y / length, z / length)


def dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def cross(a, b):
    return (
        a[1] * b[2] - a[2] * b[1],
        a[2] * b[0] - a[0] * b[2],
        a[0] * b[1] - a[1] * b[0],
    )


def add(a, b):
    return (a[0] + b[0], a[1] + b[1], a[2] + b[2])


def sub(a, b):
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def mul(a, s):
    return (a[0] * s, a[1] * s, a[2] * s)


def quat_norm(q):
    x, y, z, w = q
    length = math.sqrt(x * x + y * y + z * z + w * w)
    if length < 1e-8:
        return (0.0, 0.0, 0.0, 1.0)
    return (x / length, y / length, z / length, w / length)


def quat_from_x(direction):
    """Quaternion xyzw that maps +X onto direction."""
    src = (1.0, 0.0, 0.0)
    dst = norm(direction)
    c = cross(src, dst)
    d = dot(src, dst)
    if d < -0.999999:
        return (0.0, 0.0, 1.0, 0.0)  # 180 deg around Z: +X -> -X
    return quat_norm((c[0], c[1], c[2], 1.0 + d))


class Mesh(object):
    def __init__(self, positions, normals, indices):
        self.positions = positions
        self.normals = normals
        self.indices = indices

    def flipped(self):
        idx = []
        for i in range(0, len(self.indices), 3):
            idx.extend((self.indices[i], self.indices[i + 2], self.indices[i + 1]))
        norms = [(-n[0], -n[1], -n[2]) for n in self.normals]
        return Mesh(self.positions, norms, idx)


def _face_outward(mesh, outward_at):
    """Flip the mesh when the first non-degenerate triangle faces inward."""
    pos = mesh.positions
    idx = mesh.indices
    for t in range(0, len(idx), 3):
        a = pos[idx[t]]
        b = pos[idx[t + 1]]
        c = pos[idx[t + 2]]
        n = cross(sub(b, a), sub(c, a))
        if dot(n, n) < 1e-12:
            continue
        center = mul(add(add(a, b), c), 1.0 / 3.0)
        if dot(n, outward_at(center)) < 0.0:
            return mesh.flipped()
        return mesh
    return mesh


def uv_sphere():
    pos = [(0.0, 1.0, 0.0)]
    nrm = [(0.0, 1.0, 0.0)]
    for i in range(1, STACKS):
        phi = math.pi * i / STACKS
        sp, cp = math.sin(phi), math.cos(phi)
        for j in range(SLICES):
            theta = TAU * j / SLICES
            p = (sp * math.cos(theta), cp, sp * math.sin(theta))
            pos.append(p)
            nrm.append(p)
    pos.append((0.0, -1.0, 0.0))
    nrm.append((0.0, -1.0, 0.0))
    idx = []

    def ring(i, j):
        return 1 + (i - 1) * SLICES + j

    # Winding matches the outward normals. The opposite order makes
    # _face_outward flip both, so the visible crown keeps a downward normal.
    for j in range(SLICES):
        j1 = (j + 1) % SLICES
        idx.extend((0, ring(1, j1), ring(1, j)))
    for i in range(1, STACKS - 1):
        for j in range(SLICES):
            j1 = (j + 1) % SLICES
            a = ring(i, j)
            b = ring(i + 1, j)
            c = ring(i, j1)
            d = ring(i + 1, j1)
            idx.extend((a, c, b, c, d, b))
    bottom = len(pos) - 1
    last = STACKS - 1
    for j in range(SLICES):
        j1 = (j + 1) % SLICES
        idx.extend((bottom, ring(last, j), ring(last, j1)))
    return _face_outward(Mesh(pos, nrm, idx), lambda p: p)


def tapered_rod(r0, r1):
    """Rod along local +X from 0 to 1, with caps."""
    pos = []
    nrm = []
    for end, radius in ((0.0, r0), (1.0, r1)):
        for i in range(SIDES):
            a = TAU * i / SIDES
            y, z = math.cos(a), math.sin(a)
            pos.append((end, y * radius, z * radius))
            nrm.append(norm((r0 - r1, y, z)))
    center0 = len(pos)
    pos.append((0.0, 0.0, 0.0))
    nrm.append((-1.0, 0.0, 0.0))
    center1 = len(pos)
    pos.append((1.0, 0.0, 0.0))
    nrm.append((1.0, 0.0, 0.0))
    idx = []
    for i in range(SIDES):
        j = (i + 1) % SIDES
        a, c = i, j
        b, d = SIDES + i, SIDES + j
        idx.extend((a, b, c, c, b, d))
        idx.extend((center0, c, a))
        idx.extend((center1, b, d))
    return _face_outward(Mesh(pos, nrm, idx), lambda p: (0.0, p[1], p[2]))


class Builder(object):
    def __init__(self):
        self.nodes = []
        self.meshes = []
        self.sphere = None

    def add_node(self, name, parent, translation=(0, 0, 0), rotation=(0, 0, 0, 1), scale=(1, 1, 1), mesh=None, material=CHITIN):
        node = {
            "name": name,
            "translation": [float(v) for v in translation],
            "rotation": [float(v) for v in rotation],
            "scale": [float(v) for v in scale],
            "children": [],
            "mesh": None,
            "material": material,
        }
        index = len(self.nodes)
        self.nodes.append(node)
        if parent is not None:
            self.nodes[parent]["children"].append(index)
        if mesh is not None:
            node["mesh"] = len(self.meshes)
            self.meshes.append(mesh)
        return index

    def sphere_node(self, name, parent, center, radii, material=CHITIN):
        if self.sphere is None:
            self.sphere = uv_sphere()
        return self.add_node(
            name, parent, translation=center, scale=radii, mesh=self.sphere, material=material,
        )

    def rod(self, name, parent, start, end, r0, r1, material=CHITIN):
        delta = sub(end, start)
        length = math.sqrt(dot(delta, delta))
        if length < 1e-6:
            return self.add_node(name, parent, translation=start)
        rotation = quat_from_x(delta)
        mesh = tapered_rod(r0, r1)
        return self.add_node(
            name, parent,
            translation=start,
            rotation=rotation,
            scale=(length, 1.0, 1.0),
            mesh=mesh,
            material=material,
        )


def build(soldier=False):
    b = Builder()
    root = b.add_node("Ant", None)

    # Design space, head toward +X, feet on y = 0. The root scale fits this to length 1.
    # Proportions follow a fire-ant photo: a wider head, one low mesosoma, a
    # narrow two-node waist, and one long oval gaster. A soldier keeps the same
    # nodes, with a larger head and thicker jaws that stay in front of the head.
    hs = 1.38 if soldier else 1.0
    js = 1.22 if soldier else 1.0
    ps = 1.10 if soldier else 1.0
    head_rx = 0.62 * hs
    pro_front = 5.40 + 0.46 * ps
    head_c = (pro_front + 0.36 + head_rx, 1.16, 0.0)
    head = b.add_node("Head", root, translation=head_c)
    b.sphere_node("HeadShell", head, (0.0, -0.02, 0.0), (head_rx, 0.44 * hs, 0.72 * hs))
    b.sphere_node("Neck", head, (-head_rx - 0.06, -0.08, 0.0), (0.32, 0.15, 0.17))
    b.sphere_node("Clypeus", head, (0.36 * hs, -0.12, 0.0), (0.18 * hs, 0.10 * hs, 0.26 * hs))
    b.sphere_node("EyeL", head, (0.06 * hs, 0.12 * hs, 0.58 * hs), (0.16 * hs, 0.12 * hs, 0.10 * hs), EYE)
    b.sphere_node("EyeR", head, (0.06 * hs, 0.12 * hs, -0.58 * hs), (0.16 * hs, 0.12 * hs, 0.10 * hs), EYE)

    def antenna(side, zsign):
        name = "Antenna" + side
        socket = (0.28 * hs, 0.32 * hs, 0.28 * hs * zsign)
        elbow = (0.95 * hs, 0.62 * hs, 0.62 * hs * zsign)
        tip = (1.70 * hs, 0.22 * hs, 0.95 * hs * zsign)
        node = b.add_node(name, head, translation=socket)
        b.rod("Scape" + side, node, (0, 0, 0), sub(elbow, socket), 0.032, 0.026)
        b.rod("Funiculus" + side, node, sub(elbow, socket), sub(tip, socket), 0.022, 0.012)

    antenna("L", 1.0)
    antenna("R", -1.0)

    def mandible(side, zsign):
        name = "Mandible" + side
        base = (0.42 * hs, -0.14, 0.14 * hs * zsign)
        mid = (0.72 * js, -0.20, 0.16 * hs * zsign)
        tip = (0.92 * js, -0.24, 0.08 * hs * zsign)
        node = b.add_node(name, head, translation=base)
        b.rod("MandibleBase" + side, node, (0, 0, 0), sub(mid, base), 0.055 * js, 0.040 * js, MANDIBLE)
        b.rod("MandibleTip" + side, node, sub(mid, base), sub(tip, base), 0.038 * js, 0.018 * js, MANDIBLE)

    mandible("L", 1.0)
    mandible("R", -1.0)

    thorax = b.add_node("Thorax", root)
    segments = (
        ("Prothorax", (5.40, 1.18, 0.0), (0.46 * ps, 0.28 * ps, 0.50 * ps)),
        ("Mesothorax", (4.58, 1.30, 0.0), (0.56, 0.32, 0.54)),
        ("Metathorax", (3.88, 1.14, 0.0), (0.40, 0.24, 0.38)),
    )
    segment_index = {}
    for name, center, radii in segments:
        segment_index[name] = b.add_node(name, thorax, translation=center)
        b.sphere_node(name + "Shell", segment_index[name], (0, 0, 0), radii)

    def leg(side, segment, socket, joints, radii):
        """socket and joints are design-space points. joints include the foot."""
        name = "Leg" + side
        origin = segment_index[segment]
        center = next(c for n, c, _ in segments if n == segment)
        local_socket = sub(socket, center)
        node = b.add_node(name, origin, translation=local_socket)
        cursor = socket
        labels = ("Coxa", "Femur", "Tibia", "Tarsus")
        for i, point in enumerate(joints):
            b.rod(labels[i] + side, node, sub(cursor, socket), sub(point, socket), radii[i], radii[i + 1])
            cursor = point

    # Front pair on the prothorax, middle on the mesothorax, hind on the metathorax.
    # Each list is coxa end, femur end, tibia end, tarsus (foot on y = 0).
    leg_r = (0.090, 0.070, 0.050, 0.032, 0.018)
    leg("L1", "Prothorax", (5.50, 0.92, 0.42), [
        (5.85, 0.62, 0.72),
        (6.35, 0.28, 1.15),
        (6.05, 0.08, 1.48),
        (5.85, 0.00, 1.62),
    ], leg_r)
    leg("R1", "Prothorax", (5.50, 0.92, -0.42), [
        (5.85, 0.62, -0.72),
        (6.35, 0.28, -1.15),
        (6.05, 0.08, -1.48),
        (5.85, 0.00, -1.62),
    ], leg_r)
    leg("L2", "Mesothorax", (4.58, 0.92, 0.46), [
        (4.55, 0.58, 0.85),
        (4.40, 0.26, 1.35),
        (4.20, 0.08, 1.62),
        (4.05, 0.00, 1.78),
    ], leg_r)
    leg("R2", "Mesothorax", (4.58, 0.92, -0.46), [
        (4.55, 0.58, -0.85),
        (4.40, 0.26, -1.35),
        (4.20, 0.08, -1.62),
        (4.05, 0.00, -1.78),
    ], leg_r)
    leg("L3", "Metathorax", (3.95, 0.88, 0.34), [
        (3.45, 0.58, 0.72),
        (2.75, 0.26, 1.15),
        (2.25, 0.08, 1.40),
        (1.95, 0.00, 1.55),
    ], leg_r)
    leg("R3", "Metathorax", (3.95, 0.88, -0.34), [
        (3.45, 0.58, -0.72),
        (2.75, 0.26, -1.15),
        (2.25, 0.08, -1.40),
        (1.95, 0.00, -1.55),
    ], leg_r)

    petiole = b.add_node("Petiole", root, translation=(3.20, 1.22, 0.0))
    b.sphere_node("PetioleShell", petiole, (0, 0, 0), (0.10, 0.18, 0.08))
    post = b.add_node("Postpetiole", root, translation=(2.74, 1.18, 0.0))
    b.sphere_node("PostpetioleShell", post, (0, 0, 0), (0.14, 0.16, 0.12))

    gaster = b.add_node("Gaster", root, translation=(1.40, 1.18, 0.0))
    b.sphere_node("GasterRear", gaster, (0.0, 0.04, 0.0), (1.20, 0.78, 0.68))
    b.sphere_node("GasterFront", gaster, (0.88, 0.06, 0.0), (0.28, 0.24, 0.30))
    b.sphere_node("GasterTip", gaster, (-1.05, -0.02, 0.0), (0.26, 0.20, 0.24))
    return b


def mat_trs(translation, rotation, scale):
    x, y, z, w = rotation
    xx, yy, zz = x * x, y * y, z * z
    r00 = 1 - 2 * (yy + zz)
    r01 = 2 * (x * y - z * w)
    r02 = 2 * (x * z + y * w)
    r10 = 2 * (x * y + z * w)
    r11 = 1 - 2 * (xx + zz)
    r12 = 2 * (y * z - x * w)
    r20 = 2 * (x * z - y * w)
    r21 = 2 * (y * z + x * w)
    r22 = 1 - 2 * (xx + yy)
    sx, sy, sz = scale
    tx, ty, tz = translation
    # Column-major T * R * S.
    return [
        r00 * sx, r10 * sx, r20 * sx, 0.0,
        r01 * sy, r11 * sy, r21 * sy, 0.0,
        r02 * sz, r12 * sz, r22 * sz, 0.0,
        tx, ty, tz, 1.0,
    ]


def mat_mul(a, b):
    out = [0.0] * 16
    for col in range(4):
        for row in range(4):
            out[col * 4 + row] = (
                a[row] * b[col * 4]
                + a[4 + row] * b[col * 4 + 1]
                + a[8 + row] * b[col * 4 + 2]
                + a[12 + row] * b[col * 4 + 3]
            )
    return out


def transform_point(m, p):
    x, y, z = p
    return (
        m[0] * x + m[4] * y + m[8] * z + m[12],
        m[1] * x + m[5] * y + m[9] * z + m[13],
        m[2] * x + m[6] * y + m[10] * z + m[14],
    )


def world_matrices(builder):
    local = [mat_trs(n["translation"], n["rotation"], n["scale"]) for n in builder.nodes]
    parent = [-1] * len(builder.nodes)
    for i, node in enumerate(builder.nodes):
        for child in node["children"]:
            parent[child] = i
    world = [None] * len(builder.nodes)

    def visit(index, parent_world):
        here = local[index] if parent_world is None else mat_mul(parent_world, local[index])
        world[index] = here
        for child in builder.nodes[index]["children"]:
            visit(child, here)

    visit(0, None)
    return world, parent


def fit_root(builder):
    """Scale the root so the body is 1 long and the feet sit on y = 0."""
    world, _ = world_matrices(builder)
    points = []
    for index, node in enumerate(builder.nodes):
        mesh_index = node["mesh"]
        if mesh_index is None:
            continue
        mesh = builder.meshes[mesh_index]
        for p in mesh.positions:
            points.append(transform_point(world[index], p))
    xs = [p[0] for p in points]
    ys = [p[1] for p in points]
    zs = [p[2] for p in points]
    span = max(xs) - min(xs)
    scale = 1.0 / span
    center_x = (max(xs) + min(xs)) * 0.5
    center_z = (max(zs) + min(zs)) * 0.5
    min_y = min(ys)
    builder.nodes[0]["translation"] = (-scale * center_x, -scale * min_y, -scale * center_z)
    builder.nodes[0]["scale"] = (scale, scale, scale)


def pack_glb(builder, generator="myants make_black_ant_glb.py", materials=None):
    """materials: glTF material list indexed by node material id; default is the ant set."""
    blob = bytearray()
    buffer_views = []
    accessors = []

    def align():
        while len(blob) % 4:
            blob.append(0)

    def push_floats(values):
        align()
        start = len(blob)
        for value in values:
            blob.extend(struct.pack("<f", float(value)))
        return start, len(blob) - start

    def push_ushorts(values):
        align()
        start = len(blob)
        for value in values:
            blob.extend(struct.pack("<H", int(value)))
        if len(values) % 2:
            blob.extend(b"\x00\x00")
        return start, len(blob) - start

    primitives_per_mesh = []
    for mesh in builder.meshes:
        flat_p = []
        flat_n = []
        for p, n in zip(mesh.positions, mesh.normals):
            flat_p.extend(p)
            flat_n.extend(n)
        p_off, p_len = push_floats(flat_p)
        n_off, n_len = push_floats(flat_n)
        i_off, i_len = push_ushorts(mesh.indices)
        base = len(accessors)
        pv = len(buffer_views)
        buffer_views.append({"buffer": 0, "byteOffset": p_off, "byteLength": p_len})
        buffer_views.append({"buffer": 0, "byteOffset": n_off, "byteLength": n_len})
        buffer_views.append({"buffer": 0, "byteOffset": i_off, "byteLength": i_len})
        xs = flat_p[0::3]
        ys = flat_p[1::3]
        zs = flat_p[2::3]
        accessors.append({
            "bufferView": pv, "componentType": 5126, "count": len(mesh.positions), "type": "VEC3",
            "min": [min(xs), min(ys), min(zs)], "max": [max(xs), max(ys), max(zs)],
        })
        accessors.append({
            "bufferView": pv + 1, "componentType": 5126, "count": len(mesh.normals), "type": "VEC3",
        })
        accessors.append({
            "bufferView": pv + 2, "componentType": 5123, "count": len(mesh.indices), "type": "SCALAR",
        })
        primitives_per_mesh.append({
            "attributes": {"POSITION": base, "NORMAL": base + 1},
            "indices": base + 2,
        })

    gltf_meshes = []
    mesh_material = []
    nodes = []
    for node in builder.nodes:
        out = {"name": node["name"]}
        if node["children"]:
            out["children"] = node["children"]
        t = node["translation"]
        r = node["rotation"]
        s = node["scale"]
        if any(abs(v) > 1e-8 for v in t):
            out["translation"] = t
        if any(abs(r[i] - (0, 0, 0, 1)[i]) > 1e-8 for i in range(4)):
            out["rotation"] = r
        if any(abs(s[i] - 1.0) > 1e-8 for i in range(3)):
            out["scale"] = s
        if node["mesh"] is not None:
            prim = dict(primitives_per_mesh[node["mesh"]])
            prim["material"] = node["material"]
            out["mesh"] = len(gltf_meshes)
            gltf_meshes.append({"name": node["name"], "primitives": [prim]})
            mesh_material.append(node["material"])
        nodes.append(out)

    gltf = {
        "asset": {"version": "2.0", "generator": generator},
        "scene": 0,
        "scenes": [{"name": "Ant", "nodes": [0]}],
        "nodes": nodes,
        "meshes": gltf_meshes,
        "materials": materials if materials is not None else ANT_MATERIALS,
        "accessors": accessors,
        "bufferViews": buffer_views,
        "buffers": [{"byteLength": len(blob)}],
    }
    json_chunk = json.dumps(gltf, separators=(",", ":")).encode("utf-8")
    while len(json_chunk) % 4:
        json_chunk += b" "
    while len(blob) % 4:
        blob.append(0)
    chunks = b"".join((
        struct.pack("<II", len(json_chunk), 0x4E4F534A),
        json_chunk,
        struct.pack("<II", len(blob), 0x004E4942),
        bytes(blob),
    ))
    header = struct.pack("<III", 0x46546C67, 2, 12 + len(chunks))
    return header + chunks, gltf


ANT_MATERIALS = [
    {
        "name": "chitin",
        "pbrMetallicRoughness": {
            "baseColorFactor": [0.07, 0.07, 0.075, 1.0],
            "metallicFactor": 0.0,
            "roughnessFactor": 1.0,
        },
    },
    {
        "name": "eye",
        "pbrMetallicRoughness": {
            "baseColorFactor": [0.015, 0.015, 0.02, 1.0],
            "metallicFactor": 0.02,
            "roughnessFactor": 0.12,
        },
    },
    {
        "name": "mandible",
        "pbrMetallicRoughness": {
            "baseColorFactor": [0.16, 0.12, 0.09, 1.0],
            "metallicFactor": 0.0,
            "roughnessFactor": 1.0,
        },
    },
]


def check(builder):
    world, parent = world_matrices(builder)
    by_name = {}
    for i, node in enumerate(builder.nodes):
        by_name.setdefault(node["name"], i)

    def origin(name):
        m = world[by_name[name]]
        return (m[12], m[13], m[14])

    head = origin("Head")
    gaster = origin("Gaster")
    if not head[0] > gaster[0]:
        raise SystemExit("head is not in front of the gaster")
    expected = {
        "LegL1": "Prothorax", "LegR1": "Prothorax",
        "LegL2": "Mesothorax", "LegR2": "Mesothorax",
        "LegL3": "Metathorax", "LegR3": "Metathorax",
    }
    for leg, thorax in expected.items():
        seen = parent[by_name[leg]]
        names = []
        while seen != -1:
            names.append(builder.nodes[seen]["name"])
            seen = parent[seen]
        if thorax not in names:
            raise SystemExit("%s parent chain %s misses %s" % (leg, names, thorax))
    tris = sum(len(mesh.indices) // 3 for mesh in builder.meshes)
    print("nodes", len(builder.nodes), "tris", tris, "head", tuple(round(v, 3) for v in head), "gaster", tuple(round(v, 3) for v in gaster))
    return world


def preview(builder, world, path):
    """Side and top rasters so the silhouette can be checked without the engine."""
    parts = []
    for index, node in enumerate(builder.nodes):
        if node["mesh"] is None:
            continue
        mesh = builder.meshes[node["mesh"]]
        m = world[index]
        verts = [transform_point(m, p) for p in mesh.positions]
        color = (210, 176, 120) if node["material"] == MANDIBLE else (230, 230, 240) if node["material"] == EYE else (40, 40, 46)
        if node["name"].startswith("Leg") or node["name"][:4] in ("Coxa", "Femu", "Tibi", "Tars"):
            color = (70, 78, 96)
        parts.append((verts, mesh.indices, color))

    def paint(project, x0, y0, x1, y1, width, height):
        image = [[(18, 22, 28) for _ in range(width)] for _ in range(height)]
        depth = [[1e9 for _ in range(width)] for _ in range(height)]

        def px(p):
            u = (p[0] - x0) / (x1 - x0)
            v = (p[1] - y0) / (y1 - y0)
            return u * (width - 1), (1.0 - v) * (height - 1)

        for verts, indices, color in parts:
            projected = [px(project(v)) for v in verts]
            for t in range(0, len(indices), 3):
                tri = [projected[indices[t + k]] for k in range(3)]
                xs = [p[0] for p in tri]
                ys = [p[1] for p in tri]
                minx = max(0, int(min(xs)))
                maxx = min(width - 1, int(max(xs)) + 1)
                miny = max(0, int(min(ys)))
                maxy = min(height - 1, int(max(ys)) + 1)
                area = (tri[1][0] - tri[0][0]) * (tri[2][1] - tri[0][1]) - (tri[2][0] - tri[0][0]) * (tri[1][1] - tri[0][1])
                if abs(area) < 1e-6:
                    continue
                for y in range(miny, maxy + 1):
                    for x in range(minx, maxx + 1):
                        w0 = (tri[1][0] - x) * (tri[2][1] - y) - (tri[2][0] - x) * (tri[1][1] - y)
                        w1 = (tri[2][0] - x) * (tri[0][1] - y) - (tri[0][0] - x) * (tri[2][1] - y)
                        w2 = (tri[0][0] - x) * (tri[1][1] - y) - (tri[1][0] - x) * (tri[0][1] - y)
                        if area > 0:
                            inside = w0 >= 0 and w1 >= 0 and w2 >= 0
                        else:
                            inside = w0 <= 0 and w1 <= 0 and w2 <= 0
                        if inside and y < depth[y][x]:
                            depth[y][x] = y
                            shade = 0.55 + 0.45 * ((x + y) % 7) / 6.0
                            image[y][x] = tuple(max(0, min(255, int(c * shade))) for c in color)
        return image

    side = paint(lambda p: (p[0], p[1]), -0.65, -0.05, 0.65, 0.55, 480, 240)
    top = paint(lambda p: (p[0], p[2]), -0.65, -0.45, 0.65, 0.45, 480, 280)
    gap = 8
    width = 480
    height = 240 + gap + 280
    rows = []
    for y in range(240):
        row = bytearray()
        for x in range(width):
            row.extend(side[y][x])
        rows.append(row)
    for _ in range(gap):
        rows.append(bytes((12, 12, 14)) * width)
    for y in range(280):
        row = bytearray()
        for x in range(width):
            row.extend(top[y][x])
        rows.append(row)
    _write_png(path, width, height, rows)


def _write_png(path, width, height, rows):
    import zlib

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    raw = b"".join(b"\x00" + bytes(row) for row in rows)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    Path(path).write_bytes(png)


def main():
    root = Path(__file__).resolve().parents[1] / "koolApp" / "assets" / "models" / "ants"
    root.mkdir(parents=True, exist_ok=True)
    for soldier, name in ((False, "worker_black.glb"), (True, "soldier.glb")):
        builder = build(soldier)
        fit_root(builder)
        world = check(builder)
        data, _ = pack_glb(builder)
        out = root / name
        out.write_bytes(data)
        preview_path = Path("/tmp") / (name.replace(".glb", "") + ".png")
        preview(builder, world, preview_path)
        print("wrote", out, "bytes", len(data))
        print("preview", preview_path)


if __name__ == "__main__":
    main()
