# SPDX-FileCopyrightText: 2026 5thlayer
# SPDX-License-Identifier: MIT

# Temporary cover and icon, the Sanity meter slipping from Sane to Insane above the title among the
# Pillar's items, with the Encroachment creeping in from the edges, in the -works suite's style
# (A Touch of Darkness' make-cover.py), until an in-game screenshot or an artist's art replaces them.
# They carry Malcolm Riley's art through the Umbra Gear (NOTICE). Run from the repo root with Pillow
# installed.
import math
import os
import random

from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageFont

A = 'src/main/resources/assets/atouchofmadness/textures/'
BG = (10, 8, 12)
ACCENT = (196, 86, 255)  # violet, the suite's, as A Touch of Darkness' and Wickedness' covers
# the tools in the top corners, either side of the meter, and below the title the Umbra armour
# grouped head to foot in the middle, between the Flower Crown and the Umbra
TOP = ['umbra_pickaxe', 'umbra_sword']
ARMOUR = ['umbra_helmet', 'umbra_chestplate', 'umbra_leggings', 'umbra_boots']
GLOW = (200, 170, 255)
# the meter's ten places, left to right, filled from the left as Sanity slips through the Stages:
# Sane, then Uneasy, then Insane down to a half eye, and an empty eye at the end; each place's
# Stage, and how full it is
EYES = [('', 2)] * 3 + [('_uneasy', 2)] * 3 + [('_insane', 2)] * 2 + [('_insane', 1), ('_insane', 0)]


def sprite(path, px):
    return Image.open(f'{A}{path}.png').convert('RGBA').resize((px, px), Image.NEAREST)


def encroach(W, H):
    # the Encroachment's own texture, stretched over the field, dark shapes reaching in from the edges
    tex = Image.open(f'{A}misc/encroachment.png').convert('RGBA').resize((W, H), Image.BILINEAR)
    shade = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    shade.putalpha(tex.getchannel('A'))
    return shade


def field(W, H, px, seed, top):
    # the night, with the Pillar's items set through it clear of the title band, the tools at the
    # height top, a fraction of H
    rng = random.Random(seed)
    img = Image.new('RGBA', (W, H), BG + (255,))
    d = ImageDraw.Draw(img)
    for _ in range(W * H // 900):
        v = rng.randrange(10, 22)
        x, y = rng.randrange(W), rng.randrange(H)
        d.point((x, y), fill=(v + 2, v, v + 3))
    img.alpha_composite(encroach(W, H))
    # the items over the Encroachment, so it doesn't sink them
    slots = [(0.12, top, TOP[0]), (0.88, top, TOP[1]), (0.12, 0.82, 'flower_crown'), (0.88, 0.82, 'umbra')]
    slots += [(0.5 + (i - 1.5) * px * 1.35 / W, 0.82, name) for i, name in enumerate(ARMOUR)]
    for fx, fy, name in slots:
        # upright and on the slot's own column and row, so the items line up across, each centred by
        # its drawn pixels rather than its tile, since each sprite sits differently in it
        s = sprite(f'item/{name}', px)
        s = s.crop(s.getchannel('A').getbbox())
        item(img, ImageEnhance.Brightness(s).enhance(1.4), int(fx * W), int(fy * H), px)
    return img


def item(img, s, cx, cy, px):
    # an item lifted off the night by a very soft glow of its own shape
    r = px // 5
    halo = Image.new('RGBA', (s.width + 4 * r, s.height + 4 * r), GLOW + (0,))
    a = Image.new('L', halo.size, 0)
    a.paste(s.getchannel('A').point(lambda v: int(v * 0.35)), (2 * r, 2 * r))
    halo.putalpha(a.filter(ImageFilter.GaussianBlur(r)))
    img.alpha_composite(halo, (cx - halo.width // 2, cy - halo.height // 2))
    img.alpha_composite(s, (cx - s.width // 2, cy - s.height // 2))


def meter(img, cx, bottom, eye):
    # the meter's row of eyes, each eye px wide with a pixel's gap, standing on the band's top edge,
    # each place empty first, then full or half over it; the half eye turned to fill from the left as
    # the row does, and the empty eyes lifted off the night, which they'd otherwise vanish into
    u = eye // 9
    w = len(EYES) * 10 * u - u
    for place, (stage, fill) in enumerate(EYES):
        at = (cx - w // 2 + place * 10 * u, bottom - 9 * u)
        empty = sprite(f'gui/sprites/hud/sanity_empty{stage}', 9 * u)
        img.alpha_composite(ImageEnhance.Brightness(empty).enhance(3), at)
        if fill:
            s = sprite(f'gui/sprites/hud/sanity_{"full" if fill == 2 else "half"}{stage}', 9 * u)
            img.alpha_composite(s if fill == 2 else s.transpose(Image.FLIP_LEFT_RIGHT), at)


def font(names, size):
    for n in names:
        if os.path.exists(n):
            return ImageFont.truetype(n, size)
    raise SystemExit('needs one of ' + ', '.join(names))


def make(W, H, out, title_size, px, eye, stacked=False):
    # stacked puts "A touch of" above the word, for the square icon
    img = field(W, H, px, 11, 0.1 if stacked else 0.18)
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
