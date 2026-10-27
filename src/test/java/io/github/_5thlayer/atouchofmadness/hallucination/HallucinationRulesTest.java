// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.hallucination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github._5thlayer.atouchofmadness.sanity.Sanity;
import io.github._5thlayer.atouchofmadness.sanity.Stage;
import org.junit.jupiter.api.Test;

class HallucinationRulesTest {

    @Test
    void nothingPlaysWhileSane() {
        var sanity = new Sanity(60);
        assertEquals(0, HallucinationRules.colourLoss(sanity, Stage.SANE));
        assertEquals(0, HallucinationRules.colourLossReach(sanity, Stage.SANE));
        assertEquals(0, HallucinationRules.encroachment(sanity, Stage.SANE));
        assertEquals(0, HallucinationRules.fakeSoundChance(sanity, Stage.SANE));
        assertEquals(0, HallucinationRules.whisperChance(Stage.SANE));
        assertEquals(0, HallucinationRules.sway(Stage.SANE));
    }

    @Test
    void theOverlaysAreNoneWhileSaneAtAnySanity() {
        for (double value : new double[] {50.5, 75, 100}) {
            assertEquals(0, HallucinationRules.colourLossReach(new Sanity(value), Stage.SANE));
            assertEquals(0, HallucinationRules.encroachment(new Sanity(value), Stage.SANE));
        }
    }

    @Test
    void theColourLossIsHowFarSanityHasFallenFromFull() {
        assertEquals(0.5F, HallucinationRules.colourLoss(new Sanity(50), Stage.UNEASY), 1e-6);
        assertEquals(0.8F, HallucinationRules.colourLoss(new Sanity(20), Stage.INSANE), 1e-6);
        assertEquals(1F, HallucinationRules.colourLoss(new Sanity(0), Stage.INSANE), 1e-6);
    }

    @Test
    void theColourLossReachesInOnlyOnceInsane() {
        assertEquals(0, HallucinationRules.colourLossReach(new Sanity(50), Stage.UNEASY));
        assertEquals(0, HallucinationRules.colourLossReach(new Sanity(20), Stage.UNEASY));
        assertEquals(0.8F, HallucinationRules.colourLossReach(new Sanity(20), Stage.INSANE), 1e-6);
        assertEquals(1F, HallucinationRules.colourLossReach(new Sanity(0), Stage.INSANE), 1e-6);
    }

    @Test
    void theEncroachmentCreepsNearerAsSanityFallsWhileUneasy() {
        assertEquals(0.2F, HallucinationRules.encroachment(new Sanity(50), Stage.UNEASY), 1e-6);
        assertEquals(0.32F, HallucinationRules.encroachment(new Sanity(20), Stage.UNEASY), 1e-6);
    }

    @Test
    void bothOverlaysStepUpFromTheLastUneasySanityToTheFirstInsane() {
        var lastUneasy = new Sanity(20);
        var firstInsane = new Sanity(19.9);
        assertTrue(HallucinationRules.colourLossReach(firstInsane, Stage.INSANE)
                > HallucinationRules.colourLossReach(lastUneasy, Stage.UNEASY) + 0.5F);
        assertTrue(HallucinationRules.encroachment(firstInsane, Stage.INSANE)
                > HallucinationRules.encroachment(lastUneasy, Stage.UNEASY) + 0.5F);
    }

    @Test
    void theEncroachmentStepsUpAtInsaneAndIsDeepestAtNone() {
        assertEquals(0.9F, HallucinationRules.encroachment(new Sanity(20), Stage.INSANE), 1e-6);
        assertEquals(1F, HallucinationRules.encroachment(new Sanity(0), Stage.INSANE), 1e-6);
    }

    @Test
    void fakeSoundsComeOftenerAsSanityFalls() {
        assertEquals(1 / (60.0 * 20), HallucinationRules.fakeSoundChance(new Sanity(50), Stage.UNEASY), 1e-12);
        assertEquals(1 / (10.0 * 20), HallucinationRules.fakeSoundChance(new Sanity(0), Stage.INSANE), 1e-12);
        assertTrue(HallucinationRules.fakeSoundChance(new Sanity(20), Stage.INSANE)
                > HallucinationRules.fakeSoundChance(new Sanity(40), Stage.UNEASY));
    }

    @Test
    void whispersAndSwayAreInsaneAlone() {
        assertEquals(0, HallucinationRules.whisperChance(Stage.UNEASY));
        assertEquals(0, HallucinationRules.sway(Stage.UNEASY));
        assertTrue(HallucinationRules.whisperChance(Stage.INSANE) > 0);
        assertEquals(1, HallucinationRules.sway(Stage.INSANE));
    }
}
