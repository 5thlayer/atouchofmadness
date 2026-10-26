// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.gametest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.mojang.authlib.GameProfile;
import io.github._5thlayer.atouchofmadness.environment.EnvironmentRules;
import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition.Weather;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * The restore of music: while the nearest jukebox playing within 64 blocks plays a disc, 4 a minute
 * times its freshness factor, 1 for a Fresh disc and half its last play's for a Stale one, one of the
 * last two discs the player heard. Each test is a script of steps two evaluations long: a step starts
 * by playing a disc, stopping a jukebox, relogging or walking, and ends by reading the Net rate against
 * the per-minute figure. A step that plays a disc in a jukebox already playing one takes it out first,
 * so the song starts again, a new play. The player stands on the platform at noon, where nothing else
 * applies, and nearer their own test's jukebox than any other test's, so it is the one they hear.
 *
 * <p>A jukebox reaches further than tests are spaced apart, so the tests run in a batch of their own,
 * and each takes its jukeboxes away when it ends, however it ends, so none plays on into another batch.
 */
final class MusicTests {

    /** How long each step runs: two evaluations, so one comes after the step's start. */
    private static final int STEP = 2 * PlayerSanity.EVALUATION_TICKS;

    /** Where the player stands, on the platform's west edge. */
    private static final BlockPos FEET = new BlockPos(1, 1, 4);
    /** A jukebox 2 blocks from the player, and one 6 blocks away. */
    private static final BlockPos NEAR = new BlockPos(3, 1, 4);
    private static final BlockPos FAR = new BlockPos(7, 1, 4);

    /** A restore of 1 a minute, as a rate in Sanity a tick. */
    private static final double PER_MINUTE = -EnvironmentRules.drainPerTick(1);

    private MusicTests() {
    }

    /**
     * Noon and clear, as in the default batch, so nothing but music applies on the platform: a new one
     * for each batch, since the registry refuses one value under two ids.
     */
    private static Sky noon() {
        return new Sky(Sky.NOON, Weather.Type.CLEAR);
    }

    /**
     * One step: what it does at its start, and the Net rate it expects at its end, in Sanity a minute,
     * or {@code null} for a step that ends with the player logged out.
     */
    private record Step(Consumer<Scene> action, Double perMinute) {
    }

