// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.shadow;

import java.util.List;

import io.github._5thlayer.atouchofmadness.sanity.Stage;
import io.github._5thlayer.atouchofmadness.sanity.StageChange;

/**
 * The Shadow Creatures as the common config sets them, and every decision about one's state. A Haunted
 * player's cap is how many of the {@code caps} thresholds their Sanity is at or below; a spawn attempt
 * is made every {@code intervalTicks} while they are under it, {@code minDistance} to
 * {@code maxDistance} blocks away, and below {@code gauntBelow} each is a Gaunt at {@code gauntChance}.
 * An Unreal creature fades once its Owner is within {@code fadeDistance}. A Real one drains
 * {@code realFactor} times its weight nearby, and each of its hits takes {@code strike}. Plain Java,
 * with no Minecraft types.
 */
public record ShadowRules(List<Double> caps, int intervalTicks, double minDistance, double maxDistance, double fadeDistance,
        double gauntBelow, double gauntChance, double realFactor, double strike, Stats lurker, Stats gaunt) {

    /**
     * What the rules need to know of a player: their Stage and Sanity, whether they are in Survival or
     * Adventure, alive, asleep, and in a dimension that allows Shadow Creatures.
     */
    public record PlayerFacts(Stage stage, double sanity, boolean survival, boolean alive, boolean asleep, boolean shadowsAllowed) {
    }

    /** Who strikes a creature: no entity, as lava does; an entity that may hit it; or a player it isn't sent to. */
    public enum Attacker {
        NONE, ENTITY, UNSEEING_PLAYER
    }

    /** What a strike on a creature comes to: it is hurt, nothing happens, or it starts to fade. */
    public enum Struck {
        HURT, IGNORED, FADES
    }

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

    /** A farthest spawn distance nearer than the nearest is read as the nearest. */
    public ShadowRules {
        caps = List.copyOf(caps);
        maxDistance = Math.max(maxDistance, minDistance);
    }

    /** These rules with other cap thresholds. */
    public ShadowRules withCaps(List<Double> caps) {
        return new ShadowRules(caps, intervalTicks, minDistance, maxDistance, fadeDistance, gauntBelow, gauntChance, realFactor,
                strike, lurker, gaunt);
    }

    /** Whether the player is Haunted: Uneasy or Insane, in Survival or Adventure, alive and awake, in a dimension that allows them. */
    public static boolean isHaunted(PlayerFacts player) {
        return player.stage() != Stage.SANE && player.survival() && player.alive() && !player.asleep() && player.shadowsAllowed();
    }

    /** How many Shadow Creatures the player may have at once: none unless Haunted, and then as their Sanity sets. */
    public int capFor(PlayerFacts player) {
        return isHaunted(player) ? capFor(player.sanity()) : 0;
    }

    /** How many Shadow Creatures a Haunted player at this Sanity may have at once. */
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

    /** Whether a creature is Real, by its Owner's Stage: whether they are Insane. */
    public static boolean isReal(Stage owner) {
        return owner == Stage.INSANE;
    }

    /** Whether a creature is sent to a player, who alone can hit it: its Owner, or anyone Insane (ADR 0002). */
    public static boolean isSeenBy(boolean isOwner, Stage viewer) {
        return isOwner || isReal(viewer);
    }

    /** Whether a creature can neither harm nor be harmed now: Unreal, or fading. */
    public static boolean isHarmless(boolean real, boolean fading) {
        return !real || fading;
    }

    /**
     * What a strike comes to. Damage that bypasses invulnerability always hurts; a player it isn't sent to
     * can't touch it; an Unreal or fading creature fades when an entity strikes it, and is otherwise
     * unharmed; a Real one is hurt.
     */
    public static Struck struck(boolean real, boolean fading, Attacker by, boolean bypassesInvulnerability) {
        if (bypassesInvulnerability) {
            return Struck.HURT;
        }
        if (by == Attacker.UNSEEING_PLAYER) {
            return Struck.IGNORED;
        }
        if (isHarmless(real, fading)) {
            return by == Attacker.NONE ? Struck.IGNORED : Struck.FADES;
        }
        return Struck.HURT;
    }

    /** Whether a creature may land a hit: Real, not fading, and its body clear of solid blocks. */
    public static boolean mayHit(boolean real, boolean fading, boolean insideBlocks) {
        return !isHarmless(real, fading) && !insideBlocks;
    }

    /** Whether an Unreal creature this far from its Owner, squared, starts to fade; a Real one never does. */
    public boolean fadesAt(boolean real, double distanceSqr) {
        return !real && distanceSqr <= fadeDistance * fadeDistance;
    }

    /** Whether a player's Stage change changes which creatures are sent to them: whether it crosses into or out of Insane. */
    public static boolean retracks(StageChange change) {
        return change.from().isPresent() && isReal(change.from().get()) != isReal(change.to());
    }
}
