package com.eddy1.easyadventure.storage;

import com.eddy1.easyadventure.util.SavedBlockInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record StructureSnapshot(
        int sizeX,
        int sizeY,
        int sizeBelowY,
        int sizeZ,
        List<SavedBlockInfo> blocks,
        List<CompoundTag> entities,
        @Nullable String packedAt,
        @Nullable String sourceDimension
) {
    private static final String TAG_SIZE_X = "SavedSizeX";
    private static final String TAG_SIZE_Y = "SavedSizeY";
    private static final String TAG_SIZE_BELOW_Y = "SavedSizeBelowY";
    private static final String TAG_SIZE_Z = "SavedSizeZ";
    private static final String TAG_BLOCKS = "BaseData";
    private static final String TAG_ENTITIES = "EntityData";
    private static final String TAG_PACKED_AT = "PackedAt";
    private static final String TAG_SOURCE_DIMENSION = "SourceDimension";

    public StructureSnapshot(int sizeX, int sizeY, int sizeZ, List<SavedBlockInfo> blocks, List<CompoundTag> entities) {
        this(sizeX, sizeY, 0, sizeZ, blocks, entities, null, null);
    }

    public StructureSnapshot(int sizeX, int sizeY, int sizeBelowY, int sizeZ, List<SavedBlockInfo> blocks, List<CompoundTag> entities) {
        this(sizeX, sizeY, sizeBelowY, sizeZ, blocks, entities, null, null);
    }

    public StructureSnapshot(int sizeX, int sizeY, int sizeZ, List<SavedBlockInfo> blocks, List<CompoundTag> entities, @Nullable String packedAt, @Nullable String sourceDimension) {
        this(sizeX, sizeY, 0, sizeZ, blocks, entities, packedAt, sourceDimension);
    }

    public StructureSnapshot {
        blocks = blocks == null ? List.of() : blocks.stream().map(StructureSnapshot::copyBlockInfo).toList();
        entities = entities == null ? List.of() : entities.stream().map(CompoundTag::copy).toList();
        packedAt = packedAt == null || packedAt.isBlank() ? null : packedAt;
        sourceDimension = sourceDimension == null || sourceDimension.isBlank() ? null : sourceDimension;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_SIZE_X, sizeX);
        tag.putInt(TAG_SIZE_Y, sizeY);
        tag.putInt(TAG_SIZE_BELOW_Y, sizeBelowY);
        tag.putInt(TAG_SIZE_Z, sizeZ);
        if (packedAt != null) {
            tag.putString(TAG_PACKED_AT, packedAt);
        }
        if (sourceDimension != null) {
            tag.putString(TAG_SOURCE_DIMENSION, sourceDimension);
        }

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

    public int blockCount() {
        return blocks.size();
    }

    public int blockEntityCount() {
        int count = 0;
        for (SavedBlockInfo info : blocks) {
            if (info.nbt() != null) {
                count++;
            }
        }
        return count;
    }

    public int entityCount() {
        return entities.size();
    }

    public StructureSnapshot rotated(Rotation rotation) {
        if (rotation == Rotation.NONE) {
            return this;
        }

        List<SavedBlockInfo> rotatedBlocks = new ArrayList<>(blocks.size());
        for (SavedBlockInfo info : blocks) {
            rotatedBlocks.add(new SavedBlockInfo(
                    rotateRelativePos(info.relativePos(), rotation),
                    info.state().rotate(rotation),
                    info.nbt() == null ? null : info.nbt().copy()
            ));
        }

        List<CompoundTag> rotatedEntities = new ArrayList<>(entities.size());
        for (CompoundTag entityTag : entities) {
            rotatedEntities.add(rotateEntity(entityTag, rotation));
        }

        boolean swapsHorizontalSize = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90;
        return new StructureSnapshot(
                swapsHorizontalSize ? sizeZ : sizeX,
                sizeY,
                sizeBelowY,
                swapsHorizontalSize ? sizeX : sizeZ,
                rotatedBlocks,
                rotatedEntities,
                packedAt,
                sourceDimension
        );
    }

    public static StructureSnapshot fromTag(CompoundTag tag) {
        int sizeX = tag.contains(TAG_SIZE_X) ? tag.getInt(TAG_SIZE_X) : 9;
        int sizeY = tag.contains(TAG_SIZE_Y) ? tag.getInt(TAG_SIZE_Y) : 5;
        int sizeBelowY = tag.contains(TAG_SIZE_BELOW_Y) ? tag.getInt(TAG_SIZE_BELOW_Y) : 0;
        int sizeZ = tag.contains(TAG_SIZE_Z) ? tag.getInt(TAG_SIZE_Z) : 9;
        String packedAt = tag.contains(TAG_PACKED_AT) ? tag.getString(TAG_PACKED_AT) : null;
        String sourceDimension = tag.contains(TAG_SOURCE_DIMENSION) ? tag.getString(TAG_SOURCE_DIMENSION) : null;

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

        return new StructureSnapshot(sizeX, sizeY, sizeBelowY, sizeZ, blocks, entities, packedAt, sourceDimension);
    }

    private static SavedBlockInfo copyBlockInfo(SavedBlockInfo info) {
        return new SavedBlockInfo(
                info.relativePos(),
                info.state(),
                info.nbt() == null ? null : info.nbt().copy()
        );
    }

    private static net.minecraft.core.BlockPos rotateRelativePos(net.minecraft.core.BlockPos pos, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90 -> new net.minecraft.core.BlockPos(-pos.getZ(), pos.getY(), pos.getX());
            case CLOCKWISE_180 -> new net.minecraft.core.BlockPos(-pos.getX(), pos.getY(), -pos.getZ());
            case COUNTERCLOCKWISE_90 -> new net.minecraft.core.BlockPos(pos.getZ(), pos.getY(), -pos.getX());
            case NONE -> pos;
        };
    }

    private static CompoundTag rotateEntity(CompoundTag entityTag, Rotation rotation) {
        CompoundTag rotated = entityTag.copy();
        double relX = rotated.getDouble("RelX");
        double relZ = rotated.getDouble("RelZ");
        switch (rotation) {
            case CLOCKWISE_90 -> {
                rotated.putDouble("RelX", -relZ);
                rotated.putDouble("RelZ", relX);
                rotateEntityYaw(rotated, 90.0F);
            }
            case CLOCKWISE_180 -> {
                rotated.putDouble("RelX", -relX);
                rotated.putDouble("RelZ", -relZ);
                rotateEntityYaw(rotated, 180.0F);
            }
            case COUNTERCLOCKWISE_90 -> {
                rotated.putDouble("RelX", relZ);
                rotated.putDouble("RelZ", -relX);
                rotateEntityYaw(rotated, -90.0F);
            }
            case NONE -> {
            }
        }
        return rotated;
    }

    private static void rotateEntityYaw(CompoundTag entityTag, float deltaYaw) {
        if (!entityTag.contains("Rotation", Tag.TAG_LIST)) {
            return;
        }

        ListTag rotationList = entityTag.getList("Rotation", Tag.TAG_FLOAT);
        if (rotationList.size() < 2) {
            return;
        }

        float yaw = rotationList.getFloat(0);
        float pitch = rotationList.getFloat(1);
        ListTag updated = new ListTag();
        updated.add(FloatTag.valueOf(yaw + deltaYaw));
        updated.add(FloatTag.valueOf(pitch));
        entityTag.put("Rotation", updated);
    }
}
