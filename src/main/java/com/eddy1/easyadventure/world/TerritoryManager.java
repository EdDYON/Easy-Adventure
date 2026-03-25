package com.eddy1.easyadventure.world;

import com.eddy1.easyadventure.block.BaseCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class TerritoryManager {
    private static final Map<ResourceKey<Level>, DimensionIndex> DIMENSION_INDEX = new ConcurrentHashMap<>();

    private TerritoryManager() {
    }

    public static void register(ServerLevel level, BaseCoreBlockEntity core) {
        refresh(level, core);
    }

    public static void refresh(ServerLevel level, BaseCoreBlockEntity core) {
        DimensionIndex index = DIMENSION_INDEX.computeIfAbsent(level.dimension(), unused -> new DimensionIndex());
        BlockPos corePos = core.getBlockPos().immutable();
        removeCore(index, corePos);
        index.corePositions.add(corePos);

        Set<Long> coveredChunks = coveredChunks(core);
        index.coreChunkCoverage.put(corePos, coveredChunks);
        for (long chunkKey : coveredChunks) {
            index.chunkToCores.computeIfAbsent(chunkKey, unused -> ConcurrentHashMap.newKeySet()).add(corePos);
        }
    }

    public static void unregister(ServerLevel level, BlockPos pos) {
        DimensionIndex index = DIMENSION_INDEX.get(level.dimension());
        if (index == null) {
            return;
        }

        removeCore(index, pos.immutable());
        cleanupDimension(level.dimension(), index);
    }

    public static Collection<BaseCoreBlockEntity> getLoadedCores(ServerLevel level) {
        DimensionIndex index = DIMENSION_INDEX.get(level.dimension());
        if (index == null || index.corePositions.isEmpty()) {
            return java.util.List.of();
        }

        ArrayList<BaseCoreBlockEntity> cores = new ArrayList<>();
        ArrayList<BlockPos> stalePositions = new ArrayList<>();
        for (BlockPos pos : index.corePositions) {
            if (level.getBlockEntity(pos) instanceof BaseCoreBlockEntity core && !core.isRemoved()) {
                cores.add(core);
            } else {
                stalePositions.add(pos);
            }
        }

        stalePositions.forEach(stalePos -> removeCore(index, stalePos));
        cleanupDimension(level.dimension(), index);
        return cores;
    }

    public static @Nullable BaseCoreBlockEntity findContainingCore(ServerLevel level, BlockPos pos) {
        for (BaseCoreBlockEntity core : findContainingCores(level, pos)) {
            return core;
        }
        return null;
    }

    public static Collection<BaseCoreBlockEntity> findContainingCores(ServerLevel level, BlockPos pos) {
        DimensionIndex index = DIMENSION_INDEX.get(level.dimension());
        if (index == null) {
            return java.util.List.of();
        }

        Set<BlockPos> candidatePositions = index.chunkToCores.get(chunkKey(pos));
        if (candidatePositions == null || candidatePositions.isEmpty()) {
            return java.util.List.of();
        }

        Set<BaseCoreBlockEntity> cores = new LinkedHashSet<>();
        ArrayList<BlockPos> stalePositions = new ArrayList<>();
        for (BlockPos candidatePos : candidatePositions) {
            if (level.getBlockEntity(candidatePos) instanceof BaseCoreBlockEntity core && !core.isRemoved()) {
                if (core.containsTerritoryPos(pos)) {
                    cores.add(core);
                }
            } else {
                stalePositions.add(candidatePos);
            }
        }

        stalePositions.forEach(stalePos -> removeCore(index, stalePos));
        cleanupDimension(level.dimension(), index);
        return cores.isEmpty() ? java.util.List.of() : java.util.List.copyOf(cores);
    }

    private static Set<Long> coveredChunks(BaseCoreBlockEntity core) {
        LinkedHashSet<Long> covered = new LinkedHashSet<>();
        BlockPos center = core.getBlockPos();
        if (!core.isTerritoryActive()) {
            covered.add(chunkKey(center));
            return covered;
        }

        var volume = core.getTerritoryVolume();
        int minX = center.getX() - volume.halfX();
        int maxX = center.getX() + volume.halfX();
        int minZ = center.getZ() - volume.halfZ();
        int maxZ = center.getZ() + volume.halfZ();

        int minChunkX = SectionPos.blockToSectionCoord(minX);
        int maxChunkX = SectionPos.blockToSectionCoord(maxX);
        int minChunkZ = SectionPos.blockToSectionCoord(minZ);
        int maxChunkZ = SectionPos.blockToSectionCoord(maxZ);
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                covered.add(ChunkPos.asLong(chunkX, chunkZ));
            }
        }
        return covered;
    }

    private static long chunkKey(BlockPos pos) {
        return ChunkPos.asLong(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    private static void removeCore(DimensionIndex index, BlockPos corePos) {
        index.corePositions.remove(corePos);
        Set<Long> previousCoverage = index.coreChunkCoverage.remove(corePos);
        if (previousCoverage == null || previousCoverage.isEmpty()) {
            return;
        }

        for (long chunkKey : previousCoverage) {
            Set<BlockPos> chunkCores = index.chunkToCores.get(chunkKey);
            if (chunkCores == null) {
                continue;
            }
            chunkCores.remove(corePos);
            if (chunkCores.isEmpty()) {
                index.chunkToCores.remove(chunkKey);
            }
        }
    }

    private static void cleanupDimension(ResourceKey<Level> dimension, DimensionIndex index) {
        if (index.corePositions.isEmpty() && index.coreChunkCoverage.isEmpty() && index.chunkToCores.isEmpty()) {
            DIMENSION_INDEX.remove(dimension);
        }
    }

    private static final class DimensionIndex {
        private final Set<BlockPos> corePositions = ConcurrentHashMap.newKeySet();
        private final Map<BlockPos, Set<Long>> coreChunkCoverage = new ConcurrentHashMap<>();
        private final Map<Long, Set<BlockPos>> chunkToCores = new ConcurrentHashMap<>();
    }
}
