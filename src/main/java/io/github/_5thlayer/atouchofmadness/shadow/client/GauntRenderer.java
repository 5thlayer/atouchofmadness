// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.shadow.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github._5thlayer.atouchofmadness.shadow.Gaunt;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.vex.VexModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.VexRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** The Gaunt's silhouette, on the vex's winged model, drawn larger to fill its box. */
final class GauntRenderer extends MobRenderer<Gaunt, GauntRenderer.State, VexModel> {

    private static final Identifier TEXTURE = ShadowRenderers.texture("gaunt");
    /** The vex model stands about 0.8 tall: this fills the Gaunt's 1.4-high box, set in {@code ShadowCreatures}. */
    private static final float SCALE = 1.75F;

    static final class State extends VexRenderState {
        int tint = -1;
    }

    GauntRenderer(EntityRendererProvider.Context context) {
        super(context, new VexModel(context.bakeLayer(ModelLayers.VEX)), 0.6F);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(Gaunt entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        ArmedEntityRenderState.extractArmedEntityRenderState(entity, state, itemModelResolver, partialTicks);
        state.isCharging = entity.isAggressive();
        state.tint = ShadowRenderers.tint(entity, partialTicks);
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return TEXTURE;
    }

    @Override
    protected void scale(State state, PoseStack poseStack) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    protected int getModelTint(State state) {
        return state.tint;
    }

    @Override
    protected @Nullable RenderType getRenderType(State state, boolean isBodyVisible, boolean forceTransparent, boolean appearGlowing) {
        return isBodyVisible ? RenderTypes.entityTranslucent(TEXTURE) : super.getRenderType(state, isBodyVisible, forceTransparent, appearGlowing);
    }
}
