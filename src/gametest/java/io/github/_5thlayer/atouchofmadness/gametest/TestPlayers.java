// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;

/**
 * The players a test joins to the shared server: each under a name of its own, so a command finds it
 * alone, and logged out again however the test ends, so a failure leaves no player behind.
 */
final class TestPlayers {

    /** Each joined player's channel, by their id, which keeps every packet the server sends them. */
    private static final Map<UUID, EmbeddedChannel> CHANNELS = new ConcurrentHashMap<>();

    private TestPlayers() {
    }

    /** A fresh player's profile: a UUID-based name, since a UUID selector is refused as one that could pick other entities. */
    static GameProfile profile() {
        var id = UUID.randomUUID();
        return new GameProfile(id, "s" + id.toString().replace("-", "").substring(0, 15));
    }

    /**
     * A player logged in on the test's server in its default game mode, creative, as
     * {@code makeMockServerPlayerInLevel} does it, with what the player's file holds read first, as a
     * login's {@code PrepareSpawnTask} does. A connection would tick the player each server tick;
     * this one doesn't, so a test that needs it ticks the player itself.
     */
    static ServerPlayer join(GameTestHelper helper, GameProfile profile) {
        var server = helper.getLevel().getServer();
        var cookie = CommonListenerCookie.createInitial(profile, false);
        var player = new ServerPlayer(server, helper.getLevel(), profile, cookie.clientInformation());
        try (var reporter = new ProblemReporter.ScopedCollector(player.problemPath(), LogUtils.getLogger())) {
            server.getPlayerList().loadPlayerData(player.nameAndId())
                    .map(tag -> TagValueInput.create(reporter, server.registryAccess(), tag))
                    .ifPresent(player::load);
        }
        var connection = new Connection(PacketFlow.SERVERBOUND);
        CHANNELS.put(profile.id(), new EmbeddedChannel(connection));
        playerList(helper).placeNewPlayer(connection, player, cookie);
        return player;
    }

    /** Logs the profile's player out, whichever player object a respawn left it as, if it is still in. */
    static void leave(GameTestHelper helper, GameProfile profile) {
        var player = playerList(helper).getPlayer(profile.id());
        if (player != null) {
            playerList(helper).remove(player);
        }
        CHANNELS.remove(profile.id());
    }

    /** The packets sent the player since they joined or this was last asked, which it forgets. */
    static List<Object> sent(ServerPlayer player) {
        var channel = CHANNELS.get(player.getUUID());
        if (channel == null) {
            return List.of();
        }
        channel.flushOutbound();
        var sent = new ArrayList<>();
        var packet = channel.readOutbound();
        while (packet != null) {
            sent.add(packet);
            packet = channel.readOutbound();
        }
        return sent;
    }

    /** Runs {@code done} when the test passes or fails, at whatever tick. */
    static void whenDone(GameTestHelper helper, Runnable done) {
        helper.testInfo.addListener(new GameTestListener() {
            @Override
            public void testStructureLoaded(GameTestInfo testInfo) {
            }

            @Override
            public void testPassed(GameTestInfo testInfo, GameTestRunner runner) {
                done.run();
            }

            @Override
            public void testFailed(GameTestInfo testInfo, GameTestRunner runner) {
                done.run();
            }

            @Override
            public void testAddedForRerun(GameTestInfo original, GameTestInfo copy, GameTestRunner runner) {
            }
        });
    }

    static PlayerList playerList(GameTestHelper helper) {
        return helper.getLevel().getServer().getPlayerList();
    }
}
