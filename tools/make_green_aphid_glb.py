#!/usr/bin/env python3
"""Write a glTF 2.0 pea aphid.

The mesh is original. Local +X is the head, +Y is up, and the root node
scales the design so the body is 1 unit long with the feet on y = 0.
The cuticle is the pale jade green of a pea aphid (Acyrthosiphon pisum).
Run from anywhere: the .glb lands in koolApp/assets/models/insects/.
"""

import importlib.util
import json
import math
import struct
from pathlib import Path

TAU = math.pi * 2.0
CUTICLE = 0
EYE = 1
LEG = 2

_ant_path = Path(__file__).with_name("make_black_ant_glb.py")
_spec = importlib.util.spec_from_file_location("make_black_ant_glb", _ant_path)
ant = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(ant)


def revolved_pear(profile, slices, y_scale, z_scale):
    """Pear along local X. profile is (x, radius) from rear to front."""
    pos = []
    for x, radius in profile:
        for j in range(slices):
            angle = TAU * j / slices
            pos.append((
                x,
                math.cos(angle) * radius * y_scale,
                math.sin(angle) * radius * z_scale,
            ))
    rear = len(pos)
    pos.append((profile[0][0], 0.0, 0.0))
    front = len(pos)
    pos.append((profile[-1][0], 0.0, 0.0))

    def vid(ring, column):
        return ring * slices + (column % slices)

    idx = []
    rings = len(profile)
    for i in range(rings - 1):
        for j in range(slices):
            a = vid(i, j)
            b = vid(i, j + 1)
            c = vid(i + 1, j)
            d = vid(i + 1, j + 1)
            idx.extend((a, b, c, b, d, c))
    for j in range(slices):
        idx.extend((rear, vid(0, j + 1), vid(0, j)))
        idx.extend((front, vid(rings - 1, j), vid(rings - 1, j + 1)))

    normals = [[0.0, 0.0, 0.0] for _ in pos]
    for t in range(0, len(idx), 3):
        a, b, c = idx[t], idx[t + 1], idx[t + 2]
        face = ant.cross(ant.sub(pos[b], pos[a]), ant.sub(pos[c], pos[a]))
        for k in (a, b, c):
            normals[k][0] += face[0]
            normals[k][1] += face[1]
            normals[k][2] += face[2]
    packed = [ant.norm(tuple(n)) for n in normals]
    mesh = ant.Mesh(pos, packed, idx)
    return face_outward(mesh)


def face_outward(mesh):
    pos = mesh.positions
    idx = mesh.indices
    vote = 0
    for t in range(0, len(idx), 3):
        a, b, c = pos[idx[t]], pos[idx[t + 1]], pos[idx[t + 2]]
        n = ant.cross(ant.sub(b, a), ant.sub(c, a))
        if ant.dot(n, n) < 1e-12:
            continue
        center = ant.mul(ant.add(ant.add(a, b), c), 1.0 / 3.0)
        outward = (0.0, center[1], center[2])
        if abs(center[1]) + abs(center[2]) < 1e-4:
            outward = (center[0], 0.0, 0.0)
        vote += 1 if ant.dot(n, outward) >= 0.0 else -1
    if vote < 0:
        return mesh.flipped()
    return mesh


