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
 * its previous play's. A play goes on, at the factor it began with, while its jukebox plays the song it
 * started; a song started again, heard or not, is a new play.
 */
class DiscHistoryTest {

    private static final String JUKEBOX = "minecraft:overworld 0 64 0";
    private static final String OTHER = "minecraft:overworld 20 64 0";

    /** The factors of these discs played one after another on the one jukebox, each a song of its own, heard twice. */
    private static List<Double> factors(String... discs) {
        var history = DiscHistory.EMPTY;
        List<Double> factors = new ArrayList<>();
        long song = 0;
        for (String disc : discs) {
            var heard = history.hear(JUKEBOX, song, disc);
            factors.add(heard.factor());
            history = heard.history().hear(JUKEBOX, song, disc).history();
            song++;
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
        var twice = DiscHistory.EMPTY.hear(JUKEBOX, 1, "cat").history().hear(JUKEBOX, 2, "cat").history();
        var later = twice.hear(JUKEBOX, 2, "cat");
        assertEquals(0.5, later.factor());
        assertEquals(0.5, later.history().hear(JUKEBOX, 2, "cat").factor());
        assertEquals(twice, later.history());
    }

    @Test
    void aSongStartedAgainIsANewPlay() {
        var heard = DiscHistory.EMPTY.hear(JUKEBOX, 1, "cat").history().hear(JUKEBOX, 2, "cat");
        assertEquals(0.5, heard.factor());
    }

    @Test
    void aSongStartedAgainUnheardIsANewPlay() {
        // Heard once, then away while it ends and starts again, the player hears only the new song.
        var heard = DiscHistory.EMPTY.hear(JUKEBOX, 1, "cat").history().hear(JUKEBOX, 3, "cat");
        assertEquals(0.5, heard.factor());
    }

    @Test
    void anOlderPlayOfAJukeboxNeverGoesOn() {
        // The jukebox's first song is behind its second, so a song id equal to the first's is a new play.
        var history = DiscHistory.EMPTY.hear(JUKEBOX, 1, "cat").history().hear(JUKEBOX, 2, "far").history();
        assertEquals(0.5, history.hear(JUKEBOX, 1, "cat").factor());
    }

    @Test
    void walkingBetweenTwoJukeboxesMidSongIsNoNewPlay() {
        var history = DiscHistory.EMPTY.hear(JUKEBOX, 1, "cat").history().hear(OTHER, 2, "far").history();
        var back = history.hear(JUKEBOX, 1, "cat");
        assertEquals(1, back.factor());
        assertEquals(1, back.history().hear(OTHER, 2, "far").factor());
        assertEquals(0.5, back.history().hear(OTHER, 3, "cat").factor());
    }

    @Test
    void anotherJukeboxPlayingTheSameDiscIsANewPlay() {
        var heard = DiscHistory.EMPTY.hear(JUKEBOX, 1, "cat").history().hear(OTHER, 2, "cat");
        assertEquals(0.5, heard.factor());
    }

    @Test
    void anotherJukeboxIsANewPlayEvenWithTheSameSongId() {
        var heard = DiscHistory.EMPTY.hear(JUKEBOX, 1, "cat").history().hear(OTHER, 1, "cat");
        assertEquals(0.5, heard.factor());
    }

    @Test
    void onlyTheLastTwoPlaysAreKept() {
        var history = DiscHistory.EMPTY;
        long song = 0;
        for (String disc : List.of("cat", "far", "mall", "wait")) {
            history = history.hear(JUKEBOX, song++, disc).history();
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
