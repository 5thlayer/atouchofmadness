# A Touch of Madness

![A Touch of Madness](https://raw.githubusercontent.com/5thlayer/atouchofmadness/main/publish/atouchofmadness-cover.png)

A Touch of Madness adds Sanity, a new stat inspired by Don’t Starve. Sanity starts at 100. Darkness, cold, rain, the Nether, nearby monsters and bad food lower it; company, music, sleep and good food raise it.

Low Sanity changes what you see and hear: the screen loses its colour at the edges, dark shapes appear in the corners, and you hear sounds that aren’t there. Shadow Creatures start following you. They’re harmless at first, but once you’re Insane they become real and attack.

Killing one restores some Sanity and drops Umbra. With it you can craft a sword and a pickaxe faster than netherite, and armour as strong as diamond that also makes you faster.

## For pack authors

- **Where a dimension unsettles** is the dimension type tag `atouchofmadness:unsettling`, by default the Nether: standing there drains Sanity.
- **Where Shadow Creatures come** is the dimension type tag `atouchofmadness:shadows`, by default the Overworld, the Nether and the End.
- **Who unsettles** is the entity type tag `atouchofmadness:unsettling`, by default vanilla's monsters, and **what each weighs** the entity type data map `atouchofmadness:unsettling_weight`: 1 for a type it doesn't list, 3 for an Enderman, 10 for the Warden, the Wither and the Elder Guardian, and 2 for the Lurker and 4 for the Gaunt.
- **Who comforts** is the entity type tag `atouchofmadness:comforting`, by default wolves, cats, parrots, horses, donkeys, mules, llamas, nautiluses, villagers and allays, each counting only once tamed if it can be tamed.
- **What a food does to Sanity** is the item data map `atouchofmadness:sanity_modifier`, from −10 for a spider eye or pufferfish to +30 for an enchanted golden apple; a food it doesn't list does nothing.
- **What shapes the Shadow Creatures take** are the entity type tags `atouchofmadness:lurker_shapes` and `atouchofmadness:gaunt_shapes`, drawn from the monsters each biome spawns. Another mod's mobs can be added.
- **What repairs Umbra Gear** in an anvil is the item tag `atouchofmadness:repairs_umbra_gear`, by default Umbra.
- **The Shadow Creatures' drops** are the loot tables `atouchofmadness:entities/lurker` and `atouchofmadness:entities/gaunt`, which drop Umbra only on a player's kill.
- **The recipes** are `atouchofmadness:flower_crown` and `atouchofmadness:umbra_{sword,pickaxe,helmet,chestplate,leggings,boots}`, so a datapack can replace them.
- **Discovery** is a hidden advancement for each drain, restore and stage, at `atouchofmadness:discovery/<id>`. A pack removes one to count it as known, or overrides it to fire on `minecraft:tick` to mark it known from the start. The criterion triggers `atouchofmadness:sanity_changed`, with a `source`, and `atouchofmadness:stage_reached`, with a `stage`, fire them.
- **The meter's sprites** are `atouchofmadness:hud/sanity_{full,half,empty}`, each with `_uneasy` and `_insane` versions, which a resource pack can replace.

Sanity is kept on the player and survives relogs and restarts; a player who dies respawns at 50. Shadow Creatures are never saved.

The terms are in [GLOSSARY.md](GLOSSARY.md), and what each release changed for a pack is in [CHANGELOG.md](CHANGELOG.md).

## Commands

All at permission level 2:

- `/sanity get <player>` shows a player's Sanity, and returns it rounded down for `execute store`.
- `/sanity set <player> <value>` and `/sanity add <player> <value>` change it.

## Configuration

Two configs, in `config/`. Rates are in Sanity a minute, distances in blocks.

- **`atouchofmadness-common.toml`**:
  - **`sanity`**: `start` (100), `respawn` (50), `saneAbove` (50) and `insaneBelow` (20).
  - **`environment`**: `lowLightBelow` (5), and the drains `lowLight` (6), `freezing` (4), `wet` (1.5) and `unsettling` (1).
  - **`nearby`**: `hostileRadius` (8), `hostile` (1, times the weight) and `hostileCap` (10); `companyRadius` (8) and `company` (2).
  - **`music`**: `range` (64) and `fresh` (4).
  - **`moments`**, each at once: `sleep` (40), `flower` (1) and `struckByTheDark` (10).
  - **`crown`**: `restore` (1.5) and `wearDays` (1).
  - **`shadows`**: `caps` (50, 35, 20, 10), `intervalSeconds` (20), `minDistance` (12), `maxDistance` (24), `fadeDistance` (4), `gauntBelow` (10), `gauntChance` (0.5), `realFactor` (2) and `strike` (5); and in `lurker` and `gaunt`, `health` (30, 16), `damage` (4, 7), `drift` (2.5, 5) and `kill` (10, 20).
  - **`umbra`**: `sword` (2), `pickaxe` (2) and `armour` (1).
  - **`discovery`**: `enabled` (on).
- **`atouchofmadness-client.toml`**, the Hallucinations: `sway`, `colourLoss` and `encroachment`, all on.

The numbers are provisional while A Touch of Madness is below 1.0.

## Compatibility

All optional, and none nested in the jar:

- **[Jade](https://www.curseforge.com/minecraft/mc-mods/jade)** shows a playing jukebox's disc as Fresh or Stale for the player looking, once they have discovered the Stale disc.
- **[EMI](https://www.curseforge.com/minecraft/mc-mods/emi)** shows information pages on the Flower Crown, each food with a Sanity Modifier, music discs and Umbra, each hidden until discovered.
- **[A Touch of Darkness](https://github.com/5thlayer/atouchofdarkness)**: each Strike by the Dark takes 10 Sanity.

## Dependencies

None beyond NeoForge, on Minecraft 26.1.2. A Touch Of's Runtime, which draws the Sanity meter, is nested in the jar.

This is a hobby project: I’m not monetizing any part of A touch of mods suite. I’m building it for fun, to see how far I can take it.

## License

Per file: the code is LGPL-3.0-only and the content, textures and docs CC BY 4.0, as [LICENSE](LICENSE) says. Source is on [GitHub](https://github.com/5thlayer/atouchofmadness). The Umbra Gear's textures are built from Malcolm Riley's [unused-textures](https://github.com/malcolmriley/unused-textures), under CC BY 4.0; [NOTICE](NOTICE) credits each.
