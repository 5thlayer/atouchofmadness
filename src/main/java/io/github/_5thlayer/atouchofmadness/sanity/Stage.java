// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.sanity;

import java.util.Locale;

/** How far a player's Sanity has fallen, which {@link SanityRules#stageOf} reads from its value. */
public enum Stage {
    /** High enough that Sanity does nothing to the player. */
    SANE,
    /** Below Sane: Hallucinations start, and Shadow Creatures appear but cannot harm. */
    UNEASY,
    /** The lowest stage, where Shadow Creatures are real. */
    INSANE;

    /** Its id, as data names it: {@code uneasy}. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
