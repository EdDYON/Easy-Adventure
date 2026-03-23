package com.eddy1.easyadventure.network;

import com.eddy1.easyadventure.EasyAdventure;
import com.eddy1.easyadventure.block.BaseCoreBlockEntity;
import com.eddy1.easyadventure.item.BaseKeyItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SubmitKeyPasswordPayload(
        KeyOperationAction action,
        BlockPos pos,
        Direction face,
        InteractionHand hand,
        String password
) implements CustomPacketPayload {
    public static final Type<SubmitKeyPasswordPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EasyAdventure.MODID, "submit_key_password"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SubmitKeyPasswordPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.action.ordinal());
                BlockPos.STREAM_CODEC.encode(buf, payload.pos);
                buf.writeVarInt(payload.face.get3DDataValue());
                buf.writeVarInt(payload.hand.ordinal());
                buf.writeUtf(payload.password, 64);
            },
            buf -> new SubmitKeyPasswordPayload(
                    KeyOperationAction.fromId(buf.readVarInt()),
                    BlockPos.STREAM_CODEC.decode(buf),
                    Direction.from3DDataValue(buf.readVarInt()),
                    InteractionHand.values()[buf.readVarInt()],
                    buf.readUtf(64)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SubmitKeyPasswordPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }

            if (serverPlayer.distanceToSqr(
                    payload.pos().getX() + 0.5,
                    payload.pos().getY() + 0.5,
                    payload.pos().getZ() + 0.5
            ) > 144.0D) {
                return;
            }

            if (payload.action == KeyOperationAction.PACK) {
                BlockEntity blockEntity = serverPlayer.level().getBlockEntity(payload.pos());
                if (blockEntity instanceof BaseCoreBlockEntity) {
                    BaseKeyItem.handlePasswordAction(serverPlayer, payload.hand(), payload.action(), payload.pos(), payload.face(), payload.password());
                }
                return;
            }

            BaseKeyItem.handlePasswordAction(serverPlayer, payload.hand(), payload.action(), payload.pos(), payload.face(), payload.password());
        });
    }
}
