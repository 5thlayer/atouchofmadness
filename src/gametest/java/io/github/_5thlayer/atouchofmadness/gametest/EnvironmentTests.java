// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.gametest;

import java.util.List;
import java.util.function.IntPredicate;

import io.github._5thlayer.atouchofmadness.environment.EnvironmentDrains;
import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition.Weather;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * The drains of where a player stands: Low light below 5 at the eyes, with a full moon in a sky the
 * eyes can see as a relief in a dimension with a moon (ADR 0003), freezing, being wet, and an
 * unsettling dimension. Each test sets up its
 * condition, stands a player in it with no other source active, ticks them past an evaluation once the
 * light at the eyes reads what the setup built, and reads the Net rate against the per-minute figure.
 * The light engine works on its own thread, as fast as it likes, so a test that changes the light
 * waits for the brightness it set rather than for a number of ticks.
 */
final class EnvironmentTests {

    /** The evaluation the test reads, at the earliest, for a test whose setup leaves the light as it was. */
    private static final int READ_AT = 2 * PlayerSanity.EVALUATION_TICKS;

    /** Far longer than the light engine takes to catch up with a setup, however loaded the server. */
    private static final int TIMEOUT = 10 * PlayerSanity.EVALUATION_TICKS;

    /** The brightness at the eyes of a closed room once the light engine has taken the sky light out. */
    private static final IntPredicate DARK = brightness -> brightness == 0;

    /** The platform is 9 by 9 with its floor at y 0; a player stands on it at y 1. */
    private static final BlockPos MIDDLE = new BlockPos(4, 1, 4);

    private EnvironmentTests() {
    }

    static void register(ATouchOfMadnessGameTests.Registrar tests) {
        int ticks = TIMEOUT;
        tests.test("environment_an_unlit_room_drains_six", ticks, helper -> {
            closedRoom(helper);
            expectDrain(helper, MIDDLE, DARK, 6);
        });
        tests.test("environment_block_light_of_five_drains_nothing", ticks, helper -> {
            closedRoom(helper);
            helper.setBlock(MIDDLE.above(), Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 5));
            expectDrain(helper, MIDDLE, brightness -> brightness == 5, 0);
        });
        tests.test("environment_daylight_in_the_overworld_drains_nothing", ticks, helper -> expectDrain(helper, MIDDLE, 0));
        tests.test("environment_powder_snow_drains_four", ticks, helper -> {
            helper.setBlock(MIDDLE, Blocks.POWDER_SNOW);
            expectDrain(helper, MIDDLE, 4);
        });
        tests.test("environment_water_drains_one_and_a_half", ticks, helper -> {
            for (var pos : BlockPos.betweenClosed(MIDDLE.offset(-1, 0, -1), MIDDLE.offset(1, 0, 1))) {
                helper.setBlock(pos, Blocks.STONE);
            }
            helper.setBlock(MIDDLE, Blocks.WATER);
            expectDrain(helper, MIDDLE, 1.5);
        });
        tests.test("environment_the_nether_drains_one", ticks, helper -> expectDrainIn(helper, Level.NETHER, 130, 1));

        tests.test("environment_only_the_overworld_has_a_moon", 1, EnvironmentTests::onlyTheOverworldHasAMoon);
        tests.test("environment_a_datapack_makes_the_end_unsettling", 1, EnvironmentTests::theEndIsUnsettling);

        var newMoon = tests.under("new_moon_night", new Sky(Sky.NEW_MOON_MIDNIGHT, Weather.Type.CLEAR));
        newMoon.test("environment_a_moonless_night_under_open_sky_drains_six", ticks, helper -> expectDrain(helper, MIDDLE, 6));

        var fullMoon = tests.under("full_moon_night", new Sky(Sky.FULL_MOON_MIDNIGHT, Weather.Type.CLEAR));
        fullMoon.test("environment_a_full_moon_under_open_sky_drains_nothing", ticks, helper -> expectDrain(helper, MIDDLE, 0));

