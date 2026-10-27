// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.hallucination;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import io.github._5thlayer.atouchofmadness.sanity.Sanity;
import io.github._5thlayer.atouchofmadness.sanity.Stage;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Hallucinations: harmless effects of low Sanity that only the player sees or hears, all played by
 * their own client. The server tells each player's client their Sanity and Stage in a payload of the
 * Pillar's own, {@link SanityPayload}, as they join and whenever either changes, from a threshold the
 * config moves too. The Shadow Creatures' own sounds are no Hallucination's: they are
 * in {@code ShadowSounds}. The client's side is in {@code hallucination.client}.
 */
public final class Hallucinations {

    /** What each logged-in player's client was last told, kept from their join to their logout. */
    private static final Map<UUID, SanityPayload> TOLD = new HashMap<>();
    private static final List<BiConsumer<ServerPlayer, SanityPayload>> TELL_LISTENERS = new CopyOnWriteArrayList<>();

    private Hallucinations() {
    }

    public static void register(IEventBus modBus) {
        HallucinationSounds.register(modBus);
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar("1")
                .playToClient(SanityPayload.TYPE, SanityPayload.STREAM_CODEC, (payload, context) -> ClientSanity.told(payload)));
        PlayerSanity.onCheck(Hallucinations::tell);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> TOLD.remove(event.getEntity().getUUID()));
    }

    /** Adds a listener that hears of each payload the server tells a player's client from now on, whether or not that client can hear it. */
    public static void onTell(BiConsumer<ServerPlayer, SanityPayload> listener) {
        TELL_LISTENERS.add(listener);
    }

    /** Tells the player's client their Sanity and Stage, if they differ from what it was last told. */
    private static void tell(ServerPlayer player, Sanity sanity, Stage stage) {
        var payload = new SanityPayload(sanity, stage);
        if (payload.equals(TOLD.put(player.getUUID(), payload))) {
            return;
        }
        TELL_LISTENERS.forEach(listener -> listener.accept(player, payload));
        // A connection that never negotiated the channel, as a fake player's, can't be sent it.
        if (player.connection != null && player.connection.hasChannel(SanityPayload.TYPE)) {
            player.connection.send(payload);
        }
    }
}
