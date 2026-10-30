# A Touch of Madness

The Pillar of A Touch Of that owns Sanity: a per-player meter that the world wears down and comfort restores, with Shadow Creatures for those who let it fall. The terms the Pillars share, Sanity, Shadow Creature, Umbra and Sanity Modifier among them, are in the suite glossary (`5thlayer/atouchof`'s `GLOSSARY.md`); this file holds the terms only this Pillar uses.

## Language

### Parties

**A Touch of Madness**:
This Pillar, `atouchofmadness`.
_Avoid_: Madness mod, Module

**A Touch Of**:
The suite this Pillar belongs to, with A Touch of Darkness and A Touch of Cold, all drawn by one Runtime.
_Avoid_: FactoryWorks (a different suite), the pack

**Pillar**:
One of the suite's mods, each owning one danger and playable alone in vanilla.
_Avoid_: Module (a FactoryWorks mod), Consumer, addon

**Runtime**:
The small mod every Pillar nests jar-in-jar, which draws this Pillar's Sanity meter and carries the suite's neutral signals, and holds no mechanic.
_Avoid_: Core, base mod, library

### Sanity

**Drain**:
Anything that lowers Sanity; it is either continuous or instantaneous.
_Avoid_: event, sanity loss, penalty

**Continuous drain**:
A drain that takes Sanity at a rate for as long as its condition holds, such as low light or a hostile nearby.
_Avoid_: aura, tick drain

**Instantaneous drain**:
A drain that takes a fixed amount of Sanity at once, such as being struck by the Dark or eating a food with a negative Sanity Modifier.
_Avoid_: event drain, lump

**Low light**:
Light at a player's eyes too dim for their peace of mind, a brighter line than the Dark's; a night with a full moon under open sky never counts, in a dimension with a moon (ADR 0003).
_Avoid_: the Dark (A Touch of Darkness's state), darkness

**Restore**:
Anything that raises Sanity; like a drain, it is either continuous or instantaneous.
_Avoid_: heal, regen, comfort (the feel, not the mechanic)

**Moment**:
An instantaneous drain or restore: one event, one move of Sanity, such as eating a food, waking from a night's sleep or breaking a flower.
_Avoid_: event, lump

**Net rate**:
What a player's continuous drains and restores sum to at a given moment; instantaneous ones never count toward it.
_Avoid_: trend (how the meter shows it), delta

**Unsettling**:
Said of a dimension type or an entity type in this Pillar's `#atouchofmadness:unsettling` tag for its registry: being in such a dimension, or near such an entity (a hostile nearby), is a continuous drain.

**Company**:
What restores Sanity for being near: a living entity in `#atouchofmadness:comforting`, tamed if it can be, or another player in Survival or Adventure. It counts once, however many are near.
_Avoid_: friends, allies, comfort (the feel, not the mechanic)

**Play**:
One jukebox's song from its start, as a player hears it: it goes on at the freshness it began with, however the player comes and goes, until that jukebox stops or starts a song again.
_Avoid_: listen, track

**Fresh disc**:
A music disc that is not one of the last two discs a player heard, so a jukebox playing it restores fully.

**Stale disc**:
A music disc that is one of the last two discs a player heard; each play of it restores half what the last one did, until two other discs have played between.
_Avoid_: repeat, cooldown

**Flower Crown**:
This Pillar's headgear, made from flowers, that restores Sanity while worn in the head slot and wilts away once its wear is spent; it wears only while worn.

**Umbra Gear**:
This Pillar's sword, pickaxe and armour made from Umbra, each the best in its role and each draining Sanity while held or worn.
_Avoid_: shadow gear, Umbra tools (it includes armour)

### Stages

**Sane**:
The stage of a player whose Sanity is high enough that it does nothing to them.

**Uneasy**:
The stage below Sane, where Hallucinations start and Shadow Creatures appear but cannot harm.
_Avoid_: low sanity (say which stage)

**Insane**:
The lowest stage, where Shadow Creatures are real.
_Avoid_: Insanity as the name of the meter, mad (the feel, not the stage)

**Hallucination**:
A harmless effect of low Sanity that only the player sees or hears, such as a fake sound or a fading screen edge.
_Avoid_: Shadow Creature (a hallucination is never a mob)

**Encroachment**:
The Hallucination of dark shapes creeping in from the edges of the player's view, nearer as Sanity falls.
_Avoid_: tendrils, shadows, shades (taken by Shadow Creatures)

### Shadow Creatures

**Haunted**:
Said of a player who may have Shadow Creatures: Uneasy or Insane, in Survival or Adventure, alive and awake, in a dimension that allows them. Their Sanity sets how many; once they stop being Haunted, theirs vanish at once.
_Avoid_: eligible, targeted, cursed

**Owner**:
The one Haunted player a Shadow Creature exists for and hunts; besides the Owner, only a player who is Insane can see it.

**Unreal**:
The state of a Shadow Creature whose Owner is Uneasy: it can neither harm nor be harmed, and fades when approached or struck.
_Avoid_: Hallucination (never a mob), fake, ghost, shade

**Real**:
The state of a Shadow Creature whose Owner is Insane: it attacks, can be killed by a player who can hit it, which restores that player's Sanity, and leaves Umbra.

**Lurker**:
The Shadow Creature of every low stage: slow and tough. Like the Gaunt, it drifts through blocks.

**Gaunt**:
The Shadow Creature that comes only when Sanity is nearly gone: fast, fragile and hard-hitting.

**Shape**:
The mob a Shadow Creature is drawn as, taken from the monsters of the biome it appears in; a Shape changes the look, never the Lurker's or the Gaunt's behaviour.
_Avoid_: skin, disguise, variant

### Discovery

**Discovery**:
The suite's **Discovery** (A Touch Of's glossary, suite ADR-0006), here of each Cause, such as Low light, Company, a Stale disc or the Flower Crown, the first time it actually changes the player's Sanity, and of each Stage below Sane the first time they reach it. The Sanity meter itself is never discovered: it shows from the start.
_Avoid_: unlock, research, knowledge

### Dev runs

**Game-test pack**:
A datapack only this Pillar's game tests switch on, holding the data a pack would supply, such as a tag's entries or a recipe, so a test can stand on it. Never in what players download.
_Avoid_: test datapack, gametest datapack

**Dev pack**:
A datapack only this Pillar's dev client switches on, holding the data a pack would supply, so a feature a pack turns on with data can be tried by hand. Never in what players download.
_Avoid_: debug pack, test pack

### Shipped packs

**Gallery pack**:
A datapack in this Pillar's jar, off until a player turns it on, whose functions build a working scene of each of the Pillar's features, each with a sign naming it. Functions only: turning it on changes nothing about how the Pillar plays (libworks ADR 0010).
_Avoid_: demo pack, showcase pack, gallery datapack
