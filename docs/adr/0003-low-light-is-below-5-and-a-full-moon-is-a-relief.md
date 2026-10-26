---
status: provisional
---

# Low light is below 5, not the Dark's 3, and a full moon is a relief

Madness drains Sanity in low light (suite ADR-0002), but it does not borrow A Touch of Darkness's lit threshold of 3. That threshold decides when the Dark strikes. Madness's measures something else: when the night wears on the mind. Vanilla 26.1.2 gives an open sky at night a sky light of 4 (`Timelines.NIGHT_SKY_LIGHT_LEVEL`), whatever the moon, and rain pulls even daytime towards it. Below 3, then, a night outdoors would never drain.

**Decision.** Low light is raw brightness at the eyes below 5, the first value above an open night, so every night under open sky drains unless block light of 5 or more is near. A night with a full moon, with the sky visible from the eyes, is exempt, so a full moon is a relief in both Pillars. Only a dimension with a moon, one whose timelines move it through its phases, has a full moon: elsewhere vanilla reads every hour as one, which would exempt every dark open sky. Each Pillar sets its threshold in its own config.

**Considered.** The Dark's threshold of 3: only caves and unlit rooms would drain, and Don't Starve's "night itself wears on you" would be lost. Below 8: comfort would need strong light, and the edges of a torch's pool would drain. A separate night drain: a second rule that does what the threshold already does.
