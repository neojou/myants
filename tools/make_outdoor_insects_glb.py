#!/usr/bin/env python3
"""Original glTF insects for the outdoor world.

Head toward +X, feet on y = 0, length 1 after fit. Not from Sketchfab.
The beetle file is made by tools/make_stag_beetle_glb.py instead of this script.
Run from anywhere. Files land in koolApp/assets/models/insects/.
"""

import importlib.util
from pathlib import Path

_ant_path = Path(__file__).with_name("make_black_ant_glb.py")
_spec = importlib.util.spec_from_file_location("make_black_ant_glb", _ant_path)
ant = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(ant)

CHITIN = ant.CHITIN
EYE = ant.EYE
MARK = ant.MANDIBLE


def sub(a, b):
    return ant.sub(a, b)


def leg(b, thorax, center, name, socket, foot, radius=0.06):
    """One straight leg from socket to a foot on y = 0. Knee lifts the middle."""
    knee = (
        (socket[0] + foot[0]) * 0.5,
        max(socket[1], foot[1]) * 0.55 + 0.08,
        (socket[2] + foot[2]) * 0.5,
    )
    node = b.add_node(name, thorax, translation=sub(socket, center))
    b.rod("Femur" + name, node, (0, 0, 0), sub(knee, socket), radius, radius * 0.75)
    b.rod("Tibia" + name, node, sub(knee, socket), sub(foot, socket), radius * 0.75, radius * 0.4)


def pair(b, thorax, center, index, socket_y, socket_z, foot_x, foot_z, radius=0.06):
    leg(
        b, thorax, center, "LegL" + str(index),
        (foot_x, socket_y, socket_z), (foot_x, 0.0, foot_z), radius,
    )
    leg(
        b, thorax, center, "LegR" + str(index),
        (foot_x, socket_y, -socket_z), (foot_x, 0.0, -foot_z), radius,
    )


def build_prey():
    b = ant.Builder()
    root = b.add_node("Prey", None)
    for i, x in enumerate((0.35, 0.85, 1.35, 1.85)):
        b.sphere_node("Segment%d" % i, root, (x, 0.42, 0.0), (0.34, 0.30, 0.32))
    head = b.add_node("Head", root, translation=(2.40, 0.48, 0.0))
    b.sphere_node("HeadShell", head, (0.0, 0.0, 0.0), (0.36, 0.30, 0.32))
    b.sphere_node("EyeL", head, (0.12, 0.08, 0.16), (0.08, 0.08, 0.06), EYE)
    b.sphere_node("EyeR", head, (0.12, 0.08, -0.16), (0.08, 0.08, 0.06), EYE)
    center = (1.35, 0.42, 0.0)
    thorax = b.add_node("Thorax", root, translation=center)
    pair(b, thorax, center, 1, 0.22, 0.18, 1.90, 0.55, 0.045)
    pair(b, thorax, center, 2, 0.20, 0.20, 1.35, 0.62, 0.045)
    pair(b, thorax, center, 3, 0.20, 0.18, 0.80, 0.55, 0.045)
    return b


def build_predator():
    b = ant.Builder()
    root = b.add_node("Predator", None)
    b.sphere_node("Abdomen", root, (0.55, 0.46, 0.0), (0.85, 0.28, 0.26))
    b.sphere_node("Waist", root, (1.45, 0.50, 0.0), (0.18, 0.12, 0.12))
    center = (2.05, 0.58, 0.0)
    thorax = b.add_node("Thorax", root, translation=center)
    b.sphere_node("ThoraxShell", thorax, (0.0, 0.0, 0.0), (0.42, 0.28, 0.32))
    b.sphere_node("WingL", thorax, (-0.05, 0.16, 0.28), (0.55, 0.02, 0.22))
    b.sphere_node("WingR", thorax, (-0.05, 0.16, -0.28), (0.55, 0.02, 0.22))
    head = b.add_node("Head", root, translation=(2.70, 0.58, 0.0))
    b.sphere_node("HeadShell", head, (0.0, 0.0, 0.0), (0.32, 0.26, 0.28))
    b.sphere_node("EyeL", head, (0.08, 0.06, 0.16), (0.10, 0.10, 0.08), EYE)
    b.sphere_node("EyeR", head, (0.08, 0.06, -0.16), (0.10, 0.10, 0.08), EYE)
    b.rod("JawL", head, (0.20, -0.06, 0.08), (0.55, -0.12, 0.02), 0.05, 0.02, MARK)
    b.rod("JawR", head, (0.20, -0.06, -0.08), (0.55, -0.12, -0.02), 0.05, 0.02, MARK)
    pair(b, thorax, center, 1, 0.05, 0.22, 2.35, 0.70, 0.05)
    pair(b, thorax, center, 2, 0.02, 0.24, 2.05, 0.78, 0.05)
    pair(b, thorax, center, 3, 0.02, 0.22, 1.70, 0.70, 0.05)
    return b


