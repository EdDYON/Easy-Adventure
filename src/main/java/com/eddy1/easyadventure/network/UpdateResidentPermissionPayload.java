package com.eddy1.easyadventure.network;

import com.eddy1.easyadventure.EasyAdventure;
import com.eddy1.easyadventure.block.BaseCoreBlockEntity;
import com.eddy1.easyadventure.block.core.CorePermission;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record UpdateResidentPermissionPayload(
        BlockPos pos,
        UUID residentUuid,
        int permissionId,
        boolean enabled
) implements CustomPacketPayload {
    public static final Type<UpdateResidentPermissionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EasyAdventure.MODID, "update_resident_permission"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateResidentPermissionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                BlockPos.STREAM_CODEC.encode(buf, payload.pos);
                buf.writeUUID(payload.residentUuid);
                buf.writeVarInt(payload.permissionId);
                buf.writeBoolean(payload.enabled);
            },
            buf -> new UpdateResidentPermissionPayload(
                    BlockPos.STREAM_CODEC.decode(buf),
                    buf.readUUID(),
                    buf.readVarInt(),
                    buf.readBoolean()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateResidentPermissionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
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
    }
}
