# Third-party mods for progressive discovery (issues #18, #19)

Research date: 2026-10-26. Target: Minecraft 26.1.2, NeoForge 26.1.2.77 (`gradle.properties`).
Build availability comes from the Modrinth API query
`/v2/project/<slug>/version?game_versions=["26.1.2"]&loaders=["neoforge"]` and, for
CurseForge-only projects, the CurseForge site's file listing
`https://www.curseforge.com/api/v1/mods/<id>/files`. Nothing here was built or run.

## Summary and recommendation

- **Keep vanilla advancements as the single source of truth for "discovered".** Every serious
  candidate below can read advancement completion. None of them needs its own unlock store, and
  none of them is needed for the vanilla-alone surface.
- **Patchouli** (26.1-94-beta) and **Modonomicon** (26.1.2-2.17.1) are the two guide books with
  26.1.2 NeoForge builds that can gate an entry or a page on an advancement. Both work from data
  files that are inert when the mod is absent, so a "Madness journal" could ship as an optional
  integration with no compile-time dependency.
  - Patchouli is the simpler fit. An entry or page takes an `"advancement"` field, and
    `"secret": true` hides an entry until it unlocks. It is the closest to "one advancement per
    discoverable thing". Its licence is CC BY-NC-SA 3.0, but the Pillar ships only data files, not
    Patchouli's code.
  - Modonomicon (MIT) has, since 2.0.0 (2026-06-21), a full research layer of facts, hooks, nodes
    and stages. Advancement completion is one of its hook types, so it can also reproduce
    Thaumcraft-style "you learn it by meeting it" progression. Its 26.1.2 line has had 17 minor
    releases since June 2026, so expect churn.
- **JEI** (MIT, 29.47.0.111) has a true runtime hiding API: `removeIngredientsAtRuntime`,
  `hideRecipes` and `unhideRecipes`. **EMI**'s registry hides stacks and recipes only when plugins
  load, through predicates. **Jade**'s `Accessor#getPlayer()` and `IServerDataProvider` let our own
  provider gate a tooltip on the viewing player. Gating by discovery is therefore ours to write in
  every viewer. No viewer reads advancements itself.
- **Staging and quest mods** (Chapters, FTB Quests, Heracles/Theseus, Game Stages) gate items or
  quests, not knowledge. Game Stages has no build after 1.20.1. None of them is worth a dependency.
  The advancement-screen mods (Better Advancements, Paginated Advancements, Advancements Reloaded)
  are worth recommending to players if the Madness tab gets crowded, and they need no code from us.
- **Recommendation:** build #18 on vanilla advancements alone. Later, add Patchouli as an optional,
  data-only journal whose entries are `secret` and locked by those same advancement ids. Use
  Modonomicon only if the journal needs research-tree visuals. Do the Jade, EMI and JEI gating in
  our own plugin code, behind a shared "has the player discovered X" check that reads advancement
  progress. On the client that progress comes from `ClientAdvancements`.

## Candidates

