// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.compat.jade;

import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import io.github._5thlayer.atouchofmadness.music.MusicSource;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * A playing jukebox in Jade: its disc's freshness for the player looking, "Fresh", or "Stale" with the
 * share of a Fresh disc's restore it gives. The freshness is in that player's Disc History, which only
 * the server has, so it comes through Jade's server data, and the server sends none until that player
 * has discovered the Stale disc (#19).
 *
 * <p>Found by Jade's annotation scan and named nowhere else, so Jade stays a soft dependency.
 */
@WailaPlugin
public final class MadnessJadePlugin implements IWailaPlugin {

    private static final Identifier FRESHNESS = Identifier.fromNamespaceAndPath(ATouchOfMadness.MOD_ID, "freshness");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(Server.INSTANCE, JukeboxBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(Client.INSTANCE, JukeboxBlock.class);
    }

    /** The server's half: the factor of the jukebox's play for the looking player, if they may be shown it. Names no client type. */
    private enum Server implements StreamServerDataProvider<BlockAccessor, Double> {
        INSTANCE;

        @Override
        public @Nullable Double streamData(BlockAccessor accessor) {
            if (!(accessor.getPlayer() instanceof ServerPlayer player) || !(accessor.getBlockEntity() instanceof JukeboxBlockEntity jukebox)) {
                return null;
            }
            var factor = MusicSource.shownFreshness(player, jukebox);
            return factor.isPresent() ? factor.getAsDouble() : null;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Double> streamCodec() {
            return ByteBufCodecs.DOUBLE.cast();
        }

        @Override
        public Identifier getUid() {
            return FRESHNESS;
        }
    }

    /** The client's half: one line, Fresh or Stale and its share. Client-only. */
    private enum Client implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            Server.INSTANCE.decodeFromData(accessor).ifPresent(factor -> tooltip.add(factor >= 1
                    ? Component.translatable("atouchofmadness.jade.fresh")
                    : Component.translatable("atouchofmadness.jade.stale", Share.of(factor))));
        }

        @Override
        public Identifier getUid() {
            return FRESHNESS;
        }
    }
}
