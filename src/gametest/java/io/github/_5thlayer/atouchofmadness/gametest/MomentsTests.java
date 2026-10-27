// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.gametest;

import java.util.function.Consumer;
import java.util.function.Predicate;

import com.mojang.authlib.GameProfile;
import io.github._5thlayer.atouchof.signal.Strike;
import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import io.github._5thlayer.atouchofmadness.sanity.Sanity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.gametest.framework.TestEnvironmentDefinition.Weather;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
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
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The instantaneous drains and restores: eating a food moves Sanity by its Sanity Modifier, a slice of
 * cake from the placed block by the cake's, waking as the night skips restores 40 while getting up from
 * bed restores nothing, breaking a flower restores 1, and a Strike by the Dark, raised as the Runtime's
 * signal, takes 10 on Peaceful as on Hard. Each test stands a player at 50 Sanity on the platform at
 * noon, where nothing continuous applies, does the thing through vanilla's own path, or the Runtime's,
 * and reads how far Sanity moved.
 */
final class MomentsTests {

    private static final double START = 50;

    /** The platform is 9 by 9 with its floor at y 0; a player stands on it at y 1. */
    private static final BlockPos MIDDLE = new BlockPos(4, 1, 4);

    /** Long enough to eat any food. */
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
        // The night skip wakes every sleeper neither at once nor telling the level; a player getting up tells it.
        tests.test("moments_waking_as_the_night_skips_restores_forty", 1,
                helper -> expectAtOnce(helper, player -> wakeInBed(helper, player, false), 40));
        tests.test("moments_getting_up_from_bed_restores_nothing", 1,
                helper -> expectAtOnce(helper, player -> wakeInBed(helper, player, true), 0));
        // Raised as A Touch of Darkness raises it; on Peaceful its damage would be nothing, but not its cost.
        for (var difficulty : new Difficulty[] {Difficulty.PEACEFUL, Difficulty.HARD}) {
            String name = difficulty.getSerializedName();
            tests.under("moments_" + name, new TestEnvironmentDefinition.AllOf(new Sky(Sky.NOON, Weather.Type.CLEAR), new AtDifficulty(difficulty)))
                    .test("moments_struck_by_the_dark_costs_ten_on_" + name, 1, helper -> expectAtOnce(helper, Strike::raise, -10));
        }
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
     * Lays the player in a bed and wakes them as vanilla does: as the night skips, which wakes every
     * sleeper without telling the level, or, with {@code updateLevel}, as they get up themselves. The
     * night itself, and whether the bed lets them sleep, are vanilla's, and left to it.
     */
    private static void wakeInBed(GameTestHelper helper, ServerPlayer player, boolean updateLevel) {
        var foot = MIDDLE;
        var head = foot.north();
        helper.setBlock(foot, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.FOOT));
        helper.setBlock(head, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.HEAD));
        player.startSleeping(helper.absolutePos(foot));
        if (!player.isSleeping()) {
            helper.fail("the player did not lie down in the bed");
        }
        player.stopSleepInBed(false, updateLevel);
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
