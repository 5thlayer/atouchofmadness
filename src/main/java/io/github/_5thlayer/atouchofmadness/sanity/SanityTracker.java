// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.sanity;

import java.util.Optional;

/**
 * What {@link PlayerSanity} keeps in memory for one player and never saves: the Net rate of the last
 * evaluation, and the last Stage it reported. Plain Java, with no Minecraft types.
 */
final class SanityTracker {

    private double netRate;
    private Optional<Stage> reported = Optional.empty();

    /** The Net rate of the last evaluation, in Sanity a tick; 0 before the first. */
    double netRate() {
        return netRate;
    }

    /**
     * Sums the rates of the continuous drains (negative) and restores (positive) that apply into the
     * Net rate, and returns Sanity moved by it for each of the ticks the evaluation covers.
     */
    Sanity evaluate(Sanity sanity, double[] rates, int ticks) {
        double sum = 0;
        for (double rate : rates) {
            sum += rate;
        }
        netRate = sum;
        return sanity.add(sum * ticks);
    }

    /**
     * The Stage change, if any, since the last one reported: Sanity's Stage under the rules that apply
     * now, if it differs from the last reported, which it then becomes. The first observation reports a
     * change from no Stage.
     */
    Optional<StageChange> observe(Sanity sanity, SanityRules rules) {
        var stage = rules.stageOf(sanity);
        if (reported.equals(Optional.of(stage))) {
            return Optional.empty();
        }
        var change = new StageChange(reported, stage);
        reported = Optional.of(stage);
        return Optional.of(change);
    }
}
