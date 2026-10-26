// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.music;

import java.util.ArrayList;
import java.util.List;

/**
 * The last two plays a player heard, oldest first, and the freshness rule over them (ADR 0004). A disc
 * is Fresh, with a factor of 1, unless it is one of these plays' discs; a Stale disc's factor is half
 * its previous play's. A play is one jukebox's song from its start, and goes on at the factor it began
 * with, however the player comes and goes, for as long as that jukebox plays that disc without starting
 * it again. Plain Java, with no Minecraft types: a jukebox and a disc are any ids, and a time is any
 * clock in ticks.
 */
public record DiscHistory(List<Play> plays) {

    /** How many plays back a disc stays Stale. */
    public static final int REMEMBERED = 2;

    /** A player who has heard no disc. */
    public static final DiscHistory EMPTY = new DiscHistory(List.of());

    public DiscHistory {
        plays = List.copyOf(plays);
    }

    /**
     * One play: the jukebox and disc it came from, how many ticks into its song and at what time it was
     * last heard, and its freshness factor.
     */
    public record Play(String jukebox, String disc, long ticks, long heardAt, double factor) {

        /**
         * Whether hearing {@code disc} from this play's jukebox, {@code ticks} into its song at
         * {@code now}, is this play going on: a song that started after it was last heard is a new one,
         * though its disc is the same.
         */
        boolean goesOnAs(String disc, long ticks, long now) {
            return this.disc.equals(disc) && ticks >= this.ticks && now - ticks <= heardAt;
        }
    }

    /** The history once the player has heard this play, and the factor it restores by. */
    public record Heard(DiscHistory history, double factor) {
    }

    /** The player hears {@code disc} from {@code jukebox} at {@code now}, {@code ticks} into its song. */
    public Heard hear(String jukebox, String disc, long ticks, long now) {
        List<Play> next = new ArrayList<>(plays);
        // Only a jukebox's latest play can go on: any before it ended when it started.
        for (int i = next.size() - 1; i >= 0; i--) {
            var play = next.get(i);
            if (!play.jukebox().equals(jukebox)) {
                continue;
            }
            if (play.goesOnAs(disc, ticks, now)) {
                next.set(i, new Play(jukebox, disc, ticks, now, play.factor()));
                return new Heard(new DiscHistory(next), play.factor());
            }
            break;
        }
        double factor = 1;
        for (var play : plays) {
            if (play.disc().equals(disc)) {
                factor = play.factor() / 2;
            }
        }
        next.add(new Play(jukebox, disc, ticks, now, factor));
        if (next.size() > REMEMBERED) {
            next.removeFirst();
        }
        return new Heard(new DiscHistory(next), factor);
    }
}
