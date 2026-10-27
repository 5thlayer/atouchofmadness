// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.hallucination;

import io.github._5thlayer.atouchofmadness.sanity.Sanity;
import io.github._5thlayer.atouchofmadness.sanity.Stage;

/**
 * What this client was last told of its player's Sanity, for the Hallucinations it plays. The server
 * tells it on login and whenever the value or the Stage changes. Until it is told, and once it leaves
 * a world, its player is at full Sanity and Sane, so nothing plays.
 */
public final class ClientSanity {

    private static final SanityPayload UNTOLD = new SanityPayload(new Sanity(Sanity.MAX), Stage.SANE);

    private static volatile SanityPayload told = UNTOLD;

    private ClientSanity() {
    }

    public static Sanity sanity() {
        return told.sanity();
    }

    public static Stage stage() {
        return told.stage();
    }

    static void told(SanityPayload payload) {
        told = payload;
    }

    /** Forgets what it was told, as its player leaves a world. */
    public static void forget() {
        told = UNTOLD;
    }
}
