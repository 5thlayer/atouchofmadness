// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.umbra;

import java.util.List;
import java.util.function.Supplier;

import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import io.github._5thlayer.atouchofmadness.MadnessConfig;
import io.github._5thlayer.atouchofmadness.sanity.Cause;
import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import io.github._5thlayer.atouchofmadness.sanity.Rate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Umbra Gear: the Umbra Sword, the Umbra Pickaxe and four pieces of Umbra Armour, each the best in its
 * role and each a drain while in use, as its own source, {@code umbra_gear}. The Sword deals netherite's
 * damage at a faster swing and the Pickaxe out-mines netherite at its tier; each piece of armour has
 * diamond's protection and adds a little movement speed. A tool drains while held in either hand, a piece
 * of armour while worn, and nothing drains carried. Anything in {@link #REPAIR_MATERIALS}, Umbra by
 * default, repairs them.
 */
public final class UmbraGear {

    /** What repairs Umbra Gear on an anvil: Umbra by default. */
    public static final TagKey<Item> REPAIR_MATERIALS = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(ATouchOfMadness.MOD_ID, "repairs_umbra_gear"));

    /** Netherite's durability, tier and damage, mining faster than its 9. */
    private static final ToolMaterial TOOLS = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, ToolMaterial.NETHERITE.durability(),
            12.0F, ToolMaterial.NETHERITE.attackDamageBonus(), ToolMaterial.NETHERITE.enchantmentValue(), REPAIR_MATERIALS);

    /** Diamond's durability, protection and toughness, drawn as Umbra's own. */
    private static final ArmorMaterial ARMOUR = new ArmorMaterial(ArmorMaterials.DIAMOND.durability(), ArmorMaterials.DIAMOND.defense(),
            ArmorMaterials.DIAMOND.enchantmentValue(), SoundEvents.ARMOR_EQUIP_NETHERITE, ArmorMaterials.DIAMOND.toughness(),
            ArmorMaterials.DIAMOND.knockbackResistance(), REPAIR_MATERIALS,
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(ATouchOfMadness.MOD_ID, "umbra")));

    /** The movement speed each piece of armour adds, a share of the base. */
    private static final double SPEED_PER_PIECE = 0.05;

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ATouchOfMadness.MOD_ID);

    /** Netherite's damage, swinging at 2 a second to its 1.6. */
    public static final DeferredItem<Item> SWORD = ITEMS.registerSimpleItem("umbra_sword",
            properties -> properties.sword(TOOLS, 3.0F, -2.0F));
    /** Netherite's damage and swing, mining at 12 to its 9. */
    public static final DeferredItem<Item> PICKAXE = ITEMS.registerSimpleItem("umbra_pickaxe",
            properties -> properties.pickaxe(TOOLS, 1.0F, -2.8F));
    public static final DeferredItem<Item> HELMET = armour("umbra_helmet", ArmorType.HELMET);
    public static final DeferredItem<Item> CHESTPLATE = armour("umbra_chestplate", ArmorType.CHESTPLATE);
    public static final DeferredItem<Item> LEGGINGS = armour("umbra_leggings", ArmorType.LEGGINGS);
    public static final DeferredItem<Item> BOOTS = armour("umbra_boots", ArmorType.BOOTS);

    private static final List<DeferredItem<Item>> ARMOUR_PIECES = List.of(HELMET, CHESTPLATE, LEGGINGS, BOOTS);

    /** Every piece, in the order the creative tab shows them. */
    public static final List<DeferredItem<Item>> PIECES = List.of(SWORD, PICKAXE, HELMET, CHESTPLATE, LEGGINGS, BOOTS);

    private static final List<EquipmentSlot> HANDS = List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND);

    /** The config's rules. */
    private static final Supplier<UmbraRules> RULES = MadnessConfig.live(MadnessConfig::umbraRules);

    private UmbraGear() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        PlayerSanity.addSource(player -> Rate.drain(inUse(player), Cause.UMBRA_GEAR));
    }

    private static DeferredItem<Item> armour(String name, ArmorType type) {
        var slots = EquipmentSlotGroup.bySlot(type.getSlot());
        var speed = new AttributeModifier(Identifier.fromNamespaceAndPath(ATouchOfMadness.MOD_ID, "umbra_armour." + type.getName()),
                SPEED_PER_PIECE, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        return ITEMS.registerSimpleItem(name, properties -> properties.humanoidArmor(ARMOUR, type)
                .attributes(ARMOUR.createAttributes(type).withModifierAdded(Attributes.MOVEMENT_SPEED, speed, slots)));
    }

    /** The drain of the player's Umbra Gear in use, in Sanity a minute. */
    private static double inUse(ServerPlayer player) {
        int swords = 0;
        int pickaxes = 0;
        for (var hand : HANDS) {
            var held = player.getItemBySlot(hand);
            swords += held.is(SWORD) ? 1 : 0;
            pickaxes += held.is(PICKAXE) ? 1 : 0;
        }
        int armour = 0;
        for (var slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                var worn = player.getItemBySlot(slot);
                armour += ARMOUR_PIECES.stream().anyMatch(worn::is) ? 1 : 0;
            }
        }
        return RULES.get().drain(swords, pickaxes, armour);
    }
}
