// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness;

import io.github._5thlayer.libworks.WorksTab;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/**
 * Every item of the Module, in the suite's Works tab and no other (FactoryWorks ADR-0128). One
 * listener, in the order the tab shows them, which {@code CreativeTabTests} lists too.
 */
final class WorksTabContents {

    private WorksTabContents() {
    }

    static void add(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != WorksTab.KEY) {
            return;
        }
        // event.accept(MyItems.FIRST.get()), one line per item, in order.
    }
}
