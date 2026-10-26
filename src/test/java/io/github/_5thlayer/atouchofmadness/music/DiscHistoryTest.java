// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.music;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * A disc is Fresh, factor 1, unless it is one of the last two discs heard; a Stale disc's factor is half
 * its previous play's. A play goes on, at the factor it began with, while its jukebox's song goes on;
 * a song started again, heard or not, is a new play.
 */
class DiscHistoryTest {

    private static final String JUKEBOX = "minecraft:overworld 0 64 0";
    private static final String OTHER = "minecraft:overworld 20 64 0";

    /** How long a song in these tests lasts, in ticks. */
    private static final long SONG = 3000;

    /** The factors of these discs played one after another on the one jukebox, each heard from its start to its end. */
    private static List<Double> factors(String... discs) {
        var history = DiscHistory.EMPTY;
        List<Double> factors = new ArrayList<>();
        long now = 0;
        for (String disc : discs) {
            var heard = history.hear(JUKEBOX, disc, 0, now);
            factors.add(heard.factor());
            history = heard.history().hear(JUKEBOX, disc, SONG, now + SONG).history();
            now += SONG + 1;
        }
        return factors;
    }

    @Test
    void oneDiscOnRepeatHalvesEachPlay() {
        assertEquals(List.of(1.0, 0.5, 0.25, 0.125), factors("cat", "cat", "cat", "cat"));
    }

    @Test
    void twoDiscsAlternatingHalveEachSecondPlay() {
        assertEquals(List.of(1.0, 1.0, 0.5, 0.5, 0.25, 0.25), factors("cat", "far", "cat", "far", "cat", "far"));
    }

    @Test
    void threeDiscsInRotationStayFresh() {
        assertEquals(List.of(1.0, 1.0, 1.0, 1.0, 1.0, 1.0), factors("cat", "far", "mall", "cat", "far", "mall"));
    }

    @Test
    void aStaleDiscIsFreshAgainOnceTwoOthersHavePlayed() {
        assertEquals(List.of(1.0, 0.5, 1.0, 1.0, 1.0), factors("cat", "cat", "far", "mall", "cat"));
    }

    @Test
    void aStaleDiscHalvesItsOwnPreviousPlay() {
        assertEquals(List.of(1.0, 0.5, 1.0, 0.25), factors("cat", "cat", "far", "cat"));
    }

    @Test
    void aPlayGoesOnAtItsFactorWhileItsSongDoes() {
        var once = DiscHistory.EMPTY.hear(JUKEBOX, "cat", 0, 0).history().hear(JUKEBOX, "cat", SONG, SONG).history();
        var twice = once.hear(JUKEBOX, "cat", 0, SONG + 1).history();
        var later = twice.hear(JUKEBOX, "cat", 400, SONG + 401);
        assertEquals(0.5, later.factor());
        assertEquals(0.5, later.history().hear(JUKEBOX, "cat", 2000, SONG + 2001).factor());
    }

    @Test
    void aSongStartedAgainIsANewPlay() {
        var heard = DiscHistory.EMPTY.hear(JUKEBOX, "cat", 400, 400).history().hear(JUKEBOX, "cat", 20, 420);
        assertEquals(0.5, heard.factor());
    }

    @Test
    void aSongStartedAgainUnheardIsANewPlayHoweverFarItHasGone() {
        // Heard 400 ticks in, then away while it ends and starts again, and back 1000 ticks into the new song.
        var heard = DiscHistory.EMPTY.hear(JUKEBOX, "cat", 400, 400).history().hear(JUKEBOX, "cat", 1000, SONG + 1000);
        assertEquals(0.5, heard.factor());
    }

    @Test
    void walkingBetweenTwoJukeboxesMidSongIsNoNewPlay() {
        var history = DiscHistory.EMPTY.hear(JUKEBOX, "cat", 0, 0).history().hear(OTHER, "far", 0, 20).history();
        var back = history.hear(JUKEBOX, "cat", 200, 200);
        assertEquals(1, back.factor());
        assertEquals(1, back.history().hear(OTHER, "far", 200, 220).factor());
        assertEquals(0.5, back.history().hear(OTHER, "cat", 0, 220).factor());
    }

    @Test
    void anotherJukeboxPlayingTheSameDiscIsANewPlay() {
        var heard = DiscHistory.EMPTY.hear(JUKEBOX, "cat", 0, 0).history().hear(OTHER, "cat", 100, 100);
        assertEquals(0.5, heard.factor());
    }

    @Test
    void onlyTheLastTwoPlaysAreKept() {
        var history = DiscHistory.EMPTY;
        long now = 0;
        for (String disc : List.of("cat", "far", "mall", "wait")) {
            history = history.hear(JUKEBOX, disc, 0, now).history();
            now += SONG;
        }
        assertEquals(2, history.plays().size());
        assertTrue(history.plays().stream().allMatch(play -> play.disc().equals("mall") || play.disc().equals("wait")));
    }

    @Test
    void theRestoreIsTheRateTimesTheFactor() {
        assertEquals(4, MusicRules.DEFAULT.restore(1));
        assertEquals(1, MusicRules.DEFAULT.restore(0.25));
        assertEquals(64, MusicRules.DEFAULT.range());
    }
}