    static void register(ATouchOfMadnessGameTests.Registrar defaults) {
        var tests = defaults.under("music", noon());

        script(tests, "music_one_disc_on_repeat_restores_four_then_two_then_one", List.of(
                play(NEAR, Items.MUSIC_DISC_CAT, 4),
                play(NEAR, Items.MUSIC_DISC_CAT, 2),
                play(NEAR, Items.MUSIC_DISC_CAT, 1)));
        script(tests, "music_two_discs_alternating_fade_the_same_way", List.of(
                play(NEAR, Items.MUSIC_DISC_CAT, 4),
                play(NEAR, Items.MUSIC_DISC_FAR, 4),
                play(NEAR, Items.MUSIC_DISC_CAT, 2),
                play(NEAR, Items.MUSIC_DISC_FAR, 2),
                play(NEAR, Items.MUSIC_DISC_CAT, 1),
                play(NEAR, Items.MUSIC_DISC_FAR, 1)));
        script(tests, "music_three_discs_in_rotation_stay_at_four", List.of(
                play(NEAR, Items.MUSIC_DISC_CAT, 4),
                play(NEAR, Items.MUSIC_DISC_FAR, 4),
                play(NEAR, Items.MUSIC_DISC_MALL, 4),
                play(NEAR, Items.MUSIC_DISC_CAT, 4),
                play(NEAR, Items.MUSIC_DISC_FAR, 4),
                play(NEAR, Items.MUSIC_DISC_MALL, 4)));
        script(tests, "music_a_play_goes_on_through_a_relog", List.of(
                play(NEAR, Items.MUSIC_DISC_CAT, 4),
                new Step(Scene::relog, 4.0)));
        // The song starts again while the player is out, so it is a new play, and Stale only if the history was saved.
        script(tests, "music_the_history_survives_a_relog", List.of(
                play(NEAR, Items.MUSIC_DISC_CAT, 4),
                new Step(scene -> {
                    scene.leave();
                    scene.play(NEAR, Items.MUSIC_DISC_CAT);
                }, null),
                new Step(Scene::join, 2.0)));
        // The jukebox is saved and taken away while the player is out, as its chunk is when it stops
        // ticking, and comes back where its song left off, so its song goes on later than it would have:
        // still the same play, at 2, not a new one at 1.
        script(tests, "music_a_paused_song_goes_on_at_its_factor", List.of(
                play(NEAR, Items.MUSIC_DISC_CAT, 4),
                play(NEAR, Items.MUSIC_DISC_CAT, 2),
                new Step(scene -> {
                    scene.leave();
                    scene.pause(NEAR);
                }, null),
                new Step(scene -> {
                    scene.resume(NEAR);
                    scene.join();
                }, 2.0)));
        script(tests, "music_walking_to_another_jukebox_keeps_the_history", List.of(
                play(NEAR, Items.MUSIC_DISC_CAT, 4),
                new Step(scene -> {
                    scene.stop(NEAR);
                    scene.walkTo(FAR.west(2));
                    scene.play(FAR, Items.MUSIC_DISC_CAT);
                }, 2.0)));
        // Were both to count, the second step would restore 6; were the far one to, 4.
        script(tests, "music_only_the_nearest_jukebox_restores", List.of(
                play(FAR, Items.MUSIC_DISC_CAT, 4),
                play(NEAR, Items.MUSIC_DISC_CAT, 2)));
        // Until the hopper feeds it, the player hears another test's jukebox, so the disc is one no other test plays.
        script(tests, "music_a_jukebox_fed_by_a_hopper_restores", List.of(
                new Step(scene -> scene.feed(NEAR, Items.MUSIC_DISC_13), 4.0)));

        // Alone in its batch, since any other test's jukebox would be in reach once its own stops.
        var alone = defaults.under("music_alone", noon());
        script(alone, "music_a_stopped_jukebox_restores_nothing", List.of(
                play(NEAR, Items.MUSIC_DISC_CAT, 4),
                new Step(scene -> scene.stop(NEAR), 0.0)));
    }

    private static Step play(BlockPos jukebox, Item disc, double perMinute) {
        return new Step(scene -> scene.play(jukebox, disc), perMinute);
    }

    /**
     * The fixture: joins a player at {@link #FEET}, ticks them each tick they are logged in, and runs the
     * steps one after another, failing at the first whose Net rate at its end isn't the one it expects.
     */
    private static void script(ATouchOfMadnessGameTests.Registrar tests, String name, List<Step> steps) {
        tests.test(name, steps.size() * STEP + 1, helper -> {
            var scene = new Scene(helper, TestPlayers.profile());
            TestPlayers.whenDone(helper, scene::end);
            scene.join();
            helper.onEachTick(() -> {
                int tick = (int) helper.getTick();
                int index = tick / STEP;
                if (index >= steps.size()) {
                    helper.succeed();
                    return;
                }
                var step = steps.get(index);
                if (tick % STEP == 0) {
                    step.action().accept(scene);
                }
                var player = scene.player();
                if (player == null) {
                    return;
                }
                player.doTick();
                if (tick % STEP == STEP - 1 && step.perMinute() != null) {
                    double read = PlayerSanity.netRate(player) / PER_MINUTE;
                    if (Math.abs(read - step.perMinute()) > 1e-9) {
                        helper.fail("after step " + (index + 1) + " the Net rate is " + read + "/min, not " + step.perMinute());
                    }
                }
            });
        });
    }

    /** A block entity saved and taken away: its block, and what it saved. */
    private record Saved(BlockState state, CompoundTag tag) {
    }

