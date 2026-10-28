# SPDX-FileCopyrightText: 2026 5thlayer
# SPDX-License-Identifier: MIT

# Temporary cover and icon, the Sanity meter reading Uneasy above the title among the
# Pillar's items, with the Encroachment creeping in from the edges, in the -works suite's style
# (A Touch of Darkness' make-cover.py), until an in-game screenshot or an artist's art replaces them.
# They carry Malcolm Riley's art through the Umbra Gear (NOTICE). Run from the repo root with Pillow
# installed.
import math
import os
import random

from PIL import Image, ImageDraw, ImageFont

A = 'src/main/resources/assets/atouchofmadness/textures/'
BG = (10, 8, 12)
ACCENT = (196, 86, 255)  # violet, the suite's, as A Touch of Darkness' and Wickedness' covers
ITEMS = ['flower_crown', 'umbra', 'umbra_sword', 'umbra_pickaxe', 'umbra_helmet', 'umbra_chestplate',
         'umbra_leggings', 'umbra_boots']
# the meter as the HUD draws it, its ten eyes all in one Stage's icons: Uneasy at 35 of 100 Sanity
EYES = 10
STAGE = '_uneasy'
SANITY = 35


def sprite(path, px):
    return Image.open(f'{A}{path}.png').convert('RGBA').resize((px, px), Image.NEAREST)


def encroach(W, H):
    # the Encroachment's own texture, stretched over the field, dark shapes reaching in from the edges
    tex = Image.open(f'{A}misc/encroachment.png').convert('RGBA').resize((W, H), Image.BILINEAR)
    shade = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    shade.putalpha(tex.getchannel('A'))
    return shade


def field(W, H, px, seed, clear):
    # the night, with the Pillar's items scattered through it, kept clear of the title band and, above
    # it, of the meter between the fractions of the width in clear
    rng = random.Random(seed)
    img = Image.new('RGBA', (W, H), BG + (255,))
    d = ImageDraw.Draw(img)
    for _ in range(W * H // 900):
        v = rng.randrange(10, 22)
        x, y = rng.randrange(W), rng.randrange(H)
        d.point((x, y), fill=(v + 2, v, v + 3))
    slots = [(x, y) for y in (0.18, 0.82) for x in (0.12, 0.31, 0.5, 0.69, 0.88)
             if y > 0.5 or not clear[0] <= x <= clear[1]]
    rng.shuffle(slots)
    for (fx, fy), name in zip(slots, ITEMS * 2):
        # upright and on the slot's own column and row, so the items line up across and down, each
        # centred by its drawn pixels rather than its tile, since each sprite sits differently in it
        cx, cy = int(fx * W), int(fy * H)
        s = sprite(f'item/{name}', px)
        s = s.crop(s.getchannel('A').getbbox())
        img.alpha_composite(s, (cx - s.width // 2, cy - s.height // 2))
    img.alpha_composite(encroach(W, H))
    return img


def meter(img, cx, bottom, eye):
    # the meter's row of eyes, each eye px wide with a pixel's gap, standing on the band's top edge; as
    # the HUD, it fills from the right, each place empty first, then full or half over it
    u = eye // 9
    w = EYES * 10 * u - u
    halves = round(SANITY / 100 * EYES * 2)
    for place in range(EYES):
        x = cx + w // 2 - u * 9 - place * 10 * u
        img.alpha_composite(sprite(f'gui/sprites/hud/sanity_empty{STAGE}', 9 * u), (x, bottom - 9 * u))
        fill = 'full' if halves >= 2 * place + 2 else 'half' if halves == 2 * place + 1 else None
        if fill:
            img.alpha_composite(sprite(f'gui/sprites/hud/sanity_{fill}{STAGE}', 9 * u), (x, bottom - 9 * u))


def font(names, size):
    for n in names:
        if os.path.exists(n):
            return ImageFont.truetype(n, size)
    raise SystemExit('needs one of ' + ', '.join(names))


def make(W, H, out, title_size, px, eye, stacked=False):
    # stacked puts "A touch of" above the word, for the square icon
    img = field(W, H, px, 11, (0.1, 0.9) if stacked else (0.25, 0.75))
    f1 = font(['/System/Library/Fonts/Supplemental/Impact.ttf', '/usr/share/fonts/TTF/impact.ttf',
               os.path.expanduser('~/.local/share/fonts/msfonts/impact.ttf'),
               '/usr/share/fonts/noto/NotoSans-Black.ttf'], title_size)
    f2 = font(['/System/Library/Fonts/Supplemental/Apple Chancery.ttf', '/usr/share/fonts/gsfonts/Z003-MediumItalic.otf',
               '/usr/share/fonts/urw-base35/Z003-MediumItalic.otf'], int(title_size * (0.62 if stacked else 0.55)))
    # the -works title turned around: the suite's "A touch of" in a script, then the Pillar's word
    w, a = 'A touch of', 'MADNESS'
    d = ImageDraw.Draw(img)
    b1, b2 = d.textbbox((0, 0), a, font=f1), d.textbbox((0, 0), w, font=f2)
    w1, w2, h1, h2 = b1[2] - b1[0], b2[2] - b2[0], b1[3] - b1[1], b2[3] - b2[1]
    pad, gap, sh = int(title_size * 0.16), int(title_size * 0.1), max(3, title_size // 20)
    bw, ph = w2 + 2 * gap, (h2 + 2 * pad if stacked else h1)
    th = ph + gap + h1 if stacked else h1
    cy, bh = H // 2, int(th + title_size * 0.6)
    meter(img, W // 2, cy - bh // 2 - H // 40, eye)
    band = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(band).rectangle([0, cy - bh // 2, W, cy + bh // 2], fill=(5, 4, 6, 235))
    img = Image.alpha_composite(img, band)
    d = ImageDraw.Draw(img)
    lt = max(3, title_size // 25)
    d.rectangle([0, cy - bh // 2, W, cy - bh // 2 + lt], fill=ACCENT)
    d.rectangle([0, cy + bh // 2 - lt, W, cy + bh // 2], fill=ACCENT)
    if stacked:
        x, bt = (W - bw) // 2, cy - th // 2
        tx, tt = (W - w1) // 2, bt + ph + gap
    else:
        x, bt = (W - (bw + gap + w1)) // 2, cy - h1 // 2
        tx, tt = x + bw + gap, bt
    sx, sy = x + gap - b2[0], bt + (ph - h2) // 2 - b2[1]
    d.text((sx + sh, sy + sh), w, font=f2, fill=(0, 0, 0))
    d.text((sx, sy), w, font=f2, fill=(240, 232, 214))
    ty = tt - b1[1]
    d.text((tx + sh - b1[0], ty + sh), a, font=f1, fill=(0, 0, 0))
    d.text((tx - b1[0], ty), a, font=f1, fill=ACCENT)
    img.convert('RGB').save(out)


make(1280, 640, 'publish/atouchofmadness-cover.png', 150, 80, 45)
make(512, 512, 'publish/atouchofmadness-icon.png', 84, 56, 36, stacked=True)
