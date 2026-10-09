package com.test.easyadventure.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScrollStorageData extends SavedData {
    // 存档名称，决定了在 data 文件夹下的文件名
    private static final String DATA_NAME = "easyadventure_scroll_storage";

    // 内存中的映射表：UUID -> 物品处理器（27格的大箱子）
    private final Map<UUID, ItemStackHandler> storageMap = new HashMap<>();

    // 获取实例的静态方法
    public static ScrollStorageData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        ScrollStorageData::new,
                        ScrollStorageData::load,
                        null
                ),
                DATA_NAME
        );
    }

    public ScrollStorageData() {}

    // 根据 UUID 获取背包数据
    public ItemStackHandler getHandler(UUID uuid) {
        return storageMap.computeIfAbsent(uuid, k -> {
            setDirty(); // 标记需要保存
            return new ItemStackHandler(27) { // 假设卷轴容量为 27
                @Override
                protected void onContentsChanged(int slot) {
                    setDirty(); // 物品变动时保存
                }
            };
        });
    }

    // 从硬盘加载
    public static ScrollStorageData load(CompoundTag nbt, HolderLookup.Provider provider) {
        ScrollStorageData data = new ScrollStorageData();
        ListTag list = nbt.getList("Scrolls", Tag.TAG_COMPOUND);
        for (Tag tag : list) {
            CompoundTag scrollTag = (CompoundTag) tag;
            if (scrollTag.hasUUID("UUID")) {
                UUID uuid = scrollTag.getUUID("UUID");
                ItemStackHandler handler = new ItemStackHandler(27);
                handler.deserializeNBT(provider, scrollTag.getCompound("Items"));
                data.storageMap.put(uuid, handler);
            }
        }
        return data;
    }

    // 保存到硬盘
    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        storageMap.forEach((uuid, handler) -> {
            CompoundTag scrollTag = new CompoundTag();
            scrollTag.putUUID("UUID", uuid);
            scrollTag.put("Items", handler.serializeNBT(provider));
            list.add(scrollTag);
        });
        nbt.put("Scrolls", list);
        return nbt;
    }
}