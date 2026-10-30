// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import com.mojang.authlib.GameProfile;
import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * The drain and restore of who is near: each monster within 8 blocks drains its weight a minute, 1 by
 * default, an Enderman 3 and a Warden 10, up to 10 in all; any tamed pet, villager or other Survival or
 * Adventure player within 8 blocks restores 2 a minute, once. Each test shares one fixture: a player
 * stands on the platform at noon, where nothing else applies, the test places mobs with no will of their
 * own, or other players, in the test's box around them, and once an evaluation has come the Net rate is
 * read against the per-minute figure. How the Net rate sums with other sources is {@code SanityTests}'
 * to test. The tests are padded, so nothing in one is near a player in another.
 */
final class NearbyTests {

    private static final int READ_AT = 2 * PlayerSanity.EVALUATION_TICKS;

    /** Wider than the radius, so no neighbouring test's mob or player is in reach. */
    private static final int PADDING = 9;

    /** Where the player stands: on the platform's west edge, so a mob can be placed up to 7 blocks east. */
    private static final BlockPos FEET = new BlockPos(1, 1, 4);

    private NearbyTests() {
    }

    static void register(ATouchOfMadnessGameTests.Registrar defaults) {
        var tests = defaults.padded(PADDING);
        int ticks = READ_AT + 1;

        tests.test("nearby_a_zombie_drains_one", ticks, helper -> expect(helper, FEET, (player, mobs) ->
                mobs.add(spawn(helper, EntityType.ZOMBIE, 5, 4)), -1));
        tests.test("nearby_a_zombie_eight_blocks_away_drains_one", ticks, helper -> expect(helper, new BlockPos(0, 1, 4),
                (player, mobs) -> mobs.add(spawn(helper, EntityType.ZOMBIE, 8, 4)), -1));
        tests.test("nearby_an_enderman_drains_three", ticks, helper -> expect(helper, FEET, (player, mobs) ->
                mobs.add(spawn(helper, EntityType.ENDERMAN, 5, 4)), -3));
        tests.test("nearby_a_warden_drains_ten", ticks, helper -> expect(helper, FEET, (player, mobs) ->
                mobs.add(spawn(helper, EntityType.WARDEN, 5, 4)), -10));
        // The dragon's parts share its type, and are within reach too.
        tests.test("nearby_an_ender_dragon_drains_one_however_many_of_its_parts_are_near", ticks, helper -> expect(helper, FEET,
                (player, mobs) -> mobs.add(spawn(helper, EntityType.ENDER_DRAGON, 5, 4)), -1));
        tests.test("nearby_many_monsters_drain_no_more_than_ten", ticks, helper -> expect(helper, FEET, (player, mobs) -> {
            for (int z = 2; z <= 6; z++) {
                mobs.add(spawn(helper, EntityType.ENDERMAN, 4, z));
            }
        }, -10));
        tests.test("nearby_a_mob_outside_the_tag_drains_nothing", ticks, helper -> expect(helper, FEET, (player, mobs) ->
                mobs.add(spawn(helper, EntityType.PIG, 5, 4)), 0));
        // Just under 9 blocks away.
        tests.test("nearby_a_zombie_beyond_eight_blocks_drains_nothing", ticks, helper -> expect(helper, new BlockPos(0, 1, 0),
                (player, mobs) -> mobs.add(spawn(helper, EntityType.ZOMBIE, 8, 4)), 0));
        // The game-test pack adds the sheep to #atouchofmadness:unsettling, with a weight of 4.
        tests.test("nearby_a_datapack_makes_a_sheep_drain_four", ticks, helper -> expect(helper, FEET, (player, mobs) ->
                mobs.add(spawn(helper, EntityType.SHEEP, 5, 4)), -4));

        tests.test("nearby_a_tamed_wolf_restores_two", ticks, helper -> expect(helper, FEET, (player, mobs) -> {
            Wolf wolf = spawn(helper, EntityType.WOLF, 5, 4);
            wolf.tame(player);
            mobs.add(wolf);
        }, 2));
        tests.test("nearby_an_untamed_wolf_restores_nothing", ticks, helper -> expect(helper, FEET, (player, mobs) ->
                mobs.add(spawn(helper, EntityType.WOLF, 5, 4)), 0));
        tests.test("nearby_a_villager_restores_two", ticks, helper -> expect(helper, FEET, (player, mobs) ->
                mobs.add(spawn(helper, EntityType.VILLAGER, 5, 4)), 2));
        tests.test("nearby_another_survival_player_restores_two", ticks, helper -> expect(helper, FEET, (player, mobs) ->
                joinAt(helper, GameType.SURVIVAL, 5, 4), 2));
        tests.test("nearby_a_villager_and_a_tamed_wolf_still_restore_two", ticks, helper -> expect(helper, FEET, (player, mobs) -> {
            mobs.add(spawn(helper, EntityType.VILLAGER, 5, 3));
            Wolf wolf = spawn(helper, EntityType.WOLF, 5, 5);
            wolf.tame(player);
            mobs.add(wolf);
        }, 2));
        tests.test("nearby_a_creative_or_spectator_player_is_no_company", ticks, helper -> expect(helper, FEET, (player, mobs) -> {
            joinAt(helper, GameType.CREATIVE, 5, 3);
            joinAt(helper, GameType.SPECTATOR, 5, 5);
        }, 0));
        // The game-test pack adds the cow to #atouchofmadness:comforting.
        tests.test("nearby_a_datapack_makes_a_cow_company", ticks, helper -> expect(helper, FEET, (player, mobs) ->
                mobs.add(spawn(helper, EntityType.COW, 5, 4)), 2));
    }

