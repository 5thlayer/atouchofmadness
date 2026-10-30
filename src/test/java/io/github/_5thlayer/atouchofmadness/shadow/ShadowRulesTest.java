// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.shadow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Optional;

import io.github._5thlayer.atouchofmadness.sanity.Stage;
import io.github._5thlayer.atouchofmadness.sanity.StageChange;
import io.github._5thlayer.atouchofmadness.shadow.ShadowRules.Attacker;
import io.github._5thlayer.atouchofmadness.shadow.ShadowRules.PlayerFacts;
import io.github._5thlayer.atouchofmadness.shadow.ShadowRules.Struck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * By default a Haunted player has a cap of 1 Shadow Creature at 50 Sanity, 2 at 35, 3 at 20 and 4 at
 * 10 and below; a Gaunt may come below 10; and a Real creature drains twice its weight. A creature is
 * Real while its Owner is Insane, is sent to them and to the Insane, and fades within 4 blocks while
 * Unreal.
 */
class ShadowRulesTest {

    @ParameterizedTest
    @CsvSource({
            "100, 0",
            "50.5, 0",
            "50, 1",
            "35.5, 1",
            "35, 2",
            "20.5, 2",
            "20, 3",
            "10.5, 3",
            "10, 4",
            "0, 4",
    })
    void theCapRisesAsSanityFalls(double sanity, int cap) {
        assertEquals(cap, ShadowRules.DEFAULT.capFor(sanity));
    }

    @ParameterizedTest
    @CsvSource({"60, 0", "40, 1", "30, 2", "5, 2"})
    void theCapReadsTheConfiguredThresholdsInAnyOrder(double sanity, int cap) {
        var rules = ShadowRules.DEFAULT.withCaps(List.of(35.0, 50.0));
        assertEquals(cap, rules.capFor(sanity));
    }

    @ParameterizedTest
    @CsvSource({"9.5, true", "0, true", "10, false", "40, false"})
    void aGauntMayComeOnlyBelowItsThreshold(double sanity, boolean gaunts) {
        assertEquals(gaunts, ShadowRules.DEFAULT.gauntsAt(sanity));
    }

    @ParameterizedTest
    @CsvSource({"false, 2", "true, 4"})
    void aRealCreatureDrainsDoubleItsWeight(boolean real, double weight) {
        assertEquals(weight, ShadowRules.DEFAULT.weightOf(2, real));
    }

    @Test
    void aFarthestSpawnDistanceNearerThanTheNearestIsReadAsTheNearest() {
        var d = ShadowRules.DEFAULT;
        var rules = new ShadowRules(d.caps(), d.intervalTicks(), 16, 8, d.fadeDistance(), d.gauntBelow(), d.gauntChance(), d.realFactor(),
                d.strike(), d.lurker(), d.gaunt());
        assertEquals(16, rules.maxDistance());
    }

    /** An Uneasy player in Survival, alive, awake, where Shadow Creatures come: Haunted. */
    private static final PlayerFacts HAUNTED = new PlayerFacts(Stage.UNEASY, 30, true, true, false, true);

    @ParameterizedTest
    @CsvSource({
            "UNEASY, true, true, false, true, true",
            "INSANE, true, true, false, true, true",
            "SANE, true, true, false, true, false",
            "UNEASY, false, true, false, true, false",
            "UNEASY, true, false, false, true, false",
            "UNEASY, true, true, true, true, false",
            "UNEASY, true, true, false, false, false",
    })
    void aPlayerIsHauntedOnlyBelowSaneInSurvivalAliveAwakeAndWhereTheyCome(Stage stage, boolean survival, boolean alive,
            boolean asleep, boolean shadowsAllowed, boolean haunted) {
        assertEquals(haunted, ShadowRules.isHaunted(new PlayerFacts(stage, 30, survival, alive, asleep, shadowsAllowed)));
    }

    @Test
    void aHauntedPlayersCapIsTheirSanitysAndAnyoneElsesIsNone() {
        assertEquals(2, ShadowRules.DEFAULT.capFor(HAUNTED));
        assertEquals(0, ShadowRules.DEFAULT.capFor(new PlayerFacts(Stage.UNEASY, 30, true, true, true, true)));
    }

    @Test
    void aSanePlayerHasNoCapWhateverTheirSanity() {
        // At 40 the thresholds alone would give a cap of 1, as they would with Sane set below 50.
        assertEquals(0, ShadowRules.DEFAULT.capFor(new PlayerFacts(Stage.SANE, 40, true, true, false, true)));
    }

    @ParameterizedTest
    @CsvSource({"SANE, false", "UNEASY, false", "INSANE, true"})
    void aCreatureIsRealWhileItsOwnerIsInsane(Stage owner, boolean real) {
        assertEquals(real, ShadowRules.isReal(owner));
    }

    @ParameterizedTest
    @CsvSource({
            "true, SANE, true",
            "true, UNEASY, true",
            "false, SANE, false",
            "false, UNEASY, false",
            "false, INSANE, true",
    })
    void aCreatureIsSentToItsOwnerAndTheInsane(boolean owner, Stage viewer, boolean seen) {
        assertEquals(seen, ShadowRules.isSeenBy(owner, viewer));
    }

    @ParameterizedTest
    @CsvSource({
            "true, false, ENTITY, false, HURT",
            "true, false, NONE, false, HURT",
            "true, false, UNSEEING_PLAYER, false, IGNORED",
            "false, false, ENTITY, false, FADES",
            "false, false, NONE, false, IGNORED",
            "false, false, UNSEEING_PLAYER, false, IGNORED",
            "true, true, ENTITY, false, FADES",
            "true, true, NONE, false, IGNORED",
            "true, true, UNSEEING_PLAYER, false, IGNORED",
            "false, false, ENTITY, true, HURT",
            "false, false, UNSEEING_PLAYER, true, HURT",
    })
    void aStrikeHurtsARealCreatureFadesAnUnrealOneAndMissesOneTheAttackerCantSee(boolean real, boolean fading, Attacker by,
            boolean bypassesInvulnerability, Struck struck) {
        assertEquals(struck, ShadowRules.struck(real, fading, by, bypassesInvulnerability));
    }

    @ParameterizedTest
    @CsvSource({
            "true, false, false, true",
            "false, false, false, false",
            "true, true, false, false",
            "true, false, true, false",
    })
    void onlyARealCreatureNotFadingAndClearOfBlocksLandsAHit(boolean real, boolean fading, boolean insideBlocks, boolean hits) {
        assertEquals(hits, ShadowRules.mayHit(real, fading, insideBlocks));
    }

    @ParameterizedTest
    @CsvSource({"false, 16, true", "false, 16.5, false", "false, 0, true", "true, 0, false"})
    void anUnrealCreatureFadesWithinFourBlocksOfItsOwner(boolean real, double distanceSqr, boolean fades) {
        assertEquals(fades, ShadowRules.DEFAULT.fadesAt(real, distanceSqr));
    }

    @ParameterizedTest
    @CsvSource({
            "UNEASY, INSANE, true",
            "INSANE, UNEASY, true",
            "INSANE, SANE, true",
            "SANE, UNEASY, false",
            "UNEASY, SANE, false",
            "SANE, INSANE, true",
            "INSANE, INSANE, false",
    })
    void onlyCrossingTheInsaneLineRetracks(Stage from, Stage to, boolean retracks) {
        assertEquals(retracks, ShadowRules.retracks(new StageChange(Optional.of(from), to)));
    }

    @Test
    void joiningRetracksNothing() {
        assertFalse(ShadowRules.retracks(new StageChange(Optional.empty(), Stage.INSANE)));
    }
}
