// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.shadow;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * The Gaunt, the Shadow Creature that comes only when Sanity is nearly gone: fast, fragile and
 * hard-hitting, drifting through blocks as every Shadow Creature does.
 */
public final class Gaunt extends ShadowCreature {

    public Gaunt(EntityType<? extends Gaunt> type, Level level) {
        super(type, level, ShadowRules::gaunt);
    }
}
