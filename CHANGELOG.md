# Changelog

Written for Consumers: what a mod building against A Touch of Madness can use, or will see change. A published version never changes: a fix is the next patch (`docs/agents/releases.md`).

## Unreleased

- The Pillar has its own creative tab, "A Touch of Madness", and no longer nests `libworks-runtime` or fills its Works tab (#2).
- Every player has Sanity, 0 to 100: 100 when new, 50 after respawning, saved with the player. It is Sane above 50, Uneasy from 50 down to 20 and Insane below 20, all four set in `atouchofmadness-common.toml`. `/sanity get|set|add <player> [value]`, at permission level 2, reads and moves it, and returns it rounded down for `execute store` (#3).
