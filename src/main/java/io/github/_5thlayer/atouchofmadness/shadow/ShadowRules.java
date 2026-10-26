// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.shadow;

import java.util.List;

/**
 * The Shadow Creatures as the common config sets them. A player's cap is how many of the {@code caps}
 * thresholds their Sanity is at or below; a spawn attempt is made every {@code intervalTicks} while
 * they are under it, {@code minDistance} to {@code maxDistance} blocks away, and below
 * {@code gauntBelow} each is a Gaunt at {@code gauntChance}. An Unreal creature fades once its Owner is
 * within {@code fadeDistance}. A Real one drains {@code realFactor} times its weight nearby, and each
 * of its hits takes {@code strike}. Plain Java, with no Minecraft types.
 */
public record ShadowRules(List<Double> caps, int intervalTicks, double minDistance, double maxDistance, double fadeDistance,
        double gauntBelow, double gauntChance, double realFactor, double strike, Stats lurker, Stats gaunt) {

    /** A creature's health, attack damage, how fast it drifts, in blocks a second, and the Sanity its kill restores. */
    public record Stats(double health, double damage, double drift, double kill) {
    }

    /**
     * The defaults: a cap of 1 at 50, 2 at 35, 3 at 20 and 4 at 10; an attempt every 20 s, 12 to 24
     * blocks away; a Gaunt half the time below 10; fading within 4 blocks; a Real creature draining
     * twice its weight and a hit taking 5. A Lurker has 30 health, deals 4, drifts 2.5 blocks a second
     * and restores 10 to its killer; a Gaunt 16, 7, 5 and 20.
     */
    public static final ShadowRules DEFAULT = new ShadowRules(List.of(50.0, 35.0, 20.0, 10.0), 20 * 20, 12, 24, 4,
            10, 0.5, 2, 5, new Stats(30, 4, 2.5, 10), new Stats(16, 7, 5, 20));

    public ShadowRules {
        caps = List.copyOf(caps);
        if (maxDistance < minDistance) {
            throw new IllegalArgumentException("The farthest a creature spawns, " + maxDistance + ", is nearer than the nearest, " + minDistance);
        }
    }

    /** These rules with other cap thresholds. */
    public ShadowRules withCaps(List<Double> caps) {
        return new ShadowRules(caps, intervalTicks, minDistance, maxDistance, fadeDistance, gauntBelow, gauntChance, realFactor,
                strike, lurker, gaunt);
    }

    /** How many Shadow Creatures a player at this Sanity may have at once. */
    public int capFor(double sanity) {
        return (int) caps.stream().filter(threshold -> sanity <= threshold).count();
    }

    /** Whether a spawn at this Sanity may be a Gaunt. */
    public boolean gauntsAt(double sanity) {
        return sanity < gauntBelow;
    }

    /** The weight a creature of this {@code unsettling_weight} drains by, nearby. */
    public double weightOf(double weight, boolean real) {
        return real ? weight * realFactor : weight;
    }
}
