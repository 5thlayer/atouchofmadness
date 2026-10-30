// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.gametest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.mojang.authlib.GameProfile;
import io.github._5thlayer.atouchof.signal.Strike;
import io.github._5thlayer.atouchofmadness.crown.FlowerCrown;
import io.github._5thlayer.atouchofmadness.discovery.Discoverables;
import io.github._5thlayer.atouchofmadness.discovery.Discovery;
import io.github._5thlayer.atouchofmadness.sanity.Cause;
import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import io.github._5thlayer.atouchofmadness.sanity.Sanity;
import io.github._5thlayer.atouchofmadness.sanity.Stage;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.gametest.framework.TestEnvironmentDefinition.Weather;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Discovery (ADR 0006): each discoverable thing's advancement, at {@code atouchofmadness:discovery/<id>},
 * is done the first time it moves the player's Sanity, or for a Stage the first time they reach it, and
 * being done is the discovery. The root is granted on join; each child is hidden until done, toasts, and
 * keeps out of chat. A continuous source moves Sanity only while it is not already at the end the source
 * pushes toward, whatever the others do. A pack, {@code gametest_packs/discovery}, marks Wet known from
 * the start, its advancement firing on {@code minecraft:tick}, and removes Freezing's, which then counts
 * as known. Each test joins players of its own, ticks them itself, and logs them out however it ends.
 */
final class DiscoveryTests {

    /** What the server last told each player's client is known. */
    private static final Map<UUID, Set<String>> TOLD = new ConcurrentHashMap<>();

    /** The platform is 9 by 9 with its floor at y 0; a player stands on it at y 1. */
    private static final BlockPos MIDDLE = new BlockPos(4, 1, 4);
    private static final BlockPos CORNER = new BlockPos(2, 1, 2);

    /** Two evaluations, so one has come whichever tick of a second is the player's own. */
    private static final int EVALUATED = 2 * PlayerSanity.EVALUATION_TICKS;

    /** Wider than the company radius. */
    private static final int PADDING = 9;

    static {
        Discovery.onTell((player, known) -> TOLD.put(player.getUUID(), known));
    }

    private DiscoveryTests() {
    }

    static void register(ATouchOfMadnessGameTests.Registrar tests) {
        tests.test("discovery_the_root_is_granted_on_join_and_nothing_else_is_known", 20, DiscoveryTests::root);
        tests.test("discovery_each_child_is_hidden_toasts_and_keeps_out_of_chat", 1, DiscoveryTests::display);
        tests.test("discovery_each_cause_grants_its_advancement_when_it_moves_sanity", 1, DiscoveryTests::eachCause);
        tests.test("discovery_a_moment_that_moves_nothing_discovers_nothing", 1, DiscoveryTests::momentAtFull);
        tests.test("discovery_the_first_strike_that_moves_sanity_discovers_struck_by_the_dark", 1, DiscoveryTests::struck);
        tests.test("discovery_eating_a_golden_apple_discovers_food", 100, DiscoveryTests::eating);
        tests.test("discovery_low_light_is_discovered_by_each_player_it_moves_and_not_at_empty", EVALUATED + 1, DiscoveryTests::lowLight);
        tests.test("discovery_a_restore_at_full_discovers_nothing_while_a_drain_moves_sanity", PlayerSanity.EVALUATION_TICKS + 1, DiscoveryTests::restoreAtFull);
        tests.test("discovery_jumping_to_insane_reaches_uneasy_too", 1, DiscoveryTests::stages);
        tests.test("discovery_a_stage_is_reached_on_join", 1, DiscoveryTests::stageOnJoin);
        tests.test("discovery_the_client_is_told_what_is_known_on_login_and_on_each_discovery", 1, DiscoveryTests::told);
        // Padded, so the villager is company to no other test's player.
        tests.padded(PADDING).test("discovery_a_drain_at_empty_discovers_nothing_while_a_restore_moves_sanity", EVALUATED + 1,
                DiscoveryTests::drainAtEmpty);

        // The pack is on for every test in its batch.
        tests.under("discovery_pack", new TestEnvironmentDefinition.AllOf(new Sky(Sky.NOON, Weather.Type.CLEAR), new WithTestPack("discovery")))
                .test("discovery_a_pack_marks_one_known_from_the_start_and_removes_another", 20, DiscoveryTests::pack);
        // A jukebox reaches further than tests are spaced apart, as in MusicTests.
        tests.under("discovery_music", new Sky(Sky.NOON, Weather.Type.CLEAR))
                .test("discovery_a_stale_play_discovers_music_and_the_stale_disc", 2 * EVALUATED + 1, DiscoveryTests::stalePlay);
    }

