// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.discovery;

import java.util.Arrays;
import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.github._5thlayer.atouchofmadness.sanity.Cause;
import io.github._5thlayer.atouchofmadness.sanity.Stage;

/** A Cause and a Stage in data, each by its id: the triggers' {@code source} and {@code stage}. */
final class IdCodecs {

    static final Codec<Cause> CAUSE = byId(Cause.values(), Cause::id);
    static final Codec<Stage> STAGE = byId(Stage.values(), Stage::id);

    private IdCodecs() {
    }

    private static <E> Codec<E> byId(E[] values, Function<E, String> id) {
        return Codec.STRING.comapFlatMap(name -> Arrays.stream(values)
                .filter(value -> id.apply(value).equals(name))
                .findFirst()
                .map(DataResult::success)
                .orElseGet(() -> DataResult.error(() -> "no such id: " + name)), id);
    }
}
