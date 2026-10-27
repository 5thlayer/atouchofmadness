// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.shadow.client;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

/**
 * Passes on what a Shape's own renderer submits as a shadow of it: each model and model part drawn
 * translucent with its own texture, in the shadow's colour, so its outline and cut-outs stay its own.
 * Everything else, its items, blocks, name, flames, shadow and leash among them, is left out, as a
 * silhouette has none; the creature's own renderer draws its name and shadow.
 */
final class ShadowSubmits implements SubmitNodeCollector {

    /** The sampler a render type binds its texture to. */
    private static final String TEXTURE_SAMPLER = "Sampler0";

    private final OrderedSubmitNodeCollector delegate;
    private final SubmitNodeCollector root;
    private final int tint;

    ShadowSubmits(SubmitNodeCollector root, int tint) {
        this(root, root, tint);
    }

    private ShadowSubmits(OrderedSubmitNodeCollector delegate, SubmitNodeCollector root, int tint) {
        this.delegate = delegate;
        this.root = root;
        this.tint = tint;
    }

    /** The render type that draws as a shadow what this one draws, or null if it draws no texture. */
    private static @Nullable RenderType shadowOf(RenderType renderType) {
        var texture = renderType.state.textures.get(TEXTURE_SAMPLER);
        return texture == null ? null : RenderTypes.entityTranslucent(texture.location());
    }

    @Override
    public OrderedSubmitNodeCollector order(int order) {
        return new ShadowSubmits(root.order(order), root, tint);
    }

    @Override
    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords,
            int tintedColor, @Nullable TextureAtlasSprite sprite, int outlineColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
        var shadow = shadowOf(renderType);
        if (shadow != null) {
            delegate.submitModel(model, state, poseStack, shadow, lightCoords, overlayCoords, tint, sprite, outlineColor, null);
        }
    }

    @Override
    public void submitModelPart(ModelPart modelPart, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords,
            @Nullable TextureAtlasSprite sprite, boolean sheeted, boolean hasFoil, int tintedColor,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay, int outlineColor) {
        var shadow = shadowOf(renderType);
        if (shadow != null) {
            delegate.submitModelPart(modelPart, poseStack, shadow, lightCoords, overlayCoords, sprite, false, false, tint, null, outlineColor);
        }
    }

    @Override
    public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {
    }

    @Override
    public void submitNameTag(PoseStack poseStack, @Nullable Vec3 nameTagAttachment, int offset, Component name, boolean seeThrough, int lightCoords,
            double distanceToCameraSq, CameraRenderState camera) {
    }

    @Override
    public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence string, boolean dropShadow, Font.DisplayMode displayMode,
            int lightCoords, int color, int backgroundColor, int outlineColor) {
    }

    @Override
    public void submitFlame(PoseStack poseStack, EntityRenderState renderState, Quaternionf rotation) {
    }

    @Override
    public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
    }

    @Override
    public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState) {
    }

    @Override
    public void submitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts, int[] tintLayers, int lightCoords,
            int overlayCoords, int outlineColor) {
    }

    @Override
    public void submitBreakingBlockModel(PoseStack poseStack, BlockStateModel model, long seed, int progress) {
    }

    @Override
    public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor,
            int[] tintLayers, List<BakedQuad> quads, ItemStackRenderState.FoilType foilType) {
    }

    @Override
    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, CustomGeometryRenderer customGeometryRenderer) {
    }

    @Override
    public void submitParticleGroup(ParticleGroupRenderer particleGroupRenderer) {
    }
}
