// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.mojang.authlib.GameProfile;
import io.github._5thlayer.atouchofmadness.discovery.Discovery;
import io.github._5thlayer.atouchofmadness.environment.EnvironmentRules;
import io.github._5thlayer.atouchofmadness.sanity.Cause;
import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import io.github._5thlayer.atouchofmadness.sanity.Sanity;
import io.github._5thlayer.atouchofmadness.shadow.ShadowCreature;
import io.github._5thlayer.atouchofmadness.shadow.ShadowCreatures;
import io.github._5thlayer.atouchofmadness.shadow.ShadowRules;
import io.github._5thlayer.atouchofmadness.shadow.ShadowSpawner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.gametest.framework.TestEnvironmentDefinition.Weather;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * The Shadow Creatures (ADR 0002). Each player Uneasy or Insane, in Survival or Adventure, gets up to
 * their cap, 1 at 50, 2 at 35, 3 at 20 and 4 at 10 and below, Gaunts only below 10; light doesn't
 * matter, and a dimension type outside {@code #atouchofmadness:shadows} gets none. Each creature is
 * sent only to its Owner and Insane players, who alone can hit it, even one standing still as their
 * Stage changes; Unreal while the Owner is Uneasy, harming and harmed by nothing, fading when
 * approached or struck; Real while they are Insane, hunting and killable, drifting through walls,
 * landing no hit from inside a block but coming out beside its Owner to strike, dropping Umbra, still
 * on Peaceful. It goes with its Owner's logout, death, change of dimension and sleep, and when their
 * cap drops, never stops them sleeping, and is never saved. Its Owner's own nearby drain 2/min for a
 * Lurker and 4 for a Gaunt, twice that while Real; a Real one's hit takes 5; carrying Umbra drains
 * 1/min. Each shared rule is one body run for the Lurker and the Gaunt in turn, as two tests.
 *
 * <p>The game-test server turns {@code doMobSpawning} off, so no attempt comes but those a test makes
 * through {@link ShadowSpawner#attempt}. Each test joins players of its own and logs them out however
 * it ends, which takes their creatures with them, and marks their client loaded, so they can be harmed.
 * The tests are padded, so no Survival player in one
 * is company to a player in another, and those that spawn are padded past the farthest spawn distance.
 */
final class ShadowTests {

    /** A creature of each kind, its weight nearby, the Umbra it drops and the Sanity its kill restores. */
    record Kind(String name, Supplier<? extends EntityType<? extends ShadowCreature>> type, double weight, int minUmbra, int maxUmbra,
            double kill) {
    }

    static final Kind LURKER = new Kind("lurker", ShadowCreatures.LURKER, 2, 1, 2, 10);
    static final Kind GAUNT = new Kind("gaunt", ShadowCreatures.GAUNT, 4, 2, 4, 20);
    private static final List<Kind> KINDS = List.of(LURKER, GAUNT);

    /** The platform is 9 by 9 with its floor at y 0; a player stands on it at y 1. */
    private static final BlockPos MIDDLE = new BlockPos(4, 1, 4);

    /** Two evaluations, so one has come whichever tick of a second is the player's own. */
    private static final int EVALUATED = 2 * PlayerSanity.EVALUATION_TICKS;

    /** A second, long enough for anything inside a block to take suffocation damage. */
    private static final int SUFFOCATION_TICKS = 20;

    /** Wider than the company radius. */
    private static final int PADDING = 9;

    /** Wider than the farthest spawn distance, so no creature appears in another test. */
    private static final int SPAWN_PADDING = 25;

    /** Uneasy, Insane, and Insane below the Gaunts' threshold. */
    private static final double UNEASY = 40;
    private static final double INSANE = 15;
    private static final double GAUNTS = 5;

    /** A restore of 1 a minute, as a rate in Sanity a tick. */
    private static final double PER_MINUTE = -EnvironmentRules.drainPerTick(1);

    /** Each player's id and the id of each entity vanilla's tracker sends them, by NeoForge's tracking events. */
    private static final Set<String> TRACKING = ConcurrentHashMap.newKeySet();

    static {
        NeoForge.EVENT_BUS.addListener(PlayerEvent.StartTracking.class, event -> TRACKING.add(tracking(event.getEntity(), event.getTarget())));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.StopTracking.class, event -> TRACKING.remove(tracking(event.getEntity(), event.getTarget())));
    }

    private ShadowTests() {
    }

    static void register(ATouchOfMadnessGameTests.Registrar defaults) {
        var spawning = defaults.padded(SPAWN_PADDING);
        spawning.test("shadow_a_sane_player_gets_none", 1, ShadowTests::sane);
        spawning.test("shadow_each_stage_below_sane_gets_up_to_its_cap_and_no_more", 1, ShadowTests::upToTheCap);
        spawning.test("shadow_gaunts_come_only_below_ten_within_the_shared_cap", 1, ShadowTests::gaunts);
        spawning.test("shadow_creative_and_spectator_players_get_none", 1, ShadowTests::creativeAndSpectator);
        spawning.test("shadow_noon_daylight_does_not_stop_them", 1, ShadowTests::daylight);
        // The pack replaces #atouchofmadness:shadows with the Nether alone, for every test in its batch.
        defaults.under("shadow_pack", new TestEnvironmentDefinition.AllOf(new Sky(Sky.NOON, Weather.Type.CLEAR), new WithTestPack("shadows")))
                .padded(SPAWN_PADDING)
                .test("shadow_a_pack_keeps_them_out_of_a_dimension_type_it_leaves_out", 1, ShadowTests::pack);

        var tests = defaults.padded(PADDING);
        tests.test("discovery_an_own_shadow_creature_nearby_discovers_it", EVALUATED + 1, ShadowTests::discoverCreature);
        tests.test("discovery_a_hit_from_a_real_shadow_creature_discovers_the_strike", 1, ShadowTests::discoverStrike);
        tests.test("discovery_killing_a_real_shadow_creature_discovers_the_kill", 1, ShadowTests::discoverKill);
        tests.test("discovery_carrying_umbra_discovers_it", EVALUATED + 1, ShadowTests::discoverUmbra);
        tests.test("shadow_carrying_any_umbra_drains_one_however_much", EVALUATED + 1, ShadowTests::umbra);

        var peaceful = defaults.under("shadow_peaceful", new TestEnvironmentDefinition.AllOf(new Sky(Sky.NOON, Weather.Type.CLEAR), new Peaceful()))
                .padded(PADDING);
        for (var kind : KINDS) {
            String name = kind.name();
            tests.test("shadow_phase_a_real_" + name + "_reaches_its_owner_through_a_wall", 200, helper -> throughAWall(helper, kind));
            tests.test("shadow_phase_a_" + name + "_inside_a_block_is_unharmed_and_lands_no_hit", SUFFOCATION_TICKS + 1, helper -> insideABlock(helper, kind));
            tests.test("shadow_phase_a_real_" + name + "_comes_out_of_a_wall_to_keep_striking_its_owner", 300, helper -> againstAWall(helper, kind));
            tests.test("shadow_a_real_" + name + "_drops_its_umbra", 1, helper -> drops(helper, kind));
            tests.test("shadow_a_" + name + "_is_sent_to_its_owner_and_the_insane_and_only_they_hit_it", 1, helper -> sentTo(helper, kind));
            tests.test("shadow_an_unreal_" + name + "_takes_and_deals_no_harm_and_fades_when_struck", ShadowCreature.FADE_TICKS + 5,
                    helper -> unrealStruck(helper, kind));
            tests.test("shadow_an_unreal_" + name + "_fades_when_approached", ShadowCreature.FADE_TICKS + 5, helper -> approached(helper, kind));
            tests.test("shadow_a_real_" + name + "_attacks_its_owner_and_can_be_killed", 200, helper -> attacks(helper, kind));
            tests.test("shadow_a_player_standing_still_is_sent_anothers_" + name + "_only_while_insane", 20, helper -> retracked(helper, kind));
            tests.test("shadow_raising_sanity_above_twenty_turns_a_" + name + "_unreal", 2, helper -> turnsUnreal(helper, kind));
            peaceful.test("shadow_on_peaceful_a_real_" + name + "_deals_nothing_stays_and_drops_umbra", 60, helper -> onPeaceful(helper, kind));
            tests.test("shadow_a_" + name + "_goes_when_its_owner_logs_out", 5, helper -> goesWhen(helper, kind, (h, owner) -> TestPlayers.playerList(h).remove(owner)));
            tests.test("shadow_a_" + name + "_goes_when_its_owner_dies", 5, helper -> goesWhen(helper, kind, (h, owner) -> owner.kill(h.getLevel())));
            tests.test("shadow_a_" + name + "_goes_when_its_owner_changes_dimension", 5, helper -> goesWhen(helper, kind, ShadowTests::toTheNether));
            tests.test("shadow_a_real_" + name + "_never_keeps_its_owner_from_sleeping_and_goes_as_they_sleep", 5, helper -> sleeps(helper, kind));
            tests.test("shadow_the_farthest_" + name + "_goes_when_the_cap_drops", PlayerSanity.EVALUATION_TICKS + 2, helper -> capDrops(helper, kind));
            tests.test("shadow_a_" + name + "_is_never_saved", 1, helper -> neverSaved(helper, kind));
            tests.test("shadow_an_unreal_" + name + "_of_ones_own_nearby_drains_its_weight", EVALUATED + 1,
                    helper -> drains(helper, kind, UNEASY, false, kind.weight()));
            tests.test("shadow_a_real_" + name + "_of_ones_own_nearby_drains_twice_its_weight", EVALUATED + 1,
                    helper -> drains(helper, kind, INSANE, false, 2 * kind.weight()));
            tests.test("shadow_another_players_" + name + "_drains_nothing", EVALUATED + 1, helper -> drains(helper, kind, INSANE, true, 0));
            tests.test("shadow_a_hit_from_a_real_" + name + "_takes_five_sanity", 1, helper -> strike(helper, kind));
            tests.test("shadow_kill_the_owner_killing_a_real_" + name + "_restores_" + (int) kind.kill(), 1, helper -> ownerKills(helper, kind));
            tests.test("shadow_kill_an_insane_player_killing_anothers_" + name + "_gains_it_and_the_owner_nothing", 1,
                    helper -> anotherKills(helper, kind));
            tests.test("shadow_kill_a_real_" + name + "_shot_by_its_owner_restores_it", 1, helper -> shot(helper, kind));
            tests.test("shadow_kill_a_real_" + name + "_dying_of_no_player_restores_nothing", 1, helper -> diesOfNoPlayer(helper, kind));
            tests.test("shadow_kill_an_unreal_" + name + "_fading_restores_nothing", ShadowCreature.FADE_TICKS + 5,
                    helper -> fadesForNothing(helper, kind));
            tests.test("shadow_kill_a_real_" + name + "_vanishing_restores_nothing", 5, helper -> vanishesForNothing(helper, kind));
        }
    }

    // Spawning.

    private static void sane(GameTestHelper helper) {
        ground(helper);
        var player = join(helper, MIDDLE, 60, GameType.SURVIVAL);
        attempts(player, 10);
        expectCount(helper, player, 0, "a Sane player");
        helper.succeed();
    }

    /** One player for each cap, each given more attempts than it allows, each creature 12 to 24 blocks away, standing. */
    private static void upToTheCap(GameTestHelper helper) {
        ground(helper);
        double[][] caps = {{50, 1}, {35, 2}, {20, 3}, {10, 4}, {GAUNTS, 4}};
        for (var cap : caps) {
            var player = join(helper, MIDDLE, cap[0], GameType.SURVIVAL);
            attempts(player, 8);
            expectCount(helper, player, (int) cap[1], "a player at " + cap[0]);
            for (var creature : ShadowCreatures.of(player)) {
                double distance = Math.hypot(creature.getX() - player.getX(), creature.getZ() - player.getZ());
                // A creature stands in the middle of a block within the distances.
                if (distance < 12 - 0.75 || distance > 24 + 0.75) {
                    helper.fail("a creature appeared " + distance + " blocks away, not 12 to 24");
                }
                var below = creature.blockPosition().below();
                if (!creature.level().getBlockState(below).isFaceSturdy(creature.level(), below, Direction.UP)) {
                    helper.fail("a creature appeared on nothing it can stand on, at " + creature.blockPosition());
                }
            }
        }
        helper.succeed();
    }

    /** At 15 thirty attempts bring no Gaunt; at 5 thirty bring one, and the cap of 4 holds Lurkers and Gaunts together. */
    private static void gaunts(GameTestHelper helper) {
        ground(helper);
        var above = join(helper, MIDDLE, INSANE, GameType.SURVIVAL);
        var below = join(helper, MIDDLE, GAUNTS, GameType.SURVIVAL);
        if (kindsOver(above, 30).contains(ShadowCreatures.GAUNT.get())) {
            helper.fail("a Gaunt came to a player at " + INSANE);
        }
        if (!kindsOver(below, 30).contains(ShadowCreatures.GAUNT.get())) {
            helper.fail("no Gaunt came to a player at " + GAUNTS + " in thirty attempts");
        }
        attempts(below, 10);
        expectCount(helper, below, 4, "a player at " + GAUNTS);
        helper.succeed();
    }

    /** The kind of creature each of {@code times} attempts brings, each removed again before the next. */
    private static List<EntityType<?>> kindsOver(ServerPlayer player, int times) {
        List<EntityType<?>> kinds = new ArrayList<>();
        for (int i = 0; i < times; i++) {
            ShadowSpawner.attempt(player).ifPresent(creature -> {
                kinds.add(creature.getType());
                creature.discard();
            });
        }
        return kinds;
    }

    private static void creativeAndSpectator(GameTestHelper helper) {
        ground(helper);
        for (var mode : List.of(GameType.CREATIVE, GameType.SPECTATOR)) {
            var player = join(helper, MIDDLE, GAUNTS, mode);
            attempts(player, 4);
            expectCount(helper, player, 0, "a " + mode.getName() + " player");
        }
        helper.succeed();
    }

    /** The platform's open floor at noon, in full daylight. */
    private static void daylight(GameTestHelper helper) {
        ground(helper);
        var player = join(helper, MIDDLE, INSANE, GameType.SURVIVAL);
        if (helper.getLevel().getMaxLocalRawBrightness(player.blockPosition()) < 15) {
            helper.fail("the platform is not in full daylight: the fixture tests nothing");
        }
        attempts(player, 1);
        helper.succeed();
    }

    private static void pack(GameTestHelper helper) {
        ground(helper);
        var player = join(helper, MIDDLE, GAUNTS, GameType.SURVIVAL);
        attempts(player, 4);
        expectCount(helper, player, 0, "a player in the Overworld, which the pack leaves out of #shadows");
        helper.succeed();
    }

    /**
     * A stone floor level with the platform's, out past the farthest spawn distance, taken up again
     * however the test ends: the tests stand high over the game-test world's ground. It stops short of the
     * test's barrier walls.
     */
    private static void ground(GameTestHelper helper) {
        int reach = (int) ShadowRules.DEFAULT.maxDistance();
        List<BlockPos> floor = new ArrayList<>();
        for (int x = -reach; x <= 8 + reach; x++) {
            for (int z = -reach; z <= 8 + reach; z++) {
                if (x < -1 || x > 9 || z < -1 || z > 9) {
                    floor.add(new BlockPos(x, 0, z));
                }
            }
        }
        floor.forEach(pos -> helper.setBlock(pos, Blocks.STONE));
        TestPlayers.whenDone(helper, () -> floor.forEach(pos -> helper.setBlock(pos, Blocks.AIR)));
    }

    private static void attempts(ServerPlayer player, int times) {
        for (int i = 0; i < times; i++) {
            ShadowSpawner.attempt(player);
        }
    }

    private static void expectCount(GameTestHelper helper, ServerPlayer player, int count, String who) {
        int found = ShadowCreatures.of(player).size();
        if (found != count) {
            helper.fail(who + " has " + found + " Shadow Creatures, not " + count);
        }
    }

    // A Real creature.

    /** A wall across the platform, floor to ceiling: the creature, on the far side, drifts through it to strike. */
    private static void throughAWall(GameTestHelper helper, Kind kind) {
        wall(helper, 4);
        var owner = join(helper, new BlockPos(1, 1, 4), INSANE, GameType.SURVIVAL);
        place(helper, kind, owner, new BlockPos(7, 1, 4));
        float health = owner.getHealth();
        helper.onEachTick(() -> {
            if (owner.getHealth() < health) {
                helper.succeed();
            }
        });
    }

    /** A stone wall across the platform at {@code x}, floor to ceiling. */
    private static void wall(GameTestHelper helper, int x) {
        for (int y = 1; y <= 4; y++) {
            for (int z = 0; z <= 8; z++) {
                helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
            }
        }
    }

    /**
     * A Real creature in a stone pillar beside its Owner: a second in, it is unharmed and its hit lands
     * nowhere; with the pillar gone, its hit lands.
     */
    private static void insideABlock(GameTestHelper helper, Kind kind) {
        var at = new BlockPos(2, 1, 4);
        helper.setBlock(at, Blocks.STONE);
        helper.setBlock(at.above(), Blocks.STONE);
        var owner = join(helper, new BlockPos(1, 1, 4), INSANE, GameType.SURVIVAL);
        var creature = place(helper, kind, owner, at);
        creature.setNoAi(true);
        helper.runAtTickTime(SUFFOCATION_TICKS, () -> {
            if (creature.getHealth() < creature.getMaxHealth()) {
                helper.fail("a " + kind.name() + " inside a block was harmed");
            }
            if (!creature.isInsideBlocks() || creature.doHurtTarget(helper.getLevel(), owner)) {
                helper.fail("a " + kind.name() + " inside a block landed a hit");
            }
            helper.setBlock(at, Blocks.AIR);
            helper.setBlock(at.above(), Blocks.AIR);
            if (!creature.doHurtTarget(helper.getLevel(), owner)) {
                helper.fail("a " + kind.name() + " clear of the pillar landed no hit");
            }
            helper.succeed();
        });
    }

    /**
     * Its Owner stands flush against a wall, and the creature comes at them through it, from behind: a
     * Gaunt, wider than a player, would be in the wall centred on them, so it comes out beside them and
     * strikes again and again. The Owner, never ticked, would stay invulnerable after the first hit, so
     * the test lets it lapse; each strike shows in their Sanity, and three show it struck once out.
     */
    private static void againstAWall(GameTestHelper helper, Kind kind) {
        wall(helper, 0);
        var owner = join(helper, new BlockPos(1, 1, 4), INSANE, GameType.SURVIVAL);
        var flush = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 4))).add(owner.getBbWidth() / 2 - 0.5, 0, 0);
        owner.teleport(new TeleportTransition(helper.getLevel(), flush, Vec3.ZERO, 0, 0, TeleportTransition.DO_NOTHING));
        place(helper, kind, owner, new BlockPos(0, 1, 4));
        helper.onEachTick(() -> {
            owner.invulnerableTime = 0;
            owner.setHealth(owner.getMaxHealth());
            if (PlayerSanity.get(owner).value() <= INSANE - 3 * 5) {
                helper.succeed();
            }
        });
    }

    private static void attacks(GameTestHelper helper, Kind kind) {
        var owner = join(helper, new BlockPos(1, 1, 4), INSANE, GameType.SURVIVAL);
        var creature = place(helper, kind, owner, new BlockPos(5, 1, 4));
        float health = owner.getHealth();
        helper.onEachTick(() -> {
            if (owner.getHealth() >= health) {
                return;
            }
            creature.hurtServer(helper.getLevel(), owner.damageSources().playerAttack(owner), 1000);
            if (!creature.isDeadOrDying()) {
                helper.fail("a Real " + kind.name() + " was not killed by its Owner");
            }
            helper.succeed();
        });
    }

    /** Each of several kills drops between the kind's least and most Umbra; each kill's restore is undone, so the next is Real. */
    private static void drops(GameTestHelper helper, Kind kind) {
        var owner = join(helper, new BlockPos(1, 1, 4), INSANE, GameType.SURVIVAL);
        for (int i = 0; i < 8; i++) {
            PlayerSanity.set(owner, new Sanity(INSANE));
            var creature = place(helper, kind, owner, new BlockPos(5, 1, 4));
            creature.hurtServer(helper.getLevel(), owner.damageSources().playerAttack(owner), 1000);
            int umbra = takeUmbra(helper);
            if (umbra < kind.minUmbra() || umbra > kind.maxUmbra()) {
                helper.fail("a Real " + kind.name() + " dropped " + umbra + " Umbra, not " + kind.minUmbra() + " to " + kind.maxUmbra());
            }
        }
        helper.succeed();
    }

    /** Removes every dropped item in the test and counts the Umbra among them. */
    private static int takeUmbra(GameTestHelper helper) {
        int umbra = 0;
        for (var item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds())) {
            if (item.getItem().is(ShadowCreatures.UMBRA.get())) {
                umbra += item.getItem().getCount();
            }
            item.discard();
        }
        return umbra;
    }

    /** The Owner and an Insane player are sent it and a Sane one isn't; while it is Real, the Sane one's hit does nothing and the Insane one's harms it. */
    private static void sentTo(GameTestHelper helper, Kind kind) {
        var owner = join(helper, new BlockPos(0, 1, 0), INSANE, GameType.SURVIVAL);
        var sane = join(helper, new BlockPos(8, 1, 0), 60, GameType.SURVIVAL);
        var insane = join(helper, new BlockPos(0, 1, 8), GAUNTS, GameType.SURVIVAL);
        var creature = place(helper, kind, owner, new BlockPos(8, 1, 8));
        creature.setNoAi(true);
        if (!creature.broadcastToPlayer(owner) || creature.broadcastToPlayer(sane) || !creature.broadcastToPlayer(insane)) {
            helper.fail("a " + kind.name() + " is not sent to its Owner and the Insane alone");
        }
        float health = creature.getHealth();
        creature.hurtServer(helper.getLevel(), sane.damageSources().playerAttack(sane), 1);
        if (creature.getHealth() != health || creature.isFading()) {
            helper.fail("a Sane player who can't see a " + kind.name() + " hit it");
        }
        creature.hurtServer(helper.getLevel(), insane.damageSources().playerAttack(insane), 1);
        if (creature.getHealth() >= health) {
            helper.fail("an Insane player could not hit a Real " + kind.name());
        }
        helper.succeed();
    }

    /**
     * A Sane player, still, starts being sent another's creature as they turn Insane, and stops as they
     * turn Sane again. The creature is placed once both players' chunk is tracked, which takes the
     * tracker a tick or two after they join, since only a move would have it ask again.
     */
    private static void retracked(GameTestHelper helper, Kind kind) {
        var owner = join(helper, new BlockPos(0, 1, 0), INSANE, GameType.SURVIVAL);
        var viewer = join(helper, new BlockPos(8, 1, 8), 60, GameType.SURVIVAL);
        var chunkMap = helper.getLevel().getChunkSource().chunkMap;
        var chunk = ChunkPos.containing(helper.absolutePos(MIDDLE));
        ShadowCreature[] creature = {null};
        helper.onEachTick(() -> {
            if (creature[0] == null) {
                var players = chunkMap.getPlayers(chunk, false);
                if (players.contains(owner) && players.contains(viewer)) {
                    creature[0] = place(helper, kind, owner, MIDDLE);
                    creature[0].setNoAi(true);
                    TestPlayers.whenDone(helper, () -> TRACKING.removeIf(key -> key.endsWith("/" + creature[0].getId())));
                }
                return;
            }
            if (!TRACKING.contains(tracking(owner, creature[0]))) {
                helper.fail("the Owner is not sent their " + kind.name() + ": the fixture tests nothing");
            }
            expectTracked(helper, viewer, creature[0], false, "a Sane player");
            PlayerSanity.set(viewer, new Sanity(GAUNTS));
            expectTracked(helper, viewer, creature[0], true, "a player who turned Insane");
            PlayerSanity.set(viewer, new Sanity(60));
            expectTracked(helper, viewer, creature[0], false, "a player Sane again");
            helper.succeed();
        });
    }

    private static void expectTracked(GameTestHelper helper, ServerPlayer player, ShadowCreature creature, boolean tracked, String who) {
        if (TRACKING.contains(tracking(player, creature)) != tracked) {
            helper.fail(who + (tracked ? " is not" : " is") + " sent another's " + creature.getType().toShortString());
        }
    }

    private static String tracking(Entity player, Entity entity) {
        return player.getUUID() + "/" + entity.getId();
    }

    private static void turnsUnreal(GameTestHelper helper, Kind kind) {
        var owner = join(helper, new BlockPos(1, 1, 4), INSANE, GameType.SURVIVAL);
        var creature = place(helper, kind, owner, new BlockPos(7, 1, 4));
        if (!creature.isReal()) {
            helper.fail("a " + kind.name() + " of an Insane Owner is not Real");
        }
        PlayerSanity.set(owner, new Sanity(UNEASY));
        if (creature.isReal()) {
            helper.fail("a " + kind.name() + " is still Real once its Owner is Uneasy");
        }
        helper.runAtTickTime(1, () -> {
            if (creature.getTarget() != null) {
                helper.fail("an Unreal " + kind.name() + " still hunts its Owner");
            }
            helper.succeed();
        });
    }

    /** Next to its Owner on Peaceful, no hit lands; vanilla's despawn leaves it, and killed it drops Umbra. */
    private static void onPeaceful(GameTestHelper helper, Kind kind) {
        var owner = join(helper, new BlockPos(1, 1, 4), INSANE, GameType.SURVIVAL);
        var creature = place(helper, kind, owner, new BlockPos(2, 1, 4));
        float health = owner.getHealth();
        if (creature.doHurtTarget(helper.getLevel(), owner)) {
            helper.fail("a Real " + kind.name() + "'s hit landed on Peaceful");
        }
        helper.onEachTick(() -> creature.checkDespawn());
        helper.runAtTickTime(50, () -> {
            if (owner.getHealth() < health || PlayerSanity.get(owner).value() < INSANE) {
                helper.fail("a Real " + kind.name() + " harmed its Owner on Peaceful");
            }
            if (creature.isRemoved()) {
                helper.fail("a Real " + kind.name() + " was despawned on Peaceful");
            }
            creature.hurtServer(helper.getLevel(), owner.damageSources().playerAttack(owner), 1000);
            if (takeUmbra(helper) < kind.minUmbra()) {
                helper.fail("a Real " + kind.name() + " killed on Peaceful dropped no Umbra");
            }
            helper.succeed();
        });
    }

    // An Unreal creature.

    /** Its Owner's hit harms it not at all and fades it; its own hit on them lands nowhere; it pushes nothing. */
    private static void unrealStruck(GameTestHelper helper, Kind kind) {
        var owner = join(helper, new BlockPos(1, 1, 4), UNEASY, GameType.SURVIVAL);
        var creature = place(helper, kind, owner, new BlockPos(7, 1, 4));
        creature.setNoAi(true);
        float health = owner.getHealth();
        if (creature.doHurtTarget(helper.getLevel(), owner) || owner.getHealth() < health) {
            helper.fail("an Unreal " + kind.name() + " harmed its Owner");
        }
        if (creature.isPushable()) {
            helper.fail("an Unreal " + kind.name() + " collides");
        }
        creature.hurtServer(helper.getLevel(), owner.damageSources().playerAttack(owner), 5);
        if (creature.getHealth() < creature.getMaxHealth()) {
            helper.fail("an Unreal " + kind.name() + " was harmed");
        }
        if (!creature.isFading()) {
            helper.fail("an Unreal " + kind.name() + " struck did not fade");
        }
        helper.onEachTick(() -> {
            if (creature.isRemoved()) {
                helper.succeed();
            }
        });
    }

    private static void approached(GameTestHelper helper, Kind kind) {
        var owner = join(helper, new BlockPos(1, 1, 4), UNEASY, GameType.SURVIVAL);
        var creature = place(helper, kind, owner, new BlockPos(7, 1, 4));
        creature.setNoAi(true);
        helper.runAtTickTime(1, () -> {
            if (creature.isFading()) {
                helper.fail("an Unreal " + kind.name() + " 6 blocks off faded");
            }
            teleport(helper, owner, new BlockPos(4, 1, 4));
        });
        helper.onEachTick(() -> {
            if (creature.isRemoved()) {
                helper.succeed();
            }
        });
    }

    // Going.

    /** The creature is gone within a tick or two of {@code act} doing something to its Owner. */
    private static void goesWhen(GameTestHelper helper, Kind kind, BiConsumer<GameTestHelper, ServerPlayer> act) {
        var owner = join(helper, new BlockPos(1, 1, 4), INSANE, GameType.SURVIVAL);
        var creature = place(helper, kind, owner, new BlockPos(7, 1, 4));
        creature.setNoAi(true);
        helper.runAtTickTime(1, () -> act.accept(helper, owner));
        helper.onEachTick(() -> {
            if (creature.isRemoved()) {
                helper.succeed();
            }
        });
    }

    private static void toTheNether(GameTestHelper helper, ServerPlayer player) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        player.teleport(new TeleportTransition(nether, Vec3.atBottomCenterOf(helper.absolutePos(MIDDLE)), Vec3.ZERO, 0, 0,
                TeleportTransition.DO_NOTHING));
    }

    /**
     * A Real creature beside the bed is none of the monsters vanilla's bed finds keeping a player from
     * sleeping, in the box it searches, and it goes once its Insane Owner lies down. The night itself,
     * and the bed's other checks, are vanilla's, and left to it.
     */
    private static void sleeps(GameTestHelper helper, Kind kind) {
        var foot = new BlockPos(4, 1, 5);
        var head = foot.north();
        helper.setBlock(foot, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.FOOT));
        helper.setBlock(head, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.HEAD));
        var owner = join(helper, foot.east(), INSANE, GameType.SURVIVAL);
        var creature = place(helper, kind, owner, foot.west(3));
        creature.setNoAi(true);
        var level = helper.getLevel();
        var keepingAwake = level.getEntitiesOfClass(Monster.class, new AABB(helper.absolutePos(foot)).inflate(8, 5, 8),
                monster -> monster.isPreventingPlayerRest(level, owner));
        if (!keepingAwake.isEmpty()) {
            helper.fail("a Real " + kind.name() + " keeps its Owner from sleeping");
        }
        owner.startSleeping(helper.absolutePos(foot));
        helper.onEachTick(() -> {
            if (creature.isRemoved()) {
                helper.succeed();
            }
        });
    }

    /** Two creatures at 10, cap 4; at 40 the cap is 1, and by the next second the farther has gone. */
    private static void capDrops(GameTestHelper helper, Kind kind) {
        var owner = join(helper, new BlockPos(1, 1, 4), 10, GameType.SURVIVAL);
        var nearer = place(helper, kind, owner, new BlockPos(6, 1, 4));
        var farther = place(helper, kind, owner, new BlockPos(8, 1, 4));
        nearer.setNoAi(true);
        farther.setNoAi(true);
        PlayerSanity.set(owner, new Sanity(UNEASY));
        helper.onEachTick(() -> {
            owner.doTick();
            if (!farther.isRemoved()) {
                return;
            }
            if (nearer.isRemoved()) {
                helper.fail("both " + kind.name() + "s went when the cap fell to 1");
            }
            helper.succeed();
        });
    }

    private static void neverSaved(GameTestHelper helper, Kind kind) {
        var owner = join(helper, new BlockPos(1, 1, 4), INSANE, GameType.SURVIVAL);
        var creature = place(helper, kind, owner, new BlockPos(7, 1, 4));
        if (creature.getType().canSerialize() || creature.shouldBeSaved() || creature.getEncodeId() != null) {
            helper.fail("a " + kind.name() + " would be saved with its chunk");
        }
        helper.succeed();
    }

    // Drains.

    /**
     * A creature of the kind six blocks from a player at {@code sanity}, theirs or another's, and once an
     * evaluation has come their Net rate is {@code perMinute} a minute of drain. The other Owner stands
     * in the far corner, beyond the company radius.
     */
    private static void drains(GameTestHelper helper, Kind kind, double sanity, boolean anothers, double perMinute) {
        var player = join(helper, new BlockPos(0, 1, 0), sanity, GameType.SURVIVAL);
        var owner = anothers ? join(helper, new BlockPos(8, 1, 8), sanity, GameType.SURVIVAL) : player;
        place(helper, kind, owner, new BlockPos(6, 1, 0)).setNoAi(true);
        helper.onEachTick(() -> {
            player.doTick();
            if (helper.getTick() >= EVALUATED) {
                expectNetRate(helper, player, -perMinute);
                helper.succeed();
            }
        });
    }

    private static void strike(GameTestHelper helper, Kind kind) {
        var owner = join(helper, new BlockPos(1, 1, 4), INSANE, GameType.SURVIVAL);
        var creature = place(helper, kind, owner, new BlockPos(2, 1, 4));
        if (!creature.doHurtTarget(helper.getLevel(), owner)) {
            helper.fail("a Real " + kind.name() + "'s hit didn't land");
        }
        expectSanity(helper, owner, INSANE - 5, "the Owner, hit by a Real " + kind.name() + ",");
        helper.succeed();
    }

    // Kills.

    private static void ownerKills(GameTestHelper helper, Kind kind) {
        var owner = join(helper, new BlockPos(1, 1, 4), INSANE, GameType.SURVIVAL);
        var creature = place(helper, kind, owner, new BlockPos(5, 1, 4));
        creature.hurtServer(helper.getLevel(), owner.damageSources().playerAttack(owner), 1000);
        expectSanity(helper, owner, INSANE + kind.kill(), "the Owner, who killed a Real " + kind.name() + ",");
        helper.succeed();
    }

    private static void anotherKills(GameTestHelper helper, Kind kind) {
        var owner = join(helper, new BlockPos(0, 1, 0), INSANE, GameType.SURVIVAL);
        var killer = join(helper, new BlockPos(0, 1, 8), GAUNTS, GameType.SURVIVAL);
        var creature = place(helper, kind, owner, new BlockPos(8, 1, 8));
        creature.hurtServer(helper.getLevel(), killer.damageSources().playerAttack(killer), 1000);
        expectSanity(helper, killer, GAUNTS + kind.kill(), "an Insane player who killed another's Real " + kind.name() + ",");
        expectSanity(helper, owner, INSANE, "the Owner of a " + kind.name() + " another killed");
        helper.succeed();
    }

    /** A snowball the Owner threw: credit goes to the thrower, not the projectile. */
    private static void shot(GameTestHelper helper, Kind kind) {
        var owner = join(helper, new BlockPos(1, 1, 4), INSANE, GameType.SURVIVAL);
        var creature = place(helper, kind, owner, new BlockPos(5, 1, 4));
        var snowball = EntityType.SNOWBALL.create(helper.getLevel(), EntitySpawnReason.EVENT);
        creature.hurtServer(helper.getLevel(), owner.damageSources().thrown(snowball, owner), 1000);
        expectSanity(helper, owner, INSANE + kind.kill(), "the Owner, who shot a Real " + kind.name() + ",");
        helper.succeed();
    }

    /** {@code /kill}'s damage and a wolf's bite each kill one, and its Owner, beside it, gains nothing. */
    private static void diesOfNoPlayer(GameTestHelper helper, Kind kind) {
        var owner = join(helper, new BlockPos(1, 1, 4), INSANE, GameType.SURVIVAL);
        var wolf = EntityType.WOLF.create(helper.getLevel(), EntitySpawnReason.EVENT);
        var level = helper.getLevel();
        for (var source : List.of(level.damageSources().genericKill(), level.damageSources().mobAttack(wolf))) {
            var creature = place(helper, kind, owner, new BlockPos(5, 1, 4));
            creature.hurtServer(level, source, 1000);
            if (!creature.isDeadOrDying()) {
                helper.fail("a Real " + kind.name() + " survived " + source.getMsgId() + ": the fixture tests nothing");
            }
            expectSanity(helper, owner, INSANE, "the Owner of a " + kind.name() + " killed by " + source.getMsgId());
        }
        helper.succeed();
    }

    private static void fadesForNothing(GameTestHelper helper, Kind kind) {
        var owner = join(helper, new BlockPos(1, 1, 4), UNEASY, GameType.SURVIVAL);
        var creature = place(helper, kind, owner, new BlockPos(7, 1, 4));
        creature.setNoAi(true);
        creature.hurtServer(helper.getLevel(), owner.damageSources().playerAttack(owner), 1000);
        helper.onEachTick(() -> {
            if (creature.isRemoved()) {
                expectSanity(helper, owner, UNEASY, "the Owner of a " + kind.name() + " that faded");
                helper.succeed();
            }
        });
    }

    /** It vanishes as its Owner leaves Survival. */
    private static void vanishesForNothing(GameTestHelper helper, Kind kind) {
        var owner = join(helper, new BlockPos(1, 1, 4), INSANE, GameType.SURVIVAL);
        var creature = place(helper, kind, owner, new BlockPos(5, 1, 4));
        creature.setNoAi(true);
        owner.setGameMode(GameType.CREATIVE);
        helper.onEachTick(() -> {
            if (creature.isRemoved()) {
                expectSanity(helper, owner, INSANE, "the Owner of a " + kind.name() + " that vanished");
                helper.succeed();
            }
        });
    }

    private static void expectSanity(GameTestHelper helper, ServerPlayer player, double expected, String who) {
        double sanity = PlayerSanity.get(player).value();
        if (Math.abs(sanity - expected) > 1e-9) {
            helper.fail(who + " has " + sanity + " Sanity, not " + expected);
        }
    }

    /** One Umbra and a stack drain the same; creative, so no creature comes and neither is company. */
    private static void umbra(GameTestHelper helper) {
        var one = join(helper, new BlockPos(0, 1, 0), 50, GameType.CREATIVE);
        var stack = join(helper, new BlockPos(8, 1, 8), 50, GameType.CREATIVE);
        one.getInventory().add(new ItemStack(ShadowCreatures.UMBRA.get()));
        stack.getInventory().add(new ItemStack(ShadowCreatures.UMBRA.get(), 64));
        helper.onEachTick(() -> {
            one.doTick();
            stack.doTick();
            if (helper.getTick() >= EVALUATED) {
                expectNetRate(helper, one, -1);
                expectNetRate(helper, stack, -1);
                helper.succeed();
            }
        });
    }

    // Discovery.

    private static void discoverCreature(GameTestHelper helper) {
        var player = join(helper, new BlockPos(0, 1, 0), UNEASY, GameType.SURVIVAL);
        place(helper, LURKER, player, new BlockPos(6, 1, 0)).setNoAi(true);
        helper.onEachTick(() -> {
            player.doTick();
            if (helper.getTick() < EVALUATED) {
                return;
            }
            expectNetRate(helper, player, -LURKER.weight());
            if (!Discovery.isKnown(player, Cause.SHADOW_CREATURE)) {
                helper.fail("an own Shadow Creature drained Sanity and was not discovered");
            }
            if (Discovery.isKnown(player, Cause.HOSTILE)) {
                helper.fail("an own Shadow Creature was discovered as a hostile");
            }
            helper.succeed();
        });
    }

    private static void discoverStrike(GameTestHelper helper) {
        var owner = join(helper, new BlockPos(1, 1, 4), INSANE, GameType.SURVIVAL);
        var creature = place(helper, LURKER, owner, new BlockPos(2, 1, 4));
        if (Discovery.isKnown(owner, Cause.SHADOW_STRIKE)) {
            helper.fail("the strike is known before any hit");
        }
        creature.doHurtTarget(helper.getLevel(), owner);
        if (!Discovery.isKnown(owner, Cause.SHADOW_STRIKE)) {
            helper.fail("a hit from a Real Shadow Creature did not discover the strike");
        }
        helper.succeed();
    }

    private static void discoverKill(GameTestHelper helper) {
        var owner = join(helper, new BlockPos(1, 1, 4), INSANE, GameType.SURVIVAL);
        var creature = place(helper, LURKER, owner, new BlockPos(5, 1, 4));
        if (Discovery.isKnown(owner, Cause.SHADOW_KILL)) {
            helper.fail("the kill is known before any kill");
        }
        creature.hurtServer(helper.getLevel(), owner.damageSources().playerAttack(owner), 1000);
        if (!Discovery.isKnown(owner, Cause.SHADOW_KILL)) {
            helper.fail("killing a Real Shadow Creature did not discover the kill");
        }
        helper.succeed();
    }

    private static void discoverUmbra(GameTestHelper helper) {
        var player = join(helper, MIDDLE, 50, GameType.CREATIVE);
        player.getInventory().add(new ItemStack(ShadowCreatures.UMBRA.get()));
        helper.onEachTick(() -> {
            player.doTick();
            if (helper.getTick() < EVALUATED) {
                return;
            }
            expectNetRate(helper, player, -1);
            if (!Discovery.isKnown(player, Cause.UMBRA)) {
                helper.fail("carrying Umbra drained Sanity and was not discovered");
            }
            helper.succeed();
        });
    }

    // The fixture.

    /** Joins a player at {@code feet} in this game mode, logged out however the test ends, with this Sanity. */
    private static ServerPlayer join(GameTestHelper helper, BlockPos feet, double sanity, GameType mode) {
        GameProfile profile = TestPlayers.profile();
        TestPlayers.whenDone(helper, () -> TestPlayers.leave(helper, profile));
        var player = TestPlayers.join(helper, profile);
        // As a client does once it has loaded, until which nothing can harm the player.
        player.connection.markClientLoaded();
        player.setGameMode(mode);
        teleport(helper, player, feet);
        PlayerSanity.set(player, new Sanity(sanity));
        return player;
    }

    /** A creature of the kind for this Owner, standing at {@code at}. */
    private static ShadowCreature place(GameTestHelper helper, Kind kind, ServerPlayer owner, BlockPos at) {
        ShadowCreature creature = kind.type().get().create(helper.getLevel(), EntitySpawnReason.EVENT);
        creature.setOwner(owner);
        creature.snapTo(Vec3.atBottomCenterOf(helper.absolutePos(at)));
        helper.getLevel().addFreshEntity(creature);
        return creature;
    }

    private static void teleport(GameTestHelper helper, ServerPlayer player, BlockPos feet) {
        player.teleport(new TeleportTransition(helper.getLevel(), Vec3.atBottomCenterOf(helper.absolutePos(feet)), Vec3.ZERO, 0, 0,
                TeleportTransition.DO_NOTHING));
    }

    private static void expectNetRate(GameTestHelper helper, ServerPlayer player, double perMinute) {
        double read = PlayerSanity.netRate(player) / PER_MINUTE;
        if (Math.abs(read - perMinute) > 1e-9) {
            helper.fail("the Net rate is " + read + "/min, not " + perMinute);
        }
    }
}
