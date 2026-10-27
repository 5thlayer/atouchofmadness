// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.hallucination;

import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import io.github._5thlayer.atouchofmadness.sanity.Sanity;
import io.github._5thlayer.atouchofmadness.sanity.Stage;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** What the server tells a player's client of their Sanity, for its Hallucinations: the value and the Stage it is in. */
public record SanityPayload(Sanity sanity, Stage stage) implements CustomPacketPayload {

    static final Type<SanityPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ATouchOfMadness.MOD_ID, "sanity"));

    static final StreamCodec<ByteBuf, SanityPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE.map(Sanity::new, Sanity::value), SanityPayload::sanity,
            ByteBufCodecs.idMapper(id -> Stage.values()[id], Stage::ordinal), SanityPayload::stage,
            SanityPayload::new);

    @Override
    public Type<SanityPayload> type() {
        return TYPE;
    }
}
