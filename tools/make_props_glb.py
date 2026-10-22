#!/usr/bin/env python3
"""Original pebble, leaf, and clock stone. Not from Sketchfab.

The pebble matches the old unit cube. The leaf matches the old flat slab.
The clock stone matches the old sphere of radius 1, with a lumpy surface.
"""

import importlib.util
import math
from pathlib import Path

_ant_path = Path(__file__).with_name("make_black_ant_glb.py")
_spec = importlib.util.spec_from_file_location("make_black_ant_glb", _ant_path)
ant = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(ant)


def unit(i, salt):
    n = (i * 1103515245 + salt * 12345) & 0x7FFFFFFF
    return (n % 10000) / 10000.0


def noisy_sphere(stacks, slices, radius, rough, salt):
    ant.STACKS = stacks
    ant.SLICES = slices
    base = ant.uv_sphere()
    pos = []
    nrm = []
    for i, p in enumerate(base.positions):
        direction = ant.norm(p)
        wobble = 1.0 + (unit(i, salt) - 0.5) * 2.0 * rough
        pos.append(ant.mul(direction, radius * wobble))
        nrm.append(direction)
    return ant.Mesh(pos, nrm, base.indices)


def leaf_mesh():
    seg = 7
    pos = []
    nrm = []
    for i in range(seg + 1):
        t = i / seg
        x = -0.6 + 1.2 * t
        half = 0.22 * math.sin(math.pi * t)
        y = 0.04 * math.sin(math.pi * t)
        pos.append((x, y, -half))
        pos.append((x, y, half))
        nrm.append((0.0, 1.0, 0.0))
        nrm.append((0.0, 1.0, 0.0))
    idx = []
    for i in range(seg):
        a = i * 2
        b = a + 1
        c = a + 2
        d = a + 3
        idx.extend((a, c, b, b, c, d))
    # A short stem under the near end.
    stem = len(pos)
    pos.extend(((-0.62, -0.02, -0.03), (-0.62, -0.02, 0.03), (-0.78, -0.02, 0.0)))
    nrm.extend(((0.0, 1.0, 0.0),) * 3)
    idx.extend((stem, stem + 1, stem + 2))
    return ant.Mesh(pos, nrm, idx)


def pack(mesh, node_name, file_name):
    b = ant.Builder()
    root = b.add_node(node_name, None)
    b.add_node(node_name + "Mesh", root, mesh=mesh, material=ant.CHITIN)
    data, _ = ant.pack_glb(b, generator="myants make_props_glb.py")
    out = Path(__file__).resolve().parents[1] / "koolApp" / "assets" / "models" / "nature" / file_name
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(data)
    tris = len(mesh.indices) // 3
    xs = [p[0] for p in mesh.positions]
    ys = [p[1] for p in mesh.positions]
    zs = [p[2] for p in mesh.positions]
    print(
        "wrote", out, "tris", tris,
        "span", round(max(xs) - min(xs), 3), round(max(ys) - min(ys), 3), round(max(zs) - min(zs), 3),
    )


def main():
    pack(noisy_sphere(5, 8, 0.5, 0.35, 3), "Pebble", "pebble.glb")
    pack(leaf_mesh(), "Leaf", "leaf.glb")
    pack(noisy_sphere(6, 10, 1.0, 0.16, 9), "ClockStone", "clock_stone.glb")


if __name__ == "__main__":
    main()
