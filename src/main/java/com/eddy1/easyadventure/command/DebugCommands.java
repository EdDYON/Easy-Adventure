package com.eddy1.easyadventure.command;

import com.eddy1.easyadventure.util.DebugGuestMode;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class DebugCommands {
    private DebugCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("easyadventure")
                .then(Commands.literal("debug_guest")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> showStatus(context.getSource()))
                        .then(Commands.literal("on")
                                .executes(context -> setState(context.getSource(), true)))
                        .then(Commands.literal("off")
                                .executes(context -> setState(context.getSource(), false)))));
    }

    private static int showStatus(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }

        boolean enabled = DebugGuestMode.isEnabled(player);
        source.sendSuccess(() -> Component.translatable(
                enabled ? "command.easyadventure.debug_guest_enabled" : "command.easyadventure.debug_guest_disabled"
        ), false);
        return 1;
    }

    private static int setState(CommandSourceStack source, boolean enabled) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }

        DebugGuestMode.setEnabled(player, enabled);
        source.sendSuccess(() -> Component.translatable(
                enabled ? "command.easyadventure.debug_guest_enabled" : "command.easyadventure.debug_guest_disabled"
        ), false);
        return 1;
    }
}
