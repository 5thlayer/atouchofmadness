// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.meter;

import io.github._5thlayer.atouchofmadness.sanity.Sanity;

/**
 * The trend band the Sanity meter shows, −3 to +3, which the Runtime draws as chevrons (suite
 * ADR-0005): how fast the Net rate moves Sanity, negative for a drain. Below 0.5 Sanity a minute
 * either way shows none, then one chevron below 3, two below 8 and three from 8 (#15). At the end of
 * the range the rate pushes toward it shows none, as Sanity can move no further. Plain Java, with no
 * Minecraft types.
 */
public final class Trend {

    /** The Sanity a minute at which each band from 1 to 3 begins. */
    private static final double[] BAND_FROM = {0.5, 3, 8};

    private Trend() {
    }

    /** The band of a Net rate in Sanity a minute at this Sanity; 0 for one that is not a number. */
    public static int band(Sanity sanity, double netRate) {
        if (netRate > 0 && sanity.value() >= Sanity.MAX || netRate < 0 && sanity.value() <= Sanity.MIN) {
            return 0;
        }
        double perMinute = Math.abs(netRate);
        int band = 0;
        while (band < BAND_FROM.length && perMinute >= BAND_FROM[band]) {
            band++;
        }
        return netRate < 0 ? -band : band;
    }
}
