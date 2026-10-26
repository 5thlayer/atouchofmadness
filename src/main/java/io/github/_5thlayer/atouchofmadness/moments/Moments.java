// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.moments;

import com.mojang.serialization.Codec;
import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import io.github._5thlayer.atouchofmadness.MadnessConfig;
import io.github._5thlayer.atouchofmadness.sanity.Cause;
import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.StatAwardEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * The instantaneous drains and restores, each one event and one move of Sanity: eating an item moves
 * it by the item's Sanity Modifier, and each slice of a placed cake by the cake item's; waking as the
 * night skips restores {@link MomentRules#sleep()}; and breaking a block in {@code #minecraft:flowers}
 * restores {@link MomentRules#flower()}. A cake slice and a broken flower are read from the stats
 * vanilla awards once each has happened, so a break that a protected area or adventure mode refuses
 * counts for nothing, and neither does one in creative, which awards no stat.
 */
public final class Moments {

    /**
     * An item's Sanity Modifier: how far eating it moves Sanity, negative for a drain. An item with none
     * moves nothing. Synced, so a client shows the server's values, a datapack's overrides included.
     */
    public static final DataMapType<Item, Integer> SANITY_MODIFIER = DataMapType.builder(
            Identifier.fromNamespaceAndPath(ATouchOfMadness.MOD_ID, "sanity_modifier"), Registries.ITEM, Codec.INT)
            .synced(Codec.INT, false).build();

    /** The config's rules, swapped whole when it loads or reloads, possibly off the server thread. */
    private static volatile MomentRules rules = MomentRules.DEFAULT;

    private Moments() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(RegisterDataMapTypesEvent.class, event -> event.register(SANITY_MODIFIER));
        modBus.addListener(ModConfigEvent.Loading.class, Moments::configChanged);
        modBus.addListener(ModConfigEvent.Reloading.class, Moments::configChanged);
        NeoForge.EVENT_BUS.addListener(LivingEntityUseItemEvent.Finish.class, Moments::ate);
        NeoForge.EVENT_BUS.addListener(PlayerWakeUpEvent.class, Moments::wokeUp);
        // A listener that cancels the stat cancels only the count, not the slice eaten or the flower broken.
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, true, StatAwardEvent.class, Moments::awarded);
    }

    private static void ate(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyModifier(player, event.getItem().typeHolder());
        }
    }

    /** Only waking every sleeper as the night skips wakes a player neither at once nor telling the level. */
    private static void wokeUp(PlayerWakeUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.wakeImmediately() && !event.updateLevel()) {
            PlayerSanity.add(player, rules.sleep(), Cause.SLEEP);
        }
    }

    /** A stat is awarded with its new total, and reset with 0; only a rise is a slice eaten or a block broken. */
    private static void awarded(StatAwardEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getValue() <= player.getStats().getValue(event.getStat())) {
            return;
        }
        var stat = event.getStat();
        if (stat.getType() == Stats.CUSTOM && Stats.EAT_CAKE_SLICE.equals(stat.getValue())) {
            applyModifier(player, BuiltInRegistries.ITEM.wrapAsHolder(Items.CAKE));
        } else if (stat.getType() == Stats.BLOCK_MINED && stat.getValue() instanceof Block block
                && block.defaultBlockState().is(BlockTags.FLOWERS)) {
            PlayerSanity.add(player, rules.flower(), Cause.FLOWER);
        }
    }

    private static void applyModifier(ServerPlayer player, Holder<Item> item) {
        Integer modifier = item.getData(SANITY_MODIFIER);
        if (modifier != null && modifier != 0) {
            PlayerSanity.add(player, modifier, Cause.FOOD);
        }
    }

    private static void configChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() == MadnessConfig.SPEC) {
            rules = MadnessConfig.momentRules();
        }
    }
}
