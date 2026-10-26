// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.gametest;

import java.util.OptionalDouble;

import com.mojang.authlib.GameProfile;
import io.github._5thlayer.atouchofmadness.discovery.Discovery;
import io.github._5thlayer.atouchofmadness.music.MusicSource;
import io.github._5thlayer.atouchofmadness.sanity.Cause;
import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import io.github._5thlayer.atouchofmadness.sanity.Sanity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition.Weather;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * The freshness a player looking at a playing jukebox may be shown, as Jade shows it (#19): read on
 * the server from that player's own Disc History, recording nothing, and shown only once they have
 * discovered the Stale disc. Each test places its jukebox beside its players and takes it away however
 * it ends, and a jukebox reaches further than tests are spaced apart, so they run in a batch of their
 * own, as in {@link MusicTests}.
 */
final class FreshnessTests {

    /** The platform's middle, where the players stand, and the jukebox two blocks east of them. */
    private static final BlockPos FEET = new BlockPos(4, 1, 4);
    private static final BlockPos JUKEBOX = FEET.east(2);

    /** Two evaluations, so one has come whichever tick of a second is the player's own. */
    private static final int EVALUATED = 2 * PlayerSanity.EVALUATION_TICKS;

    private FreshnessTests() {
    }

    static void register(ATouchOfMadnessGameTests.Registrar defaults) {
        var tests = defaults.under("freshness", new Sky(Sky.NOON, Weather.Type.CLEAR));
        tests.test("freshness_is_hidden_until_the_stale_disc_is_discovered", 1, FreshnessTests::hidden);
        tests.test("freshness_of_a_jukebox_playing_nothing_is_nothing", 1, FreshnessTests::silent);
        tests.test("freshness_is_each_players_own_and_reading_it_records_nothing", EVALUATED + 1, FreshnessTests::ownHistory);
    }

    private static void hidden(GameTestHelper helper) {
        var player = join(helper);
        var jukebox = jukebox(helper);
        play(jukebox);
        expect(helper, MusicSource.shownFreshness(player, jukebox), OptionalDouble.empty(), "before the Stale disc is discovered");
        discover(helper, player, Cause.STALE_DISC);
        expect(helper, MusicSource.shownFreshness(player, jukebox), OptionalDouble.of(1), "once the Stale disc is discovered");
        helper.succeed();
    }

    private static void silent(GameTestHelper helper) {
        var player = join(helper);
        discover(helper, player, Cause.STALE_DISC);
        expect(helper, MusicSource.shownFreshness(player, jukebox(helper)), OptionalDouble.empty(), "with no disc in the jukebox");
        helper.succeed();
    }

    /**
     * Cat plays for one player, who hears it, while the other only has it read: once it starts again,
     * a second play, it is Stale for the one who heard it and Fresh for the other, whom the read never
     * recorded as hearing the first.
     */
    private static void ownHistory(GameTestHelper helper) {
        var listener = join(helper);
        var onlooker = join(helper);
        discover(helper, listener, Cause.STALE_DISC);
        discover(helper, onlooker, Cause.STALE_DISC);
        var jukebox = jukebox(helper);
        play(jukebox);
        expect(helper, MusicSource.shownFreshness(onlooker, jukebox), OptionalDouble.of(1), "on the first play");
        helper.onEachTick(() -> {
            listener.doTick();
            if (helper.getTick() < EVALUATED) {
                return;
            }
            play(jukebox);
            expect(helper, MusicSource.shownFreshness(listener, jukebox), OptionalDouble.of(0.5), "for the player who heard the first play");
            expect(helper, MusicSource.shownFreshness(onlooker, jukebox), OptionalDouble.of(1), "for the player who only looked");
            helper.succeed();
        });
    }

    /** Joins a player at {@link #FEET}, logged out however the test ends, at 50 Sanity so music moves it. */
    private static ServerPlayer join(GameTestHelper helper) {
        GameProfile profile = TestPlayers.profile();
        TestPlayers.whenDone(helper, () -> TestPlayers.leave(helper, profile));
        var player = TestPlayers.join(helper, profile);
        player.teleport(new TeleportTransition(helper.getLevel(), Vec3.atBottomCenterOf(helper.absolutePos(FEET)), Vec3.ZERO, 0, 0,
                TeleportTransition.DO_NOTHING));
        PlayerSanity.set(player, new Sanity(50));
        return player;
    }

    /** Places the jukebox, taken away however the test ends so it plays into no other test. */
    private static JukeboxBlockEntity jukebox(GameTestHelper helper) {
        TestPlayers.whenDone(helper, () -> {
            if (helper.getLevel().getBlockEntity(helper.absolutePos(JUKEBOX)) instanceof JukeboxBlockEntity jukebox) {
                jukebox.setTheItem(ItemStack.EMPTY);
            }
            helper.setBlock(JUKEBOX, Blocks.AIR);
        });
        helper.setBlock(JUKEBOX, Blocks.JUKEBOX);
        return helper.getBlockEntity(JUKEBOX, JukeboxBlockEntity.class);
    }

    /** Plays cat from its start, a new play. */
    private static void play(JukeboxBlockEntity jukebox) {
        jukebox.setTheItem(ItemStack.EMPTY);
        jukebox.setTheItem(new ItemStack(Items.MUSIC_DISC_CAT));
    }

    /** Grants the player the cause's discovery advancement, every criterion of it. */
    private static void discover(GameTestHelper helper, ServerPlayer player, Cause cause) {
        var advancement = helper.getLevel().getServer().getAdvancements().get(Discovery.advancementId(cause.id()));
        if (advancement == null) {
            throw helper.assertionException(Component.literal("no discovery advancement for " + cause.id()));
        }
        advancement.value().criteria().keySet().forEach(criterion -> player.getAdvancements().award(advancement, criterion));
        if (!Discovery.isKnown(player, cause)) {
            throw helper.assertionException(Component.literal(cause.id() + " is not known once its advancement is granted"));
        }
    }

    private static void expect(GameTestHelper helper, OptionalDouble read, OptionalDouble expected, String when) {
        if (!read.equals(expected)) {
            throw helper.assertionException(Component.literal(when + " the freshness shown is " + read + ", not " + expected));
        }
    }
}
