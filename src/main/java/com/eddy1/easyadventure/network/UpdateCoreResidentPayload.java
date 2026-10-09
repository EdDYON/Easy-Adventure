package com.eddy1.easyadventure.network;

import com.eddy1.easyadventure.block.BaseCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record UpdateCoreResidentPayload(
        BlockPos pos,
        BaseCoreBlockEntity.ResidentAction action,
        String residentValue
) {
    public static void encode(UpdateCoreResidentPayload payload, FriendlyByteBuf buf) {
        buf.writeBlockPos(payload.pos);
        buf.writeVarInt(payload.action.ordinal());
        buf.writeUtf(payload.residentValue, 64);
    }

    public static UpdateCoreResidentPayload decode(FriendlyByteBuf buf) {
        return new UpdateCoreResidentPayload(
                buf.readBlockPos(),
                BaseCoreBlockEntity.ResidentAction.fromId(buf.readVarInt()),
                buf.readUtf(64)
        );
    }

    public static void handle(UpdateCoreResidentPayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer serverPlayer = context.getSender();
            if (serverPlayer == null) {
                return;
            }

            if (serverPlayer.distanceToSqr(
                    payload.pos().getX() + 0.5,
                    payload.pos().getY() + 0.5,
                    payload.pos().getZ() + 0.5
            ) > 64.0D) {
                return;
            }

            BlockEntity blockEntity = serverPlayer.level().getBlockEntity(payload.pos());
            if (blockEntity instanceof BaseCoreBlockEntity core) {
                core.updateResident(serverPlayer, payload.action(), payload.residentValue());
            }
        });
        context.setPacketHandled(true);
    }
}
