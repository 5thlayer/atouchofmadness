// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.gametest;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.mojang.authlib.GameProfile;
import io.github._5thlayer.atouchofmadness.crown.CrownRules;
import io.github._5thlayer.atouchofmadness.crown.FlowerCrown;
import io.github._5thlayer.atouchofmadness.environment.EnvironmentRules;
import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * The Flower Crown: crafted from any six flowers, it restores 1.5 a minute while worn in the head slot,
 * and only there, wearing by a second each second it is worn until, after 1 in-game day of wear, it is gone;
 * nothing mends it. The wearing tests share one fixture: a Survival player, since a creative player's
 * crown never wears, stands on the platform at noon, where nothing else applies, with a crown where the
 * test puts it, and is ticked each tick. How the Net rate sums with other sources is {@code SanityTests}'
 * to test. The tests are padded, so no player in one is company to a player in another.
 */
final class CrownTests {

    private static final int READ_AT = 2 * PlayerSanity.EVALUATION_TICKS;

    /** Wider than the company radius. */
    private static final int PADDING = 9;

    private static final BlockPos FEET = new BlockPos(1, 1, 4);

    /** A restore of 1 a minute, as a rate in Sanity a tick. */
    private static final double PER_MINUTE = -EnvironmentRules.drainPerTick(1);

    /** The wear of a crown that has been worn a while, but is far from spent. */
    private static final int WORN = 100;

    private CrownTests() {
    }

    static void register(ATouchOfMadnessGameTests.Registrar defaults) {
        var tests = defaults.padded(PADDING);
        int ticks = READ_AT + 1;

        tests.test("crown_the_recipe_takes_any_six_flowers_mixed", 1, CrownTests::crafted);

        tests.test("crown_worn_restores_one_and_a_half", ticks, helper -> expectRate(helper,
                player -> player.setItemSlot(EquipmentSlot.HEAD, crown(0)), 1.5));
        tests.test("crown_held_restores_nothing_and_does_not_wear", ticks, helper -> expectUnworn(helper,
                (player, crown) -> player.setItemSlot(EquipmentSlot.MAINHAND, crown)));
        tests.test("crown_carried_restores_nothing_and_does_not_wear", ticks, helper -> expectUnworn(helper,
                (player, crown) -> player.getInventory().setItem(9, crown)));

        tests.test("crown_worn_wears_a_second_each_second", ticks, CrownTests::wears);
        tests.test("crown_is_gone_after_a_day_of_wear", ticks, CrownTests::wiltsAway);

        tests.test("crown_cannot_be_repaired_by_crafting", 1, CrownTests::notByCrafting);
        tests.test("crown_cannot_be_repaired_on_a_grindstone", 1, CrownTests::notOnAGrindstone);
        tests.test("crown_cannot_be_repaired_on_an_anvil", 1, CrownTests::notOnAnAnvil);
    }

    private static void crafted(GameTestHelper helper) {
        var flowers = List.of(new ItemStack(Items.POPPY), new ItemStack(Items.DANDELION), new ItemStack(Items.CORNFLOWER),
                new ItemStack(Items.SUNFLOWER), ItemStack.EMPTY, new ItemStack(Items.WITHER_ROSE));
        var made = craft(helper, CraftingInput.of(3, 2, flowers));
        if (!made.is(FlowerCrown.ITEM)) {
            helper.fail("six mixed flowers make " + made);
            return;
        }
        if (made.isDamaged()) {
            helper.fail("a new crown has " + made.getDamageValue() + " wear");
            return;
        }
        helper.succeed();
    }

    private static void wears(GameTestHelper helper) {
        int[] ticked = {0};
        wearing(helper, player -> player.setItemSlot(EquipmentSlot.HEAD, crown(0)), player -> {
            ticked[0]++;
            if (helper.getTick() < READ_AT) {
                return;
            }
            // Wear comes once a second, by the second's ticks.
            int wear = player.getItemBySlot(EquipmentSlot.HEAD).getDamageValue();
            if (wear % PlayerSanity.EVALUATION_TICKS != 0 || wear > ticked[0] || wear <= ticked[0] - PlayerSanity.EVALUATION_TICKS) {
                helper.fail("worn " + ticked[0] + " ticks, the crown has " + wear + " wear");
                return;
            }
            helper.succeed();
        });
    }

    private static void wiltsAway(GameTestHelper helper) {
        int lasting = crown(0).getMaxDamage();
        if (lasting != CrownRules.DEFAULT.wearTicks()) {
            helper.fail("a crown lasts " + lasting + " ticks of wear, not the default's " + CrownRules.DEFAULT.wearTicks());
            return;
        }
        wearing(helper, player -> player.setItemSlot(EquipmentSlot.HEAD, crown(lasting - 1)), player -> {
            if (!player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                return;
            }
            for (var stack : player.getInventory()) {
                if (stack.is(FlowerCrown.ITEM)) {
                    helper.fail("the spent crown went to the inventory");
                }
            }
            helper.succeed();
        });
    }

