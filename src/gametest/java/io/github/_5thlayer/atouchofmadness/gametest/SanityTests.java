// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.gametest;

import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.TagValueInput;

/**
 * Every player has Sanity: 100 when new, 50 once respawned after dying, kept through a relog, and
 * read and moved by {@code /sanity} at permission level 2, held to 0–100. Each test joins its own
 * player, under a name of its own so the command finds it alone, and logs it out again however the
 * test ends, so a failure leaves no player on the shared server.
 */
final class SanityTests {

    private SanityTests() {
    }

    static void register(ATouchOfMadnessGameTests.Registrar tests) {
        tests.test("sanity_a_new_player_has_a_hundred", 1, SanityTests::newPlayer);
        tests.test("sanity_a_respawned_player_has_fifty", 1, SanityTests::respawned);
        tests.test("sanity_leaving_the_end_keeps_it", 1, SanityTests::leftTheEnd);
        tests.test("sanity_survives_a_relog", 1, SanityTests::relog);
        tests.test("sanity_command_sets_and_adds_held_to_the_range", 1, SanityTests::setsAndAdds);
        tests.test("sanity_command_needs_permission_level_2", 1, SanityTests::permission);
    }

    private static void newPlayer(GameTestHelper helper) {
        var profile = profile();
        var player = join(helper, profile);
        try {
            expect(helper, 100, run(helper, "sanity get " + player.getPlainTextName()), "a new player's Sanity");
        } finally {
            leave(helper, profile);
        }
        helper.succeed();
    }

    private static void respawned(GameTestHelper helper) {
        var profile = profile();
        var player = join(helper, profile);
        try {
            run(helper, "sanity set " + player.getPlainTextName() + " 90");
            var respawned = playerList(helper).respawn(player, false, Entity.RemovalReason.KILLED);
            expect(helper, 50, run(helper, "sanity get " + respawned.getPlainTextName()), "a respawned player's Sanity");
        } finally {
            leave(helper, profile);
        }
        helper.succeed();
    }

    /** Leaving the End clones the player too, but not by dying, so it keeps its Sanity. */
    private static void leftTheEnd(GameTestHelper helper) {
        var profile = profile();
        var player = join(helper, profile);
        try {
            run(helper, "sanity set " + player.getPlainTextName() + " 33");
            var returned = playerList(helper).respawn(player, true, Entity.RemovalReason.CHANGED_DIMENSION);
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
        var profile = profile();
        var player = join(helper, profile);
        try {
            run(helper, "sanity set " + player.getPlainTextName() + " 37.5");
            leave(helper, profile);
            double value = PlayerSanity.get(join(helper, profile)).value();
            if (value != 37.5) {
                helper.fail("Sanity after a relog is " + value + ", not 37.5");
            }
        } finally {
            leave(helper, profile);
        }
        helper.succeed();
    }

    private static void setsAndAdds(GameTestHelper helper) {
        var profile = profile();
        String name = join(helper, profile).getPlainTextName();
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
        var profile = profile();
        String command = "sanity get " + join(helper, profile).getPlainTextName();
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

    /** A fresh player's profile: a UUID-based name, since a UUID selector is refused as one that could pick other entities. */
    private static GameProfile profile() {
        var id = UUID.randomUUID();
        return new GameProfile(id, "s" + id.toString().replace("-", "").substring(0, 15));
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

    /**
     * A survival player logged in on the test's server, as {@code makeMockServerPlayerInLevel} does it,
     * with what the player's file holds read first, as a login's {@code PrepareSpawnTask} does.
     */
    private static ServerPlayer join(GameTestHelper helper, GameProfile profile) {
        var server = helper.getLevel().getServer();
        var cookie = CommonListenerCookie.createInitial(profile, false);
        var player = new ServerPlayer(server, helper.getLevel(), profile, cookie.clientInformation());
        try (var reporter = new ProblemReporter.ScopedCollector(player.problemPath(), LogUtils.getLogger())) {
            server.getPlayerList().loadPlayerData(player.nameAndId())
                    .map(tag -> TagValueInput.create(reporter, server.registryAccess(), tag))
                    .ifPresent(player::load);
        }
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        playerList(helper).placeNewPlayer(connection, player, cookie);
        return player;
    }

    /** Logs the profile's player out, whichever player object a respawn left it as, if it is still in. */
    private static void leave(GameTestHelper helper, GameProfile profile) {
        var player = playerList(helper).getPlayer(profile.id());
        if (player != null) {
            playerList(helper).remove(player);
        }
    }

    private static PlayerList playerList(GameTestHelper helper) {
        return helper.getLevel().getServer().getPlayerList();
    }
}
