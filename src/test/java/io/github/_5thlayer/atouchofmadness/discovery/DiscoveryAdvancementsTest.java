// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.discovery;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Stream;

import io.github._5thlayer.atouchofmadness.sanity.Cause;
import io.github._5thlayer.atouchofmadness.sanity.Stage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Every discoverable thing, each Cause and each Stage below Sane, has its advancement in the Pillar's
 * data, criterion on its id, and a title and description in en_us; so does the root. A Cause added
 * without its advancement would be one a player could never discover.
 */
class DiscoveryAdvancementsTest {

    // Gradle runs the tests from the project directory.
    private static final Path ADVANCEMENTS = Path.of("src/main/resources/data/atouchofmadness/advancement/discovery");
    private static final Path LANG = Path.of("src/main/resources/assets/atouchofmadness/lang/en_us.json");

    static Stream<String> ids() {
        return Stream.concat(Arrays.stream(Cause.values()).map(Cause::id),
                Stream.of(Stage.UNEASY, Stage.INSANE).map(stage -> stage.name().toLowerCase(Locale.ROOT)));
    }

    @ParameterizedTest
    @MethodSource("ids")
    void eachHasAnAdvancementCriterionOnItsId(String id) throws IOException {
        var file = ADVANCEMENTS.resolve(id + ".json");
        assertTrue(Files.exists(file), "no advancement " + file);
        var json = Files.readString(file);
        assertTrue(json.contains("\"source\": \"" + id + "\"") || json.contains("\"stage\": \"" + id + "\""),
                file + " has no criterion on " + id);
    }

    @ParameterizedTest
    @MethodSource("idsAndRoot")
    void eachHasATitleAndDescription(String id) throws IOException {
        var lang = Files.readString(LANG);
        for (var part : new String[] {"title", "description"}) {
            var key = "\"advancements.atouchofmadness.discovery." + id + "." + part + "\"";
            assertTrue(lang.contains(key), "en_us has no " + key);
        }
    }

    static Stream<String> idsAndRoot() {
        return Stream.concat(Stream.of("root"), ids());
    }
}
