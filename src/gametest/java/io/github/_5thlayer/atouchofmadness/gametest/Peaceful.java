// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.gametest;

import com.mojang.serialization.MapCodec;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;

/** A test environment whose batch runs on Peaceful, the server's difficulty put back after it. */
record Peaceful() implements TestEnvironmentDefinition<Difficulty> {

    static final MapCodec<Peaceful> CODEC = MapCodec.unit(new Peaceful());

    @Override
    public Difficulty setup(ServerLevel level) {
        var before = level.getDifficulty();
        level.getServer().setDifficulty(Difficulty.PEACEFUL, true);
        return before;
    }

    @Override
    public void teardown(ServerLevel level, Difficulty before) {
        level.getServer().setDifficulty(before, true);
    }

    @Override
    public MapCodec<Peaceful> codec() {
        return CODEC;
    }
}
