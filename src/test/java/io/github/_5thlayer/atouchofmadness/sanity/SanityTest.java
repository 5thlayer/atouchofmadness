// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.sanity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/** A tick moves Sanity by the sum of the rates that apply, held to 0–100. */
class SanityTest {

    @Test
    void aTickMovesByTheSumOfItsRates() {
        assertEquals(59.5, new Sanity(60).tick(List.of(-1.0, 0.25, 0.25)).value());
    }

    @Test
    void aTickWithNoRatesLeavesSanityWhereItIs() {
        assertEquals(60, new Sanity(60).tick(List.of()).value());
    }

    @Test
    void aTickStopsAtZero() {
        assertEquals(0, new Sanity(0.5).tick(List.of(-2.0)).value());
    }

    @Test
    void aTickStopsAtAHundred() {
        assertEquals(100, new Sanity(99.5).tick(List.of(2.0)).value());
    }

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
