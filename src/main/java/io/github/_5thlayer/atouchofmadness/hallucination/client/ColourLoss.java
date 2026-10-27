// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.hallucination.client;

import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * The colour loss: a grey wash over the view, clear in the middle and thickest in the corners, which
 * takes the colour and the contrast out of what is under it. It is drawn at a strength from 0, nothing,
 * to 1, the deepest, and a reach from 0, the screen's edge alone, to 1, far in toward the middle, both of
 * which the caller sets, so a deeper Hallucination draws it stronger and nearer without a second wash.
 */
public final class ColourLoss {

    /**
     * White, its alpha rising from none in the middle to full toward the edges; the colour it is drawn in
     * tints it. Its inner half, stretched over the screen, is the wash at no reach, and more of it is
     * drawn as the reach grows, so the clear middle shrinks.
     */
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ATouchOfMadness.MOD_ID, "textures/misc/colour_loss.png");

    /** How much of the texture's width is drawn at no reach, and at the farthest. */
    private static final float NEAREST_SHOWN = 0.5F;
    private static final float FARTHEST_SHOWN = 0.9F;

    /** The grey the edge washes toward, and how far toward it the corners go at full strength. */
    private static final int GREY = 0x6E6E6E;
    private static final float DEEPEST = 0.8F;

    private ColourLoss() {
    }

    /** Draws the colour loss over the whole screen at this strength and reach, each from 0 to 1. */
    public static void draw(GuiGraphicsExtractor graphics, float strength, float reach) {
        float alpha = Mth.clamp(strength, 0, 1) * DEEPEST;
        if (alpha <= 0) {
            return;
        }
        float shown = Mth.lerp(Mth.clamp(reach, 0, 1), NEAREST_SHOWN, FARTHEST_SHOWN);
        ScreenTexture.draw(graphics, TEXTURE, shown, 0, 0, ARGB.color(ARGB.as8BitChannel(alpha), GREY));
    }
}
