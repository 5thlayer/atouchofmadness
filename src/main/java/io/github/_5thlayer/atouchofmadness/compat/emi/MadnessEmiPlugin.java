// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.compat.emi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import io.github._5thlayer.atouchofmadness.crown.FlowerCrown;
import io.github._5thlayer.atouchofmadness.moments.Moments;
import io.github._5thlayer.atouchofmadness.sanity.Cause;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/**
 * What the Pillar keeps out of sight, as Info pages in EMI (#19): the Flower Crown, each food with a
 * Sanity Modifier, and music discs. Each page is always listed, but reads "Not yet discovered" until
 * the client is told its player has discovered what it covers, which it checks each frame, so nothing
 * reloads EMI; with discovery off, the client is told everything is. The Flower Crown's recipe is
 * vanilla crafting, which EMI shows unaided. No page names a number the common config sets, which a
 * client's own config needn't match.
 *
 * <p>EMI finds this by its annotation and loads it only when EMI is installed; nothing outside this
 * package names an EMI type, so the Pillar plays without it.
 */
@EmiEntrypoint
public final class MadnessEmiPlugin implements EmiPlugin {

    private static final Component UNDISCOVERED = Component.translatable("atouchofmadness.emi.undiscovered");

    @Override
    public void register(EmiRegistry registry) {
        registry.addRecipe(new InfoPage(page("flower_crown"), List.of(EmiStack.of(FlowerCrown.ITEM.get())),
                gated(Cause.FLOWER_CROWN, () -> List.of(Component.translatable("atouchofmadness.emi.flower_crown")))));
        // The foods the server's data map lists, as it sent them, a datapack's overrides included.
        BuiltInRegistries.ITEM.getDataMap(Moments.SANITY_MODIFIER).entrySet().stream()
                .filter(entry -> entry.getValue() != 0)
                .map(entry -> BuiltInRegistries.ITEM.getValue(entry.getKey()))
                .sorted(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item)))
                .forEach(food -> registry.addRecipe(foodPage(food)));
        var discs = BuiltInRegistries.ITEM.stream().filter(item -> item.components().has(DataComponents.JUKEBOX_PLAYABLE))
                .map(EmiStack::of).toList();
        registry.addRecipe(new InfoPage(page("music"), discs, known -> {
            if (!known.test(Cause.MUSIC)) {
                return List.of(UNDISCOVERED);
            }
            List<Component> text = new ArrayList<>();
            text.add(Component.translatable("atouchofmadness.emi.music"));
            if (known.test(Cause.STALE_DISC)) {
                text.add(Component.translatable("atouchofmadness.emi.music.stale_disc"));
            }
            return text;
        }));
    }

    /** A food's page: the Sanity eating it moves, as the data map holds it at each frame. */
    private static InfoPage foodPage(Item food) {
        var id = BuiltInRegistries.ITEM.getKey(food);
        return new InfoPage(page("food/" + id.getNamespace() + "/" + id.getPath()), List.of(EmiStack.of(food)), gated(Cause.FOOD, () -> {
            int modifier = Moments.modifier(BuiltInRegistries.ITEM.wrapAsHolder(food));
            if (modifier == 0) {
                return List.of(Component.translatable("atouchofmadness.emi.food.none"));
            }
            return List.of(modifier > 0
                    ? Component.translatable("atouchofmadness.emi.food.restores", modifier)
                    : Component.translatable("atouchofmadness.emi.food.drains", -modifier));
        }));
    }

    /** The text, once the cause is known, and "Not yet discovered" until then. */
    private static Function<Predicate<Cause>, List<Component>> gated(Cause cause, Supplier<List<Component>> text) {
        return known -> known.test(cause) ? text.get() : List.of(UNDISCOVERED);
    }

    /** A page's id, synthetic as EMI asks of a recipe no recipe manager holds. */
    private static Identifier page(String path) {
        return Identifier.fromNamespaceAndPath(ATouchOfMadness.MOD_ID, "/info/" + path);
    }
}
