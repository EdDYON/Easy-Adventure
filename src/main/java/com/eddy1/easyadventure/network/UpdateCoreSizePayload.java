package com.eddy1.easyadventure.network;

import com.eddy1.easyadventure.block.BaseCoreBlockEntity;
import com.eddy1.easyadventure.block.core.CoreClearMode;
import com.eddy1.easyadventure.block.core.CoreFoundationMaterial;
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
        int sizeBelowY,
        int sizeZ,
        String baseName,
        CoreClearMode clearMode,
        boolean foundationEnabled,
        CoreFoundationMaterial foundationMaterial,
        boolean passwordEnabled,
        String password
) {
    public static void encode(UpdateCoreSizePayload payload, FriendlyByteBuf buf) {
        buf.writeBlockPos(payload.pos);
        buf.writeInt(payload.sizeX);
        buf.writeInt(payload.sizeY);
        buf.writeInt(payload.sizeBelowY);
        buf.writeInt(payload.sizeZ);
        buf.writeUtf(payload.baseName, 64);
        buf.writeEnum(payload.clearMode);
        buf.writeBoolean(payload.foundationEnabled);
        buf.writeEnum(payload.foundationMaterial);
        buf.writeBoolean(payload.passwordEnabled);
        buf.writeUtf(payload.password, CorePasswordUtil.MAX_PASSWORD_LENGTH);
    }

    public static UpdateCoreSizePayload decode(FriendlyByteBuf buf) {
        return new UpdateCoreSizePayload(
                buf.readBlockPos(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readUtf(64),
                buf.readEnum(CoreClearMode.class),
                buf.readBoolean(),
                buf.readEnum(CoreFoundationMaterial.class),
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
                        payload.sizeBelowY(),
                        payload.sizeZ(),
                        payload.baseName(),
                        payload.clearMode(),
                        payload.foundationEnabled(),
                        payload.foundationMaterial(),
                        payload.passwordEnabled(),
                        payload.password()
                );
            }
        });
        context.setPacketHandled(true);
    }
}
