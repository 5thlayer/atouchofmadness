// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.gametest;

import java.util.List;

import io.github._5thlayer.atouchofmadness.environment.EnvironmentDrains;
import io.github._5thlayer.atouchofmadness.environment.EnvironmentRules;
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
 * light has settled, and reads the Net rate against the per-minute figure.
 */
final class EnvironmentTests {

    /** Long enough for the light engine to settle before the evaluation the test reads. */
    private static final int READ_AT = 2 * PlayerSanity.EVALUATION_TICKS;

    /** The platform is 9 by 9 with its floor at y 0; a player stands on it at y 1. */
    private static final BlockPos MIDDLE = new BlockPos(4, 1, 4);

    private EnvironmentTests() {
    }

    static void register(ATouchOfMadnessGameTests.Registrar tests) {
        int ticks = READ_AT + 1;
        tests.test("environment_an_unlit_room_drains_six", ticks, helper -> {
            closedRoom(helper);
            expectDrain(helper, MIDDLE, 6);
        });
        tests.test("environment_block_light_of_five_drains_nothing", ticks, helper -> {
            closedRoom(helper);
            helper.setBlock(MIDDLE.above(), Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 5));
            expectDrain(helper, MIDDLE, 0);
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
        // The game-test pack adds the End to #atouchofmadness:unsettling.
        tests.test("environment_a_datapack_makes_the_end_unsettling", ticks, helper -> expectDrainIn(helper, Level.END, 200, 1));

        tests.test("environment_only_the_overworld_has_a_moon", 1, EnvironmentTests::onlyTheOverworldHasAMoon);

        var newMoon = tests.under("new_moon_night", new Sky(Sky.NEW_MOON_MIDNIGHT, Weather.Type.CLEAR));
        newMoon.test("environment_a_moonless_night_under_open_sky_drains_six", ticks, helper -> expectDrain(helper, MIDDLE, 6));

        var fullMoon = tests.under("full_moon_night", new Sky(Sky.FULL_MOON_MIDNIGHT, Weather.Type.CLEAR));
        fullMoon.test("environment_a_full_moon_under_open_sky_drains_nothing", ticks, helper -> expectDrain(helper, MIDDLE, 0));
        fullMoon.test("environment_a_full_moon_under_a_roof_drains_six", ticks, helper -> {
            closedRoom(helper);
            expectDrain(helper, MIDDLE, 6);
        });

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
        stand(helper, level, feet, perMinute);
    }

    private static void expectDrain(GameTestHelper helper, BlockPos feet, double perMinute) {
        stand(helper, helper.getLevel(), helper.absolutePos(feet), perMinute);
    }

    /**
     * Joins a player, stands them at {@code feet} and ticks them each tick; once an evaluation has come
     * after the light settled, the Net rate must be the drain of {@code perMinute}, and nothing else.
     */
    private static void stand(GameTestHelper helper, ServerLevel level, BlockPos feet, double perMinute) {
        var profile = TestPlayers.profile();
        TestPlayers.whenDone(helper, () -> TestPlayers.leave(helper, profile));
        TestPlayers.join(helper, profile).teleport(new TeleportTransition(level, Vec3.atBottomCenterOf(feet), Vec3.ZERO, 0, 0,
                TeleportTransition.DO_NOTHING));
        helper.onEachTick(() -> {
            var player = TestPlayers.playerList(helper).getPlayer(profile.id());
            player.doTick();
            if (helper.getTick() < READ_AT) {
                return;
            }
            double read = PlayerSanity.netRate(player) / EnvironmentRules.drainPerTick(1);
            if (Math.abs(read - perMinute) > 1e-9) {
                helper.fail("the Net rate is a drain of " + read + "/min, not " + perMinute);
            }
            helper.succeed();
        });
    }
}
