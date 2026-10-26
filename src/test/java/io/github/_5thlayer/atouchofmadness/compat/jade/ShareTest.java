// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.compat.jade;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** A Stale disc's factor, a half for each play back, is shown as the share of a Fresh disc's restore it gives. */
class ShareTest {

    @Test
    void halfQuarterAndEighthAreVulgarFractions() {
        assertEquals("½", Share.of(0.5));
        assertEquals("¼", Share.of(0.25));
        assertEquals("⅛", Share.of(0.125));
    }

    @Test
    void smallerSharesAreOneOverTheirDenominator() {
        assertEquals("1/16", Share.of(1.0 / 16));
        assertEquals("1/1024", Share.of(1.0 / 1024));
    }
}
