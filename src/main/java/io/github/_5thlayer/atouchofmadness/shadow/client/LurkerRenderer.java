// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.shadow.client;

import io.github._5thlayer.atouchofmadness.shadow.Lurker;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** The Lurker's silhouette, on the zombie's model. */
final class LurkerRenderer extends MobRenderer<Lurker, LurkerRenderer.State, HumanoidModel<LurkerRenderer.State>> {

    private static final Identifier TEXTURE = ShadowRenderers.texture("lurker");

    static final class State extends HumanoidRenderState {
        int tint = -1;
    }

    LurkerRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(Lurker entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTicks, itemModelResolver);
        state.tint = ShadowRenderers.tint(entity, partialTicks);
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return TEXTURE;
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
