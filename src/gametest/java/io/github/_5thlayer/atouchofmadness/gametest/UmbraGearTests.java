// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.mojang.authlib.GameProfile;
import io.github._5thlayer.atouchofmadness.discovery.Discovery;
import io.github._5thlayer.atouchofmadness.environment.EnvironmentRules;
import io.github._5thlayer.atouchofmadness.sanity.Cause;
import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import io.github._5thlayer.atouchofmadness.sanity.Sanity;
import io.github._5thlayer.atouchofmadness.shadow.ShadowCreatures;
import io.github._5thlayer.atouchofmadness.umbra.UmbraGear;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Umbra Gear: each piece crafts from Umbra and sticks in vanilla's shapes; the Sword hits like netherite
 * at a faster swing, the Pickaxe out-mines netherite, and each piece of armour has diamond's protection
 * and adds speed; Umbra, and whatever a pack adds to {@code #atouchofmadness:repairs_umbra_gear},
 * repairs each on an anvil. Held, the Sword drains 2 a minute, and worn, a piece of armour 1; carried,
 * nothing drains. The drain tests share one fixture, as {@code CrownTests}' do: a Survival player on the
 * platform at noon, where nothing else applies, ticked each tick. The tests are padded, so no player in
 * one is company to a player in another.
 */
final class UmbraGearTests {

    private static final int READ_AT = 2 * PlayerSanity.EVALUATION_TICKS;

    /** Wider than the company radius. */
    private static final int PADDING = 9;

    private static final BlockPos FEET = new BlockPos(1, 1, 4);

    /** A restore of 1 a minute, as a rate in Sanity a tick. */
    private static final double PER_MINUTE = -EnvironmentRules.drainPerTick(1);

    /** Each piece's recipe, as vanilla shapes its netherite or diamond one: U for Umbra, S for a stick. */
    private static final Map<Item, List<String>> RECIPES = Map.of(
            UmbraGear.SWORD.get(), List.of("U", "U", "S"),
            UmbraGear.PICKAXE.get(), List.of("UUU", " S ", " S "),
            UmbraGear.HELMET.get(), List.of("UUU", "U U"),
            UmbraGear.CHESTPLATE.get(), List.of("U U", "UUU", "UUU"),
            UmbraGear.LEGGINGS.get(), List.of("UUU", "U U", "U U"),
            UmbraGear.BOOTS.get(), List.of("U U", "U U"));

    /** Each piece of armour and the diamond piece whose protection it has. */
    private static final Map<Item, Item> DIAMOND = Map.of(
            UmbraGear.HELMET.get(), Items.DIAMOND_HELMET,
            UmbraGear.CHESTPLATE.get(), Items.DIAMOND_CHESTPLATE,
            UmbraGear.LEGGINGS.get(), Items.DIAMOND_LEGGINGS,
            UmbraGear.BOOTS.get(), Items.DIAMOND_BOOTS);

    private UmbraGearTests() {
    }

    static void register(ATouchOfMadnessGameTests.Registrar defaults) {
        var tests = defaults.padded(PADDING);
        int ticks = READ_AT + 1;

        tests.test("umbra_gear_each_piece_crafts_from_umbra", 1, UmbraGearTests::crafted);
        tests.test("umbra_gear_the_sword_hits_like_netherite_and_swings_faster", 1, UmbraGearTests::sword);
        tests.test("umbra_gear_the_pickaxe_out_mines_netherite", 1, UmbraGearTests::pickaxe);
        tests.test("umbra_gear_each_armour_piece_has_diamond_protection_and_adds_speed", 1, UmbraGearTests::armour);

        tests.test("umbra_gear_the_sword_held_drains_two", ticks, helper -> expectRate(helper,
                player -> player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(UmbraGear.SWORD.get())), 2));
        tests.test("umbra_gear_one_armour_piece_worn_drains_one", ticks, helper -> expectRate(helper,
                player -> player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(UmbraGear.CHESTPLATE.get())), 1));
        tests.test("umbra_gear_carried_drains_nothing", ticks, helper -> expectRate(helper, player -> {
            int slot = 9;
            for (var piece : UmbraGear.PIECES) {
                player.getInventory().setItem(slot++, new ItemStack(piece.get()));
            }
        }, 0));

        tests.test("umbra_gear_each_piece_repairs_with_umbra_on_an_anvil", 1, helper -> expectRepairs(helper, ShadowCreatures.UMBRA.get()));
        // The game-test pack adds an echo shard to #atouchofmadness:repairs_umbra_gear.
        tests.test("umbra_gear_a_pack_adds_a_repair_material", 1, helper -> expectRepairs(helper, Items.ECHO_SHARD));

        tests.test("discovery_holding_an_umbra_sword_discovers_umbra_gear", ticks, UmbraGearTests::discovered);
    }

    private static void crafted(GameTestHelper helper) {
        var level = helper.getLevel();
        for (var recipe : RECIPES.entrySet()) {
            var input = input(recipe.getValue());
            var made = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level)
                    .map(found -> found.value().assemble(input))
                    .orElse(ItemStack.EMPTY);
            if (!made.is(recipe.getKey())) {
                helper.fail(recipe.getValue() + " makes " + made + ", not " + recipe.getKey());
                return;
            }
        }
        helper.succeed();
    }

    private static CraftingInput input(List<String> pattern) {
        var items = new ArrayList<ItemStack>();
        for (var row : pattern) {
            for (char key : row.toCharArray()) {
                items.add(switch (key) {
                    case 'U' -> new ItemStack(ShadowCreatures.UMBRA.get());
                    case 'S' -> new ItemStack(Items.STICK);
                    default -> ItemStack.EMPTY;
                });
            }
        }
        return CraftingInput.of(pattern.getFirst().length(), pattern.size(), items);
    }

    private static void sword(GameTestHelper helper) {
        var umbra = new ItemStack(UmbraGear.SWORD.get());
        var netherite = new ItemStack(Items.NETHERITE_SWORD);
        double damage = with(umbra, Attributes.ATTACK_DAMAGE, 1, EquipmentSlot.MAINHAND);
        double netheriteDamage = with(netherite, Attributes.ATTACK_DAMAGE, 1, EquipmentSlot.MAINHAND);
        if (damage != netheriteDamage) {
            helper.fail("the Umbra Sword deals " + damage + ", netherite's " + netheriteDamage);
            return;
        }
        double speed = with(umbra, Attributes.ATTACK_SPEED, 4, EquipmentSlot.MAINHAND);
        double netheriteSpeed = with(netherite, Attributes.ATTACK_SPEED, 4, EquipmentSlot.MAINHAND);
        if (speed <= netheriteSpeed) {
            helper.fail("the Umbra Sword swings at " + speed + ", not faster than netherite's " + netheriteSpeed);
            return;
        }
        helper.succeed();
    }

    private static void pickaxe(GameTestHelper helper) {
        var umbra = new ItemStack(UmbraGear.PICKAXE.get());
        var netherite = new ItemStack(Items.NETHERITE_PICKAXE);
        for (var block : List.of(Blocks.STONE, Blocks.DEEPSLATE, Blocks.OBSIDIAN, Blocks.ANCIENT_DEBRIS)) {
            var state = block.defaultBlockState();
            if (umbra.getDestroySpeed(state) <= netherite.getDestroySpeed(state)) {
                helper.fail("the Umbra Pickaxe mines " + block + " at " + umbra.getDestroySpeed(state) + ", not faster than netherite's "
                        + netherite.getDestroySpeed(state));
                return;
            }
            if (!umbra.isCorrectToolForDrops(state)) {
                helper.fail("the Umbra Pickaxe gets no drops from " + block);
                return;
            }
        }
        helper.succeed();
    }

    private static void armour(GameTestHelper helper) {
        for (var piece : DIAMOND.entrySet()) {
            var umbra = new ItemStack(piece.getKey());
            var diamond = new ItemStack(piece.getValue());
            var slot = umbra.get(DataComponents.EQUIPPABLE).slot();
            for (var attribute : List.of(Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS)) {
                double ours = with(umbra, attribute, 0, slot);
                double theirs = with(diamond, attribute, 0, slot);
                if (ours != theirs) {
                    helper.fail(piece.getKey() + " gives " + ours + " " + attribute.getRegisteredName() + ", diamond's " + theirs);
                    return;
                }
            }
            double speed = with(umbra, Attributes.MOVEMENT_SPEED, 0.1, slot);
            if (speed <= 0.1) {
                helper.fail(piece.getKey() + " takes the movement speed from 0.1 to " + speed);
                return;
            }
        }
        helper.succeed();
    }

    /** The attribute's value from {@code base} with only the stack's modifiers in {@code slot}. */
    private static double with(ItemStack stack, Holder<Attribute> attribute, double base, EquipmentSlot slot) {
        return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).compute(attribute, base, slot);
    }

    /** Each piece, worn half through, comes out of an anvil with {@code material} less worn. */
    private static void expectRepairs(GameTestHelper helper, Item material) {
        for (var piece : UmbraGear.PIECES) {
            var menu = new AnvilMenu(0, helper.makeMockPlayer(GameType.SURVIVAL).getInventory(), ContainerLevelAccess.NULL);
            var worn = new ItemStack(piece.get());
            worn.setDamageValue(worn.getMaxDamage() / 2);
            menu.getSlot(0).set(worn);
            menu.getSlot(1).set(new ItemStack(material));
            var made = menu.getSlot(menu.getResultSlot()).getItem();
            if (!made.is(piece.get()) || made.getDamageValue() >= worn.getDamageValue()) {
                helper.fail(material + " mends a worn " + piece.get() + " into " + made + " with " + made.getDamageValue() + " wear");
                return;
            }
        }
        helper.succeed();
    }

    /** Once an evaluation has come, the Net rate must be {@code -perMinute}, a drain. */
    private static void expectRate(GameTestHelper helper, Consumer<ServerPlayer> equip, double perMinute) {
        using(helper, equip, player -> {
            if (helper.getTick() < READ_AT) {
                return;
            }
            double read = -PlayerSanity.netRate(player) / PER_MINUTE;
            if (Math.abs(read - perMinute) > 1e-9) {
                helper.fail("the Net rate drains " + read + "/min, not " + perMinute);
            }
            helper.succeed();
        });
    }

    private static void discovered(GameTestHelper helper) {
        using(helper, player -> {
            PlayerSanity.set(player, new Sanity(90));
            player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(UmbraGear.SWORD.get()));
            if (Discovery.isKnown(player, Cause.UMBRA_GEAR)) {
                helper.fail("Umbra Gear is known before it moved Sanity");
            }
        }, player -> {
            if (helper.getTick() < READ_AT) {
                return;
            }
            if (!Discovery.isKnown(player, Cause.UMBRA_GEAR)) {
                helper.fail("holding an Umbra Sword moved Sanity, but did not discover Umbra Gear");
            }
            helper.succeed();
        });
    }

    /**
     * The fixture: joins a Survival player at {@link #FEET}, equips them, and each tick ticks them and
     * hands them to {@code check}.
     */
    private static void using(GameTestHelper helper, Consumer<ServerPlayer> equip, Consumer<ServerPlayer> check) {
        GameProfile profile = TestPlayers.profile();
        TestPlayers.whenDone(helper, () -> TestPlayers.leave(helper, profile));
        var joined = TestPlayers.join(helper, profile);
        joined.setGameMode(GameType.SURVIVAL);
        joined.teleport(new TeleportTransition(helper.getLevel(), Vec3.atBottomCenterOf(helper.absolutePos(FEET)), Vec3.ZERO, 0, 0,
                TeleportTransition.DO_NOTHING));
        equip.accept(joined);
        helper.onEachTick(() -> {
            var player = TestPlayers.playerList(helper).getPlayer(profile.id());
            player.doTick();
            check.accept(player);
        });
    }
}
