// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.music;

/**
 * The restore of music, as the common config sets it: while the nearest jukebox playing within
 * {@code range} blocks plays a disc, {@code fresh} Sanity a minute × that disc's freshness factor, so
 * {@code fresh} is what a Fresh disc restores.
 * Plain Java, with no Minecraft types.
 */
public record MusicRules(double range, double fresh) {

    /** The defaults: within 64 blocks, a jukebox's sound's reach, 4/min. */
    public static final MusicRules DEFAULT = new MusicRules(64, 4);

    /** The restore of a play with this freshness factor, in Sanity a minute. */
    public double restore(double factor) {
        return fresh * factor;
    }
}
