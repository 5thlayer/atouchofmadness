// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.shadow.client;

import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import io.github._5thlayer.atouchofmadness.shadow.ShadowCreature;
import io.github._5thlayer.atouchofmadness.shadow.ShadowCreatures;
import net.minecraft.util.ARGB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Draws each Shadow Creature as a shadow of its Shape (#11): the Shape's own model and texture, near
 * black and see-through. An Unreal one is drawn fainter than a Real one, and a fading one fainter
 * still, until it is gone.
 */
@EventBusSubscriber(modid = ATouchOfMadness.MOD_ID, value = Dist.CLIENT)
public final class ShadowRenderers {

    private static final float REAL_ALPHA = 0.9F;
    private static final float UNREAL_ALPHA = 0.55F;

    /** The colour a Shape's texture is darkened by, a faintly violet black. */
    private static final int SHADOW_RGB = 0x16121E;

    /** The radius of the shadow under each, which its Shape leaves as it is. */
    private static final float LURKER_SHADOW = 0.5F;
    private static final float GAUNT_SHADOW = 0.6F;

    private ShadowRenderers() {
    }

    @SubscribeEvent
    static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ShadowCreatures.LURKER.get(), context -> new ShapeRenderer<>(context, LURKER_SHADOW));
        event.registerEntityRenderer(ShadowCreatures.GAUNT.get(), context -> new ShapeRenderer<>(context, GAUNT_SHADOW));
    }

    @SubscribeEvent
    static void unload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            ShapeRenderer.forget();
        }
    }

    /** The colour a creature's Shape is drawn in, which its texture is multiplied by. */
    static int tint(ShadowCreature creature, float partialTick) {
        float alpha = (creature.isReal() ? REAL_ALPHA : UNREAL_ALPHA) * (1 - creature.fadeProgress(partialTick));
        return ARGB.color(ARGB.as8BitChannel(alpha), SHADOW_RGB);
    }
}
