// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.environment;

/**
 * The drains of where a player stands, as the common config sets them: Low light is brightness at the
 * eyes below {@code lowLightBelow}, unless a full moon is overhead (ADR 0003), and each drain is a
 * rate in Sanity a minute. Plain Java, with no Minecraft types.
 */
public record EnvironmentRules(int lowLightBelow, double lowLight, double freezing, double wet, double unsettling) {

    /** The defaults: Low light below 5, at 6/min; Freezing 4/min, Wet 1.5/min, an unsettling dimension 1/min. */
    public static final EnvironmentRules DEFAULT = new EnvironmentRules(5, 6, 4, 1.5, 1);

    private static final int TICKS_PER_MINUTE = 20 * 60;

    /** Whether light of this brightness at the eyes is Low light; never with a full moon overhead. */
    public boolean isLowLight(int brightness, boolean fullMoonOverhead) {
        return brightness < lowLightBelow && !fullMoonOverhead;
    }

    /** A drain of this much Sanity a minute as a continuous source's rate, in Sanity a tick. */
    public static double drainPerTick(double perMinute) {
        return -perMinute / TICKS_PER_MINUTE;
    }
}
