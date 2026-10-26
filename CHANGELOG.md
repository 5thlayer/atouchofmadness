# Changelog

Written for Consumers: what a mod building against A Touch of Madness can use, or will see change. A published version never changes: a fix is the next patch (`docs/agents/releases.md`).

## Unreleased

- The Pillar has its own creative tab, "A Touch of Madness", and no longer nests `libworks-runtime` or fills its Works tab (#2).
- Every player has Sanity, 0 to 100: 100 when new, 50 after respawning, saved with the player. It is Sane above 50, Uneasy from 50 down to 20 and Insane below 20, all four set in `atouchofmadness-common.toml`. `/sanity get|set|add <player> [value]`, at permission level 2, reads and moves it, and returns it rounded down for `execute store` (#3).
- Where a player stands drains their Sanity: 6/min in Low light, light at the eyes below 5 unless a full moon is in the sky they can see, in a dimension with a moon; 4/min while freezing, from powder snow or any other cold; 1.5/min in water or rain; and 1/min in a dimension type in `#atouchofmadness:unsettling`, the Nether by default. Every rate and the Low-light threshold are in the `environment` section of `atouchofmadness-common.toml` (#4).
- Eating moves Sanity by the food's Sanity Modifier, the `atouchofmadness:sanity_modifier` item data map, which a datapack can add to or override: −10 for a spider eye or pufferfish down to +30 for an enchanted golden apple, and 0 for any food it doesn't list. Each slice of a placed cake applies the cake's. Waking from sleeping through the night restores 40 to each sleeper, and breaking a block in `#minecraft:flowers` outside creative restores 1; both are in the `moments` section of `atouchofmadness-common.toml` (#6).
