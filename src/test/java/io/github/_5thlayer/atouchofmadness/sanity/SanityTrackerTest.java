// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.sanity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static io.github._5thlayer.atouchofmadness.sanity.Cause.COMPANY;
import static io.github._5thlayer.atouchofmadness.sanity.Cause.LOW_LIGHT;
import static io.github._5thlayer.atouchofmadness.sanity.Cause.MUSIC;
import static io.github._5thlayer.atouchofmadness.sanity.Cause.STALE_DISC;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * An evaluation sums the rates into the Net rate and moves Sanity by it for every tick it covers, held
 * to 0–100. Each observation reports a Stage change only when the Stage differs from the last one
 * reported, under whichever rules apply then; the first reports one from no Stage. A rate moves Sanity,
 * and so names its causes, when it is not 0 and Sanity is not already at the end it pushes toward,
 * whatever the other rates do.
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
        assertEquals(55, tracker.evaluate(new Sanity(60), rates(-600, 300), 20).value());
        assertEquals(-300, tracker.netRate());
    }

    @Test
    void anEvaluationIsHeldToTheRange() {
        var tracker = new SanityTracker();
        assertEquals(0, tracker.evaluate(new Sanity(5), rates(-1200), 20).value());
        assertEquals(100, tracker.evaluate(new Sanity(95), rates(1200), 20).value());
    }

    @Test
    void anEvaluationWithNoRatesLeavesSanityAndANetRateOfZero() {
        var tracker = new SanityTracker();
        tracker.evaluate(new Sanity(60), rates(-1200), 20);
        assertEquals(60, tracker.evaluate(new Sanity(60), rates(), 20).value());
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

    @Test
    void aDrainAboveEmptyAndARestoreBelowFullMoveSanity() {
        assertEquals(Set.of(LOW_LIGHT, COMPANY), SanityTracker.moving(new Sanity(50),
                new Rate[] {Rate.drain(6, LOW_LIGHT), Rate.restore(6, COMPANY)}));
    }

    @Test
    void aRestoreAtFullMovesNothingEvenWhileADrainMovesSanity() {
        assertEquals(Set.of(LOW_LIGHT), SanityTracker.moving(new Sanity(100),
                new Rate[] {Rate.drain(6, LOW_LIGHT), Rate.restore(30, COMPANY)}));
    }

    @Test
    void aDrainAtEmptyMovesNothingEvenWhileARestoreMovesSanity() {
        assertEquals(Set.of(COMPANY), SanityTracker.moving(new Sanity(0),
                new Rate[] {Rate.drain(30, LOW_LIGHT), Rate.restore(6, COMPANY)}));
    }

    @Test
    void aRateOfZeroMovesNothing() {
        assertEquals(Set.of(), SanityTracker.moving(new Sanity(50), new Rate[] {Rate.drain(0, LOW_LIGHT)}));
    }

    @Test
    void aRateNamesEachOfItsCauses() {
        assertEquals(Set.of(MUSIC, STALE_DISC), SanityTracker.moving(new Sanity(50), new Rate[] {Rate.restore(3, MUSIC, STALE_DISC)}));
    }

    private static Rate[] rates(double... perMinute) {
        var rates = new Rate[perMinute.length];
        for (int i = 0; i < perMinute.length; i++) {
            rates[i] = new Rate(perMinute[i], Set.of());
        }
        return rates;
    }
}
