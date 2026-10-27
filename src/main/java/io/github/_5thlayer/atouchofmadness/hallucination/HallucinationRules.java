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

    /**
     * The mean seconds between fake sounds, from Uneasy down, fall in a line through these two: 110 at
     * full Sanity, a point the line passes through but a player Uneasy never reaches, so 60 at 50, ...
     */
    static final double FAKE_SOUND_SECONDS_AT_FULL = 110;
    /** ... and 10 at none. */
    static final double FAKE_SOUND_SECONDS_AT_EMPTY = 10;

    /** The mean seconds between whispers while Insane. */
    static final double WHISPER_SECONDS = 45;

    /** How near the Encroachment comes at no Sanity while Uneasy, a point it never reaches there, so 0.2 at 50, ... */
    static final float ENCROACHMENT_UNEASY = 0.4F;
    /** ... and how near it comes at once on turning Insane, before the rest of the way as Sanity falls to none. */
    static final float ENCROACHMENT_INSANE = 0.5F;

    private HallucinationRules() {
    }

    /**
     * How deep the screen edge's colour loss is, from 0, none, to 1, the deepest: 0 while Sane, and from
     * Uneasy down, how far Sanity has fallen from full, so half at 50 and all of it at 0.
     */
    public static float colourLoss(Sanity sanity, Stage stage) {
        return stage == Stage.SANE ? 0 : fallen(sanity);
    }

    /**
     * How far the colour loss reaches in toward the middle of the view, from 0, only the screen's edge, to
     * 1, the farthest: 0 until Insane, and then how far Sanity has fallen from full.
     */
    public static float colourLossReach(Sanity sanity, Stage stage) {
        return stage == Stage.INSANE ? fallen(sanity) : 0;
    }

    /**
     * How near the Encroachment comes and how dark it is, from 0, none, to 1, the nearest: 0 while Sane;
     * while Uneasy, a little, nearer as Sanity falls; and while Insane, a step nearer, the rest of the way
     * as Sanity falls to none.
     */
    public static float encroachment(Sanity sanity, Stage stage) {
        return switch (stage) {
            case SANE -> 0;
            case UNEASY -> ENCROACHMENT_UNEASY * fallen(sanity);
            case INSANE -> ENCROACHMENT_INSANE + (1 - ENCROACHMENT_INSANE) * fallen(sanity);
        };
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

    /** How far Sanity has fallen from full, from 0 to 1. */
    private static float fallen(Sanity sanity) {
        return (float) (1 - sanity.value() / Sanity.MAX);
    }
}
