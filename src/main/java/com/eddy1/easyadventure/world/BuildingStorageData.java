package com.eddy1.easyadventure.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BuildingStorageData extends SavedData {
    private static final String DATA_NAME = "easyadventure_buildings";
    private static final int MAX_BACKUPS = 3;

    private final Map<UUID, CompoundTag> buildingMap = new HashMap<>();
    private final Map<UUID, UUID> lockMap = new HashMap<>();
    private final Map<UUID, UUID> activeByCoreMap = new HashMap<>();
    private final Map<UUID, List<CompoundTag>> backupMap = new HashMap<>();

    public static BuildingStorageData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld is not available");
        }

        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(BuildingStorageData::new, BuildingStorageData::load, null),
                DATA_NAME
        );
    }

    public UUID saveBuilding(UUID coreUuid, CompoundTag buildingData) {
        UUID id = UUID.randomUUID();
        CompoundTag copy = buildingData.copy();
        buildingMap.put(id, copy);
        activeByCoreMap.put(coreUuid, id);
        addBackup(coreUuid, copy);
        setDirty();
        return id;
    }

    public @Nullable CompoundTag getBuilding(UUID id) {
        CompoundTag stored = buildingMap.get(id);
        return stored == null ? null : stored.copy();
    }

    public @Nullable UUID getActiveStorageId(UUID coreUuid) {
        return activeByCoreMap.get(coreUuid);
    }

    public boolean lockBuilding(UUID storageUuid, UUID coreUuid) {
        UUID currentLock = lockMap.get(storageUuid);
        if (currentLock != null && !currentLock.equals(coreUuid)) {
            return false;
        }

        if (!buildingMap.containsKey(storageUuid)) {
            return false;
        }

        lockMap.put(storageUuid, coreUuid);
        setDirty();
        return true;
    }

    public void unlockBuilding(UUID storageUuid, UUID coreUuid) {
        UUID currentLock = lockMap.get(storageUuid);
        if (currentLock != null && currentLock.equals(coreUuid)) {
            lockMap.remove(storageUuid);
            setDirty();
        }
    }

    public void removeBuilding(UUID id) {
        if (buildingMap.remove(id) == null) {
            return;
        }

        lockMap.remove(id);
        activeByCoreMap.entrySet().removeIf(entry -> entry.getValue().equals(id));
        setDirty();
    }

    public @Nullable UUID restoreLatestBackup(UUID coreUuid) {
        List<CompoundTag> backups = backupMap.get(coreUuid);
        if (backups == null || backups.isEmpty()) {
            return null;
        }

        UUID restoredId = UUID.randomUUID();
        buildingMap.put(restoredId, backups.get(0).copy());
        activeByCoreMap.put(coreUuid, restoredId);
        setDirty();
        return restoredId;
    }

    public int getBackupCount(UUID coreUuid) {
        List<CompoundTag> backups = backupMap.get(coreUuid);
        return backups == null ? 0 : backups.size();
    }

    private void addBackup(UUID coreUuid, CompoundTag buildingData) {
        List<CompoundTag> backups = backupMap.computeIfAbsent(coreUuid, unused -> new ArrayList<>());
        backups.add(0, buildingData.copy());
        while (backups.size() > MAX_BACKUPS) {
            backups.remove(backups.size() - 1);
        }
    }

    public static BuildingStorageData load(CompoundTag nbt, HolderLookup.Provider provider) {
        BuildingStorageData data = new BuildingStorageData();
        loadActiveBuildings(nbt, data);
        loadLocks(nbt, data);
        loadActiveByCore(nbt, data);
        loadBackups(nbt, data);
        return data;
    }

    private static void loadActiveBuildings(CompoundTag nbt, BuildingStorageData data) {
        String key = nbt.contains("ActiveBuildings") ? "ActiveBuildings" : "Buildings";
        ListTag list = nbt.getList(key, Tag.TAG_COMPOUND);
        for (Tag tag : list) {
            CompoundTag entry = (CompoundTag) tag;
            if (entry.hasUUID("ID")) {
                data.buildingMap.put(entry.getUUID("ID"), entry.getCompound("Data").copy());
            }
        }
    }

    private static void loadLocks(CompoundTag nbt, BuildingStorageData data) {
        ListTag list = nbt.getList("StorageLocks", Tag.TAG_COMPOUND);
        for (Tag tag : list) {
            CompoundTag entry = (CompoundTag) tag;
            if (entry.hasUUID("Storage") && entry.hasUUID("Core")) {
                data.lockMap.put(entry.getUUID("Storage"), entry.getUUID("Core"));
            }
        }
    }

    private static void loadActiveByCore(CompoundTag nbt, BuildingStorageData data) {
        ListTag list = nbt.getList("ActiveByCore", Tag.TAG_COMPOUND);
        for (Tag tag : list) {
            CompoundTag entry = (CompoundTag) tag;
            if (entry.hasUUID("Core") && entry.hasUUID("Storage")) {
                data.activeByCoreMap.put(entry.getUUID("Core"), entry.getUUID("Storage"));
            }
        }
    }

    private static void loadBackups(CompoundTag nbt, BuildingStorageData data) {
        ListTag list = nbt.getList("Backups", Tag.TAG_COMPOUND);
        for (Tag tag : list) {
            CompoundTag entry = (CompoundTag) tag;
            if (!entry.hasUUID("Core")) {
                continue;
            }

            List<CompoundTag> backups = new ArrayList<>();
            for (Tag snapshotTag : entry.getList("Entries", Tag.TAG_COMPOUND)) {
                backups.add(((CompoundTag) snapshotTag).copy());
            }
            if (!backups.isEmpty()) {
                data.backupMap.put(entry.getUUID("Core"), backups);
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider provider) {
        ListTag activeList = new ListTag();
        buildingMap.forEach((uuid, compound) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("ID", uuid);
            entry.put("Data", compound.copy());
            activeList.add(entry);
        });
        nbt.put("ActiveBuildings", activeList);

        ListTag lockList = new ListTag();
        lockMap.forEach((storageUuid, coreUuid) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Storage", storageUuid);
            entry.putUUID("Core", coreUuid);
            lockList.add(entry);
        });
        nbt.put("StorageLocks", lockList);

        ListTag activeByCoreList = new ListTag();
        activeByCoreMap.forEach((coreUuid, storageUuid) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Core", coreUuid);
            entry.putUUID("Storage", storageUuid);
            activeByCoreList.add(entry);
        });
        nbt.put("ActiveByCore", activeByCoreList);

        ListTag backupList = new ListTag();
        backupMap.forEach((coreUuid, backups) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Core", coreUuid);
            ListTag entries = new ListTag();
            for (CompoundTag backup : backups) {
                entries.add(backup.copy());
            }
            entry.put("Entries", entries);
            backupList.add(entry);
        });
        nbt.put("Backups", backupList);
        return nbt;
    }
}
