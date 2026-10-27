# SPDX-FileCopyrightText: 2026 5thlayer
# SPDX-License-Identifier: LGPL-3.0-only
"""Generates Umbra Armour's worn layers, the Pillar's own, in Umbra's palette; scripts/build-textures.py
builds Umbra Gear's item icons from Malcolm Riley's art:

- entity/equipment/humanoid/umbra.png: the helmet, chestplate and boots on the 64x32 armour layer.
- entity/equipment/humanoid_leggings/umbra.png: the leggings on theirs.

Nothing of Umbra's is bright: its lightest shade is a dim violet, so nothing made of it reads as giving light.

Run from the repo root: python3 tools/textures/umbra_gear.py
"""
import random
from PIL import Image

OUT = "src/main/resources/assets/atouchofmadness/textures/"

# Umbra's own palette, from item/umbra.png, darkest first, less the darkest, which only outlines an icon.
PALETTE = {
    "d": (22, 12, 32, 255),
    "m": (36, 20, 52, 255),
    "l": (58, 34, 84, 255),
    "h": (96, 64, 132, 255),
}


def box(u, v, w, h, d):
    """The faces of a box at (u, v) on a texture, by vanilla's layout: each as (x, y, width, height)."""
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + d + w + d, v + d, w, h),
    }


HEAD = box(0, 0, 8, 8, 8)
BODY = box(16, 16, 8, 12, 4)
ARM = box(40, 16, 4, 12, 4)
LEG = box(0, 16, 4, 12, 4)


def plate(pixels, rng, face, rows=None, cols=None):
    """Fills a face, or only its rows (from its top) and columns (from its left), with Umbra's plate."""
    x0, y0, w, h = face
    rows = range(h) if rows is None else rows
    cols = range(w) if cols is None else cols
    for dy in rows:
        for dx in cols:
            key = rng.choices("dml", weights=(3, 5, 2))[0]
            # A dim trim along each plate's top edge.
            if dy == rows[0] and h > 1:
                key = "h" if rng.random() < 0.6 else "l"
            pixels[x0 + dx, y0 + dy] = PALETTE[key]


def humanoid(seed=13):
    rng = random.Random(seed)
    image = Image.new("RGBA", (64, 32))
    pixels = image.load()
    # The helmet: the head's top, sides and back, and the front's brow and cheeks, leaving the face open.
    for name in ("top", "back", "right", "left"):
        plate(pixels, rng, HEAD[name])
    plate(pixels, rng, HEAD["front"], rows=range(0, 3))
    plate(pixels, rng, HEAD["front"], rows=range(3, 7), cols=[0, 7])
    # The chestplate: the body whole, and the arms' shoulders.
    for face in BODY.values():
        plate(pixels, rng, face)
    plate(pixels, rng, ARM["top"])
    for name in ("right", "front", "left", "back"):
        plate(pixels, rng, ARM[name], rows=range(0, 5))
    # The boots: the bottom of each leg.
    plate(pixels, rng, LEG["bottom"])
    for name in ("right", "front", "left", "back"):
        plate(pixels, rng, LEG[name], rows=range(8, 12))
    image.save(OUT + "entity/equipment/humanoid/umbra.png")


def leggings(seed=31):
    rng = random.Random(seed)
    image = Image.new("RGBA", (64, 32))
    pixels = image.load()
    # The waist, at the bottom of the body, and each leg down to the boots.
    plate(pixels, rng, BODY["bottom"])
    for name in ("right", "front", "left", "back"):
        plate(pixels, rng, BODY[name], rows=range(8, 12))
    plate(pixels, rng, LEG["top"])
    for name in ("right", "front", "left", "back"):
        plate(pixels, rng, LEG[name], rows=range(0, 9))
    image.save(OUT + "entity/equipment/humanoid_leggings/umbra.png")


if __name__ == "__main__":
    humanoid()
    leggings()
