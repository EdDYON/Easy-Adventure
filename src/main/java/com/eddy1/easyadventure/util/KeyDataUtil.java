package com.eddy1.easyadventure.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class KeyDataUtil {
    public static final String BOUND_UUID = "BoundUUID";
    public static final String STORAGE_UUID = "StorageUUID";
    public static final String OWNER_UUID = "OwnerUUID";
    public static final String OWNER_NAME = "OwnerName";
    public static final String BASE_NAME = "BaseName";
    public static final String PASSWORD_ENABLED = "PasswordEnabled";
    public static final String PASSWORD_HASH = "PasswordHash";
    public static final String SIZE_X = "StoredSizeX";
    public static final String SIZE_Y = "StoredSizeY";
    public static final String SIZE_Z = "StoredSizeZ";

    private KeyDataUtil() {
    }

    public static boolean hasBoundUuid(ItemStack stack) {
        return getBoundUuid(stack) != null;
    }

    public static boolean hasStoredStructure(ItemStack stack) {
        return getStorageUuid(stack) != null;
    }

    public static @Nullable UUID getBoundUuid(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag != null && tag.contains(BOUND_UUID) ? tag.getUUID(BOUND_UUID) : null;
    }

    public static @Nullable UUID getStorageUuid(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag != null && tag.contains(STORAGE_UUID) ? tag.getUUID(STORAGE_UUID) : null;
    }

    public static @Nullable UUID getOwnerUuid(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag != null && tag.contains(OWNER_UUID) ? tag.getUUID(OWNER_UUID) : null;
    }

    public static @Nullable String getOwnerName(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag != null && tag.contains(OWNER_NAME) ? tag.getString(OWNER_NAME) : null;
    }

    public static @Nullable String getBaseName(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag != null && tag.contains(BASE_NAME) ? tag.getString(BASE_NAME) : null;
    }

    public static boolean isPasswordEnabled(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag != null && tag.contains(PASSWORD_ENABLED) && tag.getBoolean(PASSWORD_ENABLED);
    }

    public static @Nullable String getPasswordHash(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag != null && tag.contains(PASSWORD_HASH) ? tag.getString(PASSWORD_HASH) : null;
    }

    public static int getStoredSizeX(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag != null && tag.contains(SIZE_X) ? tag.getInt(SIZE_X) : 0;
    }

    public static int getStoredSizeY(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag != null && tag.contains(SIZE_Y) ? tag.getInt(SIZE_Y) : 0;
    }

    public static int getStoredSizeZ(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag != null && tag.contains(SIZE_Z) ? tag.getInt(SIZE_Z) : 0;
    }

    public static void setKeyData(
            ItemStack stack,
            UUID boundUuid,
            @Nullable UUID storageUuid,
            @Nullable UUID ownerUuid,
            @Nullable String ownerName,
            @Nullable String baseName,
            boolean passwordEnabled,
            @Nullable String passwordHash,
            int sizeX,
            int sizeY,
            int sizeZ
    ) {
        CompoundTag tag = getOrCreateTag(stack);
        tag.putUUID(BOUND_UUID, boundUuid);
        if (storageUuid != null) {
            tag.putUUID(STORAGE_UUID, storageUuid);
            tag.putInt(SIZE_X, sizeX);
            tag.putInt(SIZE_Y, sizeY);
            tag.putInt(SIZE_Z, sizeZ);
        } else {
            tag.remove(STORAGE_UUID);
            tag.remove(SIZE_X);
            tag.remove(SIZE_Y);
            tag.remove(SIZE_Z);
        }
        if (ownerUuid != null) {
            tag.putUUID(OWNER_UUID, ownerUuid);
        }
        if (ownerName != null && !ownerName.isBlank()) {
            tag.putString(OWNER_NAME, ownerName);
        }
        if (baseName != null && !baseName.isBlank()) {
            tag.putString(BASE_NAME, baseName);
        }
        tag.putBoolean(PASSWORD_ENABLED, passwordEnabled);
        if (passwordHash != null && !passwordHash.isBlank()) {
            tag.putString(PASSWORD_HASH, passwordHash);
        } else {
            tag.remove(PASSWORD_HASH);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void setStoredStructureReference(ItemStack stack, UUID storageUuid, int sizeX, int sizeY, int sizeZ) {
        CompoundTag tag = getOrCreateTag(stack);
        tag.putUUID(STORAGE_UUID, storageUuid);
        tag.putInt(SIZE_X, sizeX);
        tag.putInt(SIZE_Y, sizeY);
        tag.putInt(SIZE_Z, sizeZ);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void clearStoredStructure(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return;
        }

        CompoundTag tag = customData.copyTag();
        tag.remove(STORAGE_UUID);
        tag.remove(SIZE_X);
        tag.remove(SIZE_Y);
        tag.remove(SIZE_Z);
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    private static @Nullable CompoundTag getTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? null : customData.copyTag();
    }

    private static CompoundTag getOrCreateTag(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag == null ? new CompoundTag() : tag;
    }
}