def build_spider():
    b = ant.Builder()
    root = b.add_node("Spider", None)
    b.sphere_node("Abdomen", root, (0.35, 0.55, 0.0), (0.70, 0.55, 0.62))
    center = (1.55, 0.48, 0.0)
    thorax = b.add_node("Thorax", root, translation=center)
    b.sphere_node("Cephalothorax", thorax, (0.0, 0.0, 0.0), (0.48, 0.32, 0.42))
    head = b.add_node("Head", thorax, translation=(0.28, 0.06, 0.0))
    b.sphere_node("EyeL", head, (0.08, 0.10, 0.12), (0.08, 0.08, 0.06), EYE)
    b.sphere_node("EyeR", head, (0.08, 0.10, -0.12), (0.08, 0.08, 0.06), EYE)
    b.sphere_node("EyeL2", head, (0.02, 0.14, 0.05), (0.04, 0.04, 0.04), EYE)
    b.sphere_node("EyeR2", head, (0.02, 0.14, -0.05), (0.04, 0.04, 0.04), EYE)
    # Eight legs. Sockets sit on the cephalothorax; feet splay forward and back.
    splay = (
        (1, 1.78, 0.22, 2.45, 0.95),
        (2, 1.62, 0.28, 1.90, 1.25),
        (3, 1.42, 0.28, 1.05, 1.20),
        (4, 1.28, 0.22, 0.45, 0.90),
    )
    for index, sx, sz, fx, fz in splay:
        leg(b, thorax, center, "LegL" + str(index), (sx, 0.16, sz), (fx, 0.0, fz), 0.045)
        leg(b, thorax, center, "LegR" + str(index), (sx, 0.16, -sz), (fx, 0.0, -fz), 0.045)
    return b


def build_ladybug():
    b = ant.Builder()
    root = b.add_node("Ladybug", None)
    b.sphere_node("Dome", root, (0.70, 0.58, 0.0), (0.85, 0.55, 0.72))
    b.sphere_node("Spot1", root, (0.95, 1.08, 0.26), (0.18, 0.10, 0.16), MARK)
    b.sphere_node("Spot2", root, (0.95, 1.08, -0.26), (0.18, 0.10, 0.16), MARK)
    b.sphere_node("Spot3", root, (0.42, 1.02, 0.22), (0.16, 0.09, 0.14), MARK)
    b.sphere_node("Spot4", root, (0.42, 1.02, -0.22), (0.16, 0.09, 0.14), MARK)
    center = (1.45, 0.42, 0.0)
    thorax = b.add_node("Thorax", root, translation=center)
    b.sphere_node("Pronotum", thorax, (0.15, 0.12, 0.0), (0.28, 0.16, 0.40))
    head = b.add_node("Head", root, translation=(1.95, 0.40, 0.0))
    b.sphere_node("HeadShell", head, (0.0, 0.0, 0.0), (0.26, 0.20, 0.32))
    b.sphere_node("EyeL", head, (0.08, 0.04, 0.14), (0.06, 0.06, 0.05), EYE)
    b.sphere_node("EyeR", head, (0.08, 0.04, -0.14), (0.06, 0.06, 0.05), EYE)
    pair(b, thorax, center, 1, 0.02, 0.22, 1.80, 0.55, 0.04)
    pair(b, thorax, center, 2, 0.00, 0.24, 1.45, 0.62, 0.04)
    pair(b, thorax, center, 3, 0.00, 0.22, 1.05, 0.55, 0.04)
    return b


BUILDERS = (
    ("prey.glb", build_prey, 6),
    ("predator.glb", build_predator, 6),
    ("spider.glb", build_spider, 8),
    ("ladybug.glb", build_ladybug, 6),
)


def check(builder, leg_count):
    world, parent = ant.world_matrices(builder)
    by_name = {}
    for i, node in enumerate(builder.nodes):
        by_name.setdefault(node["name"], i)

    def origin(name):
        m = world[by_name[name]]
        return (m[12], m[13], m[14])

    head = origin("Head")
    rear_name = "Abdomen" if "Abdomen" in by_name else "Elytra" if "Elytra" in by_name else "Dome" if "Dome" in by_name else "Segment0"
    rear = origin(rear_name)
    if not head[0] > rear[0]:
        raise SystemExit("head %s is not in front of %s %s" % (head, rear_name, rear))
    if "Thorax" not in by_name:
        raise SystemExit("missing Thorax")
    tris = sum(len(mesh.indices) // 3 for mesh in builder.meshes)
    if not 700 <= tris <= 2200:
        raise SystemExit("tris %s outside 700..2200" % tris)
    print("tris", tris, "legs", leg_count, "head", tuple(round(v, 3) for v in head))
    return world


def main():
    ant.STACKS = 8
    ant.SLICES = 10
    out_dir = Path(__file__).resolve().parents[1] / "koolApp" / "assets" / "models" / "insects"
    out_dir.mkdir(parents=True, exist_ok=True)
    for name, builder_fn, legs in BUILDERS:
        builder = builder_fn()
        ant.fit_root(builder)
        world = check(builder, legs)
        data, _ = ant.pack_glb(builder, generator="myants make_outdoor_insects_glb.py")
        out = out_dir / name
        out.write_bytes(data)
        preview = Path("/tmp") / name.replace(".glb", ".png")
        ant.preview(builder, world, preview)
        print("wrote", out, "bytes", len(data))


if __name__ == "__main__":
    main()
