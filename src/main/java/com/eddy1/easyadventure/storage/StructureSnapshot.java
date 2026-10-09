package com.eddy1.easyadventure.storage;

import com.eddy1.easyadventure.util.SavedBlockInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public record StructureSnapshot(int sizeX, int sizeY, int sizeZ, List<SavedBlockInfo> blocks, List<CompoundTag> entities) {
    private static final String TAG_SIZE_X = "SavedSizeX";
    private static final String TAG_SIZE_Y = "SavedSizeY";
    private static final String TAG_SIZE_Z = "SavedSizeZ";
    private static final String TAG_BLOCKS = "BaseData";
    private static final String TAG_ENTITIES = "EntityData";

    public StructureSnapshot {
        blocks = blocks == null ? List.of() : blocks.stream().map(StructureSnapshot::copyBlockInfo).toList();
        entities = entities == null ? List.of() : entities.stream().map(CompoundTag::copy).toList();
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_SIZE_X, sizeX);
        tag.putInt(TAG_SIZE_Y, sizeY);
        tag.putInt(TAG_SIZE_Z, sizeZ);

        ListTag blockList = new ListTag();
        for (SavedBlockInfo info : blocks) {
            blockList.add(info.toTag());
        }
        tag.put(TAG_BLOCKS, blockList);

        ListTag entityList = new ListTag();
        for (CompoundTag entityTag : entities) {
            entityList.add(entityTag.copy());
        }
        tag.put(TAG_ENTITIES, entityList);
        return tag;
    }

    public static StructureSnapshot fromTag(CompoundTag tag) {
        int sizeX = tag.contains(TAG_SIZE_X) ? tag.getInt(TAG_SIZE_X) : 9;
        int sizeY = tag.contains(TAG_SIZE_Y) ? tag.getInt(TAG_SIZE_Y) : 5;
        int sizeZ = tag.contains(TAG_SIZE_Z) ? tag.getInt(TAG_SIZE_Z) : 9;

        List<SavedBlockInfo> blocks = new ArrayList<>();
        if (tag.contains(TAG_BLOCKS)) {
            ListTag blockList = tag.getList(TAG_BLOCKS, Tag.TAG_COMPOUND);
            for (Tag entry : blockList) {
                blocks.add(SavedBlockInfo.fromTag((CompoundTag) entry));
            }
        }

        List<CompoundTag> entities = new ArrayList<>();
        if (tag.contains(TAG_ENTITIES)) {
            ListTag entityList = tag.getList(TAG_ENTITIES, Tag.TAG_COMPOUND);
            for (Tag entry : entityList) {
                entities.add(((CompoundTag) entry).copy());
            }
        }

        return new StructureSnapshot(sizeX, sizeY, sizeZ, blocks, entities);
    }

    private static SavedBlockInfo copyBlockInfo(SavedBlockInfo info) {
        return new SavedBlockInfo(
                info.relativePos(),
                info.state(),
                info.nbt() == null ? null : info.nbt().copy()
        );
    }
}