| Mod | 26.1.2 NeoForge build | Licence | Fit |
|---|---|---|---|
| Patchouli | `patchouli-neoforge-26.1-94.jar` (26.1-94-beta, 2026-07-10), Modrinth and CF file 8404543 | CC-BY-NC-SA-3.0 | **Strong.** Data-driven book. Entries and pages lock on an advancement, and `secret` entries stay hidden until unlocked. Optional, data-only. |
| Modonomicon | `modonomicon-26.1.2-neoforge-2.17.1.jar` (2026-10-13) | MIT (code), CC-BY-4.0 (assets) | **Strong.** Data-driven book with a research system (facts, hooks, nodes, stages), whose advancement hook can gate entries and pages. Optional, data or datagen. Moves fast. |
| GuideME | `guideme-26.1.15-beta.jar` (2026-09-30) | LGPL-3.0 (Markdown lib MIT) | Weak. Markdown guides and game scenes. Its docs describe no progress or advancement gating, so gating would need a custom Java tag. |
| Guide-API VP | `Guide-API-VP-26.1.2-3.0.0-beta.1.jar` (2026-05-10) | MIT | Weak. A code-built book that becomes a hard compile dependency. Its README mentions no gating. |
| Guide Lib (ogata-mizuki) | `guide_lib-26.1.2-1.0.1.jar` (2026-08-30) | MIT | Weak. A small JSON guide library with 297 downloads. |
| Lavender | none (latest 1.21.4, Fabric/Quilt) | MIT | None on 26.1. |
| Akashic Tome | none (latest 1.21.1) | CC-BY-NC-SA-3.0 | None on 26.1. Irrelevant anyway: it is a book-merging item. |
| JEI | `jei-26.1.2-neoforge-29.47.0.111.jar` (2026-10-16) | MIT | **Good viewer hook.** Hides and unhides ingredients and recipes at runtime. |
| EMI (Unofficial Port, CF 1544558) | `emi-1.1.24-63e8aef+26.1.2+neoforge.jar` (CF file 8630349, 2026-08-12) | MIT (upstream EMI) | Viewer. Its `removeEmiStacks` and `removeRecipes` predicates run at plugin load. Upstream EMI on Modrinth has no 26.1 build (latest 1.21.1). |
| Jade | `Jade-mc26.1-NeoForge-26.1.12.jar` (26.1.12, 2026-10-13). #19 pins 26.1.11 (2026-09-11). | CC-BY-NC-SA-4.0 | Viewer. Our provider sees the player and can be server-backed. |
| Chapters | `chapters-2.2.jar` (2026-10-24) | MIT | Poor. GameStages-style item, recipe and dimension locks. Wrong shape: it locks items, not knowledge. |
| Game Stages (Darkhax) | none (latest `GameStages-Forge-1.20.1-15.0.2.jar`, 2024-10-07) | LGPL-2.1 | None on 26.1. |
| AStages | none (latest 1.21.1) | ARR | None on 26.1. |
| FTB Quests | `ftb-quests-neoforge-26.1.2.9.jar` (CF, 2026-10-07) | no licence file in repo | Poor. A quest book with advancement tasks. Heavy (needs the FTB Library stack) and pack-author-facing. |
| Theseus Quests (Heracles fork) | `theseus-neoforge-26.1.2-1.1.0.jar` (2026-10-10) | MIT | Poor. Same reasons as FTB Quests. |
| JustQuests | `JustQuests-neoforge-26.1.2-0.4.2.jar` (2026-10-26) | GPL-3.0-or-later | Poor. A datapack quest book. |
| Better Advancements | `BetterAdvancements-NeoForge-26.1.2-0.6.0.76.jar` (2026-08-03) | "Don't Be a Jerk" | Player-side polish for the vanilla surface. No integration needed. |
| Paginated Advancements | `paginatedadvancements-2.8.2+26.1-neoforge.jar` (2026-04-27) | MIT | Same as Better Advancements. |
| Advancements Reloaded | `advancements_reloaded-0.13.1-neoforge-26.1.2.jar` (2026-04-16) | MIT | Same as Better Advancements. |
| Advancement Plaques / Fancy Toasts | `AdvancementPlaques-26.1.2-neoforge-1.7.1.jar` (2026-06-23) / `fancytoasts-neoforge-26.1.x-1.4.7.jar` (2026-05-20) | CC-BY-NC-ND-4.0 / MIT | Prettier discovery toasts. No integration needed. |
| KubeJS | `kubejs-neoforge-26.1.2-8.0.7.jar` (2026-10-15) | LGPL-3.0 | For pack authors only. Nothing for the Pillar. |
| Occultism / Theurgy (klikli-dev) | `occultism-26.1.2-neoforge-1.256.1.jar` (2026-10-11), `theurgy-26.1.2-neoforge-1.116.0.jar` (2026-10-06) | MIT | **Reference.** Both use Modonomicon research for an unlock-as-you-go codex. |
| Malum, Hexerei, Goety, Ars Nouveau, Iron's Spells | none (all latest 1.21.1) | various | No 26.1 builds. Usable as design references only. |
| Spirit (terrarium-earth) | none (latest 1.19.2, Forge) | ARR | No 26.1 build. |
| Eidolon (Repraised) | none on Modrinth for NeoForge 26.1. The `eidolon` slug is a 1.21.x Paper plugin. | | No 26.1 build. |
| Scribble Book | `scribble_book-0.5.0.jar` (2026-06-04) | ARR | Interesting idea, a journal filled by examining blocks. ARR, so reference only. |

