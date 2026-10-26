// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.sanity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import io.github._5thlayer.atouchofmadness.MadnessConfig;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Each player's Sanity: the Pillar's own attachment, saved with the player. A new player has the
 * configured start, and one respawned after dying the configured respawn value; a player cloned for
 * any other reason, such as leaving the End, keeps theirs. Each tick the rates of the continuous
 * drains and restores that apply sum into one net rate that moves it ({@link Sanity#tick}).
 */
@EventBusSubscriber(modid = ATouchOfMadness.MOD_ID)
public final class PlayerSanity {

    /** A continuous drain or restore: its rate for the player this tick, negative for a drain, or 0 when it does not apply. */
    @FunctionalInterface
    public interface Source {
        double rate(ServerPlayer player);
    }

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ATouchOfMadness.MOD_ID);

    private static final Supplier<AttachmentType<Sanity>> SANITY = ATTACHMENTS.register("sanity",
            () -> AttachmentType.builder(() -> MadnessConfig.sanityRules().start())
                    .serialize(Codec.DOUBLE.xmap(Sanity::new, Sanity::value).fieldOf("value"))
                    .build());

    private static final List<Source> SOURCES = new CopyOnWriteArrayList<>();

    private PlayerSanity() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }

    /** Adds a continuous drain or restore, which applies from the next tick on. */
    public static void addSource(Source source) {
        SOURCES.add(source);
    }

    public static Sanity get(ServerPlayer player) {
        return player.getData(SANITY);
    }

    public static void set(ServerPlayer player, Sanity sanity) {
        player.setData(SANITY, sanity);
    }

    /** Applies an instantaneous drain (negative) or restore (positive) now. */
    public static Sanity add(ServerPlayer player, double amount) {
        var sanity = get(player).add(amount);
        set(player, sanity);
        return sanity;
    }

    public static Stage stage(ServerPlayer player) {
        return MadnessConfig.sanityRules().stageOf(get(player));
    }

    @SubscribeEvent
    static void tick(PlayerTickEvent.Post event) {
        if (SOURCES.isEmpty() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var rates = SOURCES.stream().map(source -> source.rate(player)).toList();
        var before = get(player);
        var after = before.tick(rates);
        if (!after.equals(before)) {
            set(player, after);
        }
    }

    /** Stores a new player's start value as they join, so a later change to the config's start leaves them be. */
    @SubscribeEvent
    static void join(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            set(player, get(player));
        }
    }

    @SubscribeEvent
    static void respawn(PlayerEvent.Clone event) {
        if (event.isWasDeath() && event.getEntity() instanceof ServerPlayer player) {
            set(player, MadnessConfig.sanityRules().respawn());
        }
    }
}