def build():
    b = ant.Builder()
    root = b.add_node("Aphid", None)
    head_c = (6.95, 0.98, 0.0)
    head = b.add_node("Head", root, translation=head_c)
    b.sphere_node("HeadShell", head, (0, 0, 0), (0.40, 0.34, 0.36), CUTICLE)
    b.sphere_node("EyeL", head, (0.10, 0.08, 0.26), (0.11, 0.09, 0.08), EYE)
    b.sphere_node("EyeR", head, (0.10, 0.08, -0.26), (0.11, 0.09, 0.08), EYE)

    def antenna(side, zsign):
        socket = (0.24, 0.16, 0.10 * zsign)
        elbow = (0.78, 0.46, 0.46 * zsign)
        tip = (1.35, 0.16, 0.72 * zsign)
        node = b.add_node("Antenna" + side, head, translation=socket)
        b.rod("Scape" + side, node, (0, 0, 0), ant.sub(elbow, socket), 0.045, 0.034, CUTICLE)
        b.rod("Funiculus" + side, node, ant.sub(elbow, socket), ant.sub(tip, socket), 0.032, 0.018, CUTICLE)

    antenna("L", 1.0)
    antenna("R", -1.0)

    thorax_c = (6.15, 1.02, 0.0)
    thorax = b.add_node("Thorax", root, translation=thorax_c)
    b.sphere_node("ThoraxShell", thorax, (0, 0, 0), (0.50, 0.38, 0.44), CUTICLE)

    def leg(side, socket, joints, radii):
        name = "Leg" + side
        local_socket = ant.sub(socket, thorax_c)
        node = b.add_node(name, thorax, translation=local_socket, material=LEG)
        cursor = socket
        labels = ("Coxa", "Femur", "Tarsus")
        for i, point in enumerate(joints):
            b.rod(
                labels[i] + side, node,
                ant.sub(cursor, socket), ant.sub(point, socket),
                radii[i], radii[i + 1], LEG,
            )
            cursor = point

    thin = (0.055, 0.040, 0.026, 0.016)
    leg("L1", (6.35, 0.70, 0.36), [
        (6.70, 0.48, 0.78),
        (6.95, 0.18, 1.15),
        (6.85, 0.00, 1.48),
    ], thin)
    leg("R1", (6.35, 0.70, -0.36), [
        (6.70, 0.48, -0.78),
        (6.95, 0.18, -1.15),
        (6.85, 0.00, -1.48),
    ], thin)
    leg("L2", (6.10, 0.66, 0.40), [
        (6.05, 0.42, 0.92),
        (5.95, 0.16, 1.32),
        (5.85, 0.00, 1.62),
    ], thin)
    leg("R2", (6.10, 0.66, -0.40), [
        (6.05, 0.42, -0.92),
        (5.95, 0.16, -1.32),
        (5.85, 0.00, -1.62),
    ], thin)
    leg("L3", (5.82, 0.68, 0.34), [
        (5.40, 0.46, 0.78),
        (5.05, 0.16, 1.12),
        (4.85, 0.00, 1.38),
    ], thin)
    leg("R3", (5.82, 0.68, -0.34), [
        (5.40, 0.46, -0.78),
        (5.05, 0.16, -1.12),
        (4.85, 0.00, -1.38),
    ], thin)

    abdomen_c = (4.75, 1.18, 0.0)
    abdomen = b.add_node("Abdomen", root, translation=abdomen_c)
    profile = [
        (-1.58, 0.05),
        (-1.28, 0.46),
        (-0.92, 0.86),
        (-0.48, 1.08),
        (0.02, 0.96),
        (0.52, 0.70),
        (0.96, 0.52),
        (1.35, 0.42),
        (1.62, 0.36),
    ]
    b.add_node(
        "AbdomenShell", abdomen,
        mesh=revolved_pear(profile, 12, 0.82, 0.98),
        material=CUTICLE,
    )

    def cornicle(side, zsign):
        base = (-0.92, 0.70, 0.32 * zsign)
        direction = ant.norm((-math.cos(math.radians(40.0)), math.sin(math.radians(40.0)), 0.16 * zsign))
        tip = ant.add(base, ant.mul(direction, 1.45))
        node = b.add_node("Cornicle" + side, abdomen, translation=base)
        b.rod("CornicleTube" + side, node, (0, 0, 0), ant.sub(tip, base), 0.11, 0.055, CUTICLE)

    cornicle("L", 1.0)
    cornicle("R", -1.0)

    cauda_base = (-1.42, 0.02, 0.0)
    cauda = b.add_node("Cauda", abdomen, translation=cauda_base)
    b.rod("CaudaTip", cauda, (0, 0, 0), (-0.48, -0.08, 0.0), 0.10, 0.035, CUTICLE)
    return b


