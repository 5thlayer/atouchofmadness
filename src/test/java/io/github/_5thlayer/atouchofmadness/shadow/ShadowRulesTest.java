// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.shadow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * By default a player has a cap of 1 Shadow Creature at 50 Sanity, 2 at 35, 3 at 20 and 4 at 10 and
 * below; a Gaunt may come below 10; and a Real creature drains twice its weight.
 */
class ShadowRulesTest {

    @ParameterizedTest
    @CsvSource({
            "100, 0",
            "50.5, 0",
            "50, 1",
            "35.5, 1",
            "35, 2",
            "20.5, 2",
            "20, 3",
            "10.5, 3",
            "10, 4",
            "0, 4",
    })
    void theCapRisesAsSanityFalls(double sanity, int cap) {
        assertEquals(cap, ShadowRules.DEFAULT.capFor(sanity));
    }

    @ParameterizedTest
    @CsvSource({"60, 0", "40, 1", "30, 2", "5, 2"})
    void theCapReadsTheConfiguredThresholdsInAnyOrder(double sanity, int cap) {
        var rules = ShadowRules.DEFAULT.withCaps(List.of(35.0, 50.0));
        assertEquals(cap, rules.capFor(sanity));
    }

    @ParameterizedTest
    @CsvSource({"9.5, true", "0, true", "10, false", "40, false"})
    void aGauntMayComeOnlyBelowItsThreshold(double sanity, boolean gaunts) {
        assertEquals(gaunts, ShadowRules.DEFAULT.gauntsAt(sanity));
    }

    @ParameterizedTest
    @CsvSource({"false, 2", "true, 4"})
    void aRealCreatureDrainsDoubleItsWeight(boolean real, double weight) {
        assertEquals(weight, ShadowRules.DEFAULT.weightOf(2, real));
    }
}
