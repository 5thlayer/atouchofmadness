// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import io.github._5thlayer.atouchofmadness.MadnessConfig;
import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import io.github._5thlayer.atouchofmadness.sanity.Stage;
import io.github._5thlayer.atouchofmadness.sanity.StageChange;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Every player has Sanity: 100 when new, 50 once respawned after dying, kept through a relog, and
 * read and moved by {@code /sanity} at permission level 2, held to 0–100. A listener hears each join
 * and each Stage change, from any path, and cannot move Sanity itself; the continuous drains and
 * restores move it once a second by their Net rate, which is never saved. Each test joins its own
 * player, under a name of its own so the command finds it alone, and logs it out again however the
 * test ends, so a failure leaves no player on the shared server.
 */
final class SanityTests {

    /** Every Stage change heard, by player. */
    private static final Map<UUID, List<StageChange>> HEARD = new ConcurrentHashMap<>();
    /** The players whose Stage-change listener tries to move their Sanity, and those it was refused for. */
    private static final Set<UUID> MEDDLERS = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> REFUSED = ConcurrentHashMap.newKeySet();
    /** A continuous drain's rate, by player; the players not in it it doesn't apply to. */
    private static final Map<UUID, Double> RATES = new ConcurrentHashMap<>();

    /** The Pillar's common config, as it loaded, for a test to reload. */
    private static ModConfig config;

    static {
        PlayerSanity.onStageChange((player, change) -> {
            HEARD.computeIfAbsent(player.getUUID(), id -> new ArrayList<>()).add(change);
            if (MEDDLERS.contains(player.getUUID())) {
                try {
                    PlayerSanity.add(player, -1);
                } catch (IllegalStateException refused) {
                    REFUSED.add(player.getUUID());
                }
            }
        });
        PlayerSanity.addSource(player -> RATES.getOrDefault(player.getUUID(), 0.0));
    }

    private SanityTests() {
    }

    static void listen(IEventBus modBus) {
        modBus.addListener(ModConfigEvent.Loading.class, event -> {
            if (event.getConfig().getSpec() == MadnessConfig.SPEC) {
                config = event.getConfig();
            }
        });
    }

    static void register(ATouchOfMadnessGameTests.Registrar tests) {
        tests.test("sanity_a_new_player_has_a_hundred", 1, SanityTests::newPlayer);
        tests.test("sanity_a_respawned_player_has_fifty", 1, SanityTests::respawned);
        tests.test("sanity_leaving_the_end_keeps_it", 1, SanityTests::leftTheEnd);
        tests.test("sanity_survives_a_relog", 1, SanityTests::relog);
        tests.test("sanity_command_sets_and_adds_held_to_the_range", 1, SanityTests::setsAndAdds);
        tests.test("sanity_command_needs_permission_level_2", 1, SanityTests::permission);
        tests.test("sanity_a_listener_hears_a_join_from_no_stage", 1, SanityTests::heardJoin);
        tests.test("sanity_a_listener_hears_the_command_cross_a_threshold", 1, SanityTests::heardCommand);
        tests.test("sanity_a_listener_hears_a_respawn_into_another_stage", 1, SanityTests::heardRespawn);
        tests.test("sanity_a_listener_hears_a_reload_move_a_threshold_across_it", 1, SanityTests::heardReload);
        tests.test("sanity_a_listener_cannot_move_it", 1, SanityTests::listenerCannotMove);
        tests.test("sanity_moves_once_a_second_by_its_net_rate", 3 * PlayerSanity.EVALUATION_TICKS, SanityTests::onceASecond);
    }

    private static void heardJoin(GameTestHelper helper) {
        var profile = TestPlayers.profile();
        TestPlayers.join(helper, profile);
        try {
            expectHeard(helper, profile, List.of(change(null, Stage.SANE)));
        } finally {
            leave(helper, profile);
        }
        helper.succeed();
    }

    /** An instantaneous move across a threshold is heard; one within a Stage is not. */
    private static void heardCommand(GameTestHelper helper) {
        var profile = TestPlayers.profile();
        String name = TestPlayers.join(helper, profile).getPlainTextName();
        try {
            run(helper, "sanity set " + name + " 60");
            run(helper, "sanity set " + name + " 10");
            run(helper, "sanity add " + name + " 5");
            run(helper, "sanity add " + name + " 20");
            expectHeard(helper, profile, List.of(change(null, Stage.SANE), change(Stage.SANE, Stage.INSANE),
                    change(Stage.INSANE, Stage.UNEASY)));
        } finally {
            leave(helper, profile);
        }
        helper.succeed();
    }

