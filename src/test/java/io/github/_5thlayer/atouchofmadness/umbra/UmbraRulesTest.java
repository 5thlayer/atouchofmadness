// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.umbra;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * By default the Umbra Sword and the Umbra Pickaxe each drain 2/min while held, and each piece of Umbra
 * Armour 1/min while worn; the pieces in use sum.
 */
class UmbraRulesTest {

    private static final UmbraRules RULES = UmbraRules.DEFAULT;

    @Test
    void theDefaultsAreTwoForEachToolAndOneForEachPiece() {
        assertEquals(2, RULES.sword());
        assertEquals(2, RULES.pickaxe());
        assertEquals(1, RULES.armour());
    }

    @Test
    void nothingInUseDrainsNothing() {
        assertEquals(0, RULES.drain(0, 0, 0));
    }

    @Test
    void eachPieceInUseAdds() {
        assertEquals(2, RULES.drain(1, 0, 0));
        assertEquals(2, RULES.drain(0, 1, 0));
        assertEquals(3, RULES.drain(0, 0, 3));
        assertEquals(8, RULES.drain(1, 1, 4));
    }

    @Test
    void aToolInEachHandDrainsTwice() {
        assertEquals(4, RULES.drain(2, 0, 0));
    }

    @Test
    void theDrainFollowsTheRules() {
        assertEquals(0.5 * 1 + 3 * 1 + 0.25 * 2, new UmbraRules(0.5, 3, 0.25).drain(1, 1, 2));
    }
}
