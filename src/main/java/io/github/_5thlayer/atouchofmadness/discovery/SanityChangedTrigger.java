// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.discovery;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github._5thlayer.atouchofmadness.sanity.Cause;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code atouchofmadness:sanity_changed}: a Cause, its {@code source}, moved the player's Sanity. A
 * continuous one fires at each evaluation where it moved it, a Moment each time it moved it at all.
 */
public final class SanityChangedTrigger extends SimpleCriterionTrigger<SanityChangedTrigger.Instance> {

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    void trigger(ServerPlayer player, Cause cause) {
        trigger(player, instance -> instance.source() == cause);
    }

    public record Instance(Optional<ContextAwarePredicate> player, Cause source) implements SimpleCriterionTrigger.SimpleInstance {

        static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                IdCodecs.CAUSE.fieldOf("source").forGetter(Instance::source)
        ).apply(instance, Instance::new));
    }
}
