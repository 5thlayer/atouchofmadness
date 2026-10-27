// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.shadow;

import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The Shadow Creatures' own sounds, each with a subtitle in {@code sounds.json}, a sure one, since a creature made it. */
public final class ShadowSounds {

    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ATouchOfMadness.MOD_ID);

    /** An Unreal creature's, now and then as it stalks. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MURMUR = SOUNDS.register("shadow.murmur", SoundEvent::createVariableRangeEvent);
    /** A Real creature's, as it attacks. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CRY = SOUNDS.register("shadow.cry", SoundEvent::createVariableRangeEvent);

    private ShadowSounds() {
    }

    static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }
}
