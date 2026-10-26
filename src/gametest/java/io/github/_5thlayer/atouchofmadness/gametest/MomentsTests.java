// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.gametest;

import java.util.function.Consumer;
import java.util.function.Predicate;

import com.mojang.authlib.GameProfile;
import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import io.github._5thlayer.atouchofmadness.sanity.Sanity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.gametest.framework.TestEnvironmentDefinition.Weather;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The instantaneous drains and restores: eating a food moves Sanity by its Sanity Modifier, a slice of
 * cake from the placed block by the cake's, sleeping through the night restores 40 to each sleeper and
 * nothing to a player awake, and breaking a flower restores 1. Each test stands a player at 50 Sanity
 * on the platform at noon, where nothing continuous applies, does the thing through vanilla's own path
 * and reads how far Sanity moved.
 */
final class MomentsTests {

    private static final double START = 50;

    /** The platform is 9 by 9 with its floor at y 0; a player stands on it at y 1. */
    private static final BlockPos MIDDLE = new BlockPos(4, 1, 4);

    /** Long enough to eat any food, and a sleeper to sleep long enough to skip the night. */
    private static final int MAX_TICKS = 200;

    private MomentsTests() {
    }

    static void register(ATouchOfMadnessGameTests.Registrar tests) {
        tests.test("moments_eating_a_spider_eye_drains_ten", MAX_TICKS, helper -> expectEating(helper, new ItemStack(Items.SPIDER_EYE), -10));
        tests.test("moments_eating_a_golden_apple_restores_ten", MAX_TICKS, helper -> expectEating(helper, new ItemStack(Items.GOLDEN_APPLE), 10));
        tests.test("moments_eating_cooked_beef_changes_nothing", MAX_TICKS, helper -> expectEating(helper, new ItemStack(Items.COOKED_BEEF), 0));
        // The game-test pack overrides the cookie's +3 with -2.
        tests.test("moments_a_datapack_overrides_a_food", MAX_TICKS, helper -> expectEating(helper, new ItemStack(Items.COOKIE), -2));
        tests.test("moments_each_slice_of_a_placed_cake_restores_five", 1, MomentsTests::cake);
        tests.test("moments_breaking_a_flower_restores_one", 1, helper -> expectAtOnce(helper, player -> {
            helper.setBlock(MIDDLE, Blocks.POPPY);
            player.gameMode.destroyBlock(helper.absolutePos(MIDDLE));
        }, 1));
        tests.test("moments_breaking_a_flower_grown_with_bone_meal_restores_one", 1, MomentsTests::boneMealFlower);
        tests.test("moments_daylight_restores_nothing", 2 * PlayerSanity.EVALUATION_TICKS + 1, helper -> {
            int readAt = 2 * PlayerSanity.EVALUATION_TICKS;
            expect(helper, player -> {
            }, player -> {
                if (helper.getTick() >= readAt && PlayerSanity.netRate(player) != 0) {
                    helper.fail("the Net rate in daylight is " + PlayerSanity.netRate(player) + ", not 0");
                }
                return helper.getTick() >= readAt;
            }, 0);
        });

        // Half the players asleep skip the night, so one of two can stay up.
        var night = tests.under("sleepable_night", new TestEnvironmentDefinition.AllOf(
                new Sky(Sky.FULL_MOON_MIDNIGHT, Weather.Type.CLEAR),
                new TestEnvironmentDefinition.SetGameRules(new GameRuleMap.Builder().set(GameRules.PLAYERS_SLEEPING_PERCENTAGE, 50).build())));
        night.test("moments_sleeping_through_the_night_restores_forty_and_nothing_to_one_awake", MAX_TICKS, MomentsTests::sleep);
    }

