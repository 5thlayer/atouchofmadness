// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.hallucination.client;

import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import io.github._5thlayer.atouchofmadness.MadnessClientConfig;
import io.github._5thlayer.atouchofmadness.hallucination.ClientSanity;
import io.github._5thlayer.atouchofmadness.hallucination.HallucinationRules;
import io.github._5thlayer.atouchofmadness.hallucination.HallucinationSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Plays this client's Hallucinations from what it was last told of its player's Sanity: from Uneasy
 * down, the colour loss, the Encroachment and fake sounds near the player; while Insane, the colour loss
 * reaching in, the view's slow sway and whispers. The colour loss, the Encroachment and the sway ease in
 * and out rather than jump, and each has a switch in the client config; vanilla's Distortion Effects
 * slider scales the sway and how much the Encroachment moves.
 */
@EventBusSubscriber(modid = ATouchOfMadness.MOD_ID, value = Dist.CLIENT)
public final class HallucinationEffects {

    private static final Identifier COLOUR_LOSS_LAYER = Identifier.fromNamespaceAndPath(ATouchOfMadness.MOD_ID, "colour_loss");
    private static final Identifier ENCROACHMENT_LAYER = Identifier.fromNamespaceAndPath(ATouchOfMadness.MOD_ID, "encroachment");

    /** How much of the way to its target each eased Hallucination moves each tick. */
    private static final float EASING = 0.05F;

    /** The sway's greatest roll and pitch, in degrees, and the ticks each takes to swing there and back. */
    private static final float SWAY_ROLL = 2.5F;
    private static final float SWAY_PITCH = 1.0F;
    private static final float SWAY_ROLL_TICKS = 180;
    private static final float SWAY_PITCH_TICKS = 260;

    /** How far from the player a fake sound plays, in blocks, and a whisper. */
    private static final float FAKE_NEAREST = 4;
    private static final float FAKE_FARTHEST = 12;
    private static final float WHISPER_NEAREST = 1.5F;
    private static final float WHISPER_FARTHEST = 3;

    private static float colourLoss;
    private static float colourLossReach;
    private static float encroachment;
    private static float sway;

    private HallucinationEffects() {
    }

    @SubscribeEvent
    static void registerLayers(RegisterGuiLayersEvent event) {
        // Over the world and vanilla's vignette, under the crosshair and the rest of the HUD.
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, COLOUR_LOSS_LAYER, (graphics, deltaTracker) -> {
            if (colourLoss > 0 && Minecraft.getInstance().player != null) {
                ColourLoss.draw(graphics, colourLoss, colourLossReach);
            }
        });
        event.registerAbove(COLOUR_LOSS_LAYER, ENCROACHMENT_LAYER, (graphics, deltaTracker) -> {
            var minecraft = Minecraft.getInstance();
            if (encroachment > 0 && minecraft.player != null) {
                float time = minecraft.player.tickCount + deltaTracker.getGameTimeDeltaPartialTick(false);
                Encroachment.draw(graphics, encroachment, time, distortion());
            }
        });
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.isPaused()) {
            return;
        }
        var sanity = ClientSanity.sanity();
        var stage = ClientSanity.stage();
        boolean colourLossOn = MadnessClientConfig.colourLoss();
        colourLoss = ease(colourLoss, colourLossOn ? HallucinationRules.colourLoss(sanity, stage) : 0);
        colourLossReach = ease(colourLossReach, colourLossOn ? HallucinationRules.colourLossReach(sanity, stage) : 0);
        encroachment = ease(encroachment, MadnessClientConfig.encroachment() ? HallucinationRules.encroachment(sanity, stage) : 0);
        sway = ease(sway, MadnessClientConfig.sway() ? HallucinationRules.sway(stage) : 0);
        var random = player.getRandom();
        if (random.nextDouble() < HallucinationRules.fakeSoundChance(sanity, stage)) {
            var sound = HallucinationSounds.FAKE.get(random.nextInt(HallucinationSounds.FAKE.size()));
            playNear(player, sound.get(), FAKE_NEAREST, FAKE_FARTHEST);
        }
        if (random.nextDouble() < HallucinationRules.whisperChance(stage)) {
            playNear(player, HallucinationSounds.WHISPER.get(), WHISPER_NEAREST, WHISPER_FARTHEST);
        }
    }

    @SubscribeEvent
    static void cameraAngles(ViewportEvent.ComputeCameraAngles event) {
        var player = Minecraft.getInstance().player;
        if (sway <= 0 || player == null) {
            return;
        }
        float scale = sway * distortion();
        if (scale <= 0) {
            return;
        }
        float time = player.tickCount + (float) event.getPartialTick();
        event.setRoll(event.getRoll() + scale * SWAY_ROLL * Mth.sin(time * Mth.TWO_PI / SWAY_ROLL_TICKS));
        event.setPitch(event.getPitch() + scale * SWAY_PITCH * Mth.sin(time * Mth.TWO_PI / SWAY_PITCH_TICKS));
    }

    @SubscribeEvent
    static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientSanity.forget();
        colourLoss = 0;
        colourLossReach = 0;
        encroachment = 0;
        sway = 0;
    }

    /** Vanilla's Distortion Effects slider, from 0 to 1. */
    private static float distortion() {
        return Minecraft.getInstance().options.screenEffectScale().get().floatValue();
    }

    private static float ease(float from, float to) {
        float eased = from + (to - from) * EASING;
        return Math.abs(to - eased) < 0.001F ? to : eased;
    }

    /** Plays the sound for this client alone, from a random point around the player within the distances, about the height of their head. */
    private static void playNear(LocalPlayer player, SoundEvent sound, float nearest, float farthest) {
        var random = player.getRandom();
        float angle = random.nextFloat() * Mth.TWO_PI;
        float distance = Mth.lerp(random.nextFloat(), nearest, farthest);
        double x = player.getX() + Mth.cos(angle) * distance;
        double y = player.getEyeY() + random.nextFloat() * 3 - 1.5;
        double z = player.getZ() + Mth.sin(angle) * distance;
        player.level().playLocalSound(x, y, z, sound, SoundSource.AMBIENT, 0.8F + random.nextFloat() * 0.2F,
                0.9F + random.nextFloat() * 0.2F, false);
    }
}