    private static <E extends Mob> E spawn(GameTestHelper helper, EntityType<E> type, int x, int z) {
        return helper.spawnWithNoFreeWill(type, new BlockPos(x, 1, z));
    }

    /** Another player, at {@code x, z} on the platform in this game mode, logged out when the test ends. */
    private static void joinAt(GameTestHelper helper, GameType mode, int x, int z) {
        var profile = TestPlayers.profile();
        TestPlayers.whenDone(helper, () -> TestPlayers.leave(helper, profile));
        var player = TestPlayers.join(helper, profile);
        player.setGameMode(mode);
        teleport(helper, player, new BlockPos(x, 1, z));
    }

    /**
     * The fixture: joins a player at {@code feet}, places what {@code place} puts near them and ticks the
     * player each tick; once an evaluation has come, the Net rate must be {@code perMinute}, positive
     * for a restore, and each mob placed must still be alive.
     */
    private static void expect(GameTestHelper helper, BlockPos feet, BiConsumer<ServerPlayer, List<Mob>> place,
            double perMinute) {
        GameProfile profile = TestPlayers.profile();
        TestPlayers.whenDone(helper, () -> TestPlayers.leave(helper, profile));
        var joined = TestPlayers.join(helper, profile);
        teleport(helper, joined, feet);
        List<Mob> mobs = new ArrayList<>();
        place.accept(joined, mobs);
        helper.onEachTick(() -> {
            var player = TestPlayers.playerList(helper).getPlayer(profile.id());
            player.doTick();
            if (helper.getTick() < READ_AT) {
                return;
            }
            for (var mob : mobs) {
                if (!mob.isAlive()) {
                    helper.fail(mob.getType().toShortString() + " died before the Net rate was read");
                }
            }
            double read = PlayerSanity.netRate(player);
            if (Math.abs(read - perMinute) > 1e-9) {
                helper.fail("the Net rate is " + read + "/min, not " + perMinute);
            }
            helper.succeed();
        });
    }

    private static void teleport(GameTestHelper helper, ServerPlayer player, BlockPos feet) {
        player.teleport(new TeleportTransition(helper.getLevel(), Vec3.atBottomCenterOf(helper.absolutePos(feet)), Vec3.ZERO, 0, 0,
                TeleportTransition.DO_NOTHING));
    }
}