    private static void root(GameTestHelper helper) {
        var player = join(helper, MIDDLE, 100);
        for (var cause : Cause.values()) {
            if (Discovery.isKnown(player, cause)) {
                helper.fail(cause.id() + " is known to a new player");
            }
        }
        if (Discovery.isKnown(player, Stage.UNEASY) || Discovery.isKnown(player, Stage.INSANE)) {
            helper.fail("a stage below Sane is known to a new player");
        }
        // Granted by vanilla's tick trigger, on the player's first tick.
        helper.onEachTick(() -> {
            if (done(player, advancement(helper, Discovery.ROOT))) {
                helper.succeed();
            }
        });
    }

    private static void display(GameTestHelper helper) {
        var root = advancement(helper, Discovery.ROOT);
        var rootDisplay = root.value().display().orElseThrow();
        if (rootDisplay.isHidden() || rootDisplay.shouldShowToast() || rootDisplay.shouldAnnounceChat() || rootDisplay.getBackground().isEmpty()) {
            helper.fail("the root is hidden, toasts, announces or has no background");
        }
        for (var id : Discoverables.IDS) {
            var advancement = advancement(helper, Discovery.advancementId(id));
            var display = advancement.value().display().orElseThrow();
            if (!display.isHidden() || !display.shouldShowToast() || display.shouldAnnounceChat()) {
                helper.fail(id + " is not hidden, toasting and kept out of chat");
            }
            var parent = switch (id) {
                case "stale_disc" -> "music";
                case "insane" -> "uneasy";
                case "shadow_strike", "shadow_kill" -> "shadow_creature";
                default -> "root";
            };
            if (!advancement.value().parent().equals(Optional.of(Discovery.advancementId(parent)))) {
                helper.fail(id + " sits under " + advancement.value().parent() + ", not " + parent);
            }
        }
        helper.succeed();
    }

    /** Each Cause moved as a Moment would move it: the wiring from Cause to its advancement, for all of them. */
    private static void eachCause(GameTestHelper helper) {
        var player = join(helper, MIDDLE, 90);
        for (var cause : Cause.values()) {
            var advancement = advancement(helper, Discovery.advancementId(cause.id()));
            if (done(player, advancement)) {
                helper.fail(cause.id() + " is done before it moved Sanity");
            }
            PlayerSanity.add(player, -1, cause);
            if (!done(player, advancement) || !Discovery.isKnown(player, cause)) {
                helper.fail(cause.id() + " is not discovered once it moved Sanity");
            }
        }
        helper.succeed();
    }

    private static void momentAtFull(GameTestHelper helper) {
        var player = join(helper, MIDDLE, 100);
        PlayerSanity.add(player, 10, Cause.FOOD);
        if (Discovery.isKnown(player, Cause.FOOD)) {
            helper.fail("a food that restored nothing at full Sanity discovered food");
        }
        helper.succeed();
    }

    /**
     * The Runtime's Strike signal, raised at 0 Sanity, moves nothing and discovers nothing; raised at 50,
     * it discovers the Strike.
     */
    private static void struck(GameTestHelper helper) {
        var player = join(helper, MIDDLE, 0);
        Strike.raise(player);
        if (Discovery.isKnown(player, Cause.STRUCK_BY_THE_DARK)) {
            helper.fail("a Strike at 0 Sanity, which moved nothing, discovered struck_by_the_dark");
        }
        PlayerSanity.set(player, new Sanity(50));
        Strike.raise(player);
        if (!done(player, advancement(helper, Discovery.advancementId(Cause.STRUCK_BY_THE_DARK.id())))) {
            helper.fail("a Strike that moved Sanity did not discover struck_by_the_dark");
        }
        helper.succeed();
    }

    private static void eating(GameTestHelper helper) {
        var player = join(helper, MIDDLE, 50);
        player.getFoodData().setFoodLevel(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLDEN_APPLE));
        player.gameMode.useItem(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
        if (!player.isUsingItem()) {
            helper.fail("the player did not start eating");
        }
        helper.onEachTick(() -> {
            player.doTick();
            if (player.isUsingItem()) {
                return;
            }
            if (!Discovery.isKnown(player, Cause.FOOD)) {
                helper.fail("eating a golden apple at 50 did not discover food");
            }
            helper.succeed();
        });
    }

