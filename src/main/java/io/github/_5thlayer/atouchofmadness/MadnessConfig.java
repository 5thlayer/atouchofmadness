// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness;

import io.github._5thlayer.atouchofmadness.environment.EnvironmentRules;
import io.github._5thlayer.atouchofmadness.moments.MomentRules;
import io.github._5thlayer.atouchofmadness.music.MusicRules;
import io.github._5thlayer.atouchofmadness.nearby.NearbyRules;
import io.github._5thlayer.atouchofmadness.sanity.Sanity;
import io.github._5thlayer.atouchofmadness.sanity.SanityRules;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The common config, {@code atouchofmadness-common.toml}: where Sanity starts and where its stages
 * begin, the drains of where a player stands, the drain and restore of who is near, the restore of
 * music, and the restores of sleep and flowers.
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
        BUILDER.comment("The drain and restore of who is near a player: rates in Sanity a minute, radii in blocks.").push("nearby");
    }

    private static final ModConfigSpec.DoubleValue HOSTILE_RADIUS = BUILDER
            .comment("How near an entity in #atouchofmadness:unsettling must be to drain.")
            .defineInRange("hostileRadius", NearbyRules.DEFAULT.hostileRadius(), 0, 64);

    private static final ModConfigSpec.DoubleValue HOSTILE = BUILDER
            .comment("The drain of each such entity, times its atouchofmadness:unsettling_weight data map entry, 1 if it has none.")
            .defineInRange("hostile", NearbyRules.DEFAULT.hostile(), 0, Sanity.MAX);

    private static final ModConfigSpec.DoubleValue HOSTILE_CAP = BUILDER
            .comment("The most all such entities together drain.")
            .defineInRange("hostileCap", NearbyRules.DEFAULT.hostileCap(), 0, Sanity.MAX);

    private static final ModConfigSpec.DoubleValue COMPANY_RADIUS = BUILDER
            .comment("How near company must be to restore.")
            .defineInRange("companyRadius", NearbyRules.DEFAULT.companyRadius(), 0, 64);

    private static final ModConfigSpec.DoubleValue COMPANY = BUILDER
            .comment("The restore while any entity in #atouchofmadness:comforting, tamed if it can be, or any other Survival or Adventure player is near; once however many.")
            .defineInRange("company", NearbyRules.DEFAULT.company(), 0, Sanity.MAX);

    static {
        BUILDER.pop();
        BUILDER.comment("The restore of music: the rate in Sanity a minute, the range in blocks.").push("music");
    }

    private static final ModConfigSpec.DoubleValue MUSIC_RANGE = BUILDER
            .comment("How near a playing jukebox must be to restore. Only the nearest counts.")
            .defineInRange("range", MusicRules.DEFAULT.range(), 0, 256);

    private static final ModConfigSpec.DoubleValue FRESH = BUILDER
            .comment("The restore while that jukebox plays a Fresh disc, one not among the last two a player heard; each play of a Stale disc restores half its last play's.")
            .defineInRange("fresh", MusicRules.DEFAULT.fresh(), 0, Sanity.MAX);

    static {
        BUILDER.pop();
        BUILDER.comment("The instantaneous restores, each in Sanity. A food's is its atouchofmadness:sanity_modifier data map entry.").push("moments");
    }

    private static final ModConfigSpec.DoubleValue SLEEP = BUILDER
            .comment("The restore to each player who wakes from sleeping through the night.")
            .defineInRange("sleep", MomentRules.DEFAULT.sleep(), 0, Sanity.MAX);

    private static final ModConfigSpec.DoubleValue FLOWER = BUILDER
            .comment("The restore for each block in #minecraft:flowers a player breaks.")
            .defineInRange("flower", MomentRules.DEFAULT.flower(), 0, Sanity.MAX);

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

    /** The defaults until the config loads. */
    public static NearbyRules nearbyRules() {
        if (!SPEC.isLoaded()) {
            return NearbyRules.DEFAULT;
        }
        return new NearbyRules(HOSTILE_RADIUS.get(), HOSTILE.get(), HOSTILE_CAP.get(), COMPANY_RADIUS.get(), COMPANY.get());
    }

    /** The defaults until the config loads. */
    public static MusicRules musicRules() {
        if (!SPEC.isLoaded()) {
            return MusicRules.DEFAULT;
        }
        return new MusicRules(MUSIC_RANGE.get(), FRESH.get());
    }

    /** The defaults until the config loads. */
    public static MomentRules momentRules() {
        if (!SPEC.isLoaded()) {
            return MomentRules.DEFAULT;
        }
        return new MomentRules(SLEEP.get(), FLOWER.get());
    }
}
