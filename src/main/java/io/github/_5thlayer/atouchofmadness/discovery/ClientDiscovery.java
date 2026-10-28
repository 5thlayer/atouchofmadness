// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.discovery;

import io.github._5thlayer.atouchofmadness.sanity.Cause;
import io.github._5thlayer.atouchofmadness.sanity.Stage;

/**
 * What this client was last told its player has discovered, for what the client shows, as the Runtime's
 * record keeps it (suite ADR-0006). It never reads the advancements themselves, and until it is told,
 * nothing is known.
 */
public final class ClientDiscovery {

    private ClientDiscovery() {
    }

    public static boolean isKnown(Cause cause) {
        return Discovery.isKnownOnClient(cause.id());
    }

    /** Sane, which is no discovery, is always known. */
    public static boolean isKnown(Stage stage) {
        return stage == Stage.SANE || Discovery.isKnownOnClient(stage.id());
    }
}
