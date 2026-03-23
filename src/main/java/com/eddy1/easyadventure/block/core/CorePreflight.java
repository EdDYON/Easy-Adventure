package com.eddy1.easyadventure.block.core;

import com.eddy1.easyadventure.block.BaseCoreBlock;
import com.eddy1.easyadventure.storage.StructureSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class CorePreflight {
    private CorePreflight() {
    }

    public static CoreAreaCheckResult checkPacking(Level level, BlockPos center, CoreVolume volume) {
        if (!hasAreaLoaded(level, center, volume)) {
            return new CoreAreaCheckResult(false, 0, 0, 1, null, Component.translatable("message.easyadventure.precheck_chunks_unloaded"));
        }
        if (!isWithinBounds(level, center, volume)) {
            return new CoreAreaCheckResult(false, 0, 0, 1, null, Component.translatable("message.easyadventure.precheck_world_bounds"));
        }

        int total = 0;
        int occupied = 0;
        int blocked = 0;
        BlockPos blockedPos = null;
        Component reason = null;

        for (int y = 0; y <= volume.sizeY(); y++) {
            for (int x = -volume.halfX(); x <= volume.halfX(); x++) {
                for (int z = -volume.halfZ(); z <= volume.halfZ(); z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (pos.equals(center)) {
                        continue;
                    }

                    total++;
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }

                    occupied++;
                    if (state.getBlock() instanceof BaseCoreBlock) {
                        blocked++;
                        if (reason == null) {
                            blockedPos = pos;
                            reason = Component.translatable(
                                    "message.easyadventure.precheck_blocked",
                                    state.getBlock().getName(),
                                    pos.getX(),
                                    pos.getY(),
                                    pos.getZ()
                            );
                        }
                        continue;
                    }

                    if (state.is(CoreCompat.CANNOT_PACK) || state.getDestroySpeed(level, pos) < 0) {
                        blocked++;
                        if (reason == null) {
                            blockedPos = pos;
                            reason = Component.translatable(
                                    "message.easyadventure.precheck_blocked",
                                    state.getBlock().getName(),
                                    pos.getX(),
                                    pos.getY(),
                                    pos.getZ()
                            );
                        }
                    }
                }
            }
        }

        return new CoreAreaCheckResult(blocked == 0, total, occupied, blocked, blockedPos, reason);
    }

    public static CoreAreaCheckResult checkDeployment(ServerLevel level, BlockPos center, StructureSnapshot snapshot) {
        CoreVolume volume = new CoreVolume(snapshot.sizeX(), snapshot.sizeY(), snapshot.sizeZ());
        if (!hasAreaLoaded(level, center, volume)) {
            return new CoreAreaCheckResult(false, 0, 0, 1, null, Component.translatable("message.easyadventure.precheck_chunks_unloaded"));
        }
        if (!isWithinBounds(level, center, volume)) {
            return new CoreAreaCheckResult(false, 0, 0, 1, null, Component.translatable("message.easyadventure.precheck_world_bounds"));
        }

        int total = 0;
        int occupied = 0;
        int blocked = 0;
        BlockPos blockedPos = null;
        Component reason = null;

        for (int y = 0; y <= volume.sizeY(); y++) {
            for (int x = -volume.halfX(); x <= volume.halfX(); x++) {
                for (int z = -volume.halfZ(); z <= volume.halfZ(); z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (pos.equals(center)) {
                        continue;
                    }

                    total++;
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }

                    occupied++;
                    if (state.getBlock() instanceof BaseCoreBlock
                            || state.is(CoreCompat.DEPLOYMENT_BLOCKERS)
                            || state.getDestroySpeed(level, pos) < 0) {
                        blocked++;
                        if (reason == null) {
                            blockedPos = pos;
                            reason = Component.translatable(
                                    "message.easyadventure.precheck_blocked",
                                    state.getBlock().getName(),
                                    pos.getX(),
                                    pos.getY(),
                                    pos.getZ()
                            );
                        }
                    }
                }
            }
        }

        return new CoreAreaCheckResult(blocked == 0, total, occupied, blocked, blockedPos, reason);
    }

    private static boolean hasAreaLoaded(Level level, BlockPos center, CoreVolume volume) {
        BlockPos min = center.offset(-volume.halfX(), 0, -volume.halfZ());
        BlockPos max = center.offset(volume.halfX(), volume.sizeY(), volume.halfZ());
        return level.hasChunksAt(min, max);
    }

    private static boolean isWithinBounds(Level level, BlockPos center, CoreVolume volume) {
        for (int y = 0; y <= volume.sizeY(); y++) {
            for (int x = -volume.halfX(); x <= volume.halfX(); x++) {
                for (int z = -volume.halfZ(); z <= volume.halfZ(); z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (!level.isInWorldBounds(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