    /** A test's player, by profile so a relog finds them again, and the jukeboxes it placed. */
    private static final class Scene {

        private final GameTestHelper helper;
        private final GameProfile profile;
        private final Set<BlockPos> placed = new HashSet<>();
        /** The jukebox {@link #pause} took away. */
        private Saved paused;

        Scene(GameTestHelper helper, GameProfile profile) {
            this.helper = helper;
            this.profile = profile;
        }

        /** The player, or {@code null} while they are logged out. */
        ServerPlayer player() {
            return TestPlayers.playerList(helper).getPlayer(profile.id());
        }

        /** Plays {@code disc} from its start in a jukebox at {@code pos}, placed there if none is. */
        void play(BlockPos pos, Item disc) {
            var jukebox = jukebox(pos);
            jukebox.setTheItem(ItemStack.EMPTY);
            jukebox.setTheItem(new ItemStack(disc));
        }

        void stop(BlockPos pos) {
            jukebox(pos).setTheItem(ItemStack.EMPTY);
        }

        /**
         * Saves the jukebox at {@code pos} and takes it away, disc and all, so its song stops ticking, as a
         * jukebox's chunk is saved and unloaded.
         */
        void pause(BlockPos pos) {
            var absolute = helper.absolutePos(pos);
            var level = helper.getLevel();
            var jukebox = jukebox(pos);
            paused = new Saved(jukebox.getBlockState(), jukebox.saveWithFullMetadata(level.registryAccess()));
            level.removeBlockEntity(absolute);
            level.setBlockAndUpdate(absolute, Blocks.AIR.defaultBlockState());
        }

        /** Brings back the jukebox {@link #pause} saved at {@code pos}, as a chunk loads it, its song where it left off. */
        void resume(BlockPos pos) {
            var absolute = helper.absolutePos(pos);
            var level = helper.getLevel();
            level.setBlockAndUpdate(absolute, paused.state());
            level.setBlockEntity(BlockEntity.loadStatic(absolute, paused.state(), paused.tag(), level.registryAccess()));
        }

        /** Places an empty jukebox at {@code pos}, and a hopper above it holding {@code disc}, which it feeds down. */
        void feed(BlockPos pos, Item disc) {
            jukebox(pos);
            helper.setBlock(pos.above(), Blocks.HOPPER);
            helper.<HopperBlockEntity>getBlockEntity(pos.above(), HopperBlockEntity.class).setItem(0, new ItemStack(disc));
        }

        /** Logs the player in, at {@link #FEET}. */
        void join() {
            TestPlayers.join(helper, profile);
            walkTo(FEET);
        }

        void leave() {
            TestPlayers.leave(helper, profile);
        }

        void relog() {
            leave();
            join();
        }

        void walkTo(BlockPos feet) {
            player().teleport(new TeleportTransition(helper.getLevel(), Vec3.atBottomCenterOf(helper.absolutePos(feet)), Vec3.ZERO, 0, 0,
                    TeleportTransition.DO_NOTHING));
        }

        /** Takes every jukebox away, so none plays on, and logs the player out. */
        void end() {
            for (var pos : placed) {
                var absolute = helper.absolutePos(pos);
                if (helper.getLevel().getBlockEntity(absolute) instanceof JukeboxBlockEntity jukebox) {
                    jukebox.setTheItem(ItemStack.EMPTY);
                }
                helper.getLevel().setBlockAndUpdate(absolute, Blocks.AIR.defaultBlockState());
                helper.getLevel().setBlockAndUpdate(absolute.above(), Blocks.AIR.defaultBlockState());
            }
            TestPlayers.leave(helper, profile);
        }

        private JukeboxBlockEntity jukebox(BlockPos pos) {
            if (placed.add(pos)) {
                helper.setBlock(pos, Blocks.JUKEBOX);
            }
            return helper.getBlockEntity(pos, JukeboxBlockEntity.class);
        }
    }
}
