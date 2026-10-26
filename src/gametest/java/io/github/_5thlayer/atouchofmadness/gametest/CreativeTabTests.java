// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.gametest;

import java.util.List;

import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import io.github._5thlayer.atouchofmadness.MadnessTab;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

/**
 * The Pillar has its own creative tab, "A Touch of Madness", and every item of the Pillar is in it,
 * in {@code MadnessTab}'s order, and in no other tab. Until the Pillar has an item there is nothing
 * to order, so it checks that it has none: the first item fails that test until it is listed here.
 */
final class CreativeTabTests {

    /** The paths of the Pillar's items, as its tab shows them. */
    private static final List<String> TAB_ORDER = List.of();

    private CreativeTabTests() {
    }

    static void register(ATouchOfMadnessGameTests.Registrar tests) {
        tests.test("creative_tab_is_a_touch_of_madness", 1, CreativeTabTests::titled);
        if (TAB_ORDER.isEmpty()) {
            tests.test("creative_tab_registers_no_item_until_the_tab_lists_one", 1, CreativeTabTests::noItem);
            return;
        }
        tests.test("creative_tab_holds_every_item_in_order", 1, CreativeTabTests::inOrder);
        tests.test("creative_tab_no_other_tab_holds_a_pillar_item", 1, CreativeTabTests::noOtherTab);
    }

    private static void titled(GameTestHelper helper) {
        var tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(MadnessTab.KEY);
        if (tab == null) {
            helper.fail("no creative tab is registered at " + MadnessTab.KEY.identifier());
            return;
        }
        // The server has no client language, but NeoForge loads every mod's en_us.json into it.
        String title = Language.getInstance().getOrDefault(MadnessTab.TITLE_KEY);
        if (!title.equals("A Touch of Madness")) {
            helper.fail("the tab's title " + MadnessTab.TITLE_KEY + " reads \"" + title + "\"");
            return;
        }
        helper.succeed();
    }

    private static void noItem(GameTestHelper helper) {
        var items = BuiltInRegistries.ITEM.keySet().stream()
                .filter(id -> id.getNamespace().equals(ATouchOfMadness.MOD_ID))
                .map(Identifier::getPath)
                .toList();
        if (!items.isEmpty()) {
            helper.fail("the Pillar registers " + items + ": list them in CreativeTabTests' TAB_ORDER");
            return;
        }
        helper.succeed();
    }

    private static void inOrder(GameTestHelper helper) {
        rebuild(helper);
        var shown = pathsIn(BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(MadnessTab.KEY));
        if (!shown.equals(TAB_ORDER)) {
            helper.fail("the tab shows " + shown + ", not " + TAB_ORDER);
            return;
        }
        helper.succeed();
    }

    private static void noOtherTab(GameTestHelper helper) {
        rebuild(helper);
        for (var entry : BuiltInRegistries.CREATIVE_MODE_TAB.entrySet()) {
            // The search tab lists every other tab's items.
            if (entry.getKey().equals(MadnessTab.KEY) || entry.getValue().getType() == CreativeModeTab.Type.SEARCH) {
                continue;
            }
            var shown = pathsIn(entry.getValue());
            if (!shown.isEmpty()) {
                helper.fail("the tab " + entry.getKey().identifier() + " shows " + shown);
                return;
            }
        }
        helper.succeed();
    }

    private static void rebuild(GameTestHelper helper) {
        CreativeModeTabs.tryRebuildTabContents(helper.getLevel().enabledFeatures(), true, helper.getLevel().registryAccess());
    }

    private static List<String> pathsIn(CreativeModeTab tab) {
        return tab.getDisplayItems().stream()
                .map(ItemStack::getItem)
                .map(BuiltInRegistries.ITEM::getKey)
                .filter(id -> id.getNamespace().equals(ATouchOfMadness.MOD_ID))
                .map(Identifier::getPath)
                .toList();
    }
}
