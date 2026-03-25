package com.eddy1.easyadventure.block.core;

import com.eddy1.easyadventure.storage.StructureSnapshot;
import com.eddy1.easyadventure.util.SavedBlockInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CoreStructureWorkspace {
    private final Map<BlockPos, SavedBlockInfo> packedBlocks = new LinkedHashMap<>();
    private final Map<BlockPos, SavedBlockInfo> incomingBlocks = new LinkedHashMap<>();
    private final Map<BlockPos, CompoundTag> pendingBlockEntityLoads = new LinkedHashMap<>();
    private final List<CompoundTag> packedEntities = new ArrayList<>();
    private final List<CompoundTag> incomingEntities = new ArrayList<>();

    public void clearPacked() {
        packedBlocks.clear();
        packedEntities.clear();
    }

    public void clearIncoming() {
        incomingBlocks.clear();
        incomingEntities.clear();
        pendingBlockEntityLoads.clear();
    }

    public void clearAll() {
        clearPacked();
        clearIncoming();
    }

    public void clearPendingBlockEntityLoads() {
        pendingBlockEntityLoads.clear();
    }

    public void importIncomingSnapshot(StructureSnapshot snapshot) {
        clearIncoming();
        for (SavedBlockInfo info : snapshot.blocks()) {
            incomingBlocks.put(info.relativePos(), info);
        }
        for (CompoundTag entityTag : snapshot.entities()) {
            incomingEntities.add(entityTag.copy());
        }
    }

    public StructureSnapshot createPackedSnapshot(CoreVolume volume) {
        return new StructureSnapshot(volume.sizeX(), volume.sizeY(), volume.sizeZ(), new ArrayList<>(packedBlocks.values()), packedEntities);
    }

    public StructureSnapshot createPackedSnapshot(CoreVolume volume, @Nullable String packedAt, @Nullable String sourceDimension) {
        return new StructureSnapshot(volume.sizeX(), volume.sizeY(), volume.sizeZ(), new ArrayList<>(packedBlocks.values()), packedEntities, packedAt, sourceDimension);
    }

    public void rememberPackedBlock(SavedBlockInfo info) {
        packedBlocks.put(info.relativePos(), info);
    }

    public SavedBlockInfo findIncomingBlock(BlockPos relativePos) {
        return incomingBlocks.get(relativePos);
    }

    public Collection<SavedBlockInfo> incomingBlocks() {
        return incomingBlocks.values();
    }

    public boolean hasIncomingBlocks() {
        return !incomingBlocks.isEmpty();
    }

    public void queuePendingBlockEntityLoad(BlockPos relativePos, CompoundTag tag) {
        pendingBlockEntityLoads.put(relativePos, tag.copy());
    }

    public @Nullable CompoundTag removePendingBlockEntityLoad(BlockPos relativePos) {
        return pendingBlockEntityLoads.remove(relativePos);
    }

    public boolean hasPendingBlockEntityLoads() {
        return !pendingBlockEntityLoads.isEmpty();
    }

    public Collection<BlockPos> pendingBlockEntityPositions() {
        return pendingBlockEntityLoads.keySet();
    }

    public List<CompoundTag> packedEntities() {
        return packedEntities;
    }

    public List<CompoundTag> incomingEntities() {
        return incomingEntities;
    }

    public void save(CompoundTag tag) {
        tag.put("PackedBlocks", serializeBlocks(packedBlocks.values()));
        tag.put("IncomingBlocks", serializeBlocks(incomingBlocks.values()));
        tag.put("PendingBlockEntityLoads", serializePendingBlockEntityLoads());
        tag.put("PackedEntities", serializeEntityList(packedEntities));
        tag.put("IncomingEntities", serializeEntityList(incomingEntities));
    }

    public void load(CompoundTag tag) {
        clearAll();
        loadBlockMap(tag.getList("PackedBlocks", Tag.TAG_COMPOUND), packedBlocks);
        loadBlockMap(tag.getList("IncomingBlocks", Tag.TAG_COMPOUND), incomingBlocks);
        loadPendingBlockEntityLoads(tag.getList("PendingBlockEntityLoads", Tag.TAG_COMPOUND));
        loadEntityList(tag.getList("PackedEntities", Tag.TAG_COMPOUND), packedEntities);
        loadEntityList(tag.getList("IncomingEntities", Tag.TAG_COMPOUND), incomingEntities);
    }

    public ListTag serializePackedEntities() {
        return serializeEntityList(packedEntities);
    }

    public void loadPackedEntities(ListTag entityList) {
        packedEntities.clear();
        loadEntityList(entityList, packedEntities);
    }

    private static ListTag serializeBlocks(Collection<SavedBlockInfo> blocks) {
        ListTag list = new ListTag();
        for (SavedBlockInfo info : blocks) {
            list.add(info.toTag());
        }
        return list;
    }

    private static ListTag serializeEntityList(List<CompoundTag> entities) {
        ListTag entityList = new ListTag();
        for (CompoundTag entityTag : entities) {
            entityList.add(entityTag.copy());
        }
        return entityList;
    }

    private ListTag serializePendingBlockEntityLoads() {
        ListTag list = new ListTag();
        pendingBlockEntityLoads.forEach((relativePos, nbt) -> {
            CompoundTag entry = new CompoundTag();
            entry.putInt("X", relativePos.getX());
            entry.putInt("Y", relativePos.getY());
            entry.putInt("Z", relativePos.getZ());
            entry.put("Nbt", nbt.copy());
            list.add(entry);
        });
        return list;
    }

    private static void loadBlockMap(ListTag list, Map<BlockPos, SavedBlockInfo> output) {
        for (Tag entry : list) {
            SavedBlockInfo info = SavedBlockInfo.fromTag((CompoundTag) entry);
            output.put(info.relativePos(), info);
        }
    }

    private void loadPendingBlockEntityLoads(ListTag list) {
        pendingBlockEntityLoads.clear();
        for (Tag entry : list) {
            CompoundTag tag = (CompoundTag) entry;
            BlockPos pos = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
            pendingBlockEntityLoads.put(pos, tag.getCompound("Nbt").copy());
        }
    }

    private static void loadEntityList(ListTag entityList, List<CompoundTag> output) {
        output.clear();
        for (Tag entry : entityList) {
            output.add(((CompoundTag) entry).copy());
        }
    }
}
