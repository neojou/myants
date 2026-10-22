#!/usr/bin/env python3
"""Original sugar and protein morsels. Centered, longest side 1. Not from Sketchfab."""

import importlib.util
import math
from pathlib import Path

_ant_path = Path(__file__).with_name("make_black_ant_glb.py")
_spec = importlib.util.spec_from_file_location("make_black_ant_glb", _ant_path)
ant = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(ant)


def octahedron():
    s = 0.5
    pos = [(s, 0, 0), (-s, 0, 0), (0, s, 0), (0, -s, 0), (0, 0, s), (0, 0, -s)]
    faces = [
        (0, 2, 4), (0, 4, 3), (0, 3, 5), (0, 5, 2),
        (1, 4, 2), (1, 3, 4), (1, 5, 3), (1, 2, 5),
    ]
    nrm = []
    idx = []
    verts = []
    for a, b, c in faces:
        p0, p1, p2 = pos[a], pos[b], pos[c]
        n = ant.norm(ant.cross(ant.sub(p1, p0), ant.sub(p2, p0)))
        base = len(verts)
        verts.extend((p0, p1, p2))
        nrm.extend((n, n, n))
        idx.extend((base, base + 1, base + 2))
    return ant.Mesh(verts, nrm, idx)


def lump(seed):
    """A few fat spheres, low resolution, merged by overlapping."""
    ant.STACKS = 4
    ant.SLICES = 6
    sphere = ant.uv_sphere()
    b = ant.Builder()
    root = b.add_node("Protein", None)
    spots = (
        ((0.0, 0.0, 0.0), (0.55, 0.42, 0.48)),
        ((0.28, 0.08, 0.12), (0.32, 0.28, 0.30)),
        ((-0.22, 0.05, -0.16), (0.30, 0.26, 0.28)),
        ((0.05, -0.12, 0.18), (0.24, 0.20, 0.22)),
    )
    for i, (center, radii) in enumerate(spots):
        b.add_node(
            "Lump%d" % i, root, translation=center, scale=radii, mesh=sphere, material=ant.CHITIN,
        )
    return b


def sugar():
    b = ant.Builder()
    root = b.add_node("Sugar", None)
    crystals = (
        ((0.00, 0.00, 0.00), (0.55, 0.34, 0.28), (0.0, 0.0, 0.15, 0.99)),
        ((0.22, 0.08, 0.12), (0.28, 0.18, 0.16), (0.1, 0.2, 0.3, 0.93)),
        ((-0.18, -0.04, 0.10), (0.24, 0.16, 0.14), (0.0, 0.4, 0.1, 0.91)),
        ((0.06, 0.12, -0.16), (0.22, 0.20, 0.12), (0.3, 0.1, 0.2, 0.93)),
        ((-0.08, -0.10, -0.12), (0.18, 0.14, 0.12), (0.2, 0.3, 0.0, 0.93)),
    )
    mesh = octahedron()
    for i, (center, scale, rotation) in enumerate(crystals):
        b.add_node(
            "Crystal%d" % i, root,
            translation=center, rotation=rotation, scale=scale, mesh=mesh, material=ant.CHITIN,
        )
    return b


def center_unit(builder):
    world, _ = ant.world_matrices(builder)
    points = []
    for index, node in enumerate(builder.nodes):
        if node["mesh"] is None:
            continue
        mesh = builder.meshes[node["mesh"]]
        for p in mesh.positions:
            points.append(ant.transform_point(world[index], p))
    xs = [p[0] for p in points]
    ys = [p[1] for p in points]
    zs = [p[2] for p in points]
    span = max(max(xs) - min(xs), max(ys) - min(ys), max(zs) - min(zs))
    scale = 1.0 / span
    cx = (max(xs) + min(xs)) * 0.5
    cy = (max(ys) + min(ys)) * 0.5
    cz = (max(zs) + min(zs)) * 0.5
    builder.nodes[0]["translation"] = (-scale * cx, -scale * cy, -scale * cz)
    builder.nodes[0]["scale"] = (scale, scale, scale)


def write(builder, name):
    center_unit(builder)
    data, _ = ant.pack_glb(builder, generator="myants make_food_glb.py")
    out = Path(__file__).resolve().parents[1] / "koolApp" / "assets" / "models" / "food" / name
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(data)
    tris = sum(len(mesh.indices) // 3 for mesh in builder.meshes)
    print("wrote", out, "tris", tris, "bytes", len(data))


def main():
    write(sugar(), "sugar.glb")
    write(lump(1), "protein.glb")


if __name__ == "__main__":
    main()