        var rain = tests.under("rain_by_day", new Sky(Sky.NOON, Weather.Type.RAIN)).withSkyAccess();
        rain.test("environment_rain_under_open_sky_drains_one_and_a_half", ticks, helper -> expectDrain(helper, MIDDLE, 1.5));
    }

    /**
     * The End and the Nether have no moon, so a full moon is never overhead there. No vanilla dimension
     * has a dark open sky without a moon, and the game-test server loads no datapack dimensions, so
     * this reads the dimension types rather than standing a player under such a sky.
     */
    private static void onlyTheOverworldHasAMoon(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        for (var dimension : List.of(Level.OVERWORLD, Level.NETHER, Level.END)) {
            boolean hasMoon = EnvironmentDrains.hasMoon(server.getLevel(dimension).dimensionType());
            if (hasMoon != (dimension == Level.OVERWORLD)) {
                helper.fail(dimension.identifier() + (hasMoon ? " has a moon" : " has no moon"));
            }
        }
        helper.succeed();
    }

    /**
     * The game-test pack adds the End to {@code #atouchofmadness:unsettling}, beside the Nether by default.
     * How an unsettling dimension drains is the Nether's test; this reads the tag, which needs no light.
     */
    private static void theEndIsUnsettling(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        for (var dimension : List.of(Level.NETHER, Level.END)) {
            if (!server.getLevel(dimension).dimensionTypeRegistration().is(EnvironmentDrains.UNSETTLING)) {
                helper.fail(dimension.identifier() + " is not in " + EnvironmentDrains.UNSETTLING.location());
            }
        }
        helper.succeed();
    }

    /** A stone room over the whole platform, its walls and its roof at y 4 closed. */
    static void closedRoom(GameTestHelper helper) {
        for (var pos : BlockPos.betweenClosed(0, 1, 0, 8, 4, 8)) {
            boolean shell = pos.getX() == 0 || pos.getX() == 8 || pos.getZ() == 0 || pos.getZ() == 8 || pos.getY() == 4;
            if (shell) {
                helper.setBlock(pos, Blocks.STONE);
            }
        }
    }

    /** In another dimension, high over this test's column at {@code y}, on a stone block with full block light at the eyes. */
    private static void expectDrainIn(GameTestHelper helper, ResourceKey<Level> dimension, int y, double perMinute) {
        ServerLevel level = helper.getLevel().getServer().getLevel(dimension);
        var column = helper.absolutePos(MIDDLE);
        var feet = new BlockPos(column.getX(), y, column.getZ());
        level.setBlockAndUpdate(feet.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(feet, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(feet.above(), Blocks.LIGHT.defaultBlockState());
        stand(helper, level, feet, brightness -> brightness == LightBlock.MAX_LEVEL, perMinute);
    }

    /** For a setup that leaves the light as the platform has it. */
    private static void expectDrain(GameTestHelper helper, BlockPos feet, double perMinute) {
        expectDrain(helper, feet, brightness -> true, perMinute);
    }

    private static void expectDrain(GameTestHelper helper, BlockPos feet, IntPredicate lit, double perMinute) {
        stand(helper, helper.getLevel(), helper.absolutePos(feet), lit, perMinute);
    }

    /**
     * Joins a player, stands them at {@code feet} and ticks them each tick; once a whole second has
     * passed since the brightness at the eyes was {@code lit}, and no sooner than {@link #READ_AT},
     * the Net rate must be the drain of {@code perMinute}, and nothing else.
     */
    private static void stand(GameTestHelper helper, ServerLevel level, BlockPos feet, IntPredicate lit, double perMinute) {
        long[] litOn = {-1};
        var profile = TestPlayers.profile();
        TestPlayers.whenDone(helper, () -> TestPlayers.leave(helper, profile));
        TestPlayers.join(helper, profile).teleport(new TeleportTransition(level, Vec3.atBottomCenterOf(feet), Vec3.ZERO, 0, 0,
                TeleportTransition.DO_NOTHING));
        helper.onEachTick(() -> {
            var player = TestPlayers.playerList(helper).getPlayer(profile.id());
            player.doTick();
            if (litOn[0] < 0 && lit.test(level.getMaxLocalRawBrightness(BlockPos.containing(player.getEyePosition())))) {
                litOn[0] = helper.getTick();
            }
            // A whole second after, so the player's own tick of it has come since.
            if (litOn[0] < 0 || helper.getTick() < Math.max(READ_AT, litOn[0] + PlayerSanity.EVALUATION_TICKS + 1)) {
                return;
            }
            double read = -PlayerSanity.netRate(player);
            if (Math.abs(read - perMinute) > 1e-9) {
                helper.fail("the Net rate is a drain of " + read + "/min, not " + perMinute);
            }
            helper.succeed();
        });
    }
}