    /** Two players in the same unlit room: Low light moves the one at 50, and not the one already at 0. */
    private static void lowLight(GameTestHelper helper) {
        EnvironmentTests.closedRoom(helper);
        var moved = join(helper, MIDDLE, 50);
        var empty = join(helper, CORNER, 0);
        afterEvaluations(helper, List.of(moved, empty), () -> {
            if (!Discovery.isKnown(moved, Cause.LOW_LIGHT)) {
                helper.fail("Low light moved a player's Sanity and was not discovered");
            }
            if (Discovery.isKnown(empty, Cause.LOW_LIGHT)) {
                helper.fail("Low light was discovered by a player at 0, whose Sanity it could not move");
            }
        });
    }

    /**
     * At 100 in an unlit room, a Flower Crown worn: the first evaluation drains, and the crown restores
     * nothing. Read right after it, since from below 100 the crown moves Sanity at the next.
     */
    private static void restoreAtFull(GameTestHelper helper) {
        EnvironmentTests.closedRoom(helper);
        var player = join(helper, MIDDLE, 100);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(FlowerCrown.ITEM.get()));
        helper.onEachTick(() -> {
            player.doTick();
            if (PlayerSanity.get(player).value() >= Sanity.MAX) {
                return;
            }
            // Low light's 6 less the crown's 1.5, so the crown's restore applied.
            expectNetRate(helper, player, -4.5);
            if (!Discovery.isKnown(player, Cause.LOW_LIGHT)) {
                helper.fail("Low light moved Sanity and was not discovered");
            }
            if (Discovery.isKnown(player, Cause.FLOWER_CROWN)) {
                helper.fail("the Flower Crown was discovered at full Sanity, while a drain moved it");
            }
            helper.succeed();
        });
    }

    private static void stages(GameTestHelper helper) {
        var uneasy = join(helper, MIDDLE, 40);
        if (!Discovery.isKnown(uneasy, Stage.UNEASY) || Discovery.isKnown(uneasy, Stage.INSANE)) {
            helper.fail("a player at 40 has not reached Uneasy alone");
        }
        var insane = join(helper, CORNER, 10);
        if (!Discovery.isKnown(insane, Stage.UNEASY) || !Discovery.isKnown(insane, Stage.INSANE)) {
            helper.fail("a player who jumped from Sane to Insane has not reached both");
        }
        helper.succeed();
    }

    /** A player who left Uneasy without its advancement reaches it again as they join. */
    private static void stageOnJoin(GameTestHelper helper) {
        var profile = TestPlayers.profile();
        TestPlayers.whenDone(helper, () -> TestPlayers.leave(helper, profile));
        var player = TestPlayers.join(helper, profile);
        PlayerSanity.set(player, new Sanity(40));
        var uneasy = advancement(helper, Discovery.advancementId("uneasy"));
        uneasy.value().criteria().keySet().forEach(criterion -> player.getAdvancements().revoke(uneasy, criterion));
        if (Discovery.isKnown(player, Stage.UNEASY)) {
            helper.fail("Uneasy is still known once revoked: the fixture tests nothing");
        }
        TestPlayers.leave(helper, profile);
        var back = TestPlayers.join(helper, profile);
        if (!Discovery.isKnown(back, Stage.UNEASY)) {
            helper.fail("a player who joined Uneasy has not reached it");
        }
        helper.succeed();
    }

    /**
     * In water by day, Company near: Wet's drain is outweighed, so Sanity rises from 0, and Company is
     * discovered while Wet, which could not move Sanity below 0, is not.
     */
    private static void drainAtEmpty(GameTestHelper helper) {
        for (var pos : BlockPos.betweenClosed(MIDDLE.offset(-1, 0, -1), MIDDLE.offset(1, 0, 1))) {
            helper.setBlock(pos, Blocks.STONE);
        }
        helper.setBlock(MIDDLE, Blocks.WATER);
        helper.spawn(EntityType.VILLAGER, CORNER.west(2));
        var player = join(helper, MIDDLE, 0);
        helper.onEachTick(() -> {
            player.doTick();
            if (PlayerSanity.get(player).value() <= Sanity.MIN) {
                return;
            }
            // Company's 2 less Wet's 1.5, so Wet's drain applied.
            expectNetRate(helper, player, 0.5);
            if (!Discovery.isKnown(player, Cause.COMPANY)) {
                helper.fail("Company moved Sanity and was not discovered");
            }
            if (Discovery.isKnown(player, Cause.WET)) {
                helper.fail("Wet was discovered at 0, while Company moved Sanity");
            }
            helper.succeed();
        });
    }

    /** Wet, overridden to fire on {@code minecraft:tick}, is known after a new player's first tick; Freezing's, removed, is known at once. */
    private static void pack(GameTestHelper helper) {
        if (helper.getLevel().getServer().getAdvancements().get(Discovery.advancementId(Cause.FREEZING.id())) != null) {
            helper.fail("the pack did not remove Freezing's advancement");
        }
        var player = join(helper, MIDDLE, 100);
        if (!Discovery.isKnown(player, Cause.FREEZING)) {
            helper.fail("a Cause whose advancement is missing is not known");
        }
        helper.onEachTick(() -> {
            if (Discovery.isKnown(player, Cause.WET)) {
                helper.succeed();
            }
        });
    }

    /** Told on login, and again once Low light moves Sanity. */
    private static void told(GameTestHelper helper) {
        var player = join(helper, MIDDLE, 50);
        expectTold(helper, player, "on login");
        PlayerSanity.add(player, -1, Cause.LOW_LIGHT);
        if (!TOLD.getOrDefault(player.getUUID(), Set.of()).contains(Cause.LOW_LIGHT.id())) {
            helper.fail("the client was not told of Low light's discovery");
        }
        expectTold(helper, player, "after a discovery");
        helper.succeed();
    }

    /**
     * Cat plays twice from the jukebox beside the player: the first, Fresh, play at full Sanity
     * discovers nothing; the second, Stale, at 50 discovers music and the Stale disc both.
     */
    private static void stalePlay(GameTestHelper helper) {
        var jukeboxAt = MIDDLE.east(2);
        TestPlayers.whenDone(helper, () -> helper.setBlock(jukeboxAt, Blocks.AIR));
        helper.setBlock(jukeboxAt, Blocks.JUKEBOX);
        var player = join(helper, MIDDLE, 100);
        Consumer<GameTestHelper> play = h -> {
            var jukebox = h.getBlockEntity(jukeboxAt, JukeboxBlockEntity.class);
            jukebox.setTheItem(ItemStack.EMPTY);
            jukebox.setTheItem(new ItemStack(Items.MUSIC_DISC_CAT));
        };
        play.accept(helper);
        helper.onEachTick(() -> {
            player.doTick();
            long tick = helper.getTick();
            if (tick == EVALUATED) {
                if (Discovery.isKnown(player, Cause.MUSIC)) {
                    helper.fail("a play at full Sanity discovered music");
                }
                PlayerSanity.set(player, new Sanity(50));
                play.accept(helper);
            } else if (tick == 2 * EVALUATED) {
                if (!Discovery.isKnown(player, Cause.MUSIC) || !Discovery.isKnown(player, Cause.STALE_DISC)) {
                    helper.fail("a Stale play that moved Sanity did not discover both music and the Stale disc");
                }
                helper.succeed();
            }
        });
    }

    /** Ticks the players each tick, and once two evaluations have come checks, then succeeds. */
    private static void afterEvaluations(GameTestHelper helper, List<ServerPlayer> players, Runnable check) {
        helper.onEachTick(() -> {
            players.forEach(ServerPlayer::doTick);
            if (helper.getTick() >= EVALUATED) {
                check.run();
                helper.succeed();
            }
        });
    }

    private static void expectNetRate(GameTestHelper helper, ServerPlayer player, double perMinute) {
        double read = PlayerSanity.netRate(player);
        if (Math.abs(read - perMinute) > 1e-9) {
            helper.fail("the Net rate is " + read + "/min, not " + perMinute + ": the fixture tests nothing");
        }
    }

    private static void expectTold(GameTestHelper helper, ServerPlayer player, String when) {
        var told = TOLD.get(player.getUUID());
        if (!Discovery.known(player).equals(told)) {
            helper.fail(when + " the client was told " + told + ", not " + Discovery.known(player));
        }
    }

    /** Joins a player at {@code feet}, logged out however the test ends, with this Sanity. */
    private static ServerPlayer join(GameTestHelper helper, BlockPos feet, double sanity) {
        GameProfile profile = TestPlayers.profile();
        TestPlayers.whenDone(helper, () -> {
            TestPlayers.leave(helper, profile);
            TOLD.remove(profile.id());
        });
        var player = TestPlayers.join(helper, profile);
        player.teleport(new TeleportTransition(helper.getLevel(), Vec3.atBottomCenterOf(helper.absolutePos(feet)), Vec3.ZERO, 0, 0,
                TeleportTransition.DO_NOTHING));
        PlayerSanity.set(player, new Sanity(sanity));
        return player;
    }

    private static AdvancementHolder advancement(GameTestHelper helper, Identifier id) {
        var advancement = helper.getLevel().getServer().getAdvancements().get(id);
        if (advancement == null) {
            throw helper.assertionException(Component.literal("no advancement " + id));
        }
        return advancement;
    }

    private static boolean done(ServerPlayer player, AdvancementHolder advancement) {
        return player.getAdvancements().getOrStartProgress(advancement).isDone();
    }
}
