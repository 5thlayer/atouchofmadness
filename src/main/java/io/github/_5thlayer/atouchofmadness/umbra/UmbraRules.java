// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.umbra;

/**
 * Umbra Gear's drains, as the common config sets them, each in Sanity a minute: {@code sword} and
 * {@code pickaxe} for each hand that holds one, {@code armour} for each piece worn. Plain Java, with no
 * Minecraft types.
 */
public record UmbraRules(double sword, double pickaxe, double armour) {

    /** The defaults: 2/min for each tool held, 1/min for each piece of armour worn. */
    public static final UmbraRules DEFAULT = new UmbraRules(2, 2, 1);

    /** The drain, in Sanity a minute, of this many swords and pickaxes held and pieces of armour worn. */
    public double drain(int swordsHeld, int pickaxesHeld, int armourWorn) {
        return swordsHeld * sword + pickaxesHeld * pickaxe + armourWorn * armour;
    }
}
