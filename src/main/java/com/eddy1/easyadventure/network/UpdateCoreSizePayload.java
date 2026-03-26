package com.eddy1.easyadventure.network;

import com.eddy1.easyadventure.block.BaseCoreBlockEntity;
import com.eddy1.easyadventure.block.core.CorePasswordUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record UpdateCoreSizePayload(
        BlockPos pos,
        int sizeX,
        int sizeY,
        int sizeZ,
        boolean passwordEnabled,
        String password
) {
    public static void encode(UpdateCoreSizePayload payload, FriendlyByteBuf buf) {
        buf.writeBlockPos(payload.pos);
        buf.writeInt(payload.sizeX);
        buf.writeInt(payload.sizeY);
        buf.writeInt(payload.sizeZ);
        buf.writeBoolean(payload.passwordEnabled);
        buf.writeUtf(payload.password, CorePasswordUtil.MAX_PASSWORD_LENGTH);
    }

    public static UpdateCoreSizePayload decode(FriendlyByteBuf buf) {
        return new UpdateCoreSizePayload(
                buf.readBlockPos(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean(),
                buf.readUtf(CorePasswordUtil.MAX_PASSWORD_LENGTH)
        );
    }

    public static void handle(UpdateCoreSizePayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
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
                core.updateSettings(
                        serverPlayer,
                        payload.sizeX(),
                        payload.sizeY(),
                        payload.sizeZ(),
                        payload.passwordEnabled(),
                        payload.password()
                );
            }
        });
        context.setPacketHandled(true);
    }
}