    /** Respawning at 50 is heard from Insane, and not from Uneasy, where it already was. */
    private static void heardRespawn(GameTestHelper helper) {
        var profile = TestPlayers.profile();
        var player = TestPlayers.join(helper, profile);
        try {
            run(helper, "sanity set " + player.getPlainTextName() + " 10");
            var respawned = TestPlayers.playerList(helper).respawn(player, false, Entity.RemovalReason.KILLED);
            TestPlayers.playerList(helper).respawn(respawned, false, Entity.RemovalReason.KILLED);
            expectHeard(helper, profile, List.of(change(null, Stage.SANE), change(Stage.SANE, Stage.INSANE),
                    change(Stage.INSANE, Stage.UNEASY)));
        } finally {
            leave(helper, profile);
        }
        helper.succeed();
    }

    /**
     * Raising the Sane line from 50 to 70 makes a player at 60 Uneasy, and putting it back makes them
     * Sane again. The other tests' players stay in their Stage under either line.
     */
    private static void heardReload(GameTestHelper helper) {
        var profile = TestPlayers.profile();
        var player = TestPlayers.join(helper, profile);
        var saneAbove = (ModConfigSpec.DoubleValue) MadnessConfig.SPEC.getValues().get(List.of("sanity", "saneAbove"));
        double original = saneAbove.get();
        try {
            run(helper, "sanity set " + player.getPlainTextName() + " 60");
            reload(saneAbove, 70);
            if (PlayerSanity.stage(player) != Stage.UNEASY) {
                helper.fail("a player at 60 is " + PlayerSanity.stage(player) + " with the Sane line at 70");
            }
            reload(saneAbove, original);
            expectHeard(helper, profile, List.of(change(null, Stage.SANE), change(Stage.SANE, Stage.UNEASY),
                    change(Stage.UNEASY, Stage.SANE)));
        } finally {
            // Heard by no one when the line is already back.
            reload(saneAbove, original);
            leave(helper, profile);
        }
        helper.succeed();
    }

    private static void listenerCannotMove(GameTestHelper helper) {
        var profile = TestPlayers.profile();
        MEDDLERS.add(profile.id());
        var player = TestPlayers.join(helper, profile);
        try {
            if (!REFUSED.contains(profile.id())) {
                helper.fail("a Stage-change listener moved Sanity unrefused");
            }
            if (PlayerSanity.get(player).value() != 100) {
                helper.fail("a Stage-change listener moved Sanity to " + PlayerSanity.get(player).value());
            }
        } finally {
            MEDDLERS.remove(profile.id());
            leave(helper, profile);
        }
        helper.succeed();
    }

    /**
     * A drain of 0.05 a tick takes 1 at each of the player's evaluations, which come 20 ticks apart
     * with Sanity still between them. The test ticks the player itself. After a relog the Net rate is
     * 0 again, since it is never saved.
     */
    private static void onceASecond(GameTestHelper helper) {
        var profile = TestPlayers.profile();
        TestPlayers.whenDone(helper, () -> {
            RATES.remove(profile.id());
            leave(helper, profile);
        });
        TestPlayers.join(helper, profile);
        RATES.put(profile.id(), -0.05);
        long[] firstEvaluation = {-1};
        helper.onEachTick(() -> {
            var player = TestPlayers.playerList(helper).getPlayer(profile.id());
            player.doTick();
            double value = PlayerSanity.get(player).value();
            long tick = helper.getTick();
            if (firstEvaluation[0] < 0) {
                if (value != 100) {
                    if (value != 99 || PlayerSanity.netRate(player) != -0.05) {
                        helper.fail("the first evaluation left Sanity " + value + " and a Net rate of "
                                + PlayerSanity.netRate(player) + ", not 99 and -0.05");
                    }
                    firstEvaluation[0] = tick;
                }
                return;
            }
            long since = tick - firstEvaluation[0];
            if (since < PlayerSanity.EVALUATION_TICKS) {
                if (value != 99) {
                    helper.fail("Sanity moved to " + value + " " + since + " ticks after an evaluation");
                }
                return;
            }
            if (value != 98) {
                helper.fail("Sanity is " + value + " an evaluation later, not 98");
            }
            leave(helper, profile);
            double relogged = PlayerSanity.netRate(TestPlayers.join(helper, profile));
            if (relogged != 0) {
                helper.fail("the Net rate after a relog is " + relogged + ", not 0");
            }
            helper.succeed();
        });
    }

    /** Sets the config's Sane line in memory, without saving the file, and reloads it as a change to the file would. */
    private static void reload(ModConfigSpec.DoubleValue saneAbove, double value) {
        saneAbove.set(value);
        ModList.get().getModContainerById(ATouchOfMadness.MOD_ID).orElseThrow()
                .acceptEvent(new ModConfigEvent.Reloading(config));
    }

    private static StageChange change(Stage from, Stage to) {
        return new StageChange(Optional.ofNullable(from), to);
    }

