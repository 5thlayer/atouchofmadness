// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness;

import net.neoforged.neoforge.common.ModConfigSpec;

/** The client config, {@code atouchofmadness-client.toml}: switches for the Hallucinations a player may not want to see. */
public final class MadnessClientConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.comment("Hallucinations: harmless effects of low Sanity that only you see or hear.").push("hallucinations");
    }

    private static final ModConfigSpec.BooleanValue SWAY = BUILDER
            .comment("Whether the view sways slowly while Insane. Vanilla's Distortion Effects slider scales it too.")
            .define("sway", true);

    private static final ModConfigSpec.BooleanValue COLOUR_LOSS = BUILDER
            .comment("Whether the screen's edge loses its colour from Uneasy down, more deeply as Sanity falls, and reaches in toward the middle while Insane.")
            .define("colourLoss", true);

    private static final ModConfigSpec.BooleanValue SHAPES = BUILDER
            .comment("Whether dark shapes creep in from the screen's edges from Uneasy down, nearer as Sanity falls. Vanilla's Distortion Effects slider scales how much they move.")
            .define("shapes", true);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private MadnessClientConfig() {
    }

    /** On until the config loads. */
    public static boolean sway() {
        return !SPEC.isLoaded() || SWAY.get();
    }

    /** On until the config loads. */
    public static boolean colourLoss() {
        return !SPEC.isLoaded() || COLOUR_LOSS.get();
    }

    /** On until the config loads. */
    public static boolean shapes() {
        return !SPEC.isLoaded() || SHAPES.get();
    }
}
