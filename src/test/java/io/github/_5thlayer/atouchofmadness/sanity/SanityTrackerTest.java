// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.sanity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * An evaluation sums the rates into the Net rate and moves Sanity by it for every tick it covers, held
 * to 0–100. Each observation reports a Stage change only when the Stage differs from the last one
 * reported, under whichever rules apply then; the first reports one from no Stage.
 */
class SanityTrackerTest {

    private static final SanityRules RULES = SanityRules.DEFAULT;

    @Test
    void theNetRateIsZeroBeforeAnyEvaluation() {
        assertEquals(0, new SanityTracker().netRate());
    }

    @Test
    void anEvaluationMovesSanityByTheSumOfItsRatesForEachTick() {
        var tracker = new SanityTracker();
        assertEquals(55, tracker.evaluate(new Sanity(60), new double[] {-0.5, 0.25}, 20).value());
        assertEquals(-0.25, tracker.netRate());
    }

    @Test
    void anEvaluationIsHeldToTheRange() {
        var tracker = new SanityTracker();
        assertEquals(0, tracker.evaluate(new Sanity(5), new double[] {-1}, 20).value());
        assertEquals(100, tracker.evaluate(new Sanity(95), new double[] {1}, 20).value());
    }

    @Test
    void anEvaluationWithNoRatesLeavesSanityAndANetRateOfZero() {
        var tracker = new SanityTracker();
        tracker.evaluate(new Sanity(60), new double[] {-1}, 20);
        assertEquals(60, tracker.evaluate(new Sanity(60), new double[0], 20).value());
        assertEquals(0, tracker.netRate());
    }

    @Test
    void theFirstObservationReportsAChangeFromNoStage() {
        assertEquals(Optional.of(new StageChange(Optional.empty(), Stage.SANE)),
                new SanityTracker().observe(new Sanity(100), RULES));
    }

    @Test
    void anObservationInTheSameStageReportsNothing() {
        var tracker = new SanityTracker();
        tracker.observe(new Sanity(100), RULES);
        assertEquals(Optional.empty(), tracker.observe(new Sanity(60), RULES));
    }

    @Test
    void anObservationAcrossAThresholdReportsTheChange() {
        var tracker = new SanityTracker();
        tracker.observe(new Sanity(100), RULES);
        assertEquals(Optional.of(new StageChange(Optional.of(Stage.SANE), Stage.INSANE)),
                tracker.observe(new Sanity(10), RULES));
    }

    @Test
    void aChangeIsReportedOnce() {
        var tracker = new SanityTracker();
        tracker.observe(new Sanity(100), RULES);
        tracker.observe(new Sanity(40), RULES);
        assertEquals(Optional.empty(), tracker.observe(new Sanity(30), RULES));
    }

    @Test
    void newRulesThatMoveTheThresholdAcrossSanityReportAChange() {
        var tracker = new SanityTracker();
        tracker.observe(new Sanity(60), RULES);
        var stricter = new SanityRules(new Sanity(100), new Sanity(50), 70, 20);
        assertEquals(Optional.of(new StageChange(Optional.of(Stage.SANE), Stage.UNEASY)),
                tracker.observe(new Sanity(60), stricter));
    }
}
