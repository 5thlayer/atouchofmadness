// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.meter;

import java.util.Arrays;

import io.github._5thlayer.atouchof.meter.Meter;
import io.github._5thlayer.atouchof.meter.MeterState;
import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import io.github._5thlayer.atouchofmadness.sanity.Sanity;
import io.github._5thlayer.atouchofmadness.sanity.Stage;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The Sanity meter, which the Runtime draws (suite ADR-0005): each tick the Pillar hands it each
 * player's Sanity, their Stage, whose index is the meter's state and picks its icons, and the
 * {@link Trend} of their Net rate. The Runtime tells the client only what changed.
 */
public final class SanityMeter {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ATouchOfMadness.MOD_ID, "sanity");

    private static Meter meter;

    private SanityMeter() {
    }

    /** Registers the meter, from the mod's constructor, as the Runtime requires. */
    public static void register() {
        var states = Arrays.stream(Stage.values()).map(SanityMeter::state).toArray(MeterState[]::new);
        meter = Meter.register(ID, (float) Sanity.MAX, states);
        NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                set(player);
            }
        });
    }

    /** The icons of a Stage: {@code hud/sanity_full}, {@code _half} and {@code _empty}, then {@code _uneasy} or {@code _insane} below Sane. */
    private static MeterState state(Stage stage) {
        var suffix = stage == Stage.SANE ? "" : "_" + stage.id();
        return new MeterState(sprite("full" + suffix), sprite("half" + suffix), sprite("empty" + suffix));
    }

    private static Identifier sprite(String name) {
        return Identifier.fromNamespaceAndPath(ATouchOfMadness.MOD_ID, "hud/sanity_" + name);
    }

    private static void set(ServerPlayer player) {
        var sanity = PlayerSanity.get(player);
        meter.set(player, (float) sanity.value(), PlayerSanity.stage(player).ordinal(), Trend.band(sanity, PlayerSanity.netRate(player)));
    }
}
