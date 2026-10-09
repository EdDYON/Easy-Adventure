package com.eddy1.easyadventure.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BuildingStorageData extends SavedData {
    // 数据在 data 文件夹下的文件名：easyadventure_buildings.dat
    private static final String DATA_NAME = "easyadventure_buildings";

    // 内存缓存：UUID -> 建筑NBT数据
    private final Map<UUID, CompoundTag> buildingMap = new HashMap<>();

    // 获取实例的标准方法
    public static BuildingStorageData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        BuildingStorageData::new,
                        BuildingStorageData::load,
                        null
                ),
                DATA_NAME
        );
    }

    public BuildingStorageData() {}

    // 1. 存入建筑：返回一个 UUID 给物品
    public UUID saveBuilding(CompoundTag buildingData) {
        UUID id = UUID.randomUUID();
        buildingMap.put(id, buildingData);
        setDirty(); // 标记需要保存到硬盘
        return id;
    }

    // 2. 取出建筑
    public CompoundTag getBuilding(UUID id) {
        return buildingMap.get(id);
    }

    // 3. 删除建筑（放出后清理空间）
    public void removeBuilding(UUID id) {
        buildingMap.remove(id);
        setDirty();
    }

    // === 硬盘读写逻辑 (不要改动) ===

    public static BuildingStorageData load(CompoundTag nbt, HolderLookup.Provider provider) {
        BuildingStorageData data = new BuildingStorageData();
        ListTag list = nbt.getList("Buildings", Tag.TAG_COMPOUND);
        for (Tag tag : list) {
            CompoundTag entry = (CompoundTag) tag;
            if (entry.hasUUID("ID")) {
                data.buildingMap.put(entry.getUUID("ID"), entry.getCompound("Data"));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        buildingMap.forEach((uuid, compound) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("ID", uuid);
            entry.put("Data", compound);
            list.add(entry);
        });
        nbt.put("Buildings", list);
        return nbt;
    }
}