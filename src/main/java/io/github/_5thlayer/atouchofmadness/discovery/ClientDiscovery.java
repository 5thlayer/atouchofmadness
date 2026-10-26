// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.discovery;

import java.util.Set;

import io.github._5thlayer.atouchofmadness.sanity.Cause;
import io.github._5thlayer.atouchofmadness.sanity.Stage;

/**
 * What this client was last told its player has discovered, for what the client shows. It never reads
 * the advancements themselves: the server tells it on login, on each discovery, and on a datapack or config reload.
 * Until it is told, nothing is known.
 */
public final class ClientDiscovery {

    private static volatile Set<String> known = Set.of();

    private ClientDiscovery() {
    }

    public static boolean isKnown(Cause cause) {
        return known.contains(cause.id());
    }

    /** Sane, which is no discovery, is always known. */
    public static boolean isKnown(Stage stage) {
        return stage == Stage.SANE || known.contains(stage.id());
    }

    static void told(Set<String> told) {
        known = told;
    }
}
