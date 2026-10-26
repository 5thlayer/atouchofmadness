// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.shadow;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The Gaunt, the Shadow Creature that comes only when Sanity is nearly gone: fast, fragile,
 * hard-hitting and flying, so pillars and walls don't stop it. Its speed is its flying speed, and it
 * walks at half that.
 */
public final class Gaunt extends ShadowCreature {

    public Gaunt(EntityType<? extends Gaunt> type, Level level) {
        super(type, level, ShadowRules::gaunt);
        double speed = ShadowCreatures.rules().gaunt().speed();
        getAttribute(Attributes.FLYING_SPEED).setBaseValue(speed);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed / 2);
        moveControl = new FlyingMoveControl(this, 20, true);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        var navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    public void travel(Vec3 input) {
        travelFlying(input, getSpeed());
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
    }
}
