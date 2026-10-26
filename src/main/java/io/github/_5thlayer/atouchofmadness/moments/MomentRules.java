// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.moments;

/**
 * The instantaneous restores the common config sets: {@code sleep} to each player who wakes from
 * sleeping through the night, and {@code flower} for each flower a player breaks. A food's Sanity
 * Modifier is data, not config. Plain Java, with no Minecraft types.
 */
public record MomentRules(double sleep, double flower) {

    /** The defaults: 40 for a night's sleep, 1 a flower. */
    public static final MomentRules DEFAULT = new MomentRules(40, 1);
}