## Patchouli

- **Build:** 26.1-94-beta, `patchouli-neoforge-26.1-94.jar`, published 2026-07-10. It is the only
  26.1 build, tagged beta, and it covers 26.1 to 26.1.2. It is mirrored on CurseForge as file
  8404543. The maven is `vazkii.patchouli` on `maven.blamejared.com`.
- **Licence:** CC BY-NC-SA 3.0 (repo `LICENSE`). We would ship only our own book JSON and lang
  files, never Patchouli code.
- **Gating:** an entry's `"advancement"` field locks the entry behind that advancement. Pages can
  carry the same field, and locked pages are "simply hidden completely". Invisible advancements
  (no `display`) work as locks. `"secret": true` makes an entry invisible, rather than shown as
  "Locked", until it unlocks, and secret entries don't count toward % completion. Caveat: "All
  advancement locks can be disabled by the player in Patchouli's config."
- **Optional dependency:** books are declared in `data/<modid>/patchouli_books/<book>/book.json`,
  with content in `assets/<modid>/patchouli_books/...` when `use_resource_pack` is true. Those
  files are inert without Patchouli. The book item belongs to Patchouli, so obtaining it in game
  (a recipe or a spawn grant) would need a load condition on `patchouli`.
- **API:** `PatchouliAPI` offers opening a book or an entry (server and client), book stacks,
  config flags and custom macros. It has no unlock-state query, because unlock state *is*
  advancement progress.
- **Fit for #18:** one advancement per discoverable thing maps one-to-one onto one secret entry per
  advancement. This is the cheapest optional journal.

## Modonomicon

- **Build:** `modonomicon-26.1.2-neoforge-2.17.1.jar`, 2026-10-13. The 26.1.2 line runs from
  2.0.0 (2026-06-21) to 2.17.1 (2026-10-13), 17 minor versions in four months. The maven is on
  Cloudsmith (klikli-dev/mods).
