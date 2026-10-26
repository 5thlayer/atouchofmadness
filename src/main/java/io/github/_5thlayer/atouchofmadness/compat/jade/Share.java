// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.compat.jade;

/** The share of a Fresh disc's restore a Stale disc gives, as Jade shows it. Names no Jade type. */
final class Share {

    private Share() {
    }

    /**
     * A Stale disc's factor, a power of a half, as a fraction: 1/2, 1/4 and on. Plain digits, since the
     * game's font draws a glyph such as ½ too small to read.
     */
    static String of(double factor) {
        return "1/" + Math.round(1 / factor);
    }
}
