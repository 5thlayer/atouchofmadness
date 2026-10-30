// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.meter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.github._5thlayer.atouchofmadness.sanity.Sanity;

/**
 * The trend band the meter shows for a net rate: 0 below 0.5 Sanity a minute either way, ±1 below 3,
 * ±2 below 8 and ±3 from 8, negative for a drain; and 0 at the end of the range the rate pushes toward.
 */
class TrendTest {

    private static final Sanity MID = new Sanity(50);

    @ParameterizedTest(name = "{0}/min is band {1}")
    @CsvSource({
            "0, 0",
            "0.49, 0",
            "-0.49, 0",
            "0.5, 1",
            "-0.5, -1",
            "2, 1",
            "2.99, 1",
            "3, 2",
            "4, 2",
            "-6, -2",
            "7.99, 2",
            "-8, -3",
            "-10, -3",
            "-16, -3",
            "100, 3"
    })
    void aNetRateFallsInItsBand(double perMinute, int band) {
        assertEquals(band, Trend.band(MID, perMinute));
    }

    @Test
    void theBandIsSymmetric() {
        for (double perMinute = 0; perMinute < 12; perMinute += 0.25) {
            assertEquals(-Trend.band(MID, perMinute), Trend.band(MID, -perMinute), perMinute + "/min");
        }
    }

    @Test
    void aNonNumberShowsNoTrend() {
        assertEquals(0, Trend.band(MID, Double.NaN));
    }

    @Test
    void aRestoreAtFullShowsNoTrend() {
        assertEquals(0, Trend.band(new Sanity(Sanity.MAX), 4));
    }

    @Test
    void aDrainAtEmptyShowsNoTrend() {
        assertEquals(0, Trend.band(new Sanity(Sanity.MIN), -6));
    }

    @Test
    void aDrainAtFullAndARestoreAtEmptyStillShow() {
        assertEquals(-2, Trend.band(new Sanity(Sanity.MAX), -6));
        assertEquals(2, Trend.band(new Sanity(Sanity.MIN), 4));
    }

    @Test
    void justShortOfAnEndStillShows() {
        assertEquals(2, Trend.band(new Sanity(99.99), 4));
        assertEquals(-2, Trend.band(new Sanity(0.01), -6));
    }
}
