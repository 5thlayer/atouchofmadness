// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.crown;

import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import io.github._5thlayer.atouchofmadness.MadnessConfig;
import io.github._5thlayer.atouchofmadness.sanity.Cause;
import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import io.github._5thlayer.atouchofmadness.sanity.Rate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Flower Crown: headgear with no armour that restores Sanity continuously while worn in the head
 * slot, and nowhere else. It wears only while worn, by each evaluation's ticks, so the restore and the
 * wear move together; the wear is its damage, shown as a durability bar, out of a stack's maximum that
 * is the config's wear time. A crown worn under another wear time is set to it first, keeping the
 * share of its life it has used. Spent, it breaks. Being hit doesn't wear it, a creative player's never
 * wears, and no recipe, grindstone or anvil mends it.
 */
public final class FlowerCrown {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ATouchOfMadness.MOD_ID);

    public static final DeferredItem<Item> ITEM = ITEMS.registerSimpleItem("flower_crown", properties -> properties
            .durability(CrownRules.DEFAULT.wearTicks())
            .setNoCombineRepair()
            .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD)
                    .setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID,
                            Identifier.fromNamespaceAndPath(ATouchOfMadness.MOD_ID, "flower_crown")))
                    .setDamageOnHurt(false)
                    .build()));

    /** The config's rules, swapped whole when it loads or reloads, possibly off the server thread. */
    private static volatile CrownRules rules = CrownRules.DEFAULT;

    private FlowerCrown() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(ModConfigEvent.Loading.class, FlowerCrown::configChanged);
        modBus.addListener(ModConfigEvent.Reloading.class, FlowerCrown::configChanged);
        PlayerSanity.addSource(player -> Rate.restore(worn(player), Cause.FLOWER_CROWN));
        NeoForge.EVENT_BUS.addListener(AnvilUpdateEvent.class, FlowerCrown::anvil);
    }

    /**
     * The restore of a crown on the player's head, if any. Unlike other sources it also wears the crown
     * by the ticks the evaluation stands for, which only the once-a-second evaluation calls it to do.
     */
    private static double worn(ServerPlayer player) {
        var stack = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!stack.is(ITEM)) {
            return 0;
        }
        var current = rules;
        int lasting = current.wearTicks();
        if (stack.getMaxDamage() != lasting) {
            int wear = current.wearOf(stack.getDamageValue(), stack.getMaxDamage());
            stack.set(DataComponents.MAX_DAMAGE, lasting);
            stack.setDamageValue(wear);
        }
        stack.hurtAndBreak(PlayerSanity.EVALUATION_TICKS, player, EquipmentSlot.HEAD);
        return current.restore();
    }

    /**
     * Leaves an anvil empty for a crown with anything beside it: an anvil mends two of the same
     * damageable item whether or not they can be combined elsewhere. Renaming one alone still works.
     */
    private static void anvil(AnvilUpdateEvent event) {
        if (event.getLeft().is(ITEM) && !event.getRight().isEmpty()) {
            event.setCanceled(true);
        }
    }

    private static void configChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() == MadnessConfig.SPEC) {
            rules = MadnessConfig.crownRules();
        }
    }
}
