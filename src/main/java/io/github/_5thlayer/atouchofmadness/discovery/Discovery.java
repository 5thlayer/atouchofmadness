// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.discovery;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import io.github._5thlayer.atouchofmadness.MadnessConfig;
import io.github._5thlayer.atouchofmadness.sanity.Cause;
import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import io.github._5thlayer.atouchofmadness.sanity.Stage;
import io.github._5thlayer.atouchofmadness.sanity.StageChange;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Discovery (ADR 0006): each discoverable thing, a Cause or a Stage below Sane, is one advancement at
 * {@code atouchofmadness:discovery/<id>}, and its being done is the discovery. Its criteria are this
 * Pillar's two triggers, {@code sanity_changed} and {@code stage_reached}, which fire whether or not
 * discovery is on. {@link #isKnown} is the one check of what a player has discovered, and the server
 * tells each player's client what it answers for them on login, on each change to one of these
 * advancements, on a datapack reload and on a config reload, whenever that differs from what it last told.
 */
public final class Discovery {

    /** The tab's root, which every player is granted as they join. */
    public static final Identifier ROOT = advancementId("root");

    private static final List<Stage> STAGES = List.of(Stage.UNEASY, Stage.INSANE);

    /** Every discoverable thing's id: each Cause's, then each Stage's below Sane. */
    private static final List<String> IDS = Stream.concat(Arrays.stream(Cause.values()).map(Cause::id), STAGES.stream().map(Discovery::id))
            .toList();

    private static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, ATouchOfMadness.MOD_ID);

    private static final Supplier<SanityChangedTrigger> SANITY_CHANGED = TRIGGERS.register("sanity_changed", SanityChangedTrigger::new);
    private static final Supplier<StageReachedTrigger> STAGE_REACHED = TRIGGERS.register("stage_reached", StageReachedTrigger::new);

    /** What each logged-in player's client was last told, kept from their join to their logout. */
    private static final Map<UUID, Set<String>> TOLD = new HashMap<>();
    private static final List<BiConsumer<ServerPlayer, Set<String>>> TELL_LISTENERS = new CopyOnWriteArrayList<>();

    private Discovery() {
    }

    public static void register(IEventBus modBus) {
        TRIGGERS.register(modBus);
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar("1")
                .playToClient(KnownPayload.TYPE, KnownPayload.STREAM_CODEC, (payload, context) -> ClientDiscovery.told(payload.known())));
        modBus.addListener(ModConfigEvent.Loading.class, Discovery::configChanged);
        modBus.addListener(ModConfigEvent.Reloading.class, Discovery::configChanged);
        PlayerSanity.onMove((player, cause) -> SANITY_CHANGED.get().trigger(player, cause));
        PlayerSanity.onStageChange(Discovery::stageChanged);
        NeoForge.EVENT_BUS.addListener(OnDatapackSyncEvent.class, event -> event.getRelevantPlayers().forEach(Discovery::tell));
        NeoForge.EVENT_BUS.addListener(AdvancementEvent.AdvancementProgressEvent.class, Discovery::progressed);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> TOLD.remove(event.getEntity().getUUID()));
    }

    /**
     * Whether the player has discovered the Cause: always, when discovery is off or its advancement is
     * missing, as when a pack removed it; otherwise when its advancement is done.
     */
    public static boolean isKnown(ServerPlayer player, Cause cause) {
        return isKnown(player, cause.id());
    }

    /** As {@link #isKnown(ServerPlayer, Cause)}; Sane, which is no discovery, is always known. */
    public static boolean isKnown(ServerPlayer player, Stage stage) {
        return stage == Stage.SANE || isKnown(player, id(stage));
    }

    /** The ids of every discoverable thing {@link #isKnown} answers known for, for the player. */
    public static Set<String> known(ServerPlayer player) {
        return IDS.stream().filter(id -> isKnown(player, id)).collect(Collectors.toUnmodifiableSet());
    }

    /** The advancement a discoverable thing's id stands for, or the root's for {@code root}. */
    public static Identifier advancementId(String id) {
        return Identifier.fromNamespaceAndPath(ATouchOfMadness.MOD_ID, "discovery/" + id);
    }

    /** Adds a listener that hears of each set of ids the server tells a player's client from now on, whether or not that client can hear it. */
    public static void onTell(BiConsumer<ServerPlayer, Set<String>> listener) {
        TELL_LISTENERS.add(listener);
    }

    static String id(Stage stage) {
        return stage.name().toLowerCase(Locale.ROOT);
    }

    /** An enum by its lower-case name, as data names it. */
    static <E extends Enum<E>> Codec<E> enumCodec(Class<E> type) {
        return Codec.STRING.comapFlatMap(name -> Arrays.stream(type.getEnumConstants())
                .filter(constant -> constant.name().toLowerCase(Locale.ROOT).equals(name))
                .findFirst()
                .map(DataResult::success)
                .orElseGet(() -> DataResult.error(() -> "no " + type.getSimpleName() + " " + name)),
                constant -> constant.name().toLowerCase(Locale.ROOT));
    }

    private static boolean isKnown(ServerPlayer player, String id) {
        if (!MadnessConfig.discoveryEnabled()) {
            return true;
        }
        var advancement = player.level().getServer().getAdvancements().get(advancementId(id));
        return advancement == null || player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    /** Each Stage from Uneasy down to the one entered is reached, so one passed through on the way down is too. */
    private static void stageChanged(ServerPlayer player, StageChange change) {
        for (var stage : STAGES) {
            if (stage.compareTo(change.to()) <= 0) {
                STAGE_REACHED.get().trigger(player, stage);
            }
        }
    }

    private static void progressed(AdvancementEvent.AdvancementProgressEvent event) {
        var id = event.getAdvancement().id();
        if (event.getEntity() instanceof ServerPlayer player && id.getNamespace().equals(ATouchOfMadness.MOD_ID)
                && id.getPath().startsWith("discovery/")) {
            tell(player);
        }
    }

    /** Tells the player's client what is known, if that differs from what it was last told. */
    private static void tell(ServerPlayer player) {
        var known = known(player);
        if (known.equals(TOLD.put(player.getUUID(), known))) {
            return;
        }
        TELL_LISTENERS.forEach(listener -> listener.accept(player, known));
        // A connection that never negotiated the channel, as a fake player's, can't be sent it.
        if (player.connection != null && player.connection.hasChannel(KnownPayload.TYPE)) {
            player.connection.send(new KnownPayload(known));
        }
    }

    private static void configChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() != MadnessConfig.SPEC) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        Runnable retell = () -> server.getPlayerList().getPlayers().forEach(Discovery::tell);
        if (server.isSameThread()) {
            retell.run();
        } else {
            server.execute(retell);
        }
    }
}
