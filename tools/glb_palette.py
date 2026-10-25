"""PBR material tables for the outdoor .glb files.

baseColor matches the color the game paints on each mesh at runtime
(WorldPalette / critterTint in koolApp/.../render3d). The game still
ignores these materials and tints by instance color; they exist so the
file looks right in any glTF viewer. Keep both sides in sync.

Each list is indexed like the builder's material ids: 0 body, 1 eye, 2 mark.
"""


def pbr(name, rgb, roughness, metallic=0.0):
    return {
        "name": name,
        "pbrMetallicRoughness": {
            "baseColorFactor": [rgb[0], rgb[1], rgb[2], 1.0],
            "metallicFactor": metallic,
            "roughnessFactor": roughness,
        },
    }


EYE = (0.05, 0.04, 0.04)

# file path under koolApp/assets/models -> material list
MATERIALS = {
    "food/sugar.glb": [pbr("sugar", (1.0, 0.84, 0.2), 0.35)],           # WorldPalette.sugar3d
    "food/protein.glb": [pbr("protein", (0.8, 0.25, 0.22), 0.7)],       # WorldPalette.protein3d
    "nature/pebble.glb": [pbr("pebble", (0.35, 0.33, 0.3), 0.9)],       # WorldPalette.pebble
    "nature/leaf.glb": [pbr("leaf", (0.45, 0.32, 0.12), 0.8)],          # WorldPalette.leaf
    "nature/clock_stone.glb": [pbr("clock_stone", (0.50, 0.45, 0.38), 0.85)],  # WorldPalette.clockStone
    "nature/clover_tuft.glb": [pbr("clover", (0.22, 0.48, 0.18), 0.65)],      # WorldPalette.simGrass
    "nature/clover_flower.glb": [pbr("clover_flower", (0.95, 0.95, 0.95), 0.6)],  # WorldPalette.petalWhite
    # critterTint(kind): body, eye, mark
    "insects/prey.glb": [pbr("body", (0.35, 0.7, 0.3), 0.55), pbr("eye", EYE, 0.15), pbr("mark", (0.35, 0.7, 0.3), 0.55)],
    "insects/predator.glb": [pbr("body", (0.45, 0.25, 0.55), 0.5), pbr("eye", EYE, 0.15), pbr("mark", (0.45, 0.25, 0.55), 0.5)],
    "insects/spider.glb": [pbr("body", (0.35, 0.35, 0.38), 0.6), pbr("eye", EYE, 0.15), pbr("mark", (0.35, 0.35, 0.38), 0.6)],
    "insects/ladybug.glb": [pbr("shell", (1.0, 0.02, 0.02), 0.3), pbr("eye", EYE, 0.15), pbr("spot", (0.08, 0.04, 0.04), 0.35)],
    "insects/beetle.glb": [pbr("chitin", (0.08, 0.06, 0.05), 0.35)],    # critterTint(BEETLE).body
}
