package com.eddy1.easyadventure.network;

import com.eddy1.easyadventure.block.BaseCoreBlockEntity;
import com.eddy1.easyadventure.block.core.CorePermission;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record UpdateResidentPermissionPayload(
        BlockPos pos,
        UUID residentUuid,
        int permissionId,
        boolean enabled
) {
    public static void encode(UpdateResidentPermissionPayload payload, FriendlyByteBuf buf) {
        buf.writeBlockPos(payload.pos);
        buf.writeUUID(payload.residentUuid);
        buf.writeVarInt(payload.permissionId);
        buf.writeBoolean(payload.enabled);
    }

    public static UpdateResidentPermissionPayload decode(FriendlyByteBuf buf) {
        return new UpdateResidentPermissionPayload(
                buf.readBlockPos(),
                buf.readUUID(),
                buf.readVarInt(),
                buf.readBoolean()
        );
    }

    public static void handle(UpdateResidentPermissionPayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
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
                core.updateResidentPermission(serverPlayer, payload.residentUuid(), CorePermission.fromId(payload.permissionId()), payload.enabled());
            }
        });
        context.setPacketHandled(true);
    }
}
