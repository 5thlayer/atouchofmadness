// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.nearby;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToDoubleBiFunction;

import com.mojang.serialization.Codec;
import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import io.github._5thlayer.atouchofmadness.MadnessConfig;
import io.github._5thlayer.atouchofmadness.environment.EnvironmentRules;
import io.github._5thlayer.atouchofmadness.sanity.Cause;
import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import io.github._5thlayer.atouchofmadness.sanity.Rate;
import io.github._5thlayer.atouchofmadness.shadow.ShadowCreature;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * The continuous drain and restore of who is near a player: each living entity in {@link #UNSETTLING}
 * within the hostile radius drains its {@link #UNSETTLING_WEIGHT}, and any living entity in
 * {@link #COMFORTING}, or any other player in Survival or Adventure, within the company radius restores,
 * counted once. An entity in {@code #comforting} that can be tamed is company only once tamed. Distance
 * is from the player's feet to the entity's. A Shadow Creature is no hostile: its drain is its own, and
 * only its Owner's, which {@link #unsettlingDrain} weighs for it.
 */
public final class NearbySources {

    /** The entity types that drain Sanity nearby, vanilla's monsters by default. */
    public static final TagKey<EntityType<?>> UNSETTLING = TagKey.create(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(ATouchOfMadness.MOD_ID, "unsettling"));

    /** The entity types that are company nearby, tamed animals, villagers and allays by default. */
    public static final TagKey<EntityType<?>> COMFORTING = TagKey.create(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(ATouchOfMadness.MOD_ID, "comforting"));

    /** How many times the hostile rate an unsettling entity drains: 1 for a type with no entry. */
    public static final DataMapType<EntityType<?>, Double> UNSETTLING_WEIGHT = DataMapType.builder(
            Identifier.fromNamespaceAndPath(ATouchOfMadness.MOD_ID, "unsettling_weight"), Registries.ENTITY_TYPE,
            Codec.doubleRange(0, Double.MAX_VALUE)).build();

    private static final double DEFAULT_WEIGHT = 1;

    /** The config's rules, swapped whole when it loads or reloads, possibly off the server thread. */
    private static volatile NearbyRules rules = NearbyRules.DEFAULT;

    private NearbySources() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(RegisterDataMapTypesEvent.class, event -> event.register(UNSETTLING_WEIGHT));
        modBus.addListener(ModConfigEvent.Loading.class, NearbySources::configChanged);
        modBus.addListener(ModConfigEvent.Reloading.class, NearbySources::configChanged);
        PlayerSanity.addSource(player -> Rate.of(hostile(player), Cause.HOSTILE));
        PlayerSanity.addSource(player -> Rate.of(company(player), Cause.COMPANY));
    }

    private static double hostile(ServerPlayer player) {
        return unsettlingDrain(player, entity -> !(entity instanceof ShadowCreature), (entity, weight) -> weight);
    }

    /**
     * The drain, in Sanity a tick, of the living entities in {@link #UNSETTLING} within the hostile radius
     * of the player that {@code which} accepts, each by what {@code weigh} makes of its
     * {@link #UNSETTLING_WEIGHT}, the sum capped as the hostile drain's is.
     */
    public static double unsettlingDrain(ServerPlayer player, Predicate<Entity> which, ToDoubleBiFunction<Entity, Double> weigh) {
        var current = rules;
        var weights = near(player, current.hostileRadius(), entity -> entity.typeHolder().is(UNSETTLING) && which.test(entity)).stream()
                .map(entity -> weigh.applyAsDouble(entity, weight(entity)))
                .toList();
        return EnvironmentRules.drainPerTick(current.hostileDrain(weights));
    }

    private static double company(ServerPlayer player) {
        var current = rules;
        boolean anyCompany = !near(player, current.companyRadius(), NearbySources::isCompany).isEmpty();
        return -EnvironmentRules.drainPerTick(current.companyRestore(anyCompany));
    }

    /**
     * The living entities other than the player within {@code radius} of them that {@code which} accepts.
     * Only living ones, so an Ender Dragon's parts, which share its type, don't count it again.
     */
    private static List<Entity> near(ServerPlayer player, double radius, Predicate<Entity> which) {
        return player.level().getEntities(player, player.getBoundingBox().inflate(radius), entity -> entity instanceof LivingEntity
                && entity.isAlive() && entity.distanceToSqr(player) <= radius * radius && which.test(entity));
    }

    private static boolean isCompany(Entity entity) {
        if (entity instanceof ServerPlayer other) {
            return other.gameMode.getGameModeForPlayer().isSurvival();
        }
        if (!entity.typeHolder().is(COMFORTING)) {
            return false;
        }
        return !(entity instanceof OwnableEntity ownable) || ownable.getOwnerReference() != null;
    }

    private static double weight(Entity entity) {
        Double weight = entity.typeHolder().getData(UNSETTLING_WEIGHT);
        return weight == null ? DEFAULT_WEIGHT : weight;
    }

    private static void configChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() == MadnessConfig.SPEC) {
            rules = MadnessConfig.nearbyRules();
        }
    }
}
