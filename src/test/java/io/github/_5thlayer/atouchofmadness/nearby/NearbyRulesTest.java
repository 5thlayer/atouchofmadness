// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.nearby;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * By default each unsettling entity within 8 blocks drains its weight × 1/min, up to 10/min in all, and
 * company within 8 blocks restores 2/min however many are near.
 */
class NearbyRulesTest {

    private static final NearbyRules RULES = NearbyRules.DEFAULT;

    @Test
    void noOneNearDrainsNothing() {
        assertEquals(0, RULES.hostileDrain(List.of()));
    }

    @Test
    void eachWeightDrainsItsOwn() {
        assertEquals(1, RULES.hostileDrain(List.of(1.0)));
        assertEquals(3, RULES.hostileDrain(List.of(3.0)));
        assertEquals(10, RULES.hostileDrain(List.of(10.0)));
    }

    @Test
    void theWeightsSum() {
        assertEquals(5, RULES.hostileDrain(List.of(1.0, 3.0, 1.0)));
    }

    @Test
    void theSumIsCappedAtTen() {
        assertEquals(10, RULES.hostileDrain(List.of(3.0, 3.0, 3.0, 3.0)));
        assertEquals(10, RULES.hostileDrain(List.of(10.0, 10.0)));
    }

    @Test
    void theRateAndTheCapFollowTheRules() {
        var rules = new NearbyRules(8, 2, 5, 8, 2);
        assertEquals(4, rules.hostileDrain(List.of(1.0, 1.0)));
        assertEquals(5, rules.hostileDrain(List.of(3.0)));
    }

    @Test
    void companyRestoresTwoOnceOrNothing() {
        assertEquals(2, RULES.companyRestore(true));
        assertEquals(0, RULES.companyRestore(false));
    }

    @Test
    void theDefaultRadiiAreEight() {
        assertEquals(8, RULES.hostileRadius());
        assertEquals(8, RULES.companyRadius());
    }
}
