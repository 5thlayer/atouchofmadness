---
status: provisional
---

# Continuous drains and restores run once a second

Every continuous drain and restore is a rate per minute, and some are costly to read, such as the light at the eyes or the monsters within 8 blocks. So they are evaluated every 20 ticks rather than every tick, each player on their own tick of the 20, chosen from their id so the work spreads across the second. Sanity then moves by the Net rate for those 20 ticks at once, and the meter's trend and the stage changes it causes follow once a second. An instantaneous drain or restore, `/sanity`, joining and respawning still move Sanity at once.

**Considered.** Every tick: smooth, but twenty times the cost for nothing a player can see. Once a second for every player on the same tick: as cheap overall, but a spike on one tick in twenty on a busy server.
