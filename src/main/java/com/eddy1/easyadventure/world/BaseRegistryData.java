package com.eddy1.easyadventure.world;

import com.eddy1.easyadventure.block.core.CoreVolume;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class BaseRegistryData extends SavedData {
    private static final String DATA_NAME = "easyadventure_base_registry";
    public static final String STATE_PLACED = "placed";
    public static final String STATE_PACKED = "packed";

    private final Map<UUID, BaseRecord> recordsByCore = new LinkedHashMap<>();

    public static BaseRegistryData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        ServerLevel storageLevel = overworld == null ? level : overworld;
        return storageLevel.getDataStorage().computeIfAbsent(BaseRegistryData::load, BaseRegistryData::new, DATA_NAME);
    }

    public static BaseRegistryData load(CompoundTag tag) {
        BaseRegistryData data = new BaseRegistryData();
        ListTag records = tag.getList("Records", Tag.TAG_COMPOUND);
        for (Tag entry : records) {
            CompoundTag recordTag = (CompoundTag) entry;
            BaseRecord record = BaseRecord.fromTag(recordTag);
            if (record != null) {
                data.recordsByCore.put(record.coreUuid(), record);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag records = new ListTag();
        recordsByCore.values().forEach(record -> records.add(record.toTag()));
        tag.put("Records", records);
        return tag;
    }

    public boolean isNameTaken(UUID ownerUuid, String baseName, @Nullable UUID exceptCoreUuid) {
        String normalized = normalizeBaseName(baseName);
        if (normalized.isBlank()) {
            return false;
        }
        for (BaseRecord record : recordsByCore.values()) {
            if (!record.ownerUuid().equals(ownerUuid)) {
                continue;
            }
            if (exceptCoreUuid != null && exceptCoreUuid.equals(record.coreUuid())) {
                continue;
            }
            if (normalized.equals(record.normalizedBaseName())) {
                return true;
            }
        }
        return false;
    }

    public void registerPlaced(
            UUID coreUuid,
            UUID ownerUuid,
            String ownerName,
            String baseName,
            @Nullable UUID keyUuid,
            @Nullable ResourceLocation dimension,
            @Nullable BlockPos pos
    ) {
        registerPlaced(coreUuid, ownerUuid, ownerName, baseName, keyUuid, dimension, pos, null);
    }

    public void registerPlaced(
            UUID coreUuid,
            UUID ownerUuid,
            String ownerName,
            String baseName,
            @Nullable UUID keyUuid,
            @Nullable ResourceLocation dimension,
            @Nullable BlockPos pos,
            @Nullable CoreVolume volume
    ) {
        BaseRecord existing = recordsByCore.get(coreUuid);
        UUID effectiveKeyUuid = keyUuid != null ? keyUuid : existing == null ? null : existing.keyUuid();
        UUID storageUuid = existing == null ? null : existing.storageUuid();
        CompoundTag keyStackTag = existing == null ? null : existing.keyStackTag();
        CoreVolume effectiveVolume = volume != null ? volume : existing == null ? null : existing.volumeOrNull();
        recordsByCore.put(coreUuid, new BaseRecord(
                coreUuid,
                ownerUuid,
                safe(ownerName),
                normalizeDisplayName(baseName),
                effectiveKeyUuid,
                storageUuid,
                STATE_PLACED,
                dimension == null ? null : dimension.toString(),
                pos,
                keyStackTag == null ? null : keyStackTag.copy(),
                effectiveVolume == null ? 0 : effectiveVolume.sizeX(),
                effectiveVolume == null ? 0 : effectiveVolume.sizeY(),
                effectiveVolume == null ? 0 : effectiveVolume.sizeBelowY(),
                effectiveVolume == null ? 0 : effectiveVolume.sizeZ()
        ));
        setDirty();
    }

    public void registerPacked(
            UUID coreUuid,
            UUID ownerUuid,
            String ownerName,
            String baseName,
            @Nullable UUID keyUuid,
            UUID storageUuid,
            @Nullable String sourceDimension,
            @Nullable ItemStack keyStack
    ) {
        BaseRecord existing = recordsByCore.get(coreUuid);
        UUID effectiveKeyUuid = keyUuid != null ? keyUuid : existing == null ? null : existing.keyUuid();
        CompoundTag keyStackTag = keyStack == null || keyStack.isEmpty() ? existing == null ? null : existing.keyStackTag() : keyStack.save(new CompoundTag());
        recordsByCore.put(coreUuid, new BaseRecord(
                coreUuid,
                ownerUuid,
                safe(ownerName),
                normalizeDisplayName(baseName),
                effectiveKeyUuid,
                storageUuid,
                STATE_PACKED,
                sourceDimension,
                null,
                keyStackTag == null ? null : keyStackTag.copy(),
                0,
                0,
                0,
                0
        ));
        setDirty();
    }

    public @Nullable BaseRecord getRecord(UUID coreUuid) {
        return recordsByCore.get(coreUuid);
    }

    public boolean isCurrentKey(UUID coreUuid, @Nullable UUID keyUuid) {
        if (keyUuid == null) {
            return false;
        }
        BaseRecord record = recordsByCore.get(coreUuid);
        return record != null && keyUuid.equals(record.keyUuid());
    }

    public boolean isPackedRecordCurrent(UUID coreUuid, UUID storageUuid, UUID ownerUuid) {
        BaseRecord record = recordsByCore.get(coreUuid);
        return record != null
                && STATE_PACKED.equals(record.state())
                && ownerUuid.equals(record.ownerUuid())
                && storageUuid.equals(record.storageUuid());
    }

    public List<BaseRecord> recordsForOwner(UUID ownerUuid) {
        List<BaseRecord> records = new ArrayList<>();
        for (BaseRecord record : recordsByCore.values()) {
            if (record.ownerUuid().equals(ownerUuid)) {
                records.add(record);
            }
        }
        records.sort(Comparator.comparing(BaseRecord::baseName, String.CASE_INSENSITIVE_ORDER));
        return records;
    }

    public List<BaseRecord> placedRecordsInDimension(ResourceLocation dimension) {
        List<BaseRecord> records = new ArrayList<>();
        String dimensionId = dimension.toString();
        for (BaseRecord record : recordsByCore.values()) {
            if (STATE_PLACED.equals(record.state())
                    && dimensionId.equals(record.dimension())
                    && record.pos() != null
                    && record.volumeOrNull() != null) {
                records.add(record);
            }
        }
        return records;
    }

    public static String normalizeBaseName(String name) {
        return normalizeDisplayName(name).toLowerCase(Locale.ROOT);
    }

    public static String normalizeDisplayName(String name) {
        return name == null ? "" : name.trim().replaceAll("\\s+", " ");
    }

    private static String safe(@Nullable String value) {
        return value == null ? "" : value;
    }

    public record BaseRecord(
            UUID coreUuid,
            UUID ownerUuid,
            String ownerName,
            String baseName,
            @Nullable UUID keyUuid,
            @Nullable UUID storageUuid,
            String state,
            @Nullable String dimension,
            @Nullable BlockPos pos,
            @Nullable CompoundTag keyStackTag,
            int sizeX,
            int sizeY,
            int sizeBelowY,
            int sizeZ
    ) {
        private static @Nullable BaseRecord fromTag(CompoundTag tag) {
            if (!tag.hasUUID("CoreUUID") || !tag.hasUUID("OwnerUUID")) {
                return null;
            }
            UUID keyUuid = tag.hasUUID("KeyUUID") ? tag.getUUID("KeyUUID") : null;
            UUID storageUuid = tag.hasUUID("StorageUUID") ? tag.getUUID("StorageUUID") : null;
            BlockPos pos = null;
            if (tag.contains("X") && tag.contains("Y") && tag.contains("Z")) {
                pos = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
            }
            return new BaseRecord(
                    tag.getUUID("CoreUUID"),
                    tag.getUUID("OwnerUUID"),
                    tag.getString("OwnerName"),
                    tag.getString("BaseName"),
                    keyUuid,
                    storageUuid,
                    tag.contains("State") ? tag.getString("State") : STATE_PLACED,
                    tag.contains("Dimension") ? tag.getString("Dimension") : null,
                    pos,
                    tag.contains("KeyStack", Tag.TAG_COMPOUND) ? tag.getCompound("KeyStack").copy() : null,
                    tag.getInt("SizeX"),
                    tag.getInt("SizeY"),
                    tag.getInt("SizeBelowY"),
                    tag.getInt("SizeZ")
            );
        }

        private CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("CoreUUID", coreUuid);
            tag.putUUID("OwnerUUID", ownerUuid);
            tag.putString("OwnerName", ownerName);
            tag.putString("BaseName", baseName);
            if (keyUuid != null) {
                tag.putUUID("KeyUUID", keyUuid);
            }
            if (storageUuid != null) {
                tag.putUUID("StorageUUID", storageUuid);
            }
            tag.putString("State", state);
            if (dimension != null && !dimension.isBlank()) {
                tag.putString("Dimension", dimension);
            }
            if (pos != null) {
                tag.putInt("X", pos.getX());
                tag.putInt("Y", pos.getY());
                tag.putInt("Z", pos.getZ());
            }
            if (keyStackTag != null) {
                tag.put("KeyStack", keyStackTag.copy());
            }
            if (sizeX > 0 && sizeY > 0 && sizeZ > 0) {
                tag.putInt("SizeX", sizeX);
                tag.putInt("SizeY", sizeY);
                tag.putInt("SizeBelowY", Math.max(0, sizeBelowY));
                tag.putInt("SizeZ", sizeZ);
            }
            return tag;
        }

        public String normalizedBaseName() {
            return normalizeBaseName(baseName);
        }

        public @Nullable CoreVolume volumeOrNull() {
            if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
                return null;
            }
            return new CoreVolume(sizeX, sizeY, Math.max(0, sizeBelowY), sizeZ);
        }
    }
}
