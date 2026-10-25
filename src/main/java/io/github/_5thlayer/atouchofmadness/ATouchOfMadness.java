// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * The Module's entry point. It fills its part of the suite's Works tab. The game tests hook
 * themselves in from the {@code gametest} source set, which never ships (libworks ADR 0011).
 */
@Mod(ATouchOfMadness.MOD_ID)
public final class ATouchOfMadness {

    /** The mod id, which gradle.properties' {@code mod_id} must match. */
    public static final String MOD_ID = "atouchofmadness";

    public ATouchOfMadness(IEventBus modBus) {
        modBus.addListener(WorksTabContents::add);
    }
}
