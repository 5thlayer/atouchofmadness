// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.hallucination.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Draws the middle of a square texture stretched over the whole screen, the way the Hallucinations' overlays are drawn. */
final class ScreenTexture {

    /**
     * The units the texture's width is split into for the blit, finer than its pixels, since vanilla's
     * blit takes whole widths: only the ratios reach the GPU, so how much is drawn changes smoothly.
     */
    private static final int UNITS = 4096;

    private ScreenTexture() {
    }

    /**
     * Draws the part of the texture this fraction of its width and height across, centred on its middle
     * moved by the offsets, each a fraction of its width, over the screen, in this colour.
     */
    static void draw(GuiGraphicsExtractor graphics, Identifier texture, float shown, float offsetX, float offsetY, int colour) {
        int size = Math.round(UNITS * shown);
        float u = (UNITS - size) / 2F + UNITS * offsetX;
        float v = (UNITS - size) / 2F + UNITS * offsetY;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, u, v, graphics.guiWidth(), graphics.guiHeight(),
                size, size, UNITS, UNITS, colour);
    }
}