def pack_glb(builder):
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
        nodes.append(out)

    gltf = {
        "asset": {"version": "2.0", "generator": "myants make_green_aphid_glb.py"},
        "scene": 0,
        "scenes": [{"name": "Aphid", "nodes": [0]}],
        "nodes": nodes,
        "meshes": gltf_meshes,
        "materials": [
            {
                "name": "cuticle",
                "pbrMetallicRoughness": {
                    "baseColorFactor": [0.50, 0.78, 0.26, 1.0],
                    "metallicFactor": 0.02,
                    "roughnessFactor": 0.42,
                },
            },
            {
                "name": "eye",
                "pbrMetallicRoughness": {
                    "baseColorFactor": [0.42, 0.09, 0.06, 1.0],
                    "metallicFactor": 0.02,
                    "roughnessFactor": 0.22,
                },
            },
            {
                "name": "leg",
                "pbrMetallicRoughness": {
                    "baseColorFactor": [0.36, 0.50, 0.20, 1.0],
                    "metallicFactor": 0.02,
                    "roughnessFactor": 0.55,
                },
            },
        ],
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


def points_under(builder, world, parent, root_name, skip_prefixes=()):
    by_name = {}
    for i, node in enumerate(builder.nodes):
        by_name.setdefault(node["name"], i)
    root = by_name[root_name]
    points = []
    for i, node in enumerate(builder.nodes):
        if node["mesh"] is None:
            continue
        if any(node["name"].startswith(prefix) for prefix in skip_prefixes):
            continue
        seen = i
        under = False
        while seen != -1:
            if seen == root:
                under = True
                break
            if any(builder.nodes[seen]["name"].startswith(prefix) for prefix in skip_prefixes):
                break
            seen = parent[seen]
        if not under:
            continue
        mesh = builder.meshes[node["mesh"]]
        for p in mesh.positions:
            points.append(ant.transform_point(world[i], p))
    return points


def check(builder):
    world, parent = ant.world_matrices(builder)
    by_name = {}
    for i, node in enumerate(builder.nodes):
        by_name.setdefault(node["name"], i)

    def origin(name):
        m = world[by_name[name]]
        return (m[12], m[13], m[14])

    head = origin("Head")
    abdomen = origin("Abdomen")
    if not head[0] > abdomen[0]:
        raise SystemExit("head is not in front of the abdomen")
    for side in ("L", "R"):
        corn = origin("Cornicle" + side)
        if not corn[0] < abdomen[0]:
            raise SystemExit("Cornicle%s is not behind the abdomen" % side)
        if not corn[1] > abdomen[1]:
            raise SystemExit("Cornicle%s is not above the abdomen" % side)
    for name in ("LegL1", "LegR1", "LegL2", "LegR2", "LegL3", "LegR3"):
        seen = parent[by_name[name]]
        names = []
        while seen != -1:
            names.append(builder.nodes[seen]["name"])
            seen = parent[seen]
        if "Thorax" not in names:
            raise SystemExit("%s parent chain %s misses Thorax" % (name, names))
    cauda = origin("Cauda")
    left = origin("CornicleL")
    right = origin("CornicleR")
    if not (min(left[2], right[2]) < cauda[2] < max(left[2], right[2])):
        raise SystemExit("cauda is not between the cornicles")
    if not cauda[0] < min(left[0], right[0]):
        raise SystemExit("cauda is not behind the cornicles")
    if not cauda[1] < min(left[1], right[1]):
        raise SystemExit("cauda is not below the cornicles")

    shell = points_under(builder, world, parent, "Abdomen", ("Cornicle", "Cauda"))
    rear = min(p[0] for p in shell)
    for side in ("L", "R"):
        tube = points_under(builder, world, parent, "Cornicle" + side)
        tip = min(p[0] for p in tube)
        if not tip < rear - 0.02:
            raise SystemExit("Cornicle%s tip %.3f does not clear abdomen rear %.3f" % (side, tip, rear))

    for name in ("LegL1", "LegR1", "LegL2", "LegR2", "LegL3", "LegR3"):
        pts = points_under(builder, world, parent, name)
        foot = min(pts, key=lambda p: p[1])
        sock = origin(name)
        if abs(foot[1]) > 0.04:
            raise SystemExit("%s foot y %.3f" % (name, foot[1]))
        if abs(foot[2]) <= abs(sock[2]):
            raise SystemExit("%s foot is not outside its socket" % name)

    tris = sum(len(mesh.indices) // 3 for mesh in builder.meshes)
    if not 700 <= tris <= 2200:
        raise SystemExit("tris %s outside 700..2200" % tris)
    print("nodes", len(builder.nodes), "tris", tris)
    return world, parent


def preview(builder, world, path):
    parts = []
    for index, node in enumerate(builder.nodes):
        if node["mesh"] is None:
            continue
        mesh = builder.meshes[node["mesh"]]
        m = world[index]
        verts = [ant.transform_point(m, p) for p in mesh.positions]
        name = node["name"]
        if node["material"] == EYE:
            color = (120, 28, 22)
        elif node["material"] == LEG:
            color = (92, 128, 48)
        elif name.startswith("Cornicle") or name.startswith("Cauda"):
            color = (70, 150, 40)
        else:
            color = (128, 196, 64)
        parts.append((verts, mesh.indices, color))

    def paint(project, x0, y0, x1, y1, width, height):
        image = [[(16, 22, 18) for _ in range(width)] for _ in range(height)]
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
                            shade = 0.62 + 0.38 * ((x + y) % 7) / 6.0
                            image[y][x] = tuple(max(0, min(255, int(c * shade))) for c in color)
        return image

    side = paint(lambda p: (p[0], p[1]), -0.62, -0.08, 0.62, 0.62, 520, 280)
    top = paint(lambda p: (p[0], p[2]), -0.62, -0.42, 0.62, 0.42, 520, 300)
    gap = 8
    width = 520
    height = 280 + gap + 300
    rows = []
    for y in range(280):
        row = bytearray()
        for x in range(width):
            row.extend(side[y][x])
        rows.append(row)
    for _ in range(gap):
        rows.append(bytes((12, 16, 12)) * width)
    for y in range(300):
        row = bytearray()
        for x in range(width):
            row.extend(top[y][x])
        rows.append(row)
    ant._write_png(path, width, height, rows)


def main():
    builder = build()
    ant.fit_root(builder)
    world, _ = check(builder)
    data, _ = pack_glb(builder)
    out = Path(__file__).resolve().parents[1] / "koolApp" / "assets" / "models" / "insects" / "aphid_green.glb"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(data)
    preview_path = Path("/tmp/aphid_green.png")
    preview(builder, world, preview_path)
    print("wrote", out, "bytes", len(data))
    print("preview", preview_path)


if __name__ == "__main__":
    main()
