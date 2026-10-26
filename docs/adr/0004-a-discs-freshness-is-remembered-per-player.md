---
status: provisional
---

# A disc's freshness is remembered per player

A jukebox restores Sanity, but only rotating at least three discs keeps that restore going. A disc is a Fresh disc unless it is one of the last two discs the player heard. A Stale disc restores half what its last play did, so one disc on repeat, or two discs alternating, fade to nothing.

**Decision.** The history of discs heard belongs to the player and is saved with them, through death as through a relog, so dying never makes a Stale disc Fresh again. It covers every jukebox, and only the nearest jukebox that is playing counts, so jukeboxes never stack. A hopper that feeds three discs into one jukebox is a legitimate music room.

**Considered.** History kept per jukebox: cheaper, but two jukeboxes each playing one disc in turn would get around the rule. History cleared on death, like Sanity's own respawn value: a death would then be a way to make a disc Fresh again.
