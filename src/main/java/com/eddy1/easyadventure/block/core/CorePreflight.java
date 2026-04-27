package com.eddy1.easyadventure.block.core;

import com.eddy1.easyadventure.block.BaseCoreBlock;
import com.eddy1.easyadventure.storage.StructureSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.eddy1.easyadventure.util.SavedBlockInfo;

public final class CorePreflight {
    private CorePreflight() {
    }

    public static CoreAreaCheckResult checkPacking(Level level, BlockPos center, CoreVolume volume) {
        if (!hasAreaLoaded(level, center, volume)) {
            return new CoreAreaCheckResult(false, 0, 0, 1, 0, 0, 0, null, Component.translatable("message.easyadventure.precheck_chunks_unloaded"));
        }
        if (!isWithinBounds(level, center, volume)) {
            return new CoreAreaCheckResult(false, 0, 0, 1, 0, 0, 0, null, Component.translatable("message.easyadventure.precheck_world_bounds"));
        }

        int total = 0;
        int occupied = 0;
        int blocked = 0;
        int blockEntities = 0;
        int containers = 0;
        BlockPos blockedPos = null;
        Component reason = null;

        for (int y = volume.minYOffset(); y <= volume.maxYOffset(); y++) {
            for (int x = -volume.halfX(); x <= volume.halfX(); x++) {
                for (int z = -volume.halfZ(); z <= volume.halfZ(); z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (pos.equals(center)) {
                        continue;
                    }

                    total++;
                    BlockState state = level.getBlockState(pos);
                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (state.isAir()) {
                        continue;
                    }
                    if (CoreCompat.isIgnoredDuringPack(state)) {
                        continue;
                    }

                    occupied++;
                    if (blockEntity != null) {
                        blockEntities++;
                        if (blockEntity instanceof Container) {
                            containers++;
                        }
                    }
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

                    if (CoreCompat.isDangerousToPack(state, blockEntity)) {
                        blocked++;
                        if (reason == null) {
                            blockedPos = pos;
                            reason = Component.translatable(
                                    "message.easyadventure.precheck_dangerous",
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

        return new CoreAreaCheckResult(
                blocked == 0,
                total,
                occupied,
                blocked,
                blockEntities,
                containers,
                CoreEntityTransport.countCapturable(level, center, volume),
                blockedPos,
                reason
        );
    }

    public static CoreAreaCheckResult checkDeployment(ServerLevel level, BlockPos center, StructureSnapshot snapshot) {
        CoreVolume volume = new CoreVolume(snapshot.sizeX(), snapshot.sizeY(), snapshot.sizeBelowY(), snapshot.sizeZ());
        if (!hasAreaLoaded(level, center, volume)) {
            return new CoreAreaCheckResult(false, 0, 0, 1, snapshot.blockEntityCount(), 0, snapshot.entityCount(), null, Component.translatable("message.easyadventure.precheck_chunks_unloaded"));
        }
        if (!isWithinBounds(level, center, volume)) {
            return new CoreAreaCheckResult(false, 0, 0, 1, snapshot.blockEntityCount(), 0, snapshot.entityCount(), null, Component.translatable("message.easyadventure.precheck_world_bounds"));
        }
        SavedBlockInfo storedDangerous = firstDangerousStoredBlock(snapshot);
        if (storedDangerous != null) {
            return new CoreAreaCheckResult(
                    false,
                    0,
                    0,
                    1,
                    snapshot.blockEntityCount(),
                    0,
                    snapshot.entityCount(),
                    center.offset(storedDangerous.relativePos()),
                    Component.translatable("message.easyadventure.precheck_stored_dangerous", storedDangerous.state().getBlock().getName())
            );
        }

        int total = 0;
        int occupied = 0;
        int blocked = 0;
        BlockPos blockedPos = null;
        Component reason = null;

        for (int y = volume.minYOffset(); y <= volume.maxYOffset(); y++) {
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
                    if (CoreCompat.isIgnoredDuringPack(state)) {
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

        return new CoreAreaCheckResult(
                blocked == 0,
                total,
                occupied,
                blocked,
                snapshot.blockEntityCount(),
                0,
                snapshot.entityCount(),
                blockedPos,
                reason
        );
    }

    private static boolean hasAreaLoaded(Level level, BlockPos center, CoreVolume volume) {
        BlockPos min = center.offset(-volume.halfX(), volume.minYOffset(), -volume.halfZ());
        BlockPos max = center.offset(volume.halfX(), volume.maxYOffset(), volume.halfZ());
        return level.hasChunksAt(min, max);
    }

    private static boolean isWithinBounds(Level level, BlockPos center, CoreVolume volume) {
        for (int y = volume.minYOffset(); y <= volume.maxYOffset(); y++) {
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

    private static SavedBlockInfo firstDangerousStoredBlock(StructureSnapshot snapshot) {
        for (SavedBlockInfo info : snapshot.blocks()) {
            if (CoreCompat.isDangerousToPack(info.state(), null)) {
                return info;
            }
        }
        return null;
    }
}
