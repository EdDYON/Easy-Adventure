package com.eddy1.easyadventure.network;

import com.eddy1.easyadventure.EasyAdventure;
import com.eddy1.easyadventure.block.BaseCoreBlockEntity;
import com.eddy1.easyadventure.block.core.CorePasswordUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpdateCoreSizePayload(
        BlockPos pos,
        int sizeX,
        int sizeY,
        int sizeZ,
        boolean passwordEnabled,
        String password
) implements CustomPacketPayload {
    public static final Type<UpdateCoreSizePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EasyAdventure.MODID, "update_core_size"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateCoreSizePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                BlockPos.STREAM_CODEC.encode(buf, payload.pos);
                buf.writeInt(payload.sizeX);
                buf.writeInt(payload.sizeY);
                buf.writeInt(payload.sizeZ);
                buf.writeBoolean(payload.passwordEnabled);
                buf.writeUtf(payload.password, CorePasswordUtil.MAX_PASSWORD_LENGTH);
            },
            buf -> new UpdateCoreSizePayload(
                    BlockPos.STREAM_CODEC.decode(buf),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readUtf(CorePasswordUtil.MAX_PASSWORD_LENGTH)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateCoreSizePayload payload, IPayloadContext context) {
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
    }
}
