// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.hallucination;

import io.github._5thlayer.atouchofmadness.sanity.Sanity;
import io.github._5thlayer.atouchofmadness.sanity.Stage;

/**
 * How strongly each Hallucination plays at a Sanity and Stage: nothing while Sane, and from Uneasy down,
 * more as Sanity falls. Plain Java, with no Minecraft types.
 */
public final class HallucinationRules {

    /** The mean seconds between fake sounds at full Sanity and at none, from Uneasy down; between, it falls in a line. */
    static final double FAKE_SOUND_SECONDS_AT_FULL = 110;
    static final double FAKE_SOUND_SECONDS_AT_EMPTY = 10;

    /** The mean seconds between whispers while Insane. */
    static final double WHISPER_SECONDS = 45;

    private HallucinationRules() {
    }

    /**
     * How deep the screen edge's colour loss is, from 0, none, to 1, the deepest: 0 while Sane, and from
     * Uneasy down, how far Sanity has fallen from full, so half at 50 and all of it at 0.
     */
    public static float colourLoss(Sanity sanity, Stage stage) {
        return stage == Stage.SANE ? 0 : (float) (1 - sanity.value() / Sanity.MAX);
    }

    /** The chance each client tick that a fake sound plays: none while Sane, and from Uneasy down, more often as Sanity falls. */
    public static double fakeSoundChance(Sanity sanity, Stage stage) {
        if (stage == Stage.SANE) {
            return 0;
        }
        double seconds = FAKE_SOUND_SECONDS_AT_EMPTY
                + (FAKE_SOUND_SECONDS_AT_FULL - FAKE_SOUND_SECONDS_AT_EMPTY) * sanity.value() / Sanity.MAX;
        return 1 / (seconds * 20);
    }

    /** The chance each client tick that a whisper plays: only while Insane. */
    public static double whisperChance(Stage stage) {
        return stage == Stage.INSANE ? 1 / (WHISPER_SECONDS * 20) : 0;
    }

    /** How much the view sways, from 0 to 1, before vanilla's Distortion Effects slider scales it: fully while Insane, and not at all otherwise. */
    public static float sway(Stage stage) {
        return stage == Stage.INSANE ? 1 : 0;
    }
}
