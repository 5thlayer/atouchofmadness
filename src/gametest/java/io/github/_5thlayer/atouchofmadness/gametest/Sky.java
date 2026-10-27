// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.gametest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;

/**
 * A batch's time of day and weather, both in full from its first tick: the Overworld clock at
 * {@code time}, which picks the moon's phase too, and the weather with the rain and thunder already
 * at their height rather than easing in over five seconds. The clock is paused for the batch, so
 * however long the batch waits before its tests start, its sky is the one it set. Teardown puts all
 * back as it was, eased out at once too, so no rain lingers into the next batch.
 */
record Sky(int time, Weather.Type weather) implements TestEnvironmentDefinition<Sky.Before> {

    static final MapCodec<Sky> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("time").forGetter(Sky::time),
            Weather.Type.CODEC.fieldOf("weather").forGetter(Sky::weather)
    ).apply(instance, Sky::new));

    /** Noon, the first night of a cycle, which has a full moon, and the fifth, which has a new one. */
    static final int NOON = 6000;
    static final int FULL_MOON_MIDNIGHT = 18000;
    static final int NEW_MOON_MIDNIGHT = 4 * 24000 + 18000;

    record Before(long time, float rain, float thunder, Activation<?> weather) {
    }

    @Override
    public Before setup(ServerLevel level) {
        var clocks = level.getServer().clockManager();
        var before = new Before(clocks.getTotalTicks(overworldClock(level)), level.getRainLevel(1), level.getThunderLevel(1),
                TestEnvironmentDefinition.activate(new Weather(weather), level));
        clocks.setTotalTicks(overworldClock(level), time);
        clocks.setPaused(overworldClock(level), true);
        level.setRainLevel(weather == Weather.Type.CLEAR ? 0 : 1);
        level.setThunderLevel(weather == Weather.Type.THUNDER ? 1 : 0);
        level.updateSkyBrightness();
        return before;
    }

    @Override
    public void teardown(ServerLevel level, Before before) {
        var clocks = level.getServer().clockManager();
        clocks.setTotalTicks(overworldClock(level), before.time());
        clocks.setPaused(overworldClock(level), false);
        before.weather().teardown();
        level.setRainLevel(before.rain());
        level.setThunderLevel(before.thunder());
        level.updateSkyBrightness();
    }

    private static Holder<WorldClock> overworldClock(ServerLevel level) {
        return level.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD);
    }

    @Override
    public MapCodec<Sky> codec() {
        return CODEC;
    }
}
