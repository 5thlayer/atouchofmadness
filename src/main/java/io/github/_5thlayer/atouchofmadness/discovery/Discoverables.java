// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.discovery;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import io.github._5thlayer.atouchofmadness.sanity.Cause;
import io.github._5thlayer.atouchofmadness.sanity.Stage;

/**
 * The discoverable things, each Cause and each Stage below Sane, by the ids data names them by: each
 * one's advancement is {@code atouchofmadness:discovery/<id>}, and a client is told the ids it knows.
 * Plain Java, with no Minecraft types.
 */
public final class Discoverables {

    /** The Stages a player discovers; Sane, where Sanity does nothing to them, is none. */
    public static final List<Stage> STAGES = List.of(Stage.UNEASY, Stage.INSANE);

    /** Every discoverable thing's id: each Cause's, then each Stage's below Sane. */
    public static final List<String> IDS = Stream.concat(Arrays.stream(Cause.values()).map(Cause::id), STAGES.stream().map(Stage::id))
            .toList();

    private Discoverables() {
    }
}
