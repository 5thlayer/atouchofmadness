// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.shadow;

import java.util.EnumSet;
import java.util.UUID;
import java.util.function.Function;

import io.github._5thlayer.atouchofmadness.sanity.Cause;
import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import io.github._5thlayer.atouchofmadness.sanity.Stage;
import net.minecraft.core.Holder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A Shadow Creature (ADR 0002): one server mob with one Owner, sent only to its Owner and to Insane
 * players, and only they can hit it. It drifts through blocks as a vex does, with no gravity, straight
 * toward where it is going at its drift speed, and lands a hit only while its body is clear of solid
 * blocks. While the Owner is Uneasy it is Unreal: it stalks them at the edge of sight, takes and deals
 * no harm, collides with nothing, and fades once they come within the fade distance or strike it.
 * While they are Insane it is Real: it hunts them, coming out beside them where a block is in its way,
 * can be killed, and each of its hits takes the strike from their Sanity, while a player's kill of it
 * restores theirs. It is never saved, and vanishes as soon as its Owner is gone, dead, in another
 * dimension, asleep or out of Survival and Adventure; the spawner removes it when the Owner's cap drops
 * below their count. It never stops its Owner sleeping, being no {@code Monster}. It is drawn as its
 * Shape, which it takes as it appears and which changes nothing but its look.
 */
public abstract class ShadowCreature extends PathfinderMob {

    /** How many ticks a creature takes to fade before it is gone. */
    public static final int FADE_TICKS = 20;

    /** Unreal creatures stalk their Owner from no nearer than this, in blocks, and close in from beyond it. */
    private static final double STALK_DISTANCE = 12;

    /** How many ticks apart a Real creature's hits come. */
    private static final int ATTACK_TICKS = 20;

    /** How far into a block a creature's body may reach and still count as clear of it, as it grazes a floor. */
    private static final double GRAZE = 0.1;

    /** How many places around its target a creature stuck in a block tries, to come out beside them. */
    private static final int BESIDE_STEPS = 8;

    private static final EntityDataAccessor<Boolean> DATA_REAL = SynchedEntityData.defineId(ShadowCreature.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_FADING = SynchedEntityData.defineId(ShadowCreature.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<EntityType<?>> DATA_SHAPE = SynchedEntityData.defineId(ShadowCreature.class, Shapes.SERIALIZER);

    private @Nullable UUID owner;
    private int fadeTicks;
    /** How far it drifts a tick, in blocks. */
    private final double drift;
    /** The Sanity its kill restores to the player who makes it. */
    private final double kill;
    private final DriftControl driftControl = new DriftControl();

    protected ShadowCreature(EntityType<? extends ShadowCreature> type, Level level, Function<ShadowRules, ShadowRules.Stats> stats) {
        super(type, level);
        var own = stats.apply(ShadowCreatures.rules());
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(own.health());
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(own.damage());
        setHealth(getMaxHealth());
        drift = own.drift() / 20;
        kill = own.kill();
        moveControl = driftControl;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_REAL, false);
        entityData.define(DATA_FADING, false);
        entityData.define(DATA_SHAPE, fallbackShape());
    }

    /** The tag its kind's Shapes come from. */
    protected abstract TagKey<EntityType<?>> shapes();

    /** The Shape its kind takes where its biome spawns none of its tag; called as it is made, so a constant. */
    protected abstract EntityType<?> fallbackShape();

    /** The mob it is drawn as. */
    public EntityType<?> shape() {
        return entityData.get(DATA_SHAPE);
    }

    /** Takes a Shape from the monsters of the biome it appears in, before it is added to the level. */
    public void takeShape(Holder<Biome> biome) {
        entityData.set(DATA_SHAPE, Shapes.in(biome, shapes()).getRandom(getRandom()).orElseGet(this::fallbackShape));
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new HuntGoal());
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
        // As a vex does: no block stops it, and none suffocates it.
        noPhysics = true;
        super.tick();
        noPhysics = false;
        setNoGravity(true);
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

    /**
     * Only a Real creature attacks, and only from clear of solid blocks; each hit it lands on a player
     * takes the strike from their Sanity.
     */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (isHarmless() || isInsideBlocks()) {
            return false;
        }
        boolean hurt = super.doHurtTarget(level, target);
        if (hurt && target instanceof ServerPlayer player) {
            PlayerSanity.add(player, -ShadowCreatures.rules().strike(), Cause.SHADOW_STRIKE);
        }
        return hurt;
    }

