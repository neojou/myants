#!/usr/bin/env python3
"""Original white-clover tuft and flower. Not a trace of the reference photo.

The photo is a shape reference only: low trifoliate leaves with notched,
heart-shaped leaflets at 120 degrees, and a floret head on a short stalk. Local +Y is up. The tuft spans about 1 across
and sits on y = 0. The flower uses the same scale, with its head above the leaves.
Run from anywhere: the .glb files land in koolApp/assets/models/nature/.
"""

import importlib.util
import json
import math
import struct
from pathlib import Path

_ant_path = Path(__file__).with_name("make_black_ant_glb.py")
_spec = importlib.util.spec_from_file_location("make_black_ant_glb", _ant_path)
ant = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(ant)

TAU = math.pi * 2.0


def heart_outline(length, width, notch):
    """Obcordate leaflet outline in local (x along the leaflet, z across).

    Point at the petiole (x = 0), widest near the apex, and a notch cut
    back to `notch` of the length at the apex, like a real clover leaflet.
    Returns points counter-clockwise when seen from +Y.
    """
    half = []
    steps = 5
    for i in range(1, steps + 1):
        t = i / steps  # 0 at the base point, 1 at the lobe tip next to the notch
        # Width swells fast, then rounds over into the lobe.
        x = length * (0.08 + 0.92 * math.sin(t * math.pi * 0.5))
        z = width * 0.5 * math.sin(t * math.pi * 0.86)
        half.append((x, z))
    right = [(0.0, 0.0)] + half
    left = [(x, -z) for x, z in reversed(half)]
    # Apex notch between the two lobes.
    return right + [(length * notch, 0.0)] + left


def add_leaflet(pos, nrm, idx, yaw, length, width, notch, cup, y0):
    c = math.cos(yaw)
    s = math.sin(yaw)

    def world(x, z):
        # Lobes lift a little so the leaflet reads as a shallow cup, not a decal.
        y = y0 + cup * (x / length) ** 2
        return (c * x - s * z, y, s * x + c * z)

    outline = heart_outline(length, width, notch)
    hub = world(length * 0.42, 0.0)
    ring = [world(x, z) for x, z in outline]
    n = len(ring)
    for i in range(n):
        a = ring[i]
        b = ring[(i + 1) % n]
        # Flat-shaded triangle, wound so the face points up at the camera.
        tri = (hub, b, a)
        normal = ant.norm(ant.cross(ant.sub(tri[1], tri[0]), ant.sub(tri[2], tri[0])))
        if normal[1] < 0:
            tri = (hub, a, b)
            normal = ant.mul(normal, -1.0)
        base = len(pos)
        pos.extend(tri)
        nrm.extend((normal, normal, normal))
        idx.extend((base, base + 1, base + 2))


def add_leaf(pos, nrm, idx, yaw, reach, y0):
    # Three heart-shaped leaflets at 120 degrees around the petiole tip.
    for k in range(3):
        add_leaflet(pos, nrm, idx, yaw + TAU * k / 3.0, reach, reach * 0.95, 0.66, reach * 0.18, y0)


def tuft_mesh():
    pos = []
    nrm = []
    idx = []
    # Four trifoliate leaves; offsets keep them overlapping like a real patch.
    leaves = [
        (0.00, 0.00, 0.10, 0.30, 0.030),
        (0.40, 0.16, 1.30, 0.24, 0.015),
        (-0.34, 0.26, 2.40, 0.23, 0.010),
        (0.06, -0.40, 0.75, 0.25, 0.020),
    ]
    for cx, cz, yaw, reach, y0 in leaves:
        start = len(pos)
        add_leaf(pos, nrm, idx, yaw, reach, y0)
        for i in range(start, len(pos)):
            p = pos[i]
            pos[i] = (p[0] + cx, p[1], p[2] + cz)
    return fit_ground(ant.Mesh(pos, nrm, idx))


def flat_tri(pos, nrm, idx, a, b, c, up_hint):
    normal = ant.norm(ant.cross(ant.sub(b, a), ant.sub(c, a)))
    if ant.dot(normal, up_hint) < 0:
        b, c = c, b
        normal = ant.mul(normal, -1.0)
    base = len(pos)
    pos.extend((a, b, c))
    nrm.extend((normal, normal, normal))
    idx.extend((base, base + 1, base + 2))


