---
status: provisional
---

# Discovery is kept in vanilla advancements, and a missing one counts as known

A player learns how Sanity works by meeting it: until a thing has changed their Sanity, or for a stage until they reach it, the Pillar shows nothing of what it does (#18). Something has to remember, per player, what each one has met.

**Decision.** Each discoverable thing is one vanilla advancement in the Pillar's own tab, and that advancement being done *is* the discovery: there is no second record. Vanilla already saves it per player, keeps it through death, shows it alone in vanilla on the advancement screen, and announces it with a toast. Every check of "has this player discovered X", the Jade tooltip and EMI's pages among them (#19), reads that advancement. If a pack removes one, that thing counts as known, so a removed advancement never locks a page for good; a pack marks a thing known from the start by overriding its advancement to fire on `minecraft:tick`. The server sends the client its own list of what is discovered, so the client never reads vanilla's advancement progress.

**Considered.** A player attachment, like the Disc History (ADR 0004), with a journal item to show it: a second store and a screen to build, for what vanilla already keeps and draws. A guide-book mod, Patchouli or Modonomicon (`docs/research/progressive-discovery-mods.md`): each can only be an optional dependency, so it can't be the record a Pillar playing alone in vanilla needs, though either can later lock its entries on these same advancements.
