# SPDX-FileCopyrightText: 2026 5thlayer
# SPDX-License-Identifier: LGPL-3.0-only
"""Generates the Hallucinations' screen textures, the Pillar's own:

- colour_loss.png: white, its alpha rising from clear in the middle to full toward the edges. Its inner
  half is the colour loss at the screen's edge; the whole of it, the colour loss reaching in.
- encroachment.png: black shapes creeping in from the edges, longest toward the corners.

Run from the repo root: python3 tools/textures/hallucinations.py
"""
import math
import random
from PIL import Image

OUT = "src/main/resources/assets/atouchofmadness/textures/misc/"
SIZE = 512


def smoothstep(edge0, edge1, x):
    t = min(1.0, max(0.0, (x - edge0) / (edge1 - edge0)))
    return t * t * (3 - 2 * t)


def colour_loss():
    image = Image.new("RGBA", (SIZE, SIZE))
    pixels = image.load()
    half = SIZE / 2
    for y in range(SIZE):
        for x in range(SIZE):
            # Twice the radius of the inner half, where a radius of 1 is the edge's middle.
            r = 2 * math.hypot(x + 0.5 - half, y + 0.5 - half) / half
            pixels[x, y] = (255, 255, 255, round(255 * smoothstep(0.5, 1.4, r)))
    image.save(OUT + "colour_loss.png")


def encroachment(seed=24):
    rng = random.Random(seed)
    # Each shape: where on the border it roots, as an angle, how far in it reaches (in from the edge,
    # where 1 is the middle), how wide its root is, and how it curls.
    shapes = []
    for _ in range(46):
        angle = rng.uniform(0, 2 * math.pi)
        corner = abs(math.sin(2 * angle))
        length = (0.08 + 0.32 * corner ** 2) * rng.uniform(0.6, 1.0)
        shapes.append((angle, length, rng.uniform(0.05, 0.11), rng.uniform(-0.6, 0.6), rng.uniform(6, 14), rng.uniform(0, 2 * math.pi)))
    image = Image.new("RGBA", (SIZE, SIZE))
    pixels = image.load()
    half = SIZE / 2
    for py in range(SIZE):
        for px in range(SIZE):
            x = (px + 0.5 - half) / half
            y = (py + 0.5 - half) / half
            inward = 1 - max(abs(x), abs(y))
            angle = math.atan2(y, x)
            corner = abs(math.sin(2 * angle))
            # A soft dark band along the border, deeper in the corners.
            alpha = (1 - smoothstep(0.0, 0.05 + 0.1 * corner, inward)) * (0.45 + 0.45 * corner)
            for root, length, width, curl, waves, phase in shapes:
                if inward >= length:
                    continue
                along = inward / length
                centre = root + curl * 0.15 * along * math.sin(waves * along + phase)
                off = (angle - centre + math.pi) % (2 * math.pi) - math.pi
                # Tapering to a point at its tip.
                taper = width * (1 - along) ** 1.4
                if taper <= 0:
                    continue
                shape = 1 - smoothstep(taper * 0.55, taper, abs(off))
                alpha = max(alpha, shape * (1 - 0.35 * along))
            pixels[px, py] = (0, 0, 0, round(255 * min(1.0, alpha)))
    image.save(OUT + "encroachment.png")


if __name__ == "__main__":
    colour_loss()
    encroachment()
