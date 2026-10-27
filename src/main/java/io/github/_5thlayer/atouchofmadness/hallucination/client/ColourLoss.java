// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.hallucination.client;

import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * The screen edge's colour loss: a grey wash over the view, clear in the middle and thickest in the
 * corners, which takes the colour and the contrast out of what is under it. It is drawn at a strength
 * from 0, nothing, to 1, the deepest, which the caller sets, so a deeper Hallucination can draw it
 * stronger without a second wash beside it.
 */
public final class ColourLoss {

    /** White, its alpha rising from none in the middle to full in the corners; the colour it is drawn in tints it. */
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ATouchOfMadness.MOD_ID, "textures/misc/colour_loss.png");
    private static final int TEXTURE_SIZE = 256;

    /** The grey the edge washes toward, and how far toward it the corners go at full strength. */
    private static final int GREY = 0x6E6E6E;
    private static final float DEEPEST = 0.8F;

    private ColourLoss() {
    }

    /** Draws the colour loss over the whole screen at this strength, from 0 to 1. */
    public static void draw(GuiGraphicsExtractor graphics, float strength) {
        float alpha = Mth.clamp(strength, 0, 1) * DEEPEST;
        if (alpha <= 0) {
            return;
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, 0, 0, graphics.guiWidth(), graphics.guiHeight(),
                TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, ARGB.color(ARGB.as8BitChannel(alpha), GREY));
    }
}