    private static void expectHeard(GameTestHelper helper, GameProfile profile, List<StageChange> expected) {
        var heard = HEARD.getOrDefault(profile.id(), List.of());
        if (!heard.equals(expected)) {
            helper.fail("heard " + heard + ", not " + expected);
        }
    }

    private static void newPlayer(GameTestHelper helper) {
        var profile = TestPlayers.profile();
        var player = TestPlayers.join(helper, profile);
        try {
            expect(helper, 100, run(helper, "sanity get " + player.getPlainTextName()), "a new player's Sanity");
        } finally {
            leave(helper, profile);
        }
        helper.succeed();
    }

    private static void respawned(GameTestHelper helper) {
        var profile = TestPlayers.profile();
        var player = TestPlayers.join(helper, profile);
        try {
            run(helper, "sanity set " + player.getPlainTextName() + " 90");
            var respawned = TestPlayers.playerList(helper).respawn(player, false, Entity.RemovalReason.KILLED);
            expect(helper, 50, run(helper, "sanity get " + respawned.getPlainTextName()), "a respawned player's Sanity");
        } finally {
            leave(helper, profile);
        }
        helper.succeed();
    }

    /** Leaving the End clones the player too, but not by dying, so it keeps its Sanity. */
    private static void leftTheEnd(GameTestHelper helper) {
        var profile = TestPlayers.profile();
        var player = TestPlayers.join(helper, profile);
        try {
            run(helper, "sanity set " + player.getPlainTextName() + " 33");
            var returned = TestPlayers.playerList(helper).respawn(player, true, Entity.RemovalReason.CHANGED_DIMENSION);
            expect(helper, 33, run(helper, "sanity get " + returned.getPlainTextName()), "Sanity after leaving the End");
        } finally {
            leave(helper, profile);
        }
        helper.succeed();
    }

    /**
     * Logging out saves the player to its file and logging in reads it back. A server restart saves
     * and reads the same file, so this stands for it too.
     */
    private static void relog(GameTestHelper helper) {
        var profile = TestPlayers.profile();
        var player = TestPlayers.join(helper, profile);
        try {
            run(helper, "sanity set " + player.getPlainTextName() + " 37.5");
            leave(helper, profile);
            double value = PlayerSanity.get(TestPlayers.join(helper, profile)).value();
            if (value != 37.5) {
                helper.fail("Sanity after a relog is " + value + ", not 37.5");
            }
        } finally {
            leave(helper, profile);
        }
        helper.succeed();
    }

    private static void setsAndAdds(GameTestHelper helper) {
        var profile = TestPlayers.profile();
        String name = TestPlayers.join(helper, profile).getPlainTextName();
        try {
            expect(helper, 42, run(helper, "sanity set " + name + " 42"), "set to 42");
            expect(helper, 30, run(helper, "sanity add " + name + " -12"), "42 add -12");
            expect(helper, 100, run(helper, "sanity set " + name + " 150"), "set to 150");
            expect(helper, 0, run(helper, "sanity add " + name + " -250"), "100 add -250");
            expect(helper, 100, run(helper, "sanity add " + name + " 250"), "0 add 250");
            expect(helper, 0, run(helper, "sanity set " + name + " -5"), "set to -5");
        } finally {
            leave(helper, profile);
        }
        helper.succeed();
    }

    private static void permission(GameTestHelper helper) {
        var profile = TestPlayers.profile();
        String command = "sanity get " + TestPlayers.join(helper, profile).getPlainTextName();
        try {
            boolean moderatorRan = true;
            try {
                run(helper, command, LevelBasedPermissionSet.MODERATOR);
            } catch (IllegalStateException refused) {
                moderatorRan = false;
            }
            if (moderatorRan) {
                helper.fail("/sanity ran at permission level 1");
            }
            expect(helper, 100, run(helper, command, LevelBasedPermissionSet.GAMEMASTER), "level 2 reads Sanity");
        } finally {
            leave(helper, profile);
        }
        helper.succeed();
    }

    private static int run(GameTestHelper helper, String command) {
        return run(helper, command, LevelBasedPermissionSet.GAMEMASTER);
    }

    private static int run(GameTestHelper helper, String command, PermissionSet permissions) {
        var server = helper.getLevel().getServer();
        var source = server.createCommandSourceStack().withPermission(permissions).withSuppressedOutput();
        try {
            return server.getCommands().getDispatcher().execute(command, source);
        } catch (CommandSyntaxException e) {
            throw new IllegalStateException("/" + command + ": " + e.getMessage(), e);
        }
    }

    private static void expect(GameTestHelper helper, int expected, int actual, String what) {
        if (actual != expected) {
            helper.fail(what + " reads " + actual + " through /sanity, not " + expected);
        }
    }

    private static void leave(GameTestHelper helper, GameProfile profile) {
        TestPlayers.leave(helper, profile);
        HEARD.remove(profile.id());
    }
}
