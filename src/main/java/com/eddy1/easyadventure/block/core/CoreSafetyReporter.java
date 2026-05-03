package com.eddy1.easyadventure.block.core;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class CoreSafetyReporter {
    private CoreSafetyReporter() {
    }

    public static boolean reportSetup(ServerPlayer player, ServerLevel level, BlockPos center, CoreVolume volume) {
        Component sizeProblem = CoreTerritoryGuard.validateSize(player, volume);
        if (sizeProblem != null) {
            CorePreview.show(level, center, volume, true);
            sendHeader(player, center, volume);
            player.displayClientMessage(sizeProblem.copy().withStyle(ChatFormatting.RED), false);
            return false;
        }

        CoreAreaCheckResult result = CorePreflight.checkPacking(level, center, volume, player);
        CorePreview.show(level, center, volume, !result.ok());
        sendHeader(player, center, volume);
        player.displayClientMessage(Component.translatable(
                "message.easyadventure.safety_report_counts",
                result.totalPositions(),
                result.occupiedBlocks(),
                result.blockEntityCount(),
                result.containerCount(),
                result.entityCount(),
                result.blockedBlocks()
        ).withStyle(result.ok() ? ChatFormatting.GRAY : ChatFormatting.YELLOW), false);

        if (result.ok()) {
            player.displayClientMessage(Component.translatable("message.easyadventure.safety_report_ok").withStyle(ChatFormatting.GREEN), false);
            return true;
        }

        Component reason = result.reason() == null
                ? Component.translatable("message.easyadventure.safety_report_unknown")
                : result.reason();
        player.displayClientMessage(reason.copy().withStyle(ChatFormatting.RED), false);
        return false;
    }

    private static void sendHeader(ServerPlayer player, BlockPos center, CoreVolume volume) {
        player.displayClientMessage(Component.translatable(
                "message.easyadventure.safety_report_header",
                center.getX(),
                center.getY(),
                center.getZ(),
                volume.blockWidthX(),
                volume.totalHeight(),
                volume.blockWidthZ(),
                volume.sizeBelowY(),
                volume.blockCount()
        ).withStyle(ChatFormatting.AQUA), false);
    }
}
