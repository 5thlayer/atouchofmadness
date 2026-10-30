// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.sanity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * By default a new player starts at 100 and a respawned one at 50, and the stage is Sane above 50,
 * Uneasy from 50 down to 20, and Insane below 20.
 */
class SanityRulesTest {

    private static final SanityRules RULES = SanityRules.DEFAULT;

    @Test
    void aNewPlayerStartsAtAHundred() {
        assertEquals(100, RULES.start().value());
    }

    @Test
    void aRespawnedPlayerStartsAtFifty() {
        assertEquals(50, RULES.respawn().value());
    }

    @Test
    void justAboveFiftyIsSane() {
        assertEquals(Stage.SANE, RULES.stageOf(new Sanity(50.01)));
        assertEquals(Stage.SANE, RULES.stageOf(new Sanity(100)));
    }

    @Test
    void fiftyIsUneasy() {
        assertEquals(Stage.UNEASY, RULES.stageOf(new Sanity(50)));
    }

    @Test
    void twentyIsUneasy() {
        assertEquals(Stage.UNEASY, RULES.stageOf(new Sanity(20)));
    }

    @Test
    void justBelowTwentyIsInsane() {
        assertEquals(Stage.INSANE, RULES.stageOf(new Sanity(19.99)));
        assertEquals(Stage.INSANE, RULES.stageOf(new Sanity(0)));
    }

    @Test
    void theStagesFollowThresholdsOtherThanTheDefaults() {
        var rules = new SanityRules(new Sanity(80), new Sanity(30), 70, 10);
        assertEquals(Stage.SANE, rules.stageOf(new Sanity(71)));
        assertEquals(Stage.UNEASY, rules.stageOf(new Sanity(70)));
        assertEquals(Stage.UNEASY, rules.stageOf(new Sanity(10)));
        assertEquals(Stage.INSANE, rules.stageOf(new Sanity(9)));
        assertEquals(30, rules.respawn().value());
    }

    @Test
    void anInsaneLineAboveTheSaneOneIsReadAsTheSaneOne() {
        var rules = new SanityRules(new Sanity(100), new Sanity(50), 20, 50);
        assertEquals(20, rules.insaneBelow());
        assertEquals(Stage.UNEASY, rules.stageOf(new Sanity(20)));
        assertEquals(Stage.INSANE, rules.stageOf(new Sanity(19)));
    }
}
