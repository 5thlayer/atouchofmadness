// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.hallucination.client;

import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * The Encroachment: dark shapes creeping in from the edges of the view, longest toward the corners. It is
 * drawn at a depth from 0, nothing, to 1, the nearest and darkest, about a third of the way in; at a
 * little, only the corners' shapes show. It writhes slowly, as much as the caller's motion lets it.
 */
public final class Encroachment {

    /** Black shapes on clear, rooted at the edges; drawn whole, they come about a third of the way in. */
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ATouchOfMadness.MOD_ID, "textures/misc/encroachment.png");

    /** How much of the texture's width is drawn at no depth, which leaves only the corners' shapes on the screen, and at the deepest. */
    private static final float SHALLOWEST_SHOWN = 0.7F;
    private static final float DEEPEST_SHOWN = 1 - Encroachment.BREATH;

    /** How dark the shapes are at the faintest, and at the deepest. */
    private static final float FAINTEST = 0.2F;
    private static final float DARKEST = 1F;

    /** How far, as fractions of the texture's width, the shapes breathe in and the texture drifts, and the ticks each takes to swing there and back. */
    private static final float BREATH = 0.03F;
    private static final float DRIFT = 0.006F;
    private static final float BREATH_TICKS = 140;
    private static final float DRIFT_X_TICKS = 230;
    private static final float DRIFT_Y_TICKS = 310;

    private Encroachment() {
    }

    /**
     * Draws the Encroachment over the whole screen at this depth, from 0 to 1, at this time in ticks, and
     * moving this much, from 0, still, to 1.
     */
    public static void draw(GuiGraphicsExtractor graphics, float depth, float time, float motion) {
        float clamped = Mth.clamp(depth, 0, 1);
        if (clamped <= 0) {
            return;
        }
        // Breathing swings either side of the depth's reach, and at the deepest stays inside the texture.
        float breath = motion * BREATH * Mth.sin(time * Mth.TWO_PI / BREATH_TICKS);
        float shown = Mth.lerp(clamped, SHALLOWEST_SHOWN, DEEPEST_SHOWN) + breath;
        float driftX = motion * DRIFT * Mth.sin(time * Mth.TWO_PI / DRIFT_X_TICKS);
        float driftY = motion * DRIFT * Mth.sin(time * Mth.TWO_PI / DRIFT_Y_TICKS);
        float alpha = Mth.lerp(clamped, FAINTEST, DARKEST);
        ScreenTexture.draw(graphics, TEXTURE, shown, driftX, driftY, ARGB.white(alpha));
    }
}
