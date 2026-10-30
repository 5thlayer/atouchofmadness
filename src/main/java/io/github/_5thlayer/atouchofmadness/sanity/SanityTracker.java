// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.sanity;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * What {@link PlayerSanity} keeps in memory for one player and never saves: the Net rate of the last
 * evaluation, and the last Stage it reported. Plain Java, with no Minecraft types.
 */
final class SanityTracker {

    private static final int TICKS_PER_MINUTE = 20 * 60;

    private double netRate;
    private Optional<Stage> reported = Optional.empty();

    /** The Net rate of the last evaluation, in Sanity a minute; 0 before the first. */
    double netRate() {
        return netRate;
    }

    /**
     * Sums the rates of the continuous drains (negative) and restores (positive) that apply into the
     * Net rate, and returns Sanity moved by it, a rate a minute, for the ticks the evaluation covers.
     */
    Sanity evaluate(Sanity sanity, Rate[] rates, int ticks) {
        double sum = 0;
        for (var rate : rates) {
            sum += rate.perMinute();
        }
        netRate = sum;
        return sanity.add(sum * ticks / TICKS_PER_MINUTE);
    }

    /**
     * The causes of the rates that move Sanity from where it stands: a drain's while it is above empty,
     * a restore's while it is below full, whatever the other rates do, so two that cancel both move it.
     */
    static Set<Cause> moving(Sanity sanity, Rate[] rates) {
        var moving = EnumSet.noneOf(Cause.class);
        for (var rate : rates) {
            if (rate.perMinute() < 0 && sanity.value() > Sanity.MIN || rate.perMinute() > 0 && sanity.value() < Sanity.MAX) {
                moving.addAll(rate.causes());
            }
        }
        return moving;
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
