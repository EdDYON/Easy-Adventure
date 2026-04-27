package com.eddy1.easyadventure.command;

import com.eddy1.easyadventure.block.BaseCoreBlockEntity;
import com.eddy1.easyadventure.world.BaseRecallService;
import com.eddy1.easyadventure.world.BaseRegistryData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.UUID;

public final class EasyAdventureCommands {
    private EasyAdventureCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("easyadventure")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("admin")
                        .then(Commands.literal("list_bases")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(EasyAdventureCommands::listBases)))
                        .then(Commands.literal("recover_key")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("core_uuid", UuidArgument.uuid())
                                                .executes(EasyAdventureCommands::recoverKey))))
                        .then(Commands.literal("recover_packed")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("core_uuid", UuidArgument.uuid())
                                                .then(Commands.argument("base_name", StringArgumentType.greedyString())
                                                        .executes(EasyAdventureCommands::recoverPacked)))))
                        .then(Commands.literal("claim_placed")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .then(Commands.argument("base_name", StringArgumentType.greedyString())
                                                        .executes(EasyAdventureCommands::claimPlaced)))))));
    }

    private static int listBases(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        List<BaseRegistryData.BaseRecord> records = BaseRegistryData.get(player.serverLevel()).recordsForOwner(player.getUUID());
        if (records.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("command.easyadventure.list_bases.empty", player.getGameProfile().getName()));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.translatable("command.easyadventure.list_bases.header", player.getGameProfile().getName(), records.size()), false);
        for (BaseRegistryData.BaseRecord record : records) {
            context.getSource().sendSuccess(() -> Component.translatable(
                    "command.easyadventure.list_bases.entry",
                    record.baseName(),
                    record.coreUuid(),
                    record.state()
            ), false);
        }
        return records.size();
    }

    private static int recoverKey(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        UUID coreUuid = UuidArgument.getUuid(context, "core_uuid");
        if (!BaseRecallService.operatorRecallRegistered(player, coreUuid)) {
            context.getSource().sendFailure(Component.translatable("command.easyadventure.recover_key.missing", coreUuid));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.translatable("command.easyadventure.recover_key.success", player.getGameProfile().getName(), coreUuid), true);
        return 1;
    }

    private static int recoverPacked(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        UUID coreUuid = UuidArgument.getUuid(context, "core_uuid");
        String baseName = StringArgumentType.getString(context, "base_name");
        if (!BaseRecallService.operatorRecoverPacked(player, coreUuid, baseName)) {
            context.getSource().sendFailure(Component.translatable("command.easyadventure.recover_packed.failed", coreUuid));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.translatable("command.easyadventure.recover_packed.success", player.getGameProfile().getName(), baseName, coreUuid), true);
        return 1;
    }

    private static int claimPlaced(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        ServerLevel level = context.getSource().getLevel();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        String baseName = StringArgumentType.getString(context, "base_name");
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof BaseCoreBlockEntity core)) {
            context.getSource().sendFailure(Component.translatable("command.easyadventure.claim_placed.not_core", pos.getX(), pos.getY(), pos.getZ()));
            return 0;
        }
        if (!BaseRecallService.operatorClaimPlaced(player, core, pos, baseName)) {
            context.getSource().sendFailure(Component.translatable("command.easyadventure.claim_placed.failed", baseName));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.translatable("command.easyadventure.claim_placed.success", player.getGameProfile().getName(), baseName, pos.getX(), pos.getY(), pos.getZ()), true);
        return 1;
    }
}
