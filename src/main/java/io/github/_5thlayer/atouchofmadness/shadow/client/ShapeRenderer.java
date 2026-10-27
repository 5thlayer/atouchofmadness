// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.shadow.client;

import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github._5thlayer.atouchofmadness.shadow.ShadowCreature;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;

/**
 * Draws a Shadow Creature as its Shape: a stand-in of the Shape's mob, never added to the level, takes
 * the creature's place, turn and motion each frame, and the Shape's own renderer draws it as a shadow,
 * so any mob with a renderer, another mod's too, can serve. The creature's own box, name and shadow
 * stay its own, whatever its Shape's size.
 */
final class ShapeRenderer<T extends ShadowCreature> extends EntityRenderer<T, ShapeRenderer.State> {

    /**
     * Each creature's stand-in, made again as its Shape changes; a creature's goes with it. A stand-in
     * holds its level, which holds its creature, so they are all forgotten as a client level unloads.
     */
    private static final Map<ShadowCreature, Entity> STAND_INS = new WeakHashMap<>();

    static final class State extends EntityRenderState {
        @Nullable EntityRenderState shape;
        int tint;
    }

    ShapeRenderer(EntityRendererProvider.Context context, float shadow) {
        super(context);
        shadowRadius = shadow;
    }

    /** Forgets every stand-in, as the client's level unloads. */
    static void forget() {
        STAND_INS.clear();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T creature, State state, float partialTicks) {
        super.extractRenderState(creature, state, partialTicks);
        state.tint = ShadowRenderers.tint(creature, partialTicks);
        var standIn = standIn(creature);
        if (standIn == null) {
            state.shape = null;
            return;
        }
        follow(standIn, creature);
        var shape = entityRenderDispatcher.getRenderer(standIn).createRenderState(standIn, partialTicks);
        if (shape instanceof LivingEntityRenderState living) {
            living.walkAnimationPos = creature.walkAnimation.position(partialTicks);
            living.walkAnimationSpeed = creature.walkAnimation.speed(partialTicks);
        }
        shape.outlineColor = state.outlineColor;
        state.shape = shape;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (state.shape != null) {
            submitShape(state.shape, poseStack, new ShadowSubmits(submitNodeCollector, state.tint), camera);
        }
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    private <S extends EntityRenderState> void submitShape(S shape, PoseStack poseStack, SubmitNodeCollector shadow, CameraRenderState camera) {
        entityRenderDispatcher.getRenderer(shape).submit(shape, poseStack, shadow, camera);
    }

    /** The creature's stand-in for its Shape now, or null if its Shape can't be made on a client. */
    private @Nullable Entity standIn(T creature) {
        var shape = creature.shape();
        var standIn = STAND_INS.get(creature);
        if (standIn == null || standIn.getType() != shape) {
            standIn = shape.create(creature.level(), EntitySpawnReason.LOAD);
            if (standIn == null) {
                STAND_INS.remove(creature);
                return null;
            }
            STAND_INS.put(creature, standIn);
        }
        return standIn;
    }

    /** Puts the stand-in where the creature is, turned and moving as it does, as its Shape's renderer reads them. */
    private static void follow(Entity standIn, ShadowCreature creature) {
        standIn.setPos(creature.getX(), creature.getY(), creature.getZ());
        standIn.xo = creature.xo;
        standIn.yo = creature.yo;
        standIn.zo = creature.zo;
        standIn.xOld = creature.xOld;
        standIn.yOld = creature.yOld;
        standIn.zOld = creature.zOld;
        standIn.setYRot(creature.getYRot());
        standIn.setXRot(creature.getXRot());
        standIn.yRotO = creature.yRotO;
        standIn.xRotO = creature.xRotO;
        standIn.tickCount = creature.tickCount;
        standIn.setOnGround(creature.onGround());
        if (standIn instanceof LivingEntity living) {
            living.yBodyRot = creature.yBodyRot;
            living.yBodyRotO = creature.yBodyRotO;
            living.yHeadRot = creature.yHeadRot;
            living.yHeadRotO = creature.yHeadRotO;
            living.attackAnim = creature.attackAnim;
            living.oAttackAnim = creature.oAttackAnim;
            living.swinging = creature.swinging;
            living.hurtTime = creature.hurtTime;
            living.deathTime = creature.deathTime;
        }
        if (standIn instanceof Mob mob) {
            mob.setAggressive(creature.isAggressive());
        }
    }
}
