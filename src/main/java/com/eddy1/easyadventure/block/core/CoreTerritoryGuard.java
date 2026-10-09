package com.eddy1.easyadventure.block.core;

import com.eddy1.easyadventure.block.BaseCoreBlockEntity;
import com.eddy1.easyadventure.config.EasyAdventureServerConfig;
import com.eddy1.easyadventure.world.BaseRegistryData;
import com.eddy1.easyadventure.world.TerritoryManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class CoreTerritoryGuard {
    private CoreTerritoryGuard() {
    }

    public static @Nullable Component validateSize(@Nullable Player player, CoreVolume volume) {
        EasyAdventureServerConfig.SizeLimits limits = EasyAdventureServerConfig.limitsFor(player);
        if (volume.sizeX() > limits.maxHorizontalSize() || volume.sizeZ() > limits.maxHorizontalSize()) {
            return Component.translatable("message.easyadventure.size_limit_horizontal", limits.maxHorizontalSize());
        }
        if (volume.sizeY() > limits.maxUpwardSize()) {
            return Component.translatable("message.easyadventure.size_limit_upward", limits.maxUpwardSize());
        }
        if (volume.sizeBelowY() > limits.maxBelowSize()) {
            return Component.translatable("message.easyadventure.size_limit_below", limits.maxBelowSize());
        }
        long blockCount = volume.blockCount();
        if (blockCount > limits.maxBlocks()) {
            return Component.translatable("message.easyadventure.size_limit_volume", blockCount, limits.maxBlocks());
        }
        return null;
    }

    public static @Nullable BaseCoreBlockEntity findOverlappingCore(
            ServerLevel level,
            BlockPos center,
            CoreVolume volume,
            @Nullable UUID exceptCoreUuid
    ) {
        if (!EasyAdventureServerConfig.preventBaseOverlap()) {
            return null;
        }

        for (BaseCoreBlockEntity core : TerritoryManager.getLoadedCores(level)) {
            if (!core.isTerritoryActive()) {
                continue;
            }
            if (exceptCoreUuid != null && exceptCoreUuid.equals(core.getCoreUUID())) {
                continue;
            }
            if (overlaps(center, volume, core.getBlockPos(), core.getTerritoryVolume())) {
                return core;
            }
        }
        return null;
    }

    public static @Nullable OverlapProblem findOverlapProblem(
            ServerLevel level,
            BlockPos center,
            CoreVolume volume,
            @Nullable UUID exceptCoreUuid
    ) {
        BaseCoreBlockEntity loadedCore = findOverlappingCore(level, center, volume, exceptCoreUuid);
        if (loadedCore != null) {
            return new OverlapProblem(loadedCore.getBlockPos(), overlapMessage(loadedCore));
        }

        if (!EasyAdventureServerConfig.preventBaseOverlap()) {
            return null;
        }

        for (BaseRegistryData.BaseRecord record : BaseRegistryData.get(level).placedRecordsInDimension(level.dimension().location())) {
            if (exceptCoreUuid != null && exceptCoreUuid.equals(record.coreUuid())) {
                continue;
            }
            CoreVolume recordVolume = record.volumeOrNull();
            BlockPos recordPos = record.pos();
            if (recordVolume == null || recordPos == null) {
                continue;
            }
            if (!isKnownPlacedCore(level, record)) {
                continue;
            }
            if (overlaps(center, volume, recordPos, recordVolume)) {
                return new OverlapProblem(recordPos, overlapMessage(record));
            }
        }
        return null;
    }

    public static Component overlapMessage(BaseCoreBlockEntity core) {
        BlockPos pos = core.getBlockPos();
        return Component.translatable(
                "message.easyadventure.base_overlap_blocked",
                core.getBaseName(),
                pos.getX(),
                pos.getY(),
                pos.getZ()
        );
    }

    public static Component overlapMessage(BaseRegistryData.BaseRecord record) {
        BlockPos pos = record.pos();
        return Component.translatable(
                "message.easyadventure.base_overlap_blocked",
                record.baseName(),
                pos == null ? 0 : pos.getX(),
                pos == null ? 0 : pos.getY(),
                pos == null ? 0 : pos.getZ()
        );
    }

    private static boolean isKnownPlacedCore(ServerLevel level, BaseRegistryData.BaseRecord record) {
        BlockPos pos = record.pos();
        if (pos == null || !level.isLoaded(pos)) {
            return true;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof BaseCoreBlockEntity core && record.coreUuid().equals(core.getCoreUUID());
    }

    private static boolean overlaps(BlockPos firstCenter, CoreVolume firstVolume, BlockPos secondCenter, CoreVolume secondVolume) {
        return minX(firstCenter, firstVolume) <= maxX(secondCenter, secondVolume)
                && maxX(firstCenter, firstVolume) >= minX(secondCenter, secondVolume)
                && minY(firstCenter, firstVolume) <= maxY(secondCenter, secondVolume)
                && maxY(firstCenter, firstVolume) >= minY(secondCenter, secondVolume)
                && minZ(firstCenter, firstVolume) <= maxZ(secondCenter, secondVolume)
                && maxZ(firstCenter, firstVolume) >= minZ(secondCenter, secondVolume);
    }

    private static int minX(BlockPos center, CoreVolume volume) {
        return center.getX() - volume.halfX();
    }

    private static int maxX(BlockPos center, CoreVolume volume) {
        return center.getX() + volume.halfX();
    }

    private static int minY(BlockPos center, CoreVolume volume) {
        return center.getY() + volume.minYOffset();
    }

    private static int maxY(BlockPos center, CoreVolume volume) {
        return center.getY() + volume.maxYOffset();
    }

    private static int minZ(BlockPos center, CoreVolume volume) {
        return center.getZ() - volume.halfZ();
    }

    private static int maxZ(BlockPos center, CoreVolume volume) {
        return center.getZ() + volume.halfZ();
    }

    public record OverlapProblem(BlockPos pos, Component message) {
    }
}
