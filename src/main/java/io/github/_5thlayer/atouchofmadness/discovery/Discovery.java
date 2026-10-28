// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.discovery;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import io.github._5thlayer.atouchof.discovery.DiscoverySync;
import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import io.github._5thlayer.atouchofmadness.MadnessConfig;
import io.github._5thlayer.atouchofmadness.sanity.Cause;
import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import io.github._5thlayer.atouchofmadness.sanity.Stage;
import io.github._5thlayer.atouchofmadness.sanity.StageChange;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Discovery (suite ADR-0006): each discoverable thing, a Cause or a Stage below Sane, is one advancement
 * at {@code atouchofmadness:discovery/<id>}, and its being done is the discovery. Its criteria are this
 * Pillar's two triggers, {@code sanity_changed} and {@code stage_reached}, which fire whether or not
 * discovery is on. The Runtime keeps the record: {@link #isKnown} asks it, and it tells each player's
 * client what that answers for them, which {@link ClientDiscovery} reads.
 */
public final class Discovery {

    /** The tab's root, which every player is granted as they join. */
    public static final Identifier ROOT = advancementId("root");

    private static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, ATouchOfMadness.MOD_ID);

    private static final Supplier<SanityChangedTrigger> SANITY_CHANGED = TRIGGERS.register("sanity_changed", SanityChangedTrigger::new);
    private static final Supplier<StageReachedTrigger> STAGE_REACHED = TRIGGERS.register("stage_reached", StageReachedTrigger::new);

    /** The Runtime's record of this Pillar's Discoveries, from its registration on. */
    private static volatile io.github._5thlayer.atouchof.discovery.Discovery record;

    private Discovery() {
    }

    public static void register(IEventBus modBus) {
        record = io.github._5thlayer.atouchof.discovery.Discovery.register(modBus, ATouchOfMadness.MOD_ID, Discoverables.IDS,
                MadnessConfig::discoveryEnabled);
        TRIGGERS.register(modBus);
        PlayerSanity.onMove((player, cause) -> SANITY_CHANGED.get().trigger(player, cause));
        PlayerSanity.onStageChange(Discovery::stageChanged);
    }

    /**
     * Whether the player has discovered the Cause: always, when discovery is off or its advancement is
     * missing, as when a pack removed it; otherwise when its advancement is done.
     */
    public static boolean isKnown(ServerPlayer player, Cause cause) {
        return record.isKnown(player, cause.id());
    }

    /** As {@link #isKnown(ServerPlayer, Cause)}; Sane, which is no discovery, is always known. */
    public static boolean isKnown(ServerPlayer player, Stage stage) {
        return stage == Stage.SANE || record.isKnown(player, stage.id());
    }

    /** The ids of every discoverable thing {@link #isKnown} answers known for, for the player. */
    public static Set<String> known(ServerPlayer player) {
        return record.known(player);
    }

    /** The advancement a discoverable thing's id stands for, or the root's for {@code root}. */
    public static Identifier advancementId(String id) {
        return Identifier.fromNamespaceAndPath(ATouchOfMadness.MOD_ID, "discovery/" + id);
    }

    /** Adds a listener that hears of each set of ids the server tells a player's client from now on, whether or not that client can hear it. */
    public static void onTell(BiConsumer<ServerPlayer, Set<String>> listener) {
        DiscoverySync.onSend((player, payload) -> {
            if (payload.namespace().equals(ATouchOfMadness.MOD_ID)) {
                listener.accept(player, payload.known());
            }
        });
    }

    /** Client-side, whether the thing's id is known, as the Runtime was last told. */
    static boolean isKnownOnClient(String id) {
        return record.isKnownOnClient(id);
    }

    /** Each Stage from Uneasy down to the one entered is reached, so one passed through on the way down is too. */
    private static void stageChanged(ServerPlayer player, StageChange change) {
        for (var stage : Discoverables.STAGES) {
            if (stage.compareTo(change.to()) <= 0) {
                STAGE_REACHED.get().trigger(player, stage);
            }
        }
    }
}
