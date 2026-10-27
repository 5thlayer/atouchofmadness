// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.gametest;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.authlib.GameProfile;
import io.github._5thlayer.atouchof.meter.MeterPayload;
import io.github._5thlayer.atouchof.meter.MeterSync;
import io.github._5thlayer.atouchofmadness.meter.SanityMeter;
import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import io.github._5thlayer.atouchofmadness.sanity.Sanity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * The Sanity meter: each tick the Pillar hands the Runtime a player's Sanity, as the half steps the
 * Runtime sends, their Stage's index and the trend band of their Net rate. What the Runtime draws
 * from it is checked by hand in the dev client, which no game test loads.
 */
final class MeterTests {

    /** What the Runtime last told each player's client of the Sanity meter. */
    private static final Map<UUID, MeterPayload> TOLD = new ConcurrentHashMap<>();

    /** The platform is 9 by 9 with its floor at y 0; a player stands on it at y 1. */
    private static final BlockPos MIDDLE = new BlockPos(4, 1, 4);

    /** Past the evaluation that first reads the water, at whichever tick of the second the player's falls. */
    private static final int READ_AT = 2 * PlayerSanity.EVALUATION_TICKS + 1;

    static {
        MeterSync.onSend((player, payload) -> {
            if (payload.meter().equals(SanityMeter.ID)) {
                TOLD.put(player.getUUID(), payload);
            }
        });
    }

    private MeterTests() {
    }

    static void register(ATouchOfMadnessGameTests.Registrar tests) {
        tests.test("meter_shows_sanity_in_half_steps_and_its_stage", 1, MeterTests::halvesAndStage);
        tests.test("meter_shows_a_drain_as_a_falling_trend", 4 * PlayerSanity.EVALUATION_TICKS, helper -> inWater(helper, 50, -1));
        tests.test("meter_shows_no_trend_for_a_drain_at_empty", 4 * PlayerSanity.EVALUATION_TICKS, helper -> inWater(helper, 0, 0));
    }

    /** 40 Sanity fills 8 half steps of the 20, Uneasy, and 7 fills 2, Insane; 100 fills all 20, Sane. */
    private static void halvesAndStage(GameTestHelper helper) {
        var player = join(helper);
        expect(helper, player, 40, 8, 1);
        expect(helper, player, 7, 2, 2);
        expect(helper, player, 100, 20, 0);
        helper.succeed();
    }

    private static void expect(GameTestHelper helper, ServerPlayer player, double sanity, int halves, int state) {
        PlayerSanity.set(player, new Sanity(sanity));
        player.doTick();
        var told = TOLD.get(player.getUUID());
        if (told == null || told.halves() != halves || told.state() != state) {
            helper.fail("at " + sanity + " Sanity the meter was told " + told + ", not " + halves + " halves in state " + state);
        }
    }

    /** Water drains 1.5/min, one falling chevron, unless Sanity is already at 0. */
    private static void inWater(GameTestHelper helper, double sanity, int trend) {
        for (var pos : BlockPos.betweenClosed(MIDDLE.offset(-1, 0, -1), MIDDLE.offset(1, 0, 1))) {
            helper.setBlock(pos, Blocks.STONE);
        }
        helper.setBlock(MIDDLE, Blocks.WATER);
        var player = join(helper);
        PlayerSanity.set(player, new Sanity(sanity));
        player.teleport(new TeleportTransition(helper.getLevel(), Vec3.atBottomCenterOf(helper.absolutePos(MIDDLE)), Vec3.ZERO, 0, 0,
                TeleportTransition.DO_NOTHING));
        helper.onEachTick(() -> {
            player.doTick();
            if (helper.getTick() < READ_AT) {
                return;
            }
            var told = TOLD.get(player.getUUID());
            if (told == null || told.trend() != trend) {
                helper.fail("in water at " + sanity + " Sanity the meter was told " + told + ", not a trend of " + trend);
            }
            helper.succeed();
        });
    }

    private static ServerPlayer join(GameTestHelper helper) {
        GameProfile profile = TestPlayers.profile();
        TestPlayers.whenDone(helper, () -> {
            TestPlayers.leave(helper, profile);
            TOLD.remove(profile.id());
        });
        return TestPlayers.join(helper, profile);
    }
}
