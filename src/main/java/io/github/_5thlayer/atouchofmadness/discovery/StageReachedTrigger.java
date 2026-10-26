// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.discovery;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github._5thlayer.atouchofmadness.sanity.Stage;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code atouchofmadness:stage_reached}: the player reached a Stage, entering it or passing through it
 * on the way to a lower one, by whatever moved them, their join included.
 */
public final class StageReachedTrigger extends SimpleCriterionTrigger<StageReachedTrigger.Instance> {

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    void trigger(ServerPlayer player, Stage stage) {
        trigger(player, instance -> instance.stage() == stage);
    }

    public record Instance(Optional<ContextAwarePredicate> player, Stage stage) implements SimpleCriterionTrigger.SimpleInstance {

        static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                IdCodecs.STAGE.fieldOf("stage").forGetter(Instance::stage)
        ).apply(instance, Instance::new));
    }
}
