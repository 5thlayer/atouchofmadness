// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Pillar's own creative tab, "A Touch of Madness", which holds every item of the Pillar and is
 * the only tab that does. A Pillar shows its items in a tab of its own, not FactoryWorks' Works tab
 * (suite ADR-0002). Its items are listed in {@link #displayItems}, in the order the tab shows them,
 * which {@code CreativeTabTests} lists too.
 */
public final class MadnessTab {

    public static final ResourceKey<CreativeModeTab> KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(ATouchOfMadness.MOD_ID, "a_touch_of_madness"));

    /** The tab's title key, which en_us.json translates to "A Touch of Madness". */
    public static final String TITLE_KEY = "itemGroup." + ATouchOfMadness.MOD_ID;

    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ATouchOfMadness.MOD_ID);

    static {
        // A vanilla item until the Pillar has one of its own. An empty tab is hidden from the creative screen.
        TABS.register(KEY.identifier().getPath(), () -> CreativeModeTab.builder()
                .title(Component.translatable(TITLE_KEY))
                .icon(() -> new ItemStack(Items.ENDER_EYE))
                .displayItems(MadnessTab::displayItems)
                .build());
    }

    private MadnessTab() {
    }

    static void register(IEventBus modBus) {
        TABS.register(modBus);
    }

    private static void displayItems(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        // output.accept(MyItems.FIRST.get()), one line per item, in order.
    }
}
