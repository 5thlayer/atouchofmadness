// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.gametest;

import java.util.Locale;

import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * In the {@code client} run only, shows each player their Sanity, its Stage and the Net rate on the
 * action bar once a second, so a drain or restore can be tried by walking into it. The meter is the
 * Runtime's to draw, and this Pillar doesn't nest it yet. Like {@link DevPacks}, it lives in the
 * {@code gametest} source set, which the jar leaves out (libworks ADR 0004).
 */
@EventBusSubscriber(modid = ATouchOfMadness.MOD_ID)
final class DevReadout {

    private static final boolean CLIENT_RUN = System.getProperty("libworks.run", "").equals("client");
    private static final int TICKS_PER_MINUTE = 20 * 60;

    private DevReadout() {
    }

    @SubscribeEvent
    static void tick(PlayerTickEvent.Post event) {
        if (!CLIENT_RUN || !(event.getEntity() instanceof ServerPlayer player)
                || player.tickCount % PlayerSanity.EVALUATION_TICKS != 0) {
            return;
        }
        String text = String.format(Locale.ROOT, "Sanity %.1f (%s) · %+.1f/min", PlayerSanity.get(player).value(),
                PlayerSanity.stage(player).name().toLowerCase(Locale.ROOT), PlayerSanity.netRate(player) * TICKS_PER_MINUTE);
        player.sendSystemMessage(Component.literal(text), true);
    }
}
