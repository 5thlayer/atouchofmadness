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
 * of its hits takes {@code strike}; carrying Umbra drains {@code umbra} a minute. Plain Java, with no
 * Minecraft types.
 */
public record ShadowRules(List<Double> caps, int intervalTicks, double minDistance, double maxDistance, double fadeDistance,
        double gauntBelow, double gauntChance, double realFactor, double strike, double umbra, Stats lurker, Stats gaunt) {

    /** A creature's health, attack damage and movement speed, its flying speed for one that flies. */
    public record Stats(double health, double damage, double speed) {
    }

    /**
     * The defaults: a cap of 1 at 50, 2 at 35, 3 at 20 and 4 at 10; an attempt every 20 s, 12 to 24
     * blocks away; a Gaunt half the time below 10; fading within 4 blocks; a Real creature draining
     * twice its weight, a hit taking 5 and Umbra 1 a minute. A Lurker has 30 health and deals 4, a
     * Gaunt 16 and 7.
     */
    public static final ShadowRules DEFAULT = new ShadowRules(List.of(50.0, 35.0, 20.0, 10.0), 20 * 20, 12, 24, 4,
            10, 0.5, 2, 5, 1, new Stats(30, 4, 0.2), new Stats(16, 7, 0.3));

    public ShadowRules {
        caps = List.copyOf(caps);
        if (maxDistance < minDistance) {
            throw new IllegalArgumentException("The farthest a creature spawns, " + maxDistance + ", is nearer than the nearest, " + minDistance);
        }
    }

    /** These rules with other cap thresholds. */
    public ShadowRules withCaps(List<Double> caps) {
        return new ShadowRules(caps, intervalTicks, minDistance, maxDistance, fadeDistance, gauntBelow, gauntChance, realFactor,
                strike, umbra, lurker, gaunt);
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
