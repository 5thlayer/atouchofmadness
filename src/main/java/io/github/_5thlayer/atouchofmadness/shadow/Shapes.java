// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.shadow;

import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The Shapes a Shadow Creature is drawn as: each creature takes one as it appears, from the monsters of
 * the biome it appears in that are in its kind's tag, weighted as the biome spawns them, or its kind's
 * fallback if none is. A Shape is synced to clients as entity data and changes only the look.
 */
public final class Shapes {

    /** The mobs a Lurker may take the Shape of, where its biome spawns them. */
    public static final TagKey<EntityType<?>> LURKER_SHAPES = tag("lurker_shapes");

    /** The mobs a Gaunt may take the Shape of, where its biome spawns them. */
    public static final TagKey<EntityType<?>> GAUNT_SHAPES = tag("gaunt_shapes");

    /** Syncs a Shape, by its id in the entity type registry. */
    static final EntityDataSerializer<EntityType<?>> SERIALIZER = EntityDataSerializer.forValueType(ByteBufCodecs.registry(Registries.ENTITY_TYPE));

    private static final DeferredRegister<EntityDataSerializer<?>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, ATouchOfMadness.MOD_ID);

    static {
        SERIALIZERS.register("shape", () -> SERIALIZER);
    }

    private Shapes() {
    }

    static void register(IEventBus modBus) {
        SERIALIZERS.register(modBus);
    }

    /** The Shapes a creature appearing in this biome may take from this tag: its monsters in the tag, weighted as it spawns them. */
    public static WeightedList<EntityType<?>> in(Holder<Biome> biome, TagKey<EntityType<?>> shapes) {
        var found = WeightedList.<EntityType<?>>builder();
        for (var spawn : biome.value().getMobSettings().getMobs(MobCategory.MONSTER).unwrap()) {
            var type = spawn.value().type();
            if (type.builtInRegistryHolder().is(shapes)) {
                found.add(type, spawn.weight());
            }
        }
        return found.build();
    }

    private static TagKey<EntityType<?>> tag(String name) {
        return TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(ATouchOfMadness.MOD_ID, name));
    }
}
