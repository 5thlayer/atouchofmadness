// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.music;

import java.util.ArrayList;
import java.util.List;

/**
 * The last two plays a player heard, oldest first, and the freshness rule over them (ADR 0004). A disc
 * is Fresh, with a factor of 1, unless it is one of these plays' discs; a Stale disc's factor is half
 * its previous play's. A play is one jukebox's song from its start, and goes on at the factor it began
 * with, however the player comes and goes, until that jukebox stops or starts a song again. Each start
 * gives the song a new id on its jukebox, so the song a jukebox plays is the same play for as long as
 * its id is. Plain Java, with no Minecraft types: a jukebox, a song and a disc are any ids.
 */
public record DiscHistory(List<Play> plays) {

    /** How many plays back a disc stays Stale. */
    public static final int REMEMBERED = 2;

    /** A player who has heard no disc. */
    public static final DiscHistory EMPTY = new DiscHistory(List.of());

    public DiscHistory {
        plays = List.copyOf(plays);
    }

    /** One play: the jukebox it came from, the id its song started with there, its disc, and its factor. */
    public record Play(String jukebox, long songId, String disc, double factor) {
    }

    /** The history once the player has heard this play, and the factor it restores by. */
    public record Heard(DiscHistory history, double factor) {
    }

    /** The player hears {@code disc} from {@code jukebox}, playing the song it started as {@code songId}. */
    public Heard hear(String jukebox, long songId, String disc) {
        List<Play> next = new ArrayList<>(plays);
        // Only a jukebox's latest play can go on: any before it ended when it started.
        for (int i = next.size() - 1; i >= 0; i--) {
            var play = next.get(i);
            if (!play.jukebox().equals(jukebox)) {
                continue;
            }
            if (play.songId() == songId && play.disc().equals(disc)) {
                return new Heard(this, play.factor());
            }
            break;
        }
        double factor = 1;
        for (var play : plays) {
            if (play.disc().equals(disc)) {
                factor = play.factor() / 2;
            }
        }
        next.add(new Play(jukebox, songId, disc, factor));
        if (next.size() > REMEMBERED) {
            next.removeFirst();
        }
        return new Heard(new DiscHistory(next), factor);
    }
}
