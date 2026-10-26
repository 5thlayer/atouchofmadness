// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.crown;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * By default a Flower Crown restores 1.5/min while worn and lasts 1 in-game day of wear, 24000 ticks;
 * a crown worn under another wear time keeps the share of its life it has used.
 */
class CrownRulesTest {

    private static final CrownRules RULES = CrownRules.DEFAULT;

    @Test
    void theDefaultRestoreIsOneAndAHalf() {
        assertEquals(1.5, RULES.restore());
    }

    @Test
    void aDayOfWearIs24000Ticks() {
        assertEquals(24000, RULES.wearTicks());
    }

    @Test
    void theWearTimeFollowsTheRules() {
        assertEquals(12000, new CrownRules(1.5, 0.5).wearTicks());
        assertEquals(72000, new CrownRules(1.5, 3).wearTicks());
    }

    @Test
    void theWearTimeIsNeverUnderATick() {
        assertEquals(1, new CrownRules(1.5, 0).wearTicks());
        assertEquals(1, new CrownRules(1.5, 0.00001).wearTicks());
    }

    @Test
    void aCrownWornUnderTheseRulesKeepsItsWear() {
        assertEquals(0, RULES.wearOf(0, 24000));
        assertEquals(500, RULES.wearOf(500, 24000));
    }

    @Test
    void aCrownWornUnderAnotherWearTimeKeepsItsShare() {
        assertEquals(12000, RULES.wearOf(6000, 12000));
        assertEquals(8000, RULES.wearOf(24000, 72000));
        assertEquals(0, RULES.wearOf(0, 12000));
    }

    @Test
    void aSpentShareStaysSpent() {
        assertEquals(24000, RULES.wearOf(12000, 12000));
    }

    @Test
    void theShareRoundsToTheNearestTick() {
        var tenTicks = new CrownRules(1.5, 10.0 / 24000);
        assertEquals(10, tenTicks.wearTicks());
        assertEquals(5, tenTicks.wearOf(13000, 24000));
        assertEquals(6, tenTicks.wearOf(14000, 24000));
    }
}
