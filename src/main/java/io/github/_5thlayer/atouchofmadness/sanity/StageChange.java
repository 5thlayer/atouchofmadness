// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.sanity;

import java.util.Optional;

/** A player's Stage changing: from the one they were last known in, or from none as they join, to another. */
public record StageChange(Optional<Stage> from, Stage to) {
}
