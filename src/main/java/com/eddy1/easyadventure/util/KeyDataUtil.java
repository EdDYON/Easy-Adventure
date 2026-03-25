package com.eddy1.easyadventure.util;

import com.eddy1.easyadventure.block.core.CoreResident;
import com.eddy1.easyadventure.block.core.CoreUpgrade;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class KeyDataUtil {
    private static final UUID LEGACY_TEST_IDENTITY_UUID = UUID.fromString("3d8d2f6d-72c2-4a92-9f5b-4b46f7221001");
    private static final String LEGACY_TEST_IDENTITY_NAME = "测试身份1";

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
    public static final String RESIDENTS = "Residents";
    public static final String COLLABORATORS = "Collaborators";
    public static final String UPGRADE_FUEL = "UpgradeFuel";
    public static final String UPGRADE_FUEL_QUEUE = "UpgradeFuelQueue";
    public static final String UPGRADE_BITS = "UnlockedUpgradeBits";
    public static final String PACKED_AT = "PackedAt";
    public static final String SOURCE_DIMENSION = "SourceDimension";
    public static final String BLOCK_COUNT = "BlockCount";
    public static final String BLOCK_ENTITY_COUNT = "BlockEntityCount";
    public static final String ENTITY_COUNT = "EntityCount";
    public static final String DEPLOY_ROTATION = "DeployRotation";

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

    public static Map<UUID, CoreResident> getResidents(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        if (tag == null) {
            return Map.of();
        }

        if (tag.contains(RESIDENTS, Tag.TAG_LIST)) {
            return readResidents(tag.getList(RESIDENTS, Tag.TAG_COMPOUND), false);
        }
        if (tag.contains(COLLABORATORS, Tag.TAG_LIST)) {
            return readResidents(tag.getList(COLLABORATORS, Tag.TAG_COMPOUND), true);
        }
        return Map.of();
    }

    public static Map<CoreUpgrade, Integer> getUpgradeFuelTicks(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        if (tag == null) {
            return Map.of();
        }

        if (tag.contains(UPGRADE_FUEL, Tag.TAG_LIST)) {
            return readUpgradeFuel(tag.getList(UPGRADE_FUEL, Tag.TAG_COMPOUND));
        }
        int legacyBits = tag.contains(UPGRADE_BITS) ? tag.getInt(UPGRADE_BITS) : 0;
        if (legacyBits == 0) {
            return Map.of();
        }

        java.util.EnumMap<CoreUpgrade, Integer> migratedFuel = new java.util.EnumMap<>(CoreUpgrade.class);
        for (int i = 0; i < CoreUpgrade.values().length; i++) {
            if ((legacyBits & (1 << i)) != 0) {
                CoreUpgrade upgrade = CoreUpgrade.fromId(i);
                migratedFuel.put(upgrade, upgrade.maxDurationTicks());
            }
        }
        return migratedFuel;
    }

    public static int getUpgradeFuelTicks(ItemStack stack, CoreUpgrade upgrade) {
        return getUpgradeFuelTicks(stack).getOrDefault(upgrade, 0);
    }

    public static Map<CoreUpgrade, Integer> getQueuedUpgradeFuelCounts(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        if (tag == null || !tag.contains(UPGRADE_FUEL_QUEUE, Tag.TAG_LIST)) {
            return Map.of();
        }
        return readUpgradeFuelCounts(tag.getList(UPGRADE_FUEL_QUEUE, Tag.TAG_COMPOUND));
    }

    public static boolean hasActiveUpgrade(ItemStack stack, CoreUpgrade upgrade) {
        return getUpgradeFuelTicks(stack, upgrade) > 0;
    }

    public static @Nullable String getPackedAt(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag != null && tag.contains(PACKED_AT) ? tag.getString(PACKED_AT) : null;
    }

    public static @Nullable String getSourceDimension(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag != null && tag.contains(SOURCE_DIMENSION) ? tag.getString(SOURCE_DIMENSION) : null;
    }

    public static int getBlockCount(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag != null && tag.contains(BLOCK_COUNT) ? tag.getInt(BLOCK_COUNT) : 0;
    }

    public static int getBlockEntityCount(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag != null && tag.contains(BLOCK_ENTITY_COUNT) ? tag.getInt(BLOCK_ENTITY_COUNT) : 0;
    }

    public static int getEntityCount(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag != null && tag.contains(ENTITY_COUNT) ? tag.getInt(ENTITY_COUNT) : 0;
    }

    public static int getDeployRotation(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return normalizeRotation(tag != null && tag.contains(DEPLOY_ROTATION) ? tag.getInt(DEPLOY_ROTATION) : 0);
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
            int sizeZ,
            Map<UUID, CoreResident> residents,
            Map<CoreUpgrade, Integer> upgradeFuelTicks,
            Map<CoreUpgrade, Integer> queuedUpgradeFuelCounts
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
            tag.remove(PACKED_AT);
            tag.remove(SOURCE_DIMENSION);
            tag.remove(BLOCK_COUNT);
            tag.remove(BLOCK_ENTITY_COUNT);
            tag.remove(ENTITY_COUNT);
            tag.remove(DEPLOY_ROTATION);
            tag.remove(UPGRADE_FUEL_QUEUE);
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
        writeResidents(tag, residents);
        writeUpgradeFuel(tag, upgradeFuelTicks);
        writeUpgradeFuelCounts(tag, queuedUpgradeFuelCounts);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void setStoredStructureReference(
            ItemStack stack,
            UUID storageUuid,
            int sizeX,
            int sizeY,
            int sizeZ,
            @Nullable String packedAt,
            @Nullable String sourceDimension,
            int blockCount,
            int blockEntityCount,
            int entityCount
    ) {
        CompoundTag tag = getOrCreateTag(stack);
        tag.putUUID(STORAGE_UUID, storageUuid);
        tag.putInt(SIZE_X, sizeX);
        tag.putInt(SIZE_Y, sizeY);
        tag.putInt(SIZE_Z, sizeZ);
        setOptionalString(tag, PACKED_AT, packedAt);
        setOptionalString(tag, SOURCE_DIMENSION, sourceDimension);
        tag.putInt(BLOCK_COUNT, Math.max(0, blockCount));
        tag.putInt(BLOCK_ENTITY_COUNT, Math.max(0, blockEntityCount));
        tag.putInt(ENTITY_COUNT, Math.max(0, entityCount));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void setDeployRotation(ItemStack stack, int rotation) {
        CompoundTag tag = getOrCreateTag(stack);
        tag.putInt(DEPLOY_ROTATION, normalizeRotation(rotation));
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
        tag.remove(PACKED_AT);
        tag.remove(SOURCE_DIMENSION);
        tag.remove(BLOCK_COUNT);
        tag.remove(BLOCK_ENTITY_COUNT);
        tag.remove(ENTITY_COUNT);
        tag.remove(DEPLOY_ROTATION);
        tag.remove(UPGRADE_FUEL_QUEUE);
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

    private static Map<UUID, CoreResident> readResidents(ListTag residentList, boolean legacyCollaborators) {
        LinkedHashMap<UUID, CoreResident> residents = new LinkedHashMap<>();
        for (Tag entry : residentList) {
            CompoundTag residentTag = (CompoundTag) entry;
            if (!residentTag.hasUUID("UUID")) {
                continue;
            }

            UUID uuid = residentTag.getUUID("UUID");
            String name = residentTag.contains("Name") ? residentTag.getString("Name") : uuid.toString();
            if (isLegacyTestResident(uuid, name)) {
                continue;
            }
            int permissionBits = legacyCollaborators
                    ? CoreResident.LEGACY_COLLABORATOR_PERMISSION_BITS
                    : residentTag.contains("Permissions")
                    ? residentTag.getInt("Permissions")
                    : CoreResident.DEFAULT_PERMISSION_BITS;
            residents.put(uuid, new CoreResident(uuid, name, permissionBits));
        }
        return residents.isEmpty() ? Map.of() : Collections.unmodifiableMap(residents);
    }

    private static void writeResidents(CompoundTag tag, Map<UUID, CoreResident> residents) {
        if (residents == null || residents.isEmpty()) {
            tag.remove(RESIDENTS);
            return;
        }

        ListTag list = new ListTag();
        residents.forEach((uuid, resident) -> {
            if (uuid == null || isLegacyTestResident(uuid, resident == null ? null : resident.name())) {
                return;
            }

            CompoundTag residentTag = new CompoundTag();
            residentTag.putUUID("UUID", uuid);
            residentTag.putString("Name", resident.name());
            residentTag.putInt("Permissions", resident.permissionBits());
            list.add(residentTag);
        });

        if (list.isEmpty()) {
            tag.remove(RESIDENTS);
        } else {
            tag.put(RESIDENTS, list);
        }
    }

    private static Map<CoreUpgrade, Integer> readUpgradeFuel(ListTag upgradeFuelList) {
        java.util.EnumMap<CoreUpgrade, Integer> upgradeFuelTicks = new java.util.EnumMap<>(CoreUpgrade.class);
        for (Tag entry : upgradeFuelList) {
            CompoundTag fuelTag = (CompoundTag) entry;
            if (!fuelTag.contains("Upgrade") || !fuelTag.contains("Ticks")) {
                continue;
            }

            try {
                CoreUpgrade upgrade = CoreUpgrade.valueOf(fuelTag.getString("Upgrade"));
                int ticks = Math.max(0, fuelTag.getInt("Ticks"));
                if (ticks > 0) {
                    upgradeFuelTicks.put(upgrade, Math.min(upgrade.maxDurationTicks(), ticks));
                }
            } catch (IllegalArgumentException exception) {
                // Ignore invalid upgrade ids.
            }
        }
        return upgradeFuelTicks.isEmpty() ? Map.of() : Collections.unmodifiableMap(upgradeFuelTicks);
    }

    private static void writeUpgradeFuel(CompoundTag tag, Map<CoreUpgrade, Integer> upgradeFuelTicks) {
        if (upgradeFuelTicks == null || upgradeFuelTicks.isEmpty()) {
            tag.remove(UPGRADE_FUEL);
            tag.remove(UPGRADE_BITS);
            return;
        }

        ListTag list = new ListTag();
        upgradeFuelTicks.forEach((upgrade, ticks) -> {
            if (upgrade == null || ticks == null || ticks <= 0) {
                return;
            }

            CompoundTag fuelTag = new CompoundTag();
            fuelTag.putString("Upgrade", upgrade.name());
            fuelTag.putInt("Ticks", Math.min(upgrade.maxDurationTicks(), ticks));
            list.add(fuelTag);
        });

        if (list.isEmpty()) {
            tag.remove(UPGRADE_FUEL);
            tag.remove(UPGRADE_BITS);
        } else {
            tag.put(UPGRADE_FUEL, list);
            tag.remove(UPGRADE_BITS);
        }
    }

    private static Map<CoreUpgrade, Integer> readUpgradeFuelCounts(ListTag upgradeFuelList) {
        java.util.EnumMap<CoreUpgrade, Integer> queuedCounts = new java.util.EnumMap<>(CoreUpgrade.class);
        for (Tag entry : upgradeFuelList) {
            CompoundTag fuelTag = (CompoundTag) entry;
            if (!fuelTag.contains("Upgrade") || !fuelTag.contains("Count")) {
                continue;
            }

            try {
                CoreUpgrade upgrade = CoreUpgrade.valueOf(fuelTag.getString("Upgrade"));
                int count = Math.max(0, fuelTag.getInt("Count"));
                if (count > 0) {
                    queuedCounts.put(upgrade, Math.min(upgrade.material().getDefaultMaxStackSize(), count));
                }
            } catch (IllegalArgumentException exception) {
                // Ignore invalid upgrade ids.
            }
        }
        return queuedCounts.isEmpty() ? Map.of() : Collections.unmodifiableMap(queuedCounts);
    }

    private static void writeUpgradeFuelCounts(CompoundTag tag, Map<CoreUpgrade, Integer> queuedUpgradeFuelCounts) {
        if (queuedUpgradeFuelCounts == null || queuedUpgradeFuelCounts.isEmpty()) {
            tag.remove(UPGRADE_FUEL_QUEUE);
            return;
        }

        ListTag list = new ListTag();
        queuedUpgradeFuelCounts.forEach((upgrade, count) -> {
            if (upgrade == null || count == null || count <= 0) {
                return;
            }

            CompoundTag fuelTag = new CompoundTag();
            fuelTag.putString("Upgrade", upgrade.name());
            fuelTag.putInt("Count", count);
            list.add(fuelTag);
        });

        if (list.isEmpty()) {
            tag.remove(UPGRADE_FUEL_QUEUE);
        } else {
            tag.put(UPGRADE_FUEL_QUEUE, list);
        }
    }

    private static void setOptionalString(CompoundTag tag, String key, @Nullable String value) {
        if (value == null || value.isBlank()) {
            tag.remove(key);
            return;
        }
        tag.putString(key, value);
    }

    private static boolean isLegacyTestResident(UUID uuid, @Nullable String name) {
        return LEGACY_TEST_IDENTITY_UUID.equals(uuid) || LEGACY_TEST_IDENTITY_NAME.equalsIgnoreCase(name == null ? "" : name.trim());
    }

    private static int normalizeRotation(int rotation) {
        return Math.floorMod(rotation, 4);
    }
}
