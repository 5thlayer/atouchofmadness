# SPDX-FileCopyrightText: 2026 5thlayer
# SPDX-License-Identifier: MIT

# Temporary cover and icon, the Sanity meter slipping from Sane to Insane above the title among the
# Pillar's items, with the Encroachment creeping in from the edges, in the -works suite's style
# (A Touch of Darkness' make-cover.py), until an in-game screenshot or an artist's art replaces them.
# They carry Malcolm Riley's art through the Umbra Gear (NOTICE), and draw the Shadow Creatures' Shapes
# from vanilla's mob textures, read from the client jar Gradle has cached. Run from the repo root with
# Pillow installed, after a build.
import glob
import io
import math
import os
import random
import zipfile

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


# the Shadow Creatures standing over the meter, each Shape drawn front on as its model's front faces,
# vanilla's boxes in model units, y downwards: the Lurker's over the Uneasy eyes, the Gaunt's over the
# Insane, and none over the Sane. A box is (u, v, w, h, d, x, y, mirrored), its front face the texture's
# w by h at (u + d, v + d), drawn at (x, y); a leg is a spider's, (x, y, its Y and Z turns).
SHADOW = (0x16, 0x12, 0x1E)  # ShadowRenderers.SHADOW_RGB
HEAD, BODY = (0, 0, 8, 8, 8, -4, -8, False), (16, 16, 8, 12, 4, -4, 0, False)
SHAPES = {
    'zombie/zombie': [(0, 16, 4, 12, 4, -4, 12, False), (0, 16, 4, 12, 4, 0, 12, True), BODY,
                      # its arms held out at the onlooker, seen end on
                      (44, 12, 4, 4, 4, -8, 0, False), (44, 12, 4, 4, 4, 4, 0, True), HEAD],
    'skeleton/skeleton': [(0, 16, 2, 12, 2, -3, 12, False), (0, 16, 2, 12, 2, 1, 12, True), BODY,
                          (40, 16, 2, 12, 2, -6, 0, False), (40, 16, 2, 12, 2, 4, 0, True), HEAD],
    'creeper/creeper': [(0, 16, 4, 6, 4, -4, 18, False), (0, 16, 4, 6, 4, 0, 18, False),
                        (16, 16, 8, 12, 4, -4, 6, False), (0, 0, 8, 8, 8, -4, -2, False)],
    'enderman/enderman': [(56, 0, 2, 30, 2, -3, -5, False), (56, 0, 2, 30, 2, 1, -5, True),
                          (32, 16, 8, 12, 4, -4, -14, False), (56, 0, 2, 30, 2, -6, -14, False),
                          (56, 0, 2, 30, 2, 4, -14, True), (0, 0, 8, 8, 8, -4, -21, False)],
    'spider/spider': [(-4, 15, math.pi / 4, -math.pi / 4), (-4, 15, math.pi / 8, -0.58119464),
                      (-4, 15, -math.pi / 8, -0.58119464), (-4, 15, -math.pi / 4, -math.pi / 4),
                      (0, 12, 10, 8, 12, -5, 11, False), (32, 4, 8, 8, 8, -4, 11, False)],
}
LURKERS = ['zombie/zombie', 'skeleton/skeleton']
GAUNTS = ['creeper/creeper', 'enderman/enderman', 'spider/spider']



def vanilla(path):
    jars = sorted(glob.glob(os.path.expanduser('~/.gradle/caches/neoformruntime/artifacts/minecraft_*_client.jar')))
    if not jars:
        raise SystemExit('needs the client jar Gradle caches: run sh ./gradlew build first')
    with zipfile.ZipFile(jars[-1]) as jar:
        return Image.open(io.BytesIO(jar.read(f'assets/minecraft/textures/entity/{path}.png'))).convert('RGBA')


def shape(path, k):
    # a Shape front on, k pixels to the unit, tinted as the Shadow Creature's renderer tints it but
    # in greys lifted off the night, which it'd otherwise vanish into; returns it and its feet's x and y
    tex = vanilla(path)
    parts = SHAPES[path]
    pts = []
    for p in parts:
        if len(p) == 4:
            x, y, ry, rz = p
            pts += [(x, y), (x + 15 * math.copysign(1, x) * math.cos(ry) * math.cos(rz),
                     y + 15 * math.copysign(1, x) * math.cos(ry) * math.sin(rz))]
            pts += [(-a, b) for a, b in pts[-2:]]
        else:
            pts += [(p[5], p[6]), (p[5] + p[2], p[6] + p[3])]
    x0, y0 = min(a for a, _ in pts) - 1, min(b for _, b in pts) - 1
    x1, y1 = max(a for a, _ in pts) + 1, max(b for _, b in pts) + 1
    img = Image.new('RGBA', (math.ceil((x1 - x0) * k), math.ceil((y1 - y0) * k)), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for p in parts:
        if len(p) == 4:
            # a pair of legs, the right and its mirror, in the leg's own colour
            x, y, ry, rz = p
            leg = tex.crop((20, 2, 36, 4))
            c = tuple(sum(px[i] for px in leg.getdata()) // 32 for i in range(3))
            for side in (1, -1):
                sx = side * x
                ex = sx + 15 * side * math.copysign(1, x) * math.cos(ry) * math.cos(rz)
                ey = y + 15 * math.copysign(1, x) * math.cos(ry) * math.sin(rz)
                d.line([((sx - x0) * k, (y - y0) * k), ((ex - x0) * k, (ey - y0) * k)], fill=c + (255,), width=2 * k)
        else:
            u, v, w, h, dz, x, y, mirrored = p
            face = tex.crop((u + dz, v + dz, u + dz + w, v + dz + h)).resize((w * k, h * k), Image.NEAREST)
            if mirrored:
                face = face.transpose(Image.FLIP_LEFT_RIGHT)
            img.alpha_composite(face, (round((x - x0) * k), round((y - y0) * k)))
    grey, a = img.convert('L'), img.getchannel('A')
    tint = [min(255, c * 4) for c in SHADOW]
    img = Image.merge('RGBA', [grey.point(lambda v, t=t: v * t // 255) for t in tint] + [a.point(lambda v: v * 9 // 10)])
    return img, -x0 * k, (y1 - 1 - y0) * k


def shadows(img, cx, w, baseline, k, places):
    # the creatures on the meter, their feet just above it, each group side by side over the middle of
    # its places
    for names, first, count in ((LURKERS, 3, 3), (GAUNTS, 6, 4)):
        made = [shape(name, k) for name in names]
        gap = 3 * k
        x = cx - w // 2 + first * places + (count * places - sum(m[0].width for m in made) - gap * (len(made) - 1)) // 2
        for s, fx, fy in made:
            item(img, s, int(x + s.width // 2), int(baseline - fy + s.height // 2), places)
            x += s.width + gap


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


def make(W, H, out, title_size, px, eye, mob, stacked=False):
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
    u = eye // 9
    shadows(img, W // 2, len(EYES) * 10 * u - u, cy - bh // 2 - H // 40 - 9 * u - H // 80, mob, 10 * u)
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


make(1280, 640, 'publish/atouchofmadness-cover.png', 150, 80, 45, 3)
make(512, 512, 'publish/atouchofmadness-icon.png', 84, 56, 36, 2, stacked=True)
