#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 5thlayer
# SPDX-License-Identifier: LGPL-3.0-only
"""Builds Umbra Gear's item icons from Malcolm Riley's art in data/art/ (NOTICE).

The four pieces of armour are his lead armour, recoloured to Umbra's palette by luminance rank: the set's
distinct shades, those a step or two apart taken as one, sorted, and spread across Umbra's five colours,
darkest to darkest. All four share the one ranking, so a shade is the same colour on each.

The sword and the pickaxe are cast from his moulds: each tool is the mould's cavity, the pixels darker
than its stone, the sword's blade and the whole pickaxe a pixel thinner on the side away from the light.
A cavity's walls are shaded as a hollow is, dark where a solid is lit, so the shading is turned over:
the cavity's darkest pixels become the tool's lightest. The blade or head takes Umbra's four lighter
colours, the grip a handle's browns, and the tool's edge is outlined in Umbra's darkest.

Umbra's colours are item/umbra.png's. Run from the repository root; it needs Pillow. With --check it
writes nothing and fails when a committed texture's pixels differ from what it builds.
"""

import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ART = ROOT / "data" / "art"
ITEMS = ROOT / "src" / "main" / "resources" / "assets" / "atouchofmadness" / "textures" / "item"

# Umbra's colours, darkest first.
UMBRA = [(10, 6, 16), (22, 12, 32), (36, 20, 52), (58, 34, 84), (96, 64, 132)]
# A grip's browns, darkest first.
HANDLE = [(46, 30, 16), (62, 42, 24), (98, 70, 40), (124, 92, 54)]

# Shades of the lead armour this near are one shade.
SAME_SHADE = 4
# A mould's cavity is darker than this; its stone is lighter.
CAVITY_BELOW = 67

ARMOUR = {"helmet": "armor_helmet_lead.png", "chestplate": "armor_chest_lead.png",
          "leggings": "armor_legs_lead.png", "boots": "armor_boots_lead.png"}


def luminance(c):
    return round(0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2])


def art(name):
    return Image.open(ART / name).convert("RGBA")


def cells(image):
    return [(x, y) for y in range(image.height) for x in range(image.width)]


def spread(rank, ranks, palette):
    return palette[round(rank / max(1, ranks - 1) * (len(palette) - 1))] + (255,)


def armour():
    pieces = {name: art(file) for name, file in ARMOUR.items()}
    shades = sorted({luminance(piece.getpixel(cell)) for piece in pieces.values() for cell in cells(piece) if piece.getpixel(cell)[3]})
    # Each shade's rank, counting a shade within SAME_SHADE of the one below it as that one.
    ranks, rank = {}, -1
    for i, shade in enumerate(shades):
        if i == 0 or shade - shades[i - 1] > SAME_SHADE:
            rank += 1
        ranks[shade] = rank
    out = {}
    for name, piece in pieces.items():
        image = Image.new("RGBA", piece.size)
        for cell in cells(piece):
            c = piece.getpixel(cell)
            if c[3]:
                image.putpixel(cell, spread(ranks[luminance(c)], rank + 1, UMBRA))
        out[name] = image
    return out


def on_edge(cell, cavity, sides):
    """Whether the cell is on the cavity's edge on any of these sides, each an (x, y) step."""
    return any((cell[0] + dx, cell[1] + dy) not in cavity for dx, dy in sides)


ALL_SIDES = ((1, 0), (-1, 0), (0, 1), (0, -1))
# Left and down, a pair a mirror across a tool's axis maps onto itself.
LEFT_AND_DOWN = ((-1, 0), (0, 1))


def cast(mould, axis, is_grip, thinned):
    """The tool cast in the mould, symmetric about the diagonal x + y = axis as its handle runs, less the edge
    cells thinned leaves off: its blade or head in Umbra's lighter colours, its grip, where is_grip says, in
    browns, each lit as the cavity's shading turned over, and outlined."""
    cavity = {cell for cell in cells(mould) if luminance(mould.getpixel(cell)) < CAVITY_BELOW}
    tool = {cell for cell in cavity if not thinned(cell, cavity)}
    # Only what its mirror holds too, so a stray pixel of the mould's leaves no lopsided edge.
    tool = {(x, y) for x, y in tool if (axis - y, axis - x) in tool}
    image = Image.new("RGBA", mould.size)
    for part, palette in ((lambda cell: not is_grip(cell), UMBRA[1:]), (is_grip, HANDLE)):
        shades = sorted({luminance(mould.getpixel(cell)) for cell in tool if part(cell)}, reverse=True)
        for cell in tool:
            if part(cell):
                image.putpixel(cell, spread(shades.index(luminance(mould.getpixel(cell))), len(shades), palette))
    for x, y in tool:
        for edge in ((x + dx, y + dy) for dx, dy in ALL_SIDES):
            if 0 <= edge[0] < image.width and 0 <= edge[1] < image.height and edge not in tool:
                image.putpixel(edge, UMBRA[0] + (255,))
    return image


def along(cell):
    """How far along a tool's axis the cell lies, up and right positive."""
    return cell[0] - cell[1]


def sword_thinned(cell, cavity):
    # The blade, above the guard, loses a pixel from each edge; the guard and grip are too thin to.
    return along(cell) >= -1 and on_edge(cell, cavity, ALL_SIDES)


def pickaxe_haft(cell):
    """Whether the cell is the pickaxe's haft, below its head."""
    return along(cell) <= 1


def pickaxe_thinned(cell, cavity):
    # The head loses its inner edge, toward the haft, and the haft both its sides, leaving its middle lane.
    if pickaxe_haft(cell):
        return on_edge(cell, cavity, ALL_SIDES)
    return on_edge(cell, cavity, LEFT_AND_DOWN)


def textures():
    out = {ITEMS / f"umbra_{name}.png": image for name, image in armour().items()}
    # The sword's grip runs from below its guard to the corner, the pickaxe's haft from below its head.
    out[ITEMS / "umbra_sword.png"] = cast(art("cast_sword.png"), 15, lambda cell: along(cell) <= -8, sword_thinned)
    out[ITEMS / "umbra_pickaxe.png"] = cast(art("cast_pickaxe.png"), 16, pickaxe_haft, pickaxe_thinned)
    return out


def main():
    check = "--check" in sys.argv
    stale = 0
    for path, image in textures().items():
        name = path.relative_to(ROOT)
        if check:
            # Pixels, not the file's bytes, which another Pillow may encode differently.
            current = path.is_file() and Image.open(path).convert("RGBA").tobytes() == image.tobytes()
            print(("ok   %s is current" if current else "FAIL %s is stale -- re-run scripts/build-textures.py") % name)
            stale += not current
        else:
            image.save(path)
            print(f"wrote {name}")
    return 1 if stale else 0


if __name__ == "__main__":
    raise SystemExit(main())
