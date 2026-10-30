// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.sanity;

import java.util.Set;

/**
 * A continuous drain or restore's rate for a player at one evaluation, in Sanity a minute, negative for
 * a drain, and what it is the rate of: none, for one no player can discover, or more than one, as a
 * Stale disc's play is music too. Plain Java, with no Minecraft types.
 */
public record Rate(double perMinute, Set<Cause> causes) {

    /** A rate of 0, which applies to no one. */
    public static final Rate NONE = new Rate(0, Set.of());

    public Rate {
        causes = Set.copyOf(causes);
    }

    /** A drain of this much Sanity a minute. */
    public static Rate drain(double perMinute, Cause... causes) {
        return new Rate(-perMinute, Set.of(causes));
    }

    /** A restore of this much Sanity a minute. */
    public static Rate restore(double perMinute, Cause... causes) {
        return new Rate(perMinute, Set.of(causes));
    }
}
