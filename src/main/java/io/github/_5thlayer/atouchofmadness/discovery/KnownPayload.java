// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.discovery;

import java.util.HashSet;
import java.util.Set;

import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** What the server tells a player's client is known: the ids of every discoverable thing its check answers known for. */
record KnownPayload(Set<String> known) implements CustomPacketPayload {

    static final Type<KnownPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ATouchOfMadness.MOD_ID, "known"));

    static final StreamCodec<ByteBuf, KnownPayload> STREAM_CODEC = ByteBufCodecs.STRING_UTF8
            .apply(ByteBufCodecs.collection(HashSet::new))
            .map(known -> new KnownPayload(Set.copyOf(known)), payload -> new HashSet<>(payload.known()));

    KnownPayload {
        known = Set.copyOf(known);
    }

    @Override
    public Type<KnownPayload> type() {
        return TYPE;
    }
}
