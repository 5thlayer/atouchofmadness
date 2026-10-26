// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.shadow;

import java.util.EnumSet;
import java.util.UUID;
import java.util.function.Function;

import io.github._5thlayer.atouchofmadness.sanity.Cause;
import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import io.github._5thlayer.atouchofmadness.sanity.Stage;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * A Shadow Creature (ADR 0002): one server mob with one Owner, sent only to its Owner and to Insane
 * players, and only they can hit it. While the Owner is Uneasy it is Unreal: it stalks them at the edge
 * of sight, takes and deals no harm, collides with nothing, and fades once they come within the fade
 * distance or strike it. While they are Insane it is Real: it hunts them, can be killed, and each of its
 * hits takes the strike from their Sanity. It is never saved, and vanishes as soon as its Owner is
 * gone, dead, in another dimension, asleep or out of Survival and Adventure; the spawner removes it when
 * the Owner's cap drops below their count. It never stops its Owner sleeping, being no {@code Monster}.
 */
public abstract class ShadowCreature extends PathfinderMob {

    /** How many ticks a creature takes to fade before it is gone. */
    public static final int FADE_TICKS = 20;

    /** Unreal creatures stalk their Owner from no nearer than this, in blocks, and close in from beyond it. */
    private static final double STALK_DISTANCE = 12;

    private static final EntityDataAccessor<Boolean> DATA_REAL = SynchedEntityData.defineId(ShadowCreature.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_FADING = SynchedEntityData.defineId(ShadowCreature.class, EntityDataSerializers.BOOLEAN);

    private @Nullable UUID owner;
    private int fadeTicks;

    protected ShadowCreature(EntityType<? extends ShadowCreature> type, Level level, Function<ShadowRules, ShadowRules.Stats> stats) {
        super(type, level);
        var own = stats.apply(ShadowCreatures.rules());
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(own.health());
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(own.damage());
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(own.speed());
        setHealth(getMaxHealth());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_REAL, false);
        entityData.define(DATA_FADING, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1, true));
        goalSelector.addGoal(2, new StalkGoal());
    }

    /** Gives the creature its Owner, before it is added to the level. */
    public void setOwner(ServerPlayer player) {
        owner = player.getUUID();
        entityData.set(DATA_REAL, isRealFor(player));
    }

    public @Nullable UUID ownerId() {
        return owner;
    }

    public boolean isOwnedBy(ServerPlayer player) {
        return player.getUUID().equals(owner);
    }

    /** The Owner, if logged in. */
    public @Nullable ServerPlayer owner() {
        return owner == null || !(level() instanceof ServerLevel level) ? null : level.getServer().getPlayerList().getPlayer(owner);
    }

    /** Whether it is Real: on the server, whether its Owner is Insane now; on a client, as the server last said. */
    public boolean isReal() {
        if (level().isClientSide()) {
            return entityData.get(DATA_REAL);
        }
        var player = owner();
        return player != null && isRealFor(player);
    }

    /** Whether a creature is Real to this player: whether they are Insane. */
    private static boolean isRealFor(ServerPlayer player) {
        return PlayerSanity.stage(player) == Stage.INSANE;
    }

    /** Whether it can neither harm nor be harmed now: Unreal, or fading. */
    private boolean isHarmless() {
        return !isReal() || isFading();
    }

    public boolean isFading() {
        return entityData.get(DATA_FADING);
    }

    /** How far through its fade it is, 0 before it starts and 1 as it goes, for a client to draw. */
    public float fadeProgress(float partialTick) {
        return isFading() ? Math.min(1, (fadeTicks + partialTick) / FADE_TICKS) : 0;
    }

    /** Starts it fading, if Unreal; it is gone once faded. */
    public void fade() {
        if (!isReal()) {
            entityData.set(DATA_FADING, true);
        }
    }

    @Override
    public boolean broadcastToPlayer(ServerPlayer player) {
        return canBeSeenBy(player);
    }

    /** Its Owner and any Insane player see it, and only they can hit it. */
    public boolean canBeSeenBy(ServerPlayer player) {
        return isOwnedBy(player) || isRealFor(player);
    }

    @Override
    public void tick() {
        super.tick();
        if (isFading() && ++fadeTicks >= FADE_TICKS && !level().isClientSide()) {
            discard();
        }
        if (level().isClientSide() || isRemoved()) {
            return;
        }
        var player = owner();
        if (player == null || vanishesFrom(player)) {
            discard();
            return;
        }
        boolean real = isRealFor(player);
        entityData.set(DATA_REAL, real);
        if (real) {
            entityData.set(DATA_FADING, false);
            fadeTicks = 0;
        } else {
            double fadeDistance = ShadowCreatures.rules().fadeDistance();
            if (distanceToSqr(player) <= fadeDistance * fadeDistance) {
                fade();
            }
        }
        setTarget(real && !isFading() ? player : null);
    }

    private boolean vanishesFrom(ServerPlayer player) {
        return !player.isAlive() || player.level() != level() || player.isSleeping() || !player.gameMode.isSurvival();
    }

    /** An Unreal creature, or one fading, is struck by an entity only to fade, and harmed by nothing short of a kill. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(level, source, damage);
        }
        if (source.getEntity() instanceof ServerPlayer player && !canBeSeenBy(player)) {
            return false;
        }
        if (isHarmless()) {
            if (source.getEntity() != null) {
                fade();
            }
            return false;
        }
        return super.hurtServer(level, source, damage);
    }

    /** Only a Real creature attacks, and each hit it lands on a player takes the strike from their Sanity. */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (isHarmless()) {
            return false;
        }
        boolean hurt = super.doHurtTarget(level, target);
        if (hurt && target instanceof ServerPlayer player) {
            PlayerSanity.add(player, -ShadowCreatures.rules().strike(), Cause.SHADOW_STRIKE);
        }
        return hurt;
    }

    @Override
    public boolean isPushable() {
        return isReal() && super.isPushable();
    }

    @Override
    protected void pushEntities() {
        if (isReal()) {
            super.pushEntities();
        }
    }

    /** Never despawned by vanilla's rules, on Peaceful or for distance: it goes only as its Owner does. */
    @Override
    public void checkDespawn() {
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!level().isClientSide()) {
            ShadowCreatures.added(this);
        }
    }

    @Override
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
        if (!level().isClientSide()) {
            ShadowCreatures.removed(this);
        }
    }

    /** While Unreal, keeps its Owner in sight from the stalking distance, closing in from beyond it. */
    private final class StalkGoal extends Goal {

        StalkGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !isReal() && owner() != null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            var player = owner();
            if (player == null) {
                return;
            }
            getLookControl().setLookAt(player, 30, 30);
            if (distanceToSqr(player) > STALK_DISTANCE * STALK_DISTANCE) {
                if (getNavigation().isDone()) {
                    getNavigation().moveTo(player, 1);
                }
            } else {
                getNavigation().stop();
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }
}
