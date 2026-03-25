package com.eddy1.easyadventure.network;

import com.eddy1.easyadventure.EasyAdventure;
import com.eddy1.easyadventure.block.BaseCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpdateCoreResidentPayload(
        BlockPos pos,
        BaseCoreBlockEntity.ResidentAction action,
        String residentValue
) implements CustomPacketPayload {
    public static final Type<UpdateCoreResidentPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EasyAdventure.MODID, "update_core_resident"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateCoreResidentPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                BlockPos.STREAM_CODEC.encode(buf, payload.pos);
                buf.writeVarInt(payload.action.ordinal());
                buf.writeUtf(payload.residentValue, 64);
            },
            buf -> new UpdateCoreResidentPayload(
                    BlockPos.STREAM_CODEC.decode(buf),
                    BaseCoreBlockEntity.ResidentAction.fromId(buf.readVarInt()),
                    buf.readUtf(64)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateCoreResidentPayload payload, IPayloadContext context) {
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
                core.updateResident(serverPlayer, payload.action(), payload.residentValue());
            }
        });
    }
}
