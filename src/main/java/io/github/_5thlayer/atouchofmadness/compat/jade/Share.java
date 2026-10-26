// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.compat.jade;

/** The share of a Fresh disc's restore a Stale disc gives, as Jade shows it. Names no Jade type. */
final class Share {

    private Share() {
    }

    /** A Stale disc's factor, a power of a half, as a fraction: ½, ¼ and ⅛, then 1/16 and on. */
    static String of(double factor) {
        long denominator = Math.round(1 / factor);
        return switch ((int) Math.min(denominator, Integer.MAX_VALUE)) {
            case 2 -> "½";
            case 4 -> "¼";
            case 8 -> "⅛";
            default -> "1/" + denominator;
        };
    }
}
