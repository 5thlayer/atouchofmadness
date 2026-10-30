// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.environment;

import java.util.function.Supplier;

import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import io.github._5thlayer.atouchofmadness.MadnessConfig;
import io.github._5thlayer.atouchofmadness.sanity.Cause;
import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import io.github._5thlayer.atouchofmadness.sanity.Rate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.dimension.DimensionType;
import net.neoforged.bus.api.IEventBus;

/**
 * The continuous drains of where a player stands, each read from vanilla state (suite ADR-0002):
 * Low light at the eye block, Freezing while {@code getTicksFrozen() > 0}, Wet while
 * {@code isInWaterOrRain()}, and a dimension type in {@link #UNSETTLING}. A Touch of Darkness's carried
 * light and A Touch of Cold's freezing are vanilla state too, so they count with no code of their own.
 */
public final class EnvironmentDrains {

    /** The dimension types that drain Sanity, the Nether by default. */
    public static final TagKey<DimensionType> UNSETTLING = TagKey.create(Registries.DIMENSION_TYPE,
            Identifier.fromNamespaceAndPath(ATouchOfMadness.MOD_ID, "unsettling"));

    /** The config's rules. */
    private static final Supplier<EnvironmentRules> RULES = MadnessConfig.live(MadnessConfig::environmentRules);

    private EnvironmentDrains() {
    }

    public static void register(IEventBus modBus) {
        PlayerSanity.addSource(player -> drainIf(isInLowLight(player), RULES.get().lowLight(), Cause.LOW_LIGHT));
        PlayerSanity.addSource(player -> drainIf(player.getTicksFrozen() > 0, RULES.get().freezing(), Cause.FREEZING));
        PlayerSanity.addSource(player -> drainIf(player.isInWaterOrRain(), RULES.get().wet(), Cause.WET));
        PlayerSanity.addSource(player -> drainIf(player.level().dimensionTypeRegistration().is(UNSETTLING), RULES.get().unsettling(), Cause.UNSETTLING_DIMENSION));
    }

    /**
     * Whether the max local raw brightness at the eye block is Low light, a full moon counting as
     * overhead only in a dimension with a moon, and only when the eye block can see the sky (ADR 0003).
     */
    private static boolean isInLowLight(ServerPlayer player) {
        var level = player.level();
        var eye = BlockPos.containing(player.getEyePosition());
        boolean fullMoonOverhead = hasMoon(level.dimensionType()) && level.canSeeSky(eye)
                && level.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, eye) == MoonPhase.FULL_MOON;
        return RULES.get().isLowLight(level.getMaxLocalRawBrightness(eye), fullMoonOverhead);
    }

    /**
     * Whether one of the dimension type's timelines moves the moon through its phases, as the
     * Overworld's does. Elsewhere vanilla reads every hour as a full moon, which is no moon at all.
     */
    public static boolean hasMoon(DimensionType type) {
        return type.timelines().stream().anyMatch(timeline -> timeline.value().attributes().contains(EnvironmentAttributes.MOON_PHASE));
    }

    private static Rate drainIf(boolean applies, double perMinute, Cause cause) {
        return applies ? Rate.drain(perMinute, cause) : Rate.NONE;
    }
}
