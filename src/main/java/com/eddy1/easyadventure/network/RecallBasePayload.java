package com.eddy1.easyadventure.network;

import com.eddy1.easyadventure.init.ModBlocks;
import com.eddy1.easyadventure.world.BaseRecallService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record RecallBasePayload(BlockPos tablePos, UUID coreUuid) {
    public static void encode(RecallBasePayload payload, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(payload.tablePos);
        buffer.writeUUID(payload.coreUuid);
    }

    public static RecallBasePayload decode(FriendlyByteBuf buffer) {
        return new RecallBasePayload(buffer.readBlockPos(), buffer.readUUID());
    }

    public static void handle(RecallBasePayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            if (player.distanceToSqr(
                    payload.tablePos().getX() + 0.5,
                    payload.tablePos().getY() + 0.5,
                    payload.tablePos().getZ() + 0.5
            ) > 64.0D) {
                return;
            }
            if (!player.level().getBlockState(payload.tablePos()).is(ModBlocks.KEY_RECALL_TABLE.get())) {
                return;
            }
            BaseRecallService.recall(player, payload.coreUuid());
        });
        context.setPacketHandled(true);
    }
}
