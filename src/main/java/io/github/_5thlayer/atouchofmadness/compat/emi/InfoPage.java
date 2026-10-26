// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.compat.emi;

import java.util.List;
import java.util.function.Supplier;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

/**
 * An information page in EMI's own Info category, laid out as EMI's are: the thing it is about in one
 * slot, cycling through each stack it covers, and its text below. Unlike EMI's own, it draws its text
 * anew each frame, so a page changes as the player discovers what it covers, with nothing reloading
 * EMI. Its height is its text's when EMI lays it out, so text that grows while it is open may run past
 * its foot until it is opened again.
 */
final class InfoPage implements EmiRecipe {

    /** EMI's Info pages' width, and the space their text keeps from the edge. */
    private static final int WIDTH = 144;
    private static final int PADDING = 4;
    private static final int SLOT = 18;
    private static final int TEXT_COLOR = 0xFF000000;

    private final Identifier id;
    private final EmiIngredient stacks;
    private final Supplier<List<Component>> text;

    /** A page with this id, about these stacks, whose paragraphs {@code text} gives at each frame. */
    InfoPage(Identifier id, List<EmiStack> stacks, Supplier<List<Component>> text) {
        this.id = id;
        this.stacks = EmiIngredient.of(stacks);
        this.text = text;
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return VanillaEmiRecipeCategories.INFO;
    }

    @Override
    public Identifier getId() {
        return id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return List.of(stacks);
    }

    @Override
    public List<EmiStack> getOutputs() {
        return stacks.getEmiStacks();
    }

    @Override
    public boolean supportsRecipeTree() {
        return false;
    }

    @Override
    public int getDisplayWidth() {
        return WIDTH;
    }

    @Override
    public int getDisplayHeight() {
        return SLOT + PADDING + lines().size() * Minecraft.getInstance().font.lineHeight;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(stacks, (WIDTH - SLOT) / 2, 0);
        var font = Minecraft.getInstance().font;
        widgets.addDrawable(0, SLOT + PADDING, WIDTH, widgets.getHeight() - SLOT - PADDING, (graphics, mouseX, mouseY, delta) -> {
            var lines = lines();
            for (int i = 0; i < lines.size(); i++) {
                graphics.text(font, lines.get(i), 0, i * font.lineHeight, TEXT_COLOR, false);
            }
        });
    }

    /** The text as it stands now, wrapped to the page's width. */
    private List<FormattedCharSequence> lines() {
        var font = Minecraft.getInstance().font;
        return text.get().stream().flatMap(paragraph -> font.split(paragraph, WIDTH - PADDING).stream()).toList();
    }
}
