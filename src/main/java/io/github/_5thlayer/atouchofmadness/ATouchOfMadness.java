// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness;

import io.github._5thlayer.atouchofmadness.crown.FlowerCrown;
import io.github._5thlayer.atouchofmadness.discovery.Discovery;
import io.github._5thlayer.atouchofmadness.environment.EnvironmentDrains;
import io.github._5thlayer.atouchofmadness.hallucination.Hallucinations;
import io.github._5thlayer.atouchofmadness.meter.SanityMeter;
import io.github._5thlayer.atouchofmadness.moments.Moments;
import io.github._5thlayer.atouchofmadness.music.MusicSource;
import io.github._5thlayer.atouchofmadness.nearby.NearbySources;
import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import io.github._5thlayer.atouchofmadness.shadow.ShadowCreatures;
import io.github._5thlayer.atouchofmadness.umbra.UmbraGear;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/**
 * The Pillar's entry point. It registers the Pillar's own creative tab, each player's Sanity, the
 * drains of where they stand, the drain and restore of who is near, the restore of music, the Flower
 * Crown, the moments that move it at once, the Shadow Creatures and Umbra, Umbra Gear, discovery,
 * Hallucinations, the Sanity meter the Runtime draws, and the common and client configs. The game tests
 * hook themselves in from the {@code gametest} source set, which never ships (libworks ADR 0011).
 */
@Mod(ATouchOfMadness.MOD_ID)
public final class ATouchOfMadness {

    /** The mod id, which gradle.properties' {@code mod_id} must match. */
    public static final String MOD_ID = "atouchofmadness";

    public ATouchOfMadness(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, MadnessConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, MadnessClientConfig.SPEC);
        MadnessConfig.register(modBus);
        MadnessTab.register(modBus);
        PlayerSanity.register(modBus);
        EnvironmentDrains.register(modBus);
        NearbySources.register(modBus);
        MusicSource.register(modBus);
        FlowerCrown.register(modBus);
        Moments.register(modBus);
        ShadowCreatures.register(modBus);
        UmbraGear.register(modBus);
        Discovery.register(modBus);
        Hallucinations.register(modBus);
        SanityMeter.register();
    }
}
