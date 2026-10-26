// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.sanity;

import java.util.Locale;

/**
 * What moved a player's Sanity, of the drains and restores a player can discover: each continuous one,
 * and each kind of Moment. The set is fixed here, and a later drain or restore adds its own. Plain Java,
 * with no Minecraft types.
 */
public enum Cause {
    LOW_LIGHT,
    FREEZING,
    WET,
    UNSETTLING_DIMENSION,
    HOSTILE,
    COMPANY,
    MUSIC,
    STALE_DISC,
    FLOWER_CROWN,
    /** A Shadow Creature of the player's own nearby, reported apart from {@link #HOSTILE}. */
    SHADOW_CREATURE,
    /** Raw Umbra carried. */
    UMBRA,
    /** Eating any food with a Sanity Modifier other than 0, one cause for all of them. */
    FOOD,
    SLEEP,
    FLOWER,
    /** A hit from a Real Shadow Creature. */
    SHADOW_STRIKE;

    /** Its id, as data names it: {@code low_light}. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
