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

public record UpdateCoreSizePayload(BlockPos pos, int sizeX, int sizeY, int sizeZ) implements CustomPacketPayload {

    public static final Type<UpdateCoreSizePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EasyAdventure.MODID, "update_core_size"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateCoreSizePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                BlockPos.STREAM_CODEC.encode(buf, payload.pos);
                buf.writeInt(payload.sizeX);
                buf.writeInt(payload.sizeY);
                buf.writeInt(payload.sizeZ);
            },
            buf -> new UpdateCoreSizePayload(
                    BlockPos.STREAM_CODEC.decode(buf),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final UpdateCoreSizePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                BlockEntity be = serverPlayer.level().getBlockEntity(payload.pos());
                if (be instanceof BaseCoreBlockEntity core) {
                    core.initializeFoundation(payload.sizeX(), payload.sizeY(), payload.sizeZ());
                }
            }
        });
    }
}