// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.crown;

/**
 * The Flower Crown, as the common config sets it: worn, it restores {@code restore} Sanity a minute,
 * and it lasts {@code wearDays} in-game days of wear. Plain Java, with no Minecraft types.
 */
public record CrownRules(double restore, double wearDays) {

    /** The defaults: 1.5/min, for 1 in-game day of wear. */
    public static final CrownRules DEFAULT = new CrownRules(1.5, 1);

    private static final int TICKS_PER_DAY = 24000;

    /** How many ticks of wear a crown lasts: at least 1. */
    public int wearTicks() {
        return (int) Math.max(1, Math.round(wearDays * TICKS_PER_DAY));
    }

    /**
     * The wear, out of {@link #wearTicks}, of a crown that has worn {@code wear} ticks out of
     * {@code lasting}: the same share of its life, to the nearest tick.
     */
    public int wearOf(int wear, int lasting) {
        int ticks = wearTicks();
        return lasting == ticks ? wear : (int) Math.round((double) wear * ticks / lasting);
    }
}
