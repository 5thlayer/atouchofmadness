// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.sanity;

/**
 * Where Sanity starts and where its stages begin, as the common config sets them: a new player starts
 * at {@code start}, a respawned one at {@code respawn}; a player is Sane above {@code saneAbove},
 * Insane below {@code insaneBelow}, and Uneasy from the one down to the other.
 */
public record SanityRules(Sanity start, Sanity respawn, double saneAbove, double insaneBelow) {

    /** The defaults: start at 100, respawn at 50, Sane above 50, Insane below 20. */
    public static final SanityRules DEFAULT = new SanityRules(new Sanity(100), new Sanity(50), 50, 20);

    public SanityRules {
        if (insaneBelow > saneAbove) {
            throw new IllegalArgumentException("Insane below " + insaneBelow + " is above Sane above " + saneAbove);
        }
    }

    public Stage stageOf(Sanity sanity) {
        if (sanity.value() > saneAbove) {
            return Stage.SANE;
        }
        return sanity.value() < insaneBelow ? Stage.INSANE : Stage.UNEASY;
    }
}
