// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness;

import io.github._5thlayer.atouchofmadness.environment.EnvironmentRules;
import io.github._5thlayer.atouchofmadness.sanity.Sanity;
import io.github._5thlayer.atouchofmadness.sanity.SanityRules;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The common config, {@code atouchofmadness-common.toml}: where Sanity starts and where its stages
 * begin, and the drains of where a player stands.
 */
public final class MadnessConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.comment("A player's Sanity, from 0 to 100.").push("sanity");
    }

    private static final ModConfigSpec.DoubleValue START = BUILDER
            .comment("The Sanity a new player starts with.")
            .defineInRange("start", SanityRules.DEFAULT.start().value(), Sanity.MIN, Sanity.MAX);

    private static final ModConfigSpec.DoubleValue RESPAWN = BUILDER
            .comment("The Sanity a player respawns with after dying.")
            .defineInRange("respawn", SanityRules.DEFAULT.respawn().value(), Sanity.MIN, Sanity.MAX);

    private static final ModConfigSpec.DoubleValue SANE_ABOVE = BUILDER
            .comment("A player is Sane above this Sanity, and Uneasy from it down to insaneBelow.")
            .defineInRange("saneAbove", SanityRules.DEFAULT.saneAbove(), Sanity.MIN, Sanity.MAX);

    private static final ModConfigSpec.DoubleValue INSANE_BELOW = BUILDER
            .comment("A player is Insane below this Sanity. Set above saneAbove, it is read as saneAbove.")
            .defineInRange("insaneBelow", SanityRules.DEFAULT.insaneBelow(), Sanity.MIN, Sanity.MAX);

    static {
        BUILDER.pop();
        BUILDER.comment("The drains of where a player stands: each in Sanity a minute, and the Low-light threshold.").push("environment");
    }

    private static final ModConfigSpec.IntValue LOW_LIGHT_BELOW = BUILDER
            .comment("Light at a player's eyes below this is Low light, unless a full moon is in the sky they can see.")
            .defineInRange("lowLightBelow", EnvironmentRules.DEFAULT.lowLightBelow(), 0, 16);

    private static final ModConfigSpec.DoubleValue LOW_LIGHT = BUILDER
            .comment("The drain while a player is in Low light.")
            .defineInRange("lowLight", EnvironmentRules.DEFAULT.lowLight(), 0, Sanity.MAX);

    private static final ModConfigSpec.DoubleValue FREEZING = BUILDER
            .comment("The drain while a player is freezing, in powder snow or from any other cold.")
            .defineInRange("freezing", EnvironmentRules.DEFAULT.freezing(), 0, Sanity.MAX);

    private static final ModConfigSpec.DoubleValue WET = BUILDER
            .comment("The drain while a player is in water or rain.")
            .defineInRange("wet", EnvironmentRules.DEFAULT.wet(), 0, Sanity.MAX);

    private static final ModConfigSpec.DoubleValue UNSETTLING = BUILDER
            .comment("The drain while a player is in a dimension type in #atouchofmadness:unsettling, the Nether by default.")
            .defineInRange("unsettling", EnvironmentRules.DEFAULT.unsettling(), 0, Sanity.MAX);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private MadnessConfig() {
    }

    /** The defaults until the config loads. */
    public static SanityRules sanityRules() {
        if (!SPEC.isLoaded()) {
            return SanityRules.DEFAULT;
        }
        double saneAbove = SANE_ABOVE.get();
        return new SanityRules(new Sanity(START.get()), new Sanity(RESPAWN.get()), saneAbove,
                Math.min(INSANE_BELOW.get(), saneAbove));
    }

    /** The defaults until the config loads. */
    public static EnvironmentRules environmentRules() {
        if (!SPEC.isLoaded()) {
            return EnvironmentRules.DEFAULT;
        }
        return new EnvironmentRules(LOW_LIGHT_BELOW.get(), LOW_LIGHT.get(), FREEZING.get(), WET.get(), UNSETTLING.get());
    }
}
