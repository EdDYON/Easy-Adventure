package com.eddy1.easyadventure.item;

import com.eddy1.easyadventure.world.BaseRegistryData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class BaseArchiveBookItem extends Item {
    private static final int MAX_VISIBLE_RECORDS = 8;

    public BaseArchiveBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        List<BaseRegistryData.BaseRecord> records = BaseRegistryData.get(serverPlayer.serverLevel()).recordsForOwner(serverPlayer.getUUID());
        if (records.isEmpty()) {
            serverPlayer.displayClientMessage(Component.translatable("message.easyadventure.no_registered_bases").withStyle(ChatFormatting.YELLOW), false);
            return InteractionResultHolder.success(stack);
        }

        serverPlayer.displayClientMessage(Component.translatable("message.easyadventure.archive_header", records.size()).withStyle(ChatFormatting.AQUA), false);
        int visible = Math.min(MAX_VISIBLE_RECORDS, records.size());
        for (int i = 0; i < visible; i++) {
            BaseRegistryData.BaseRecord record = records.get(i);
            Component state = Component.translatable("message.easyadventure.registry_state." + record.state());
            Component position = record.pos() == null
                    ? Component.translatable("gui.easyadventure.none")
                    : Component.literal(record.pos().getX() + ", " + record.pos().getY() + ", " + record.pos().getZ());
            String size = record.sizeX() > 0 && record.sizeY() > 0 && record.sizeZ() > 0
                    ? record.sizeX() + "x" + record.sizeY() + "x" + record.sizeZ()
                    : "-";
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.easyadventure.archive_entry",
                    record.baseName(),
                    state,
                    size,
                    record.dimension() == null ? "-" : record.dimension(),
                    position
            ).withStyle(ChatFormatting.GRAY), false);
        }
        if (records.size() > visible) {
            serverPlayer.displayClientMessage(Component.translatable("message.easyadventure.recall_table_more", records.size() - visible).withStyle(ChatFormatting.DARK_GRAY), false);
        }
        return InteractionResultHolder.success(stack);
    }
}
