// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.environment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * By default Low light is brightness at the eyes below 5, unless a full moon is overhead, and the
 * drains are 6/min for Low light, 4 for freezing, 1.5 for being wet and 1 for an unsettling dimension.
 */
class EnvironmentRulesTest {

    private static final EnvironmentRules RULES = EnvironmentRules.DEFAULT;

    @Test
    void belowFiveIsLowLight() {
        assertTrue(RULES.isLowLight(4, false));
        assertTrue(RULES.isLowLight(0, false));
    }

    @Test
    void fiveIsNotLowLight() {
        assertFalse(RULES.isLowLight(5, false));
        assertFalse(RULES.isLowLight(15, false));
    }

    @Test
    void aFullMoonOverheadIsNeverLowLight() {
        assertFalse(RULES.isLowLight(4, true));
        assertFalse(RULES.isLowLight(0, true));
    }

    @Test
    void theThresholdFollowsTheRules() {
        var rules = new EnvironmentRules(8, 6, 4, 1.5, 1);
        assertTrue(rules.isLowLight(7, false));
        assertFalse(rules.isLowLight(8, false));
    }

    @Test
    void theDefaultRatesArePerMinute() {
        assertEquals(6, RULES.lowLight());
        assertEquals(4, RULES.freezing());
        assertEquals(1.5, RULES.wet());
        assertEquals(1, RULES.unsettling());
    }

    @Test
    void aDrainPerMinuteIsANegativeRatePerTick() {
        assertEquals(-6.0 / 1200, EnvironmentRules.drainPerTick(6));
        assertEquals(0, EnvironmentRules.drainPerTick(0), 0);
    }
}
