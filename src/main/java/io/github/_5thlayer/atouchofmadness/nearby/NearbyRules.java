// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.nearby;

import java.util.Collection;

/**
 * The drains and restores of who is near, as the common config sets them: each unsettling entity within
 * {@code hostileRadius} blocks drains its weight × {@code hostile}, the sum capped at {@code hostileCap};
 * company within {@code companyRadius} blocks restores {@code company}, counted once. Each rate is in
 * Sanity a minute. Plain Java, with no Minecraft types.
 */
public record NearbyRules(double hostileRadius, double hostile, double hostileCap, double companyRadius, double company) {

    /** The defaults: within 8 blocks, 1/min per weight up to 10/min, and company 2/min. */
    public static final NearbyRules DEFAULT = new NearbyRules(8, 1, 10, 8, 2);

    /** The drain of unsettling entities of these weights nearby, in Sanity a minute. */
    public double hostileDrain(Collection<Double> weights) {
        double sum = weights.stream().mapToDouble(Double::doubleValue).sum();
        return Math.min(sum * hostile, hostileCap);
    }

    /** The restore of company, in Sanity a minute: the same for one companion as for many. */
    public double companyRestore(boolean anyCompany) {
        return anyCompany ? company : 0;
    }
}