- **Licence:** MIT for code (`LICENSES/MIT.txt`, SPDX headers), CC-BY-4.0 for assets (Modrinth).
- **Gating:** since 2.0.0 ("research system", PR #374), book conditions reference research:
  `researchNodeUnlocked` and `researchStageCompleted`, plus `and`, `or`, `not`, `mod_loaded`,
  `true` and `false` (`BookConditionTypeRegistry`). Research hooks turn game events into facts. The
  hook types are advancement completion (`AdvancementHookSpec`), entry viewed once, item acquired
  and item crafted. Facts complete nodes, and nodes group into stages. `AdvancementTriggerHandler`
  replays advancements the player already holds, so late installation works. The demo book's
  `ConditionAdvancementEntry` gates an entry and a page this way. Research definitions and state
  are synced to the client (`SyncResearchDataMessage`, `SyncResearchStateMessage`), and the book
  has a "recently unlocked" screen and research toasts.
- **Optional dependency:** books and research are data, from datagen or JSON. Datagen would need
  Modonomicon on the datagen classpath only (a `compileOnly` dependency in a separate source set),
  and the generated JSON is inert without it.
- **API:** `ModonomiconAPI` covers context helpers, multiblocks and runtime content batches, plus
  the `EntryClickedEvent` and `EntryFirstReadEvent` events. No public "is node unlocked" query was
  found in `api/`.
- **Reference use:** Occultism's 26.1.2 `OccultismResearch` uses `entry_viewed_once` hooks as read
  gates. It shows "codex unlock" done entirely on this layer.
- **Fit:** richer than Patchouli (a graph layout and research progress), but it brings a second
  progress model alongside advancements. Use it only if the advancement-hook route stays the
  single source of truth.

## Viewers: JEI, EMI, Jade (brief; the local jars are covered separately)

- **JEI** 29.47.0.111 (MIT). `IIngredientManager#removeIngredientsAtRuntime` and
  `#addIngredientsAtRuntime`, `IRecipeManager#hideRecipes` and `#unhideRecipes`, and
  `#hideRecipeCategory` and `#unhideRecipeCategory` (branch `26.1`, `Common/src/api/...`) let a
  client listener re-hide or reveal entries when advancements sync.
- **EMI** (upstream 1.21 branch API): `EmiRegistry#removeEmiStacks(Predicate)` and
  `#removeRecipes(Predicate)` "run on all current and future EmiStacks" at registration. `EmiApi`
  exposes no reload or hide call, so per-player gating means re-running the plugin (an EMI reload)
  or putting the gating inside our own recipes' and info pages' widgets. The 26.1.2 build is
  linkfgfgui's unofficial port (CF 1544558, file 8630349, build+60).
- **Jade** 26.1-neoforge branch. `Accessor#getPlayer()`, `#getServerData()` and
  `#isServerConnected()`, together with `IServerDataProvider#appendServerData` and
  `#shouldRequestData`, give a per-player hook. The provider can check discovery on the server and
  send a flag, or check `ClientAdvancements` on the client. Jade itself has no gating.

## Staging, quest and advancement-screen mods

- **Chapters** 2.2 (MIT, NeoForge 26.1) locks items, fluids, recipes, dimensions and whole mods
  behind stages, hiding them in JEI. It is aimed at pack authors and gates *use*, not
  *information*. Not a fit.
- **Game Stages** has nothing after 1.20.1 Forge (2024-10-07). AStages stops at 1.21.1.
- **FTB Quests** (26.1.2.9 on CF), **Theseus** (an MIT Heracles fork) and **JustQuests** are quest
  books. They overlap the advancement screen and are heavier, so none is worth a dependency.
- **Better Advancements**, **Paginated Advancements** and **Advancements Reloaded** all have
  26.1.2 NeoForge builds. They make a large custom advancement tab easier to browse and need
  nothing from us. They are worth a line in the README.

## Sources

- Modrinth projects and versions (API): https://api.modrinth.com/v2/project/patchouli/version, https://api.modrinth.com/v2/project/modonomicon/version, https://api.modrinth.com/v2/project/guideme/version, https://api.modrinth.com/v2/project/guide-api/version, https://api.modrinth.com/v2/project/ogata-mizuki-guide-lib, https://api.modrinth.com/v2/project/lavender, https://api.modrinth.com/v2/project/akashic-tome, https://api.modrinth.com/v2/project/jei/version, https://api.modrinth.com/v2/project/emi, https://api.modrinth.com/v2/project/jade/version, https://api.modrinth.com/v2/project/chapters, https://api.modrinth.com/v2/project/game-stages, https://api.modrinth.com/v2/project/astages, https://api.modrinth.com/v2/project/theseus-quests/version, https://api.modrinth.com/v2/project/justquests/version, https://api.modrinth.com/v2/project/better-advancements/version, https://api.modrinth.com/v2/project/paginatedadvancements/version, https://api.modrinth.com/v2/project/advancements-reloaded/version, https://api.modrinth.com/v2/project/advancement-plaques/version, https://api.modrinth.com/v2/project/fancy-toasts/version, https://api.modrinth.com/v2/project/kubejs/version, https://api.modrinth.com/v2/project/occultism/version, https://api.modrinth.com/v2/project/theurgy/version, https://api.modrinth.com/v2/project/malum, https://api.modrinth.com/v2/project/hexerei, https://api.modrinth.com/v2/project/goety, https://api.modrinth.com/v2/project/ars-nouveau, https://api.modrinth.com/v2/project/irons-spells-n-spellbooks, https://api.modrinth.com/v2/project/spirit, https://api.modrinth.com/v2/project/eidolon, https://api.modrinth.com/v2/project/scribble-book
- CurseForge file listings: https://www.curseforge.com/api/v1/mods/1544558/files (EMI Unofficial Port), https://www.curseforge.com/api/v1/mods/306770/files (Patchouli), https://www.curseforge.com/api/v1/mods/289412/files (FTB Quests), https://www.curseforge.com/api/v1/mods/268655/files (Game Stages)
- Patchouli: https://github.com/VazkiiMods/Patchouli/tree/26.1, https://github.com/VazkiiMods/Patchouli/blob/26.1/web/docs/patchouli-basics/advancement-locking.md, https://github.com/VazkiiMods/Patchouli/blob/26.1/web/docs/reference/entry-json.md, https://github.com/VazkiiMods/Patchouli/blob/26.1/web/docs/patchouli-basics/getting-started.md, https://github.com/VazkiiMods/Patchouli/blob/26.1/Xplat/src/main/java/vazkii/patchouli/api/PatchouliAPI.java, https://github.com/VazkiiMods/Patchouli/blob/26.1/LICENSE
- Modonomicon: https://github.com/klikli-dev/modonomicon/tree/version/26.1.2, https://github.com/klikli-dev/modonomicon/blob/version/26.1.2/CHANGELOG.md, https://github.com/klikli-dev/modonomicon/blob/version/26.1.2/common/src/main/java/com/klikli_dev/modonomicon/registry/BookConditionTypeRegistry.java, https://github.com/klikli-dev/modonomicon/blob/version/26.1.2/common/src/main/java/com/klikli_dev/modonomicon/api/datagen/research/AdvancementHookSpec.java, https://github.com/klikli-dev/modonomicon/blob/version/26.1.2/common/src/main/java/com/klikli_dev/modonomicon/research/hook/handlers/AdvancementTriggerHandler.java, https://github.com/klikli-dev/modonomicon/blob/version/26.1.2/common/src/main/java/com/klikli_dev/modonomicon/datagen/book/demo/features/ConditionAdvancementEntry.java, https://github.com/klikli-dev/modonomicon/blob/version/26.1.2/common/src/main/java/com/klikli_dev/modonomicon/api/ModonomiconAPI.java, https://klikli-dev.github.io/modonomicon/
- Occultism research: https://github.com/klikli-dev/occultism/blob/version/26.1.2/src/main/java/com/klikli_dev/occultism/datagen/OccultismResearch.java
- GuideME: https://github.com/AppliedEnergistics/GuideME/blob/main/docs/docs/20-data-driven-guides.md, https://github.com/AppliedEnergistics/GuideME/blob/main/docs/docs/99-license.md, https://github.com/AppliedEnergistics/GuideME/blob/main/docs/docs/02-changelog.md
- Guide-API VP: https://github.com/maxanier/Guide-API
- JEI: https://github.com/mezz/JustEnoughItems/blob/26.1/Common/src/api/java/mezz/jei/api/runtime/IIngredientManager.java, https://github.com/mezz/JustEnoughItems/blob/26.1/Common/src/api/java/mezz/jei/api/recipe/IRecipeManager.java
- EMI: https://github.com/emilyploszaj/emi/blob/1.21/xplat/src/main/java/dev/emi/emi/api/EmiRegistry.java, https://github.com/emilyploszaj/emi/blob/1.21/xplat/src/main/java/dev/emi/emi/api/EmiApi.java
- Jade: https://github.com/Snownee/Jade/blob/26.1-neoforge/src/main/java/snownee/jade/api/Accessor.java, https://github.com/Snownee/Jade/blob/26.1-neoforge/src/main/java/snownee/jade/api/IServerDataProvider.java
- Chapters: https://modrinth.com/mod/chapters, https://github.com/GabinFqt/chapters
- FTB Quests: https://github.com/FTBTeam/FTB-Quests (no LICENSE file at repo root)