def flower_mesh():
    """Low-poly white-clover bloom: a crown of pointed florets, not a smooth ball.

    A square stalk, a lower ring of florets bent outward and down, an upper
    ring angled up, and a small faceted cap. Colour comes from the runtime
    (rose / gold / lilac per spot, see cloverPetal in render3d).
    """
    pos = []
    nrm = []
    idx = []
    stalk_h = 0.30
    stalk_r = 0.02
    sides = 4
    for i in range(sides):
        a0 = TAU * i / sides
        a1 = TAU * (i + 1) / sides
        p0 = (math.cos(a0) * stalk_r, 0.0, math.sin(a0) * stalk_r)
        p1 = (math.cos(a1) * stalk_r, 0.0, math.sin(a1) * stalk_r)
        p2 = (p1[0], stalk_h, p1[2])
        p3 = (p0[0], stalk_h, p0[2])
        out = (math.cos((a0 + a1) / 2), 0.0, math.sin((a0 + a1) / 2))
        flat_tri(pos, nrm, idx, p0, p1, p2, out)
        flat_tri(pos, nrm, idx, p0, p2, p3, out)

    cy = 0.40
    # (count, radius at base, tip reach, tip height, base height, phase)
    rings = [
        (8, 0.09, 0.19, cy - 0.05, cy - 0.01, 0.0),
        (8, 0.07, 0.14, cy + 0.07, cy + 0.03, 0.5),
    ]
    for count, r0, reach, tip_y, base_y, phase in rings:
        half = TAU / count * 0.55
        for i in range(count):
            a = TAU * (i + phase) / count
            left = (math.cos(a - half) * r0, base_y, math.sin(a - half) * r0)
            right = (math.cos(a + half) * r0, base_y, math.sin(a + half) * r0)
            tip = (math.cos(a) * reach, tip_y, math.sin(a) * reach)
            mid = (math.cos(a) * r0 * 0.6, base_y + 0.035, math.sin(a) * r0 * 0.6)
            # Two faces per floret: a shallow ridge so it catches light from above.
            flat_tri(pos, nrm, idx, left, tip, mid, (math.cos(a) * 0.3, 1.0, math.sin(a) * 0.3))
            flat_tri(pos, nrm, idx, mid, tip, right, (math.cos(a) * 0.3, 1.0, math.sin(a) * 0.3))

    cap_n = 6
    top = (0.0, cy + 0.12, 0.0)
    for i in range(cap_n):
        a0 = TAU * i / cap_n
        a1 = TAU * (i + 1) / cap_n
        p0 = (math.cos(a0) * 0.07, cy + 0.06, math.sin(a0) * 0.07)
        p1 = (math.cos(a1) * 0.07, cy + 0.06, math.sin(a1) * 0.07)
        flat_tri(pos, nrm, idx, top, p0, p1, (0.0, 1.0, 0.0))
    return ant.Mesh(pos, nrm, idx)


def fit_ground(mesh):
    xs = [p[0] for p in mesh.positions]
    zs = [p[2] for p in mesh.positions]
    ys = [p[1] for p in mesh.positions]
    span = max(max(xs) - min(xs), max(zs) - min(zs))
    scale = 1.0 / span
    cx = (max(xs) + min(xs)) * 0.5
    cz = (max(zs) + min(zs)) * 0.5
    floor = min(ys)
    pos = [((p[0] - cx) * scale, p[1] - floor, (p[2] - cz) * scale) for p in mesh.positions]
    return ant.Mesh(pos, mesh.normals, mesh.indices)


# Clover's own materials. The game ignores glb materials and tints by
# WorldPalette (grass, and cloverPetal for blooms); these keep glTF viewers honest.
CLOVER_LEAF = {
    "name": "clover",
    "pbrMetallicRoughness": {"baseColorFactor": [0.22, 0.48, 0.18, 1.0], "metallicFactor": 0.0, "roughnessFactor": 0.65},
}
CLOVER_FLOWER = {
    "name": "clover_flower",
    "pbrMetallicRoughness": {"baseColorFactor": [0.85, 0.35, 0.45, 1.0], "metallicFactor": 0.0, "roughnessFactor": 0.6},
}


def with_material(data, material):
    """Swap the ant materials pack_glb writes for one clover material. BIN is untouched."""
    json_len = struct.unpack_from("<I", data, 12)[0]
    gltf = json.loads(data[20:20 + json_len])
    gltf["materials"] = [material]
    gltf["scenes"][0]["name"] = "Clover"
    for mesh in gltf["meshes"]:
        for prim in mesh["primitives"]:
            prim["material"] = 0
    bin_chunk = data[20 + json_len:]
    json_chunk = json.dumps(gltf, separators=(",", ":")).encode("utf-8")
    while len(json_chunk) % 4:
        json_chunk += b" "
    body = struct.pack("<II", len(json_chunk), 0x4E4F534A) + json_chunk + bin_chunk
    return struct.pack("<III", 0x46546C67, 2, 12 + len(body)) + body


def pack(mesh, node_name, file_name, material):
    b = ant.Builder()
    root = b.add_node(node_name, None)
    b.add_node(node_name + "Mesh", root, mesh=mesh, material=0)
    data, _ = ant.pack_glb(b, generator="myants make_clover_glb.py")
    data = with_material(data, material)
    out = Path(__file__).resolve().parents[1] / "koolApp" / "assets" / "models" / "nature" / file_name
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(data)
    bounds(mesh, file_name)
    return mesh


def bounds(mesh, label):
    xs = [p[0] for p in mesh.positions]
    ys = [p[1] for p in mesh.positions]
    zs = [p[2] for p in mesh.positions]
    print(
        label,
        "tris", len(mesh.indices) // 3,
        "span", round(max(xs) - min(xs), 3), round(max(ys) - min(ys), 3), round(max(zs) - min(zs), 3),
        "ymin", round(min(ys), 4),
        "ymax", round(max(ys), 3),
    )


def main():
    tuft = pack(tuft_mesh(), "CloverTuft", "clover_tuft.glb", CLOVER_LEAF)
    flower = pack(flower_mesh(), "CloverFlower", "clover_flower.glb", CLOVER_FLOWER)
    tuft_top = max(p[1] for p in tuft.positions)
    flower_top = max(p[1] for p in flower.positions)
    if flower_top <= tuft_top:
        raise SystemExit("flower head is not above the leaves")


if __name__ == "__main__":
    main()
