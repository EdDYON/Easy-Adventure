package com.eddy1.easyadventure.network;

import com.eddy1.easyadventure.block.core.CoreSafetyReporter;
import com.eddy1.easyadventure.block.core.CoreVolume;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PreviewCorePayload(
        BlockPos pos,
        int sizeX,
        int sizeY,
        int sizeBelowY,
        int sizeZ
) {
    public static void encode(PreviewCorePayload payload, FriendlyByteBuf buf) {
        buf.writeBlockPos(payload.pos);
        buf.writeInt(payload.sizeX);
        buf.writeInt(payload.sizeY);
        buf.writeInt(payload.sizeBelowY);
        buf.writeInt(payload.sizeZ);
    }

    public static PreviewCorePayload decode(FriendlyByteBuf buf) {
        return new PreviewCorePayload(
                buf.readBlockPos(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt()
        );
    }

    public static void handle(PreviewCorePayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.level() instanceof ServerLevel level)) {
                return;
            }
            if (player.distanceToSqr(payload.pos.getX() + 0.5D, payload.pos.getY() + 0.5D, payload.pos.getZ() + 0.5D) > 64.0D) {
                return;
            }

            CoreSafetyReporter.reportSetup(player, level, payload.pos, new CoreVolume(
                    payload.sizeX,
                    payload.sizeY,
                    payload.sizeBelowY,
                    payload.sizeZ
            ));
        });
        context.setPacketHandled(true);
    }
}
