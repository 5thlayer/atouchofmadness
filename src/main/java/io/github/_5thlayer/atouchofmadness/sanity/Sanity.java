// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.sanity;

/** A player's Sanity, from {@link #MIN} to {@link #MAX}, held to that range however it moves. Plain Java, with no Minecraft types. */
public record Sanity(double value) {

    public static final double MIN = 0;
    public static final double MAX = 100;

    public Sanity {
        value = Math.clamp(value, MIN, MAX);
    }

    /** Sanity moved by a drain (negative) or restore (positive). */
    public Sanity add(double amount) {
        return new Sanity(value + amount);
    }
}