    /** Hungry, so every food can be eaten, and eating it to the end. */
    private static void expectEating(GameTestHelper helper, ItemStack food, double moved) {
        expect(helper, player -> {
            player.getFoodData().setFoodLevel(0);
            player.setItemInHand(InteractionHand.MAIN_HAND, food);
            player.gameMode.useItem(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
            if (!player.isUsingItem()) {
                helper.fail("the player did not start eating " + food);
            }
        }, player -> !player.isUsingItem(), moved);
    }

    /** Two slices, each eaten hungry from the block in the world, empty-handed. */
    private static void cake(GameTestHelper helper) {
        expectAtOnce(helper, player -> {
            helper.setBlock(MIDDLE, Blocks.CAKE);
            var pos = helper.absolutePos(MIDDLE);
            for (int slice = 0; slice < 2; slice++) {
                player.getFoodData().setFoodLevel(0);
                player.gameMode.useItemOn(player, player.level(), ItemStack.EMPTY, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            }
            int bites = helper.getBlockState(MIDDLE).getValue(CakeBlock.BITES);
            if (bites != 2) {
                helper.fail("the cake has " + bites + " bites taken, not 2");
            }
        }, 10);
    }

    /** A torchflower crop on wet farmland, bone-mealed until it is a torchflower, then broken. */
    private static void boneMealFlower(GameTestHelper helper) {
        expectAtOnce(helper, player -> {
            helper.setBlock(MIDDLE.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE));
            helper.setBlock(MIDDLE, Blocks.TORCHFLOWER_CROP);
            var pos = helper.absolutePos(MIDDLE);
            for (int i = 0; i < 2; i++) {
                BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), helper.getLevel(), pos);
            }
            if (!helper.getBlockState(MIDDLE).is(Blocks.TORCHFLOWER)) {
                helper.fail("bone meal grew " + helper.getBlockState(MIDDLE) + ", not a torchflower");
            }
            player.gameMode.destroyBlock(pos);
        }, 1);
    }

    /**
     * One player sleeps in a bed while another stays up beside it, both in full block light so the
     * night drains neither. Once the sleeper has slept long enough the night skips and wakes them.
     */
    private static void sleep(GameTestHelper helper) {
        var sleeper = TestPlayers.profile();
        var awake = TestPlayers.profile();
        TestPlayers.whenDone(helper, () -> {
            TestPlayers.leave(helper, sleeper);
            TestPlayers.leave(helper, awake);
        });
        var head = MIDDLE.north();
        helper.setBlock(MIDDLE, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.FOOT));
        helper.setBlock(head, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.HEAD));
        helper.setBlock(MIDDLE.above(), Blocks.LIGHT);
        helper.setBlock(head.above(), Blocks.LIGHT);
        joinAt(helper, sleeper, MIDDLE.east());
        joinAt(helper, awake, MIDDLE.west());
        boolean[] slept = {false};
        helper.onEachTick(() -> {
            var players = TestPlayers.playerList(helper);
            var asleep = players.getPlayer(sleeper.id());
            var up = players.getPlayer(awake.id());
            // The bed reads the night from the level's environment, which may lag the clock set on the first tick.
            if (!slept[0]) {
                slept[0] = asleep.startSleepInBed(helper.absolutePos(MIDDLE)).right().isPresent();
                if (!slept[0] && helper.getTick() >= PlayerSanity.EVALUATION_TICKS) {
                    helper.fail("the sleeper cannot sleep: " + asleep.startSleepInBed(helper.absolutePos(MIDDLE)).left().orElseThrow());
                }
            }
            asleep.doTick();
            up.doTick();
            if (!slept[0] || asleep.isSleeping()) {
                return;
            }
            expectSanity(helper, asleep, START + 40, "the sleeper");
            expectSanity(helper, up, START, "the player awake");
            helper.succeed();
        });
    }

    /**
     * Joins a survival player at 50 Sanity in the middle of the platform, does {@code act} with them, and ticks
     * them each tick until {@code done}; Sanity must then have moved by {@code moved}.
     */
    private static void expect(GameTestHelper helper, Consumer<ServerPlayer> act, Predicate<ServerPlayer> done, double moved) {
        var profile = TestPlayers.profile();
        TestPlayers.whenDone(helper, () -> TestPlayers.leave(helper, profile));
        act.accept(joinAt(helper, profile, MIDDLE));
        helper.onEachTick(() -> {
            var player = TestPlayers.playerList(helper).getPlayer(profile.id());
            player.doTick();
            if (!done.test(player)) {
                return;
            }
            expectSanity(helper, player, START + moved, "the player");
            helper.succeed();
        });
    }

    /** As {@link #expect}, with Sanity read on the first tick after {@code act}. */
    private static void expectAtOnce(GameTestHelper helper, Consumer<ServerPlayer> act, double moved) {
        expect(helper, act, player -> true, moved);
    }

    private static ServerPlayer joinAt(GameTestHelper helper, GameProfile profile, BlockPos feet) {
        var player = TestPlayers.join(helper, profile);
        // A creative break awards no stat, and so restores nothing.
        player.setGameMode(GameType.SURVIVAL);
        player.teleport(new TeleportTransition(helper.getLevel(), Vec3.atBottomCenterOf(helper.absolutePos(feet)), Vec3.ZERO, 0, 0,
                TeleportTransition.DO_NOTHING));
        PlayerSanity.set(player, new Sanity(START));
        return player;
    }

    private static void expectSanity(GameTestHelper helper, ServerPlayer player, double expected, String who) {
        double value = PlayerSanity.get(player).value();
        if (Math.abs(value - expected) > 1e-9) {
            helper.fail(who + "'s Sanity is " + value + ", not " + expected);
        }
    }
}
