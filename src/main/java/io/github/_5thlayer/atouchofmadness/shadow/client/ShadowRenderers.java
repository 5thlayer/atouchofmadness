// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.shadow.client;

import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import io.github._5thlayer.atouchofmadness.shadow.ShadowCreature;
import io.github._5thlayer.atouchofmadness.shadow.ShadowCreatures;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Draws the Shadow Creatures as plain shadow silhouettes until Shapes land (#11): the Lurker on the
 * zombie's model, the Gaunt on the vex's, each in a near-black texture of its own. An Unreal one is
 * drawn fainter than a Real one, and a fading one fainter still, until it is gone.
 */
@EventBusSubscriber(modid = ATouchOfMadness.MOD_ID, value = Dist.CLIENT)
public final class ShadowRenderers {

    private static final float REAL_ALPHA = 0.9F;
    private static final float UNREAL_ALPHA = 0.55F;

    private ShadowRenderers() {
    }

    @SubscribeEvent
    static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ShadowCreatures.LURKER.get(), LurkerRenderer::new);
        event.registerEntityRenderer(ShadowCreatures.GAUNT.get(), GauntRenderer::new);
    }

    static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(ATouchOfMadness.MOD_ID, "textures/entity/shadow/" + name + ".png");
    }

    /** The colour a creature's silhouette is drawn in, white but for its alpha. */
    static int tint(ShadowCreature creature, float partialTick) {
        float alpha = (creature.isReal() ? REAL_ALPHA : UNREAL_ALPHA) * (1 - creature.fadeProgress(partialTick));
        return ARGB.white(alpha);
    }
}
