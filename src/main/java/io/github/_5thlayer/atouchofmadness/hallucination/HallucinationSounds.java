// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.hallucination;

import java.util.List;

import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Pillar's sound events, each with a subtitle in {@code sounds.json}: the fake sounds of
 * Hallucinations, whose subtitles are uncertain ("Something hisses?"), and the Shadow Creatures' own.
 */
public final class HallucinationSounds {

    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ATouchOfMadness.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> HISS = register("hallucination.hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> GROAN = register("hallucination.groan");
    public static final DeferredHolder<SoundEvent, SoundEvent> FOOTSTEPS = register("hallucination.footsteps");
    public static final DeferredHolder<SoundEvent, SoundEvent> CAVE = register("hallucination.cave");
    public static final DeferredHolder<SoundEvent, SoundEvent> DOOR = register("hallucination.door");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHISPER = register("hallucination.whisper");

    /** An Unreal Shadow Creature's, now and then as it stalks. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MURMUR = register("shadow.murmur");
    /** A Real Shadow Creature's, as it attacks. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CRY = register("shadow.cry");

    /** The fake sounds that play near a player from Uneasy down, one picked at random each time. */
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> FAKE = List.of(HISS, GROAN, FOOTSTEPS, CAVE, DOOR);

    private HallucinationSounds() {
    }

    static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, SoundEvent::createVariableRangeEvent);
    }
}
