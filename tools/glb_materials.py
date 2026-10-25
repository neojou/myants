"""Write or check the PBR materials of the outdoor .glb files.

    python3 tools/glb_materials.py          # rewrite the JSON chunk, keep the BIN chunk byte for byte
    python3 tools/glb_materials.py --check  # fail unless every file matches glb_palette.MATERIALS

Rewriting only JSON keeps the committed geometry untouched (re-running the
make_*_glb.py generators also changes normals on some files; see
docs/programming/assets_gltf_pipeline.md). beetle.glb is a Blender export,
so this is also how it gets its material.
"""
import importlib.util
import json
import struct
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "koolApp" / "assets" / "models"
_spec = importlib.util.spec_from_file_location("glb_palette", Path(__file__).resolve().parent / "glb_palette.py")
palette = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(palette)


def read(path):
    data = path.read_bytes()
    magic, version, _length = struct.unpack_from("<III", data, 0)
    assert magic == 0x46546C67 and version == 2, path
    jlen, jtype = struct.unpack_from("<II", data, 12)
    assert jtype == 0x4E4F534A, path
    gltf = json.loads(data[20:20 + jlen])
    return gltf, data[20 + jlen:]


def write(path, gltf, rest):
    chunk = json.dumps(gltf, separators=(",", ":")).encode("utf-8")
    while len(chunk) % 4:
        chunk += b" "
    body = struct.pack("<II", len(chunk), 0x4E4F534A) + chunk + rest
    path.write_bytes(struct.pack("<III", 0x46546C67, 2, 12 + len(body)) + body)


def problems(rel, gltf):
    want = palette.MATERIALS[rel]
    out = []
    if gltf.get("materials") != want:
        out.append("materials %s != %s" % ([m["name"] for m in gltf.get("materials", [])], [m["name"] for m in want]))
    for mesh in gltf["meshes"]:
        for prim in mesh["primitives"]:
            m = prim.get("material")
            if m is None or not 0 <= m < len(want):
                out.append("mesh %s material %s" % (mesh["name"], m))
    return out


def main():
    check = "--check" in sys.argv[1:]
    bad = 0
    for rel, want in palette.MATERIALS.items():
        path = ROOT / rel
        gltf, rest = read(path)
        if not check:
            gltf["materials"] = want
            for mesh in gltf["meshes"]:
                for prim in mesh["primitives"]:
                    if prim.get("material") is None or prim["material"] >= len(want):
                        prim["material"] = 0
            write(path, gltf, rest)
            assert read(path)[1] == rest
        found = problems(rel, read(path)[0])
        bad += bool(found)
        names = ", ".join(
            "%s %s r%.2f" % (m["name"], [round(c, 3) for c in m["pbrMetallicRoughness"]["baseColorFactor"][:3]],
                             m["pbrMetallicRoughness"]["roughnessFactor"])
            for m in read(path)[0].get("materials", [])
        )
        print("%-26s %s %s" % (rel, "FAIL " + "; ".join(found) if found else "ok", names))
    if bad:
        raise SystemExit("%d file(s) with wrong materials" % bad)


if __name__ == "__main__":
    main()
