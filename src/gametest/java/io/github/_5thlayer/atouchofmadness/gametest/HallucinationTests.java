// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.gametest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import io.github._5thlayer.atouchofmadness.hallucination.Hallucinations;
import io.github._5thlayer.atouchofmadness.hallucination.SanityPayload;
import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import io.github._5thlayer.atouchofmadness.sanity.Sanity;
import io.github._5thlayer.atouchofmadness.sanity.Stage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Hallucinations: the server tells each player's client their Sanity and Stage, in the Pillar's own
 * payload, as they join and whenever either changes, and at no other time. What the client plays from
 * it is checked by hand in the dev client, which no game test loads. Each test joins players of its
 * own and logs them out however it ends.
 */
final class HallucinationTests {

    /** Every payload the server told each player's client, oldest first. */
    private static final Map<UUID, List<SanityPayload>> TOLD = new ConcurrentHashMap<>();

    static {
        Hallucinations.onTell((player, payload) -> TOLD.computeIfAbsent(player.getUUID(), id -> new ArrayList<>()).add(payload));
    }

    private HallucinationTests() {
    }

    static void register(ATouchOfMadnessGameTests.Registrar tests) {
        tests.test("hallucinations_the_client_is_told_sanity_and_stage_on_login", 1, HallucinationTests::onLogin);
        tests.test("hallucinations_the_client_is_told_each_stage_change", 1, HallucinationTests::onStageChange);
        tests.test("hallucinations_the_client_is_told_a_change_of_value_and_nothing_twice", 1, HallucinationTests::onValueChange);
        tests.test("hallucinations_each_sound_event_has_its_sounds_and_a_subtitle", 1, HallucinationTests::soundsJson);
    }

    /** A player who left Uneasy at 40 is told so as they join again, though nothing moved. */
    private static void onLogin(GameTestHelper helper) {
        var profile = TestPlayers.profile();
        whenDone(helper, profile);
        var player = TestPlayers.join(helper, profile);
        PlayerSanity.set(player, new Sanity(40));
        TestPlayers.leave(helper, profile);
        TOLD.remove(profile.id());
        TestPlayers.join(helper, profile);
        expectTold(helper, profile.id(), List.of(new SanityPayload(new Sanity(40), Stage.UNEASY)), "on login");
        helper.succeed();
    }

    private static void onStageChange(GameTestHelper helper) {
        var player = join(helper, 100);
        PlayerSanity.set(player, new Sanity(40));
        PlayerSanity.set(player, new Sanity(10));
        PlayerSanity.set(player, new Sanity(80));
        expectTold(helper, player.getUUID(), List.of(
                new SanityPayload(new Sanity(100), Stage.SANE),
                new SanityPayload(new Sanity(40), Stage.UNEASY),
                new SanityPayload(new Sanity(10), Stage.INSANE),
                new SanityPayload(new Sanity(80), Stage.SANE)), "across the stages");
        helper.succeed();
    }

    /** A move within Uneasy is told, since the colour loss deepens with it; a set to the value already told is not. */
    private static void onValueChange(GameTestHelper helper) {
        var player = join(helper, 40);
        PlayerSanity.set(player, new Sanity(30));
        PlayerSanity.set(player, new Sanity(30));
        PlayerSanity.add(player, 0);
        expectTold(helper, player.getUUID(), List.of(
                new SanityPayload(new Sanity(100), Stage.SANE),
                new SanityPayload(new Sanity(40), Stage.UNEASY),
                new SanityPayload(new Sanity(30), Stage.UNEASY)), "within a stage");
        helper.succeed();
    }

    /**
     * The client finds each registered sound's sounds and subtitle in {@code sounds.json}, which only it
     * reads, and the subtitle in en_us: a fake one's, a Hallucination's, uncertain, ending in a question
     * mark, and a Shadow Creature's, which is real, sure.
     */
    private static void soundsJson(GameTestHelper helper) {
        var registered = BuiltInRegistries.SOUND_EVENT.keySet().stream()
                .filter(id -> id.getNamespace().equals(ATouchOfMadness.MOD_ID))
                .map(Identifier::getPath)
                .collect(Collectors.toSet());
        if (registered.isEmpty()) {
            helper.fail("the Pillar registers no sound event: the test checks nothing");
        }
        var sounds = asset(helper, "sounds.json");
        if (!sounds.keySet().equals(registered)) {
            helper.fail("sounds.json lists " + sounds.keySet() + ", while the registered sound events are " + registered);
        }
        var lang = asset(helper, "lang/en_us.json");
        for (var name : registered) {
            var subtitle = sounds.getAsJsonObject(name).get("subtitle");
            if (subtitle == null || !lang.has(subtitle.getAsString())) {
                helper.fail(name + " has no subtitle en_us names");
                return;
            }
            var text = lang.get(subtitle.getAsString()).getAsString();
            if (text.endsWith("?") != name.startsWith("hallucination.")) {
                helper.fail(name + "'s subtitle, \"" + text + "\", is uncertain only if it is a Hallucination's");
            }
        }
        helper.succeed();
    }

    private static JsonObject asset(GameTestHelper helper, String path) {
        try (var stream = ATouchOfMadness.class.getResourceAsStream("/assets/" + ATouchOfMadness.MOD_ID + "/" + path)) {
            if (stream == null) {
                throw helper.assertionException(Component.literal("no " + path));
            }
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw helper.assertionException(Component.literal(path + " can't be read: " + e));
        }
    }

    private static void expectTold(GameTestHelper helper, UUID id, List<SanityPayload> expected, String when) {
        var told = TOLD.getOrDefault(id, List.of());
        if (!told.equals(expected)) {
            helper.fail(when + " the client was told " + told + ", not " + expected);
        }
    }

    /** Joins a player, logged out however the test ends, and sets their Sanity. */
    private static ServerPlayer join(GameTestHelper helper, double sanity) {
        var profile = TestPlayers.profile();
        whenDone(helper, profile);
        var player = TestPlayers.join(helper, profile);
        PlayerSanity.set(player, new Sanity(sanity));
        return player;
    }

    private static void whenDone(GameTestHelper helper, GameProfile profile) {
        TestPlayers.whenDone(helper, () -> {
            TestPlayers.leave(helper, profile);
            TOLD.remove(profile.id());
        });
    }
}
