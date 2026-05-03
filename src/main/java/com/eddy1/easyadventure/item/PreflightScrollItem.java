package com.eddy1.easyadventure.item;

import com.eddy1.easyadventure.block.BaseCoreBlockEntity;
import com.eddy1.easyadventure.block.core.CoreSafetyReporter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

public class PreflightScrollItem extends Item {
    public PreflightScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(context.getClickedPos());
        if (!(blockEntity instanceof BaseCoreBlockEntity core)) {
            player.displayClientMessage(Component.translatable("message.easyadventure.preflight_scroll_need_core").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.FAIL;
        }
        if (!core.canPlayerManage(player)) {
            player.displayClientMessage(Component.translatable("message.easyadventure.not_authorized_operation").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        CoreSafetyReporter.reportSetup(player, level, core.getBlockPos(), core.getTerritoryVolume());
        return InteractionResult.SUCCESS;
    }
}
