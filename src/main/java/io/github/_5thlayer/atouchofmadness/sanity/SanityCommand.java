// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.sanity;

import java.util.Locale;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /sanity get|set|add <player> [value]}, for pack authors and tests to read and move a player's
 * Sanity, at permission level 2. A value is held to 0–100. Each returns the player's Sanity after it,
 * rounded down, for {@code execute store}.
 */
@EventBusSubscriber(modid = ATouchOfMadness.MOD_ID)
public final class SanityCommand {

    private static final String PLAYER = "player";
    private static final String VALUE = "value";

    private SanityCommand() {
    }

    @SubscribeEvent
    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("sanity")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("get")
                        .then(Commands.argument(PLAYER, EntityArgument.player())
                                .executes(SanityCommand::get)))
                .then(Commands.literal("set")
                        .then(Commands.argument(PLAYER, EntityArgument.player())
                                .then(Commands.argument(VALUE, DoubleArgumentType.doubleArg())
                                        .executes(SanityCommand::set))))
                .then(Commands.literal("add")
                        .then(Commands.argument(PLAYER, EntityArgument.player())
                                .then(Commands.argument(VALUE, DoubleArgumentType.doubleArg())
                                        .executes(SanityCommand::add)))));
    }

    private static int get(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var player = EntityArgument.getPlayer(context, PLAYER);
        return report(context, player, "get");
    }

    private static int set(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var player = EntityArgument.getPlayer(context, PLAYER);
        PlayerSanity.set(player, new Sanity(DoubleArgumentType.getDouble(context, VALUE)));
        return report(context, player, "set");
    }

    private static int add(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var player = EntityArgument.getPlayer(context, PLAYER);
        PlayerSanity.add(player, DoubleArgumentType.getDouble(context, VALUE));
        return report(context, player, "add");
    }

    private static int report(CommandContext<CommandSourceStack> context, ServerPlayer player, String action) {
        var sanity = PlayerSanity.get(player);
        var stage = Component.translatable(ATouchOfMadness.MOD_ID + ".stage." + PlayerSanity.stage(player).name().toLowerCase(Locale.ROOT));
        var value = String.format(Locale.ROOT, "%.1f", sanity.value());
        context.getSource().sendSuccess(() -> Component.translatable(
                "commands." + ATouchOfMadness.MOD_ID + ".sanity." + action, player.getDisplayName(), value, stage), !action.equals("get"));
        return (int) Math.floor(sanity.value());
    }
}
