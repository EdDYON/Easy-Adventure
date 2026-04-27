package com.eddy1.easyadventure.network;

import com.eddy1.easyadventure.block.BaseCoreBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SubmitBaseNamePayload(BlockPos pos, String baseName) {
    public static void encode(SubmitBaseNamePayload payload, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(payload.pos);
        buffer.writeUtf(payload.baseName, 64);
    }

    public static SubmitBaseNamePayload decode(FriendlyByteBuf buffer) {
        return new SubmitBaseNamePayload(buffer.readBlockPos(), buffer.readUtf(64));
    }

    public static void handle(SubmitBaseNamePayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            if (player.distanceToSqr(
                    payload.pos().getX() + 0.5,
                    payload.pos().getY() + 0.5,
                    payload.pos().getZ() + 0.5
            ) > 64.0D) {
                return;
            }

            BlockEntity blockEntity = player.level().getBlockEntity(payload.pos());
            if (!(blockEntity instanceof BaseCoreBlockEntity core)) {
                return;
            }
            if (!core.canPlayerManage(player)) {
                player.sendSystemMessage(Component.translatable("message.easyadventure.not_authorized_operation").withStyle(ChatFormatting.RED));
                return;
            }

            core.renameBase(player, payload.baseName());
        });
        context.setPacketHandled(true);
    }
}
