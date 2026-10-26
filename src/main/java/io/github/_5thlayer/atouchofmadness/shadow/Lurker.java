// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.shadow;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** The Lurker, the Shadow Creature of every low stage: slow, tough and on the ground, so walls stop it. */
public final class Lurker extends ShadowCreature {

    public Lurker(EntityType<? extends Lurker> type, Level level) {
        super(type, level, ShadowRules::lurker);
    }
}
