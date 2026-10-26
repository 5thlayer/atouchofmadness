// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.sanity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import io.github._5thlayer.atouchofmadness.MadnessConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Each player's Sanity, and the one place it moves. The value is the Pillar's own attachment, saved
 * with the player: a new player has the configured start, and one respawned after dying the configured
 * respawn value; a player cloned for any other reason, such as leaving the End, keeps theirs.
 *
 * <p>Once a second, on a tick of its own for each player (ADR 0005), the rates of the continuous drains
 * and restores sum into the Net rate, and Sanity moves by it for the 20 ticks. An instantaneous drain or
 * restore, {@code /sanity}, joining and respawning move it at once. Whenever it moves, and whenever the
 * config moves a threshold, the Stage-change listeners hear of a player whose Stage differs from the
 * one they last heard; a player who joins is heard of from no Stage. The Net rate and the last Stage
 * heard of are kept in memory from a player's join to their logout, and never saved; a player who never
 * joined, such as a fake one, is heard of by no one.
 */
@EventBusSubscriber(modid = ATouchOfMadness.MOD_ID)
public final class PlayerSanity {

    /** A continuous drain or restore: its rate for the player, in Sanity a tick, negative for a drain, or 0 when it does not apply. */
    @FunctionalInterface
    public interface Source {
        double rate(ServerPlayer player);
    }

    /** Hears of a player's Stage changing. It must not move Sanity itself. */
    @FunctionalInterface
    public interface StageListener {
        void changed(ServerPlayer player, StageChange change);
    }

    /** How many ticks apart the continuous drains and restores are evaluated (ADR 0005). */
    public static final int EVALUATION_TICKS = 20;

    /** The config's rules, swapped whole when it loads or reloads, possibly off the server thread. */
    private static volatile SanityRules rules = SanityRules.DEFAULT;
    private static boolean notifying;

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ATouchOfMadness.MOD_ID);

    private static final Supplier<AttachmentType<Sanity>> SANITY = ATTACHMENTS.register("sanity",
            () -> AttachmentType.builder(() -> rules.start())
                    .serialize(Codec.DOUBLE.xmap(Sanity::new, Sanity::value).fieldOf("value"))
                    .build());

    private static final List<Source> SOURCES = new CopyOnWriteArrayList<>();
    private static final List<StageListener> LISTENERS = new CopyOnWriteArrayList<>();
    private static final Map<UUID, SanityTracker> TRACKERS = new HashMap<>();

    private PlayerSanity() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        modBus.addListener(ModConfigEvent.Loading.class, PlayerSanity::configChanged);
        modBus.addListener(ModConfigEvent.Reloading.class, PlayerSanity::configChanged);
    }

    /** Adds a continuous drain or restore, which applies from the next evaluation on. */
    public static void addSource(Source source) {
        SOURCES.add(source);
    }

    /** Adds a listener that hears of every Stage change from now on, including each join. */
    public static void onStageChange(StageListener listener) {
        LISTENERS.add(listener);
    }

    public static Sanity get(ServerPlayer player) {
        return player.getData(SANITY);
    }

    public static Stage stage(ServerPlayer player) {
        return rules.stageOf(get(player));
    }

    /** The Net rate of the player's last evaluation, in Sanity a tick: 0 when nothing applied, or before the first since they joined. */
    public static double netRate(ServerPlayer player) {
        var tracker = TRACKERS.get(player.getUUID());
        return tracker == null ? 0 : tracker.netRate();
    }

    /** Sets the player's Sanity now. Not from a Stage-change listener. */
    public static void set(ServerPlayer player, Sanity sanity) {
        if (notifying) {
            throw new IllegalStateException("A Stage-change listener moved " + player.getPlainTextName() + "'s Sanity");
        }
        player.setData(SANITY, sanity);
        observe(player);
    }

    /** Applies an instantaneous drain (negative) or restore (positive) now. Not from a Stage-change listener. */
    public static Sanity add(ServerPlayer player, double amount) {
        var sanity = get(player).add(amount);
        set(player, sanity);
        return sanity;
    }

    private static void observe(ServerPlayer player) {
        var tracker = TRACKERS.get(player.getUUID());
        if (tracker == null) {
            return;
        }
        tracker.observe(get(player), rules).ifPresent(change -> {
            notifying = true;
            try {
                LISTENERS.forEach(listener -> listener.changed(player, change));
            } finally {
                notifying = false;
            }
        });
    }

    @SubscribeEvent
    static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !evaluatesNow(player)) {
            return;
        }
        var tracker = TRACKERS.get(player.getUUID());
        if (tracker == null) {
            return;
        }
        double[] rates = SOURCES.stream().mapToDouble(source -> source.rate(player)).toArray();
        var before = get(player);
        var after = tracker.evaluate(before, rates, EVALUATION_TICKS);
        if (!after.equals(before)) {
            set(player, after);
        }
    }

    /** Whether this server tick is the player's own of every {@link #EVALUATION_TICKS}, chosen from their id so players spread across them. */
    private static boolean evaluatesNow(ServerPlayer player) {
        long tick = player.level().getServer().getTickCount() + Math.floorMod(player.getUUID().hashCode(), EVALUATION_TICKS);
        return tick % EVALUATION_TICKS == 0;
    }

    /** Stores a new player's start value as they join, so a later change to the config's start leaves them be. */
    @SubscribeEvent
    static void join(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TRACKERS.put(player.getUUID(), new SanityTracker());
            set(player, get(player));
        }
    }

    @SubscribeEvent
    static void leave(PlayerEvent.PlayerLoggedOutEvent event) {
        TRACKERS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    static void respawn(PlayerEvent.Clone event) {
        if (event.isWasDeath() && event.getEntity() instanceof ServerPlayer player) {
            set(player, rules.respawn());
        }
    }

    /** Takes the config's new rules, and tells the listeners of every player they move across a threshold. */
    private static void configChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() != MadnessConfig.SPEC) {
            return;
        }
        rules = MadnessConfig.sanityRules();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        Runnable recheck = () -> server.getPlayerList().getPlayers().forEach(PlayerSanity::observe);
        if (server.isSameThread()) {
            recheck.run();
        } else {
            server.execute(recheck);
        }
    }
}