    /**
     * A kill while Real restores its kind's Sanity to the player behind the killing blow, their arrows
     * and all; a death with no player behind it, or one a listener cancels, restores nothing.
     */
    @Override
    public void die(DamageSource source) {
        boolean real = !level().isClientSide() && !dead && isReal();
        super.die(source);
        if (real && dead && source.getEntity() instanceof ServerPlayer player) {
            PlayerSanity.add(player, kill, Cause.SHADOW_KILL);
        }
    }

    /** Whether its body is in a solid block, more than grazing one. */
    public boolean isInsideBlocks() {
        return !level().noBlockCollision(this, getBoundingBox().deflate(GRAZE));
    }

    /** Moves by its own momentum alone, which its drift sets, through blocks and with no gravity. */
    @Override
    public void travel(Vec3 input) {
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(DriftControl.SLOWING));
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
                getMoveControl().setWantedPosition(player.getX(), player.getY(), player.getZ(), 1);
            } else {
                driftControl.stop();
            }
        }

        @Override
        public void stop() {
            driftControl.stop();
        }
    }

    /**
     * While Real, drifts straight at its Owner, its target, until in reach, and strikes whenever it can:
     * in reach but inside a block, it comes out beside them first.
     */
    private final class HuntGoal extends Goal {

        private int cooldown;

        HuntGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return getTarget() != null && getTarget().isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            setAggressive(true);
        }

        @Override
        public void tick() {
            var target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30, 30);
            // In reach it holds where it is clear, so it doesn't drift back into the block it came out of.
            if (!isWithinMeleeAttackRange(target)) {
                driftControl.setWantedPosition(target.getX(), target.getY(), target.getZ(), 1);
            } else if (isInsideBlocks()) {
                var clear = clearBeside(target);
                driftControl.setWantedPosition(clear.x, clear.y, clear.z, 1);
            } else {
                driftControl.stop();
                setDeltaMovement(Vec3.ZERO);
            }
            if (--cooldown <= 0 && isWithinMeleeAttackRange(target) && level() instanceof ServerLevel level && doHurtTarget(level, target)) {
                swing(InteractionHand.MAIN_HAND);
                cooldown = ATTACK_TICKS;
            }
        }

        @Override
        public void stop() {
            setAggressive(false);
            driftControl.stop();
        }

        /**
         * The nearest place beside the target, at its feet, a block above or below, where this creature's
         * body is clear of solid blocks; or the target's own place, if there is none.
         */
        private Vec3 clearBeside(Entity target) {
            double reach = (getBbWidth() + target.getBbWidth()) / 2;
            Vec3 nearest = target.position();
            double nearestDistance = Double.MAX_VALUE;
            for (int dy = -1; dy <= 1; dy++) {
                for (int step = 0; step < BESIDE_STEPS; step++) {
                    double angle = step * Mth.TWO_PI / BESIDE_STEPS;
                    var at = target.position().add(Math.cos(angle) * reach, dy, Math.sin(angle) * reach);
                    double distance = at.distanceToSqr(position());
                    if (distance < nearestDistance && level().noBlockCollision(ShadowCreature.this,
                            getDimensions(getPose()).makeBoundingBox(at).deflate(GRAZE))) {
                        nearest = at;
                        nearestDistance = distance;
                    }
                }
            }
            return nearest;
        }
    }

    /** Sets its momentum straight toward where it is wanted, at its drift, through whatever is between. */
    private final class DriftControl extends MoveControl {

        /** How much of its momentum it keeps from one tick to the next, once it is told to stop short of arriving. */
        static final double SLOWING = 0.6;

        /** Nearer than this, in blocks, it has arrived: well inside the graze, as its last step lands it on the spot. */
        private static final double ARRIVED = 0.01;

        DriftControl() {
            super(ShadowCreature.this);
        }

        void stop() {
            operation = Operation.WAIT;
        }

        @Override
        public void tick() {
            if (operation != Operation.MOVE_TO) {
                return;
            }
            var toward = new Vec3(wantedX - getX(), wantedY - getY(), wantedZ - getZ());
            double length = toward.length();
            if (length < ARRIVED) {
                operation = Operation.WAIT;
                setDeltaMovement(Vec3.ZERO);
                return;
            }
            setDeltaMovement(toward.scale(Math.min(drift * speedModifier, length) / length));
            setYRot((float) (Mth.atan2(toward.z, toward.x) * Mth.RAD_TO_DEG) - 90);
            yBodyRot = getYRot();
        }
    }
}
