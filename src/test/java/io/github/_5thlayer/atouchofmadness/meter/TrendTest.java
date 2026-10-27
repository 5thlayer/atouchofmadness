// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.meter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The trend band the meter shows for a net rate: 0 below 0.5 Sanity a minute either way, ±1 below 3,
 * ±2 below 8 and ±3 from 8, negative for a drain.
 */
class TrendTest {

    private static double perTick(double perMinute) {
        return perMinute / (20 * 60);
    }

    @ParameterizedTest(name = "{0}/min is band {1}")
    @CsvSource({
            "0, 0",
            "0.49, 0",
            "-0.49, 0",
            "0.5, 1",
            "-0.5, -1",
            // Company alone.
            "2, 1",
            "2.99, 1",
            "3, 2",
            // Music alone, and Low light alone.
            "4, 2",
            "-6, -2",
            "7.99, 2",
            "-8, -3",
            // Low light and freezing together, and every nearby hostile.
            "-10, -3",
            "-16, -3",
            "100, 3"
    })
    void aNetRateFallsInItsBand(double perMinute, int band) {
        assertEquals(band, Trend.band(perTick(perMinute)));
    }

    @Test
    void theBandIsSymmetric() {
        for (double perMinute = 0; perMinute < 12; perMinute += 0.25) {
            assertEquals(-Trend.band(perTick(perMinute)), Trend.band(perTick(-perMinute)), perMinute + "/min");
        }
    }

    @Test
    void aNonNumberShowsNoTrend() {
        assertEquals(0, Trend.band(Double.NaN));
    }
}
