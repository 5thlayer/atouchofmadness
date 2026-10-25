---
status: provisional
---

# A Shadow Creature is a server mob sent only to its Owner

A Shadow Creature exists only for a player whose Sanity is low, and it turns real as Sanity falls further (suite glossary). Each Shadow Creature is one ordinary server entity with one Owner. Vanilla's own per-player tracking check, `Entity.broadcastToPlayer`, sends it to the Owner and to no one else. The same entity is Unreal while its Owner is Uneasy and Real while they are Insane, and it switches back and forth as Sanity crosses the threshold. It is never saved: it is removed when its Owner logs out, dies, changes dimension or sleeps, or when Sanity rises above its spawn cap. It does not stop its Owner sleeping (`isPreventingPlayerRest`).

**Considered.** Client-only fakes for the Unreal stage, with a server mob spawned at the Real threshold: the server would save the cost of the Unreal ones, but there would be two implementations, a visible swap at the threshold, and no Unreal creature that a game test could see. An ordinary mob that every player sees but that targets only its Owner: simplest, but then a Shadow Creature no longer exists only for its Owner.
