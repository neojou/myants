#!/usr/bin/env python3
"""Original white-clover tuft and flower. Not a trace of the reference photo.

The photo is a shape reference only: low trifoliate leaves, and a white
flower head on a short stalk. Local +Y is up. The tuft spans about 1 across
and sits on y = 0. The flower uses the same scale, with its head above the leaves.
Run from anywhere: the .glb files land in koolApp/assets/models/nature/.
"""

import importlib.util
import math
from pathlib import Path

_ant_path = Path(__file__).with_name("make_black_ant_glb.py")
_spec = importlib.util.spec_from_file_location("make_black_ant_glb", _ant_path)
ant = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(ant)
_palette_spec = importlib.util.spec_from_file_location("glb_palette", Path(__file__).resolve().parent / "glb_palette.py")
palette = importlib.util.module_from_spec(_palette_spec)
_palette_spec.loader.exec_module(palette)

TAU = math.pi * 2.0


def add_ellipse(pos, nrm, idx, cx, cy, cz, rx, rz, yaw, lift):
    c = math.cos(yaw)
    s = math.sin(yaw)

    def put(x, y, z):
        pos.append((cx + c * x - s * z, cy + y, cz + s * x + c * z))
        nrm.append((0.0, 1.0, 0.0))

    base = len(pos)
    put(0.0, lift * 0.35, 0.0)
    n = 6
    for i in range(n):
        a = TAU * i / n
        put(rx * math.cos(a), lift * max(0.0, math.cos(a)), rz * math.sin(a))
        # Clockwise in the loop would face -Y. The camera looks down, so the front is +Y.
        idx.extend((base, base + 1 + ((i + 1) % n), base + 1 + i))


def add_leaf(pos, nrm, idx, yaw, reach):
    # One petiole and three oval leaflets, the clover shape seen from above.
    add_ellipse(pos, nrm, idx, reach * 0.62, 0.02, 0.0, 0.16, 0.09, yaw, 0.05)
    side = 0.95
    add_ellipse(pos, nrm, idx, reach * 0.38, 0.015, reach * 0.16, 0.11, 0.07, yaw + side, 0.035)
    add_ellipse(pos, nrm, idx, reach * 0.38, 0.015, -reach * 0.16, 0.11, 0.07, yaw - side, 0.035)


def tuft_mesh():
    pos = []
    nrm = []
    idx = []
    for i in range(6):
        yaw = TAU * i / 6.0
        reach = 0.42 if i % 2 == 0 else 0.34
        add_leaf(pos, nrm, idx, yaw, reach)
    return fit_ground(ant.Mesh(pos, nrm, idx))


def flower_mesh():
    pos = []
    nrm = []
    idx = []
    sides = 5
    stalk_h = 0.22
    stalk_r = 0.018
    for i in range(sides):
        a0 = TAU * i / sides
        a1 = TAU * (i + 1) / sides
        ring = [
            (math.cos(a0) * stalk_r, 0.0, math.sin(a0) * stalk_r),
            (math.cos(a1) * stalk_r, 0.0, math.sin(a1) * stalk_r),
            (math.cos(a1) * stalk_r, stalk_h, math.sin(a1) * stalk_r),
            (math.cos(a0) * stalk_r, stalk_h, math.sin(a0) * stalk_r),
        ]
        base = len(pos)
        pos.extend(ring)
        nrm.extend((ant.norm((p[0], 0.0, p[2])) for p in ring))
        idx.extend((base, base + 2, base + 1, base, base + 3, base + 2))
    ant.STACKS = 4
    ant.SLICES = 8
    head = ant.uv_sphere()
    base = len(pos)
    # Wide enough that the white head still reads when the whole tuft is a few pixels across.
    radius = 0.22
    center_y = 0.40
    for p, _stored in zip(head.positions, head.normals):
        pos.append((p[0] * radius, center_y + p[1] * radius, p[2] * radius))
        # The shared sphere stores normals that can point inward. Light the outside.
        nrm.append(ant.norm(p))
    idx.extend(i + base for i in head.indices)
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


def pack(mesh, node_name, file_name):
    b = ant.Builder()
    root = b.add_node(node_name, None)
    b.add_node(node_name + "Mesh", root, mesh=mesh, material=ant.CHITIN)
    data, _ = ant.pack_glb(b, generator="myants make_clover_glb.py", materials=palette.MATERIALS["nature/" + file_name])
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
    tuft = pack(tuft_mesh(), "CloverTuft", "clover_tuft.glb")
    flower = pack(flower_mesh(), "CloverFlower", "clover_flower.glb")
    tuft_top = max(p[1] for p in tuft.positions)
    flower_top = max(p[1] for p in flower.positions)
    if flower_top <= tuft_top:
        raise SystemExit("flower head is not above the leaves")


if __name__ == "__main__":
    main()
