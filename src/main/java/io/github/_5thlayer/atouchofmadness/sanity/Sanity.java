// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.sanity;

import java.util.Collection;

/**
 * A player's Sanity, from {@link #MIN} to {@link #MAX}. Each tick the continuous drains and restores
 * that apply sum into one net rate and the value moves by it; an instantaneous drain or restore moves
 * it by its amount at once. Either way it is held to 0–100. Plain Java, with no Minecraft types.
 */
public record Sanity(double value) {

    public static final double MIN = 0;
    public static final double MAX = 100;

    public Sanity {
        value = Math.clamp(value, MIN, MAX);
    }

    /** The one rate the given drains (negative) and restores (positive) sum into, in Sanity a tick. */
    public static double netRate(Collection<Double> rates) {
        double sum = 0;
        for (double rate : rates) {
            sum += rate;
        }
        return sum;
    }

    /** Sanity a tick later, moved by the net rate of the drains and restores that apply. */
    public Sanity tick(Collection<Double> rates) {
        return add(netRate(rates));
    }

    /** Sanity moved by an instantaneous drain (negative) or restore (positive). */
    public Sanity add(double amount) {
        return new Sanity(value + amount);
    }
}
