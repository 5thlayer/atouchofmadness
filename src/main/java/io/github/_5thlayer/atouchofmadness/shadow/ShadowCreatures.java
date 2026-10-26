// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.shadow;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import io.github._5thlayer.atouchofmadness.MadnessConfig;
import io.github._5thlayer.atouchofmadness.environment.EnvironmentRules;
import io.github._5thlayer.atouchofmadness.nearby.NearbySources;
import io.github._5thlayer.atouchofmadness.sanity.Cause;
import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import io.github._5thlayer.atouchofmadness.sanity.Rate;
import io.github._5thlayer.atouchofmadness.sanity.Stage;
import io.github._5thlayer.atouchofmadness.sanity.StageChange;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.dimension.DimensionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Shadow Creatures, the Lurker and the Gaunt, and Umbra, which they leave when killed Real (ADR
 * 0002). Each player's own creatures nearby drain them as their own source, {@code shadow_creature}:
 * each in {@code #atouchofmadness:unsettling} by its {@code atouchofmadness:unsettling_weight}, twice
 * that while Real, at the hostile rate within the hostile radius and under its cap. Carrying any raw
 * Umbra drains a flat rate, however much, as {@code umbra}. {@link ShadowSpawner} brings them.
 */
public final class ShadowCreatures {

    /** The dimension types Shadow Creatures come to, all three of vanilla's by default. */
    public static final TagKey<DimensionType> SHADOWS = TagKey.create(Registries.DIMENSION_TYPE,
            Identifier.fromNamespaceAndPath(ATouchOfMadness.MOD_ID, "shadows"));

    private static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(ATouchOfMadness.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ATouchOfMadness.MOD_ID);

    // Never saved, and allowed on Peaceful; MISC, so no creature counts toward a natural spawn cap.
    public static final DeferredHolder<EntityType<?>, EntityType<Lurker>> LURKER = ENTITIES.registerEntityType("lurker", Lurker::new,
            MobCategory.MISC, builder -> builder.sized(0.6F, 1.95F).eyeHeight(1.74F).clientTrackingRange(8).noSave());
    public static final DeferredHolder<EntityType<?>, EntityType<Gaunt>> GAUNT = ENTITIES.registerEntityType("gaunt", Gaunt::new,
            MobCategory.MISC, builder -> builder.sized(0.9F, 1.4F).eyeHeight(1.0F).clientTrackingRange(8).noSave());

    public static final DeferredItem<Item> UMBRA = ITEMS.registerSimpleItem("umbra");

    /** How far a creature can track its Owner, in blocks: beyond the farthest it spawns. */
    private static final double FOLLOW_RANGE = 48;

    /** The config's rules, swapped whole when it loads or reloads, possibly off the server thread. */
    private static volatile ShadowRules rules = ShadowRules.DEFAULT;

    /** Each Owner's creatures in a level, in the order they came; server thread only. */
    private static final Map<UUID, Set<ShadowCreature>> BY_OWNER = new HashMap<>();

    private ShadowCreatures() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(EntityAttributeCreationEvent.class, ShadowCreatures::attributes);
        modBus.addListener(ModConfigEvent.Loading.class, ShadowCreatures::configChanged);
        modBus.addListener(ModConfigEvent.Reloading.class, ShadowCreatures::configChanged);
        PlayerSanity.addSource(player -> Rate.of(nearby(player), Cause.SHADOW_CREATURE));
        PlayerSanity.addSource(player -> Rate.of(carried(player), Cause.UMBRA));
        PlayerSanity.onStageChange(ShadowCreatures::retrack);
        ShadowSpawner.register();
    }

    public static ShadowRules rules() {
        return rules;
    }

    /** The player's creatures in the level, oldest first. */
    public static List<ShadowCreature> of(ServerPlayer player) {
        return BY_OWNER.getOrDefault(player.getUUID(), Set.of()).stream().filter(creature -> !creature.isRemoved()).toList();
    }

    static void added(ShadowCreature creature) {
        if (creature.ownerId() != null) {
            BY_OWNER.computeIfAbsent(creature.ownerId(), id -> new LinkedHashSet<>()).add(creature);
        }
    }

    static void removed(ShadowCreature creature) {
        var owned = BY_OWNER.get(creature.ownerId());
        if (owned != null && owned.remove(creature) && owned.isEmpty()) {
            BY_OWNER.remove(creature.ownerId());
        }
    }

    /**
     * Has vanilla's tracker ask again whom each creature in the player's level but their own is sent to,
     * as they become Insane or stop being it: it asks only when a creature or a player changes section,
     * so a player standing still would go on seeing another's creatures, or not seeing them (ADR 0002).
     * Each is sent anew to everyone it is sent to.
     */
    private static void retrack(ServerPlayer player, StageChange change) {
        if (change.from().isEmpty() || (change.from().get() == Stage.INSANE) == (change.to() == Stage.INSANE)) {
            return;
        }
        var level = player.level();
        BY_OWNER.values().stream()
                .flatMap(Set::stream)
                .filter(creature -> !creature.isRemoved() && creature.level() == level && !creature.isOwnedBy(player))
                .toList()
                .forEach(creature -> {
                    level.getChunkSource().removeEntity(creature);
                    level.getChunkSource().addEntity(creature);
                });
    }

    private static double nearby(ServerPlayer player) {
        var current = rules;
        return NearbySources.unsettlingDrain(player, entity -> entity instanceof ShadowCreature creature && creature.isOwnedBy(player),
                (entity, weight) -> current.weightOf(weight, ((ShadowCreature) entity).isReal()));
    }

    private static double carried(ServerPlayer player) {
        return player.getInventory().contains(stack -> stack.is(UMBRA)) ? EnvironmentRules.drainPerTick(rules.umbra()) : 0;
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(LURKER.get(), creatureAttributes().build());
        event.put(GAUNT.get(), creatureAttributes().add(Attributes.FLYING_SPEED).build());
    }

    /** The attributes each creature has; it sets its stats from the config as it is made. */
    private static AttributeSupplier.Builder creatureAttributes() {
        return Mob.createMobAttributes().add(Attributes.ATTACK_DAMAGE).add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
    }

    private static void configChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() == MadnessConfig.SPEC) {
            rules = MadnessConfig.shadowRules();
        }
    }
}
