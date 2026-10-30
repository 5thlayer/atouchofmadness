// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.sanity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;

/** A drain is a negative rate a minute and a restore a positive one, each naming its causes. */
class RateTest {

    @Test
    void aDrainIsNegative() {
        assertEquals(new Rate(-6, Set.of(Cause.LOW_LIGHT)), Rate.drain(6, Cause.LOW_LIGHT));
    }

    @Test
    void aRestoreIsPositive() {
        assertEquals(new Rate(3, Set.of(Cause.MUSIC, Cause.STALE_DISC)), Rate.restore(3, Cause.MUSIC, Cause.STALE_DISC));
    }
}
