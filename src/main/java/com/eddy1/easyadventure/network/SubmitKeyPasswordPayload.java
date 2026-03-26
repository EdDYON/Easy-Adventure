package com.eddy1.easyadventure.network;

import com.eddy1.easyadventure.block.BaseCoreBlockEntity;
import com.eddy1.easyadventure.item.BaseKeyItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SubmitKeyPasswordPayload(
        KeyOperationAction action,
        BlockPos pos,
        Direction face,
        InteractionHand hand,
        String password
) {
    public static void encode(SubmitKeyPasswordPayload payload, FriendlyByteBuf buf) {
        buf.writeVarInt(payload.action.ordinal());
        buf.writeBlockPos(payload.pos);
        buf.writeVarInt(payload.face.get3DDataValue());
        buf.writeEnum(payload.hand);
        buf.writeUtf(payload.password, 64);
    }

    public static SubmitKeyPasswordPayload decode(FriendlyByteBuf buf) {
        return new SubmitKeyPasswordPayload(
                KeyOperationAction.fromId(buf.readVarInt()),
                buf.readBlockPos(),
                Direction.from3DDataValue(buf.readVarInt()),
                buf.readEnum(InteractionHand.class),
                buf.readUtf(64)
        );
    }

    public static void handle(SubmitKeyPasswordPayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
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
        context.setPacketHandled(true);
    }
}
