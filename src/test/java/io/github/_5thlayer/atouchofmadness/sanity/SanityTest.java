// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.sanity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Sanity is held to 0–100 however it moves. */
class SanityTest {

    @Test
    void anInstantaneousDrainStopsAtZero() {
        assertEquals(0, new Sanity(10).add(-30).value());
    }

    @Test
    void anInstantaneousRestoreStopsAtAHundred() {
        assertEquals(100, new Sanity(90).add(30).value());
    }

    @Test
    void aValueSetOutsideTheRangeIsHeldToIt() {
        assertEquals(100, new Sanity(150).value());
        assertEquals(0, new Sanity(-5).value());
    }
}
