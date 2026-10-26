// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness;

import io.github._5thlayer.atouchofmadness.environment.EnvironmentDrains;
import io.github._5thlayer.atouchofmadness.moments.Moments;
import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/**
 * The Pillar's entry point. It registers the Pillar's own creative tab, each player's Sanity, the
 * drains of where they stand, the moments that move it at once and the common config. The game
 * tests hook themselves in from the {@code gametest} source set, which never ships (libworks ADR 0011).
 */
@Mod(ATouchOfMadness.MOD_ID)
public final class ATouchOfMadness {

    /** The mod id, which gradle.properties' {@code mod_id} must match. */
    public static final String MOD_ID = "atouchofmadness";

    public ATouchOfMadness(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, MadnessConfig.SPEC);
        MadnessTab.register(modBus);
        PlayerSanity.register(modBus);
        EnvironmentDrains.register(modBus);
        Moments.register(modBus);
    }
}