    private static void notByCrafting(GameTestHelper helper) {
        var helmets = craft(helper, CraftingInput.of(2, 1, List.of(worn(new ItemStack(Items.IRON_HELMET)), worn(new ItemStack(Items.IRON_HELMET)))));
        if (helmets.isEmpty()) {
            helper.fail("two worn iron helmets craft into nothing: the fixture tests nothing");
            return;
        }
        var made = craft(helper, CraftingInput.of(2, 1, List.of(crown(WORN), crown(WORN))));
        if (!made.isEmpty()) {
            helper.fail("two worn crowns craft into " + made + " with " + made.getDamageValue() + " wear");
            return;
        }
        helper.succeed();
    }

    private static void notOnAGrindstone(GameTestHelper helper) {
        var menu = new GrindstoneMenu(0, helper.makeMockPlayer(GameType.SURVIVAL).getInventory());
        expectNoResult(helper, menu, 2, Items.IRON_HELMET);
    }

    private static void notOnAnAnvil(GameTestHelper helper) {
        var menu = new AnvilMenu(0, helper.makeMockPlayer(GameType.SURVIVAL).getInventory(), ContainerLevelAccess.NULL);
        expectNoResult(helper, menu, menu.getResultSlot(), Items.IRON_HELMET);
    }

    /**
     * Puts two worn crowns in the menu's first two slots, and expects nothing in its result slot; first
     * checks that two worn {@code control}s there do make something, so an empty result is the crown's.
     */
    private static void expectNoResult(GameTestHelper helper, AbstractContainerMenu menu, int result, Item control) {
        var mended = place(menu, result, worn(new ItemStack(control)), worn(new ItemStack(control)));
        if (mended.isEmpty()) {
            helper.fail("two worn " + control + " mend into nothing: the fixture tests nothing");
            return;
        }
        var made = place(menu, result, crown(WORN), crown(WORN));
        if (!made.isEmpty()) {
            helper.fail("two worn crowns mend into " + made + " with " + made.getDamageValue() + " wear");
            return;
        }
        helper.succeed();
    }

    private static ItemStack place(AbstractContainerMenu menu, int result, ItemStack first, ItemStack second) {
        menu.getSlot(0).set(first);
        menu.getSlot(1).set(second);
        return menu.getSlot(result).getItem();
    }

    private static ItemStack craft(GameTestHelper helper, CraftingInput input) {
        var level = helper.getLevel();
        return level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level)
                .map(recipe -> recipe.value().assemble(input))
                .orElse(ItemStack.EMPTY);
    }

    private static ItemStack crown(int wear) {
        var crown = new ItemStack(FlowerCrown.ITEM.get());
        crown.setDamageValue(wear);
        return crown;
    }

    private static ItemStack worn(ItemStack stack) {
        stack.setDamageValue(stack.getMaxDamage() / 2);
        return stack;
    }

    /** Once an evaluation has come, the Net rate must be {@code perMinute}. */
    private static void expectRate(GameTestHelper helper, Consumer<ServerPlayer> dress, double perMinute) {
        wearing(helper, dress, player -> {
            if (helper.getTick() < READ_AT) {
                return;
            }
            double read = PlayerSanity.netRate(player) / PER_MINUTE;
            if (Math.abs(read - perMinute) > 1e-9) {
                helper.fail("the Net rate is " + read + "/min, not " + perMinute);
            }
            helper.succeed();
        });
    }

    /** Once an evaluation has come, a worn crown put where {@code put} puts it must restore nothing and keep its wear. */
    private static void expectUnworn(GameTestHelper helper, BiConsumer<ServerPlayer, ItemStack> put) {
        var crown = crown(WORN);
        wearing(helper, player -> put.accept(player, crown), player -> {
            if (helper.getTick() < READ_AT) {
                return;
            }
            if (PlayerSanity.netRate(player) != 0) {
                helper.fail("the Net rate is " + PlayerSanity.netRate(player) / PER_MINUTE + "/min, not 0");
            }
            if (crown.getDamageValue() != WORN) {
                helper.fail("the crown went from " + WORN + " wear to " + crown.getDamageValue());
            }
            helper.succeed();
        });
    }

    /**
     * The fixture: joins a Survival player at {@link #FEET}, dresses them, and each tick ticks them and
     * hands them to {@code check}.
     */
    private static void wearing(GameTestHelper helper, Consumer<ServerPlayer> dress, Consumer<ServerPlayer> check) {
        GameProfile profile = TestPlayers.profile();
        TestPlayers.whenDone(helper, () -> TestPlayers.leave(helper, profile));
        var joined = TestPlayers.join(helper, profile);
        joined.setGameMode(GameType.SURVIVAL);
        joined.teleport(new TeleportTransition(helper.getLevel(), Vec3.atBottomCenterOf(helper.absolutePos(FEET)), Vec3.ZERO, 0, 0,
                TeleportTransition.DO_NOTHING));
        dress.accept(joined);
        helper.onEachTick(() -> {
            var player = TestPlayers.playerList(helper).getPlayer(profile.id());
            player.doTick();
            check.accept(player);
        });
    }
}
