package com.eddy1.easyadventure.block.core;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record CoreStoredState(
        String baseName,
        UUID coreUUID,
        @Nullable UUID ownerUUID,
        @Nullable String ownerName,
        @Nullable UUID activeStorageUUID,
        boolean passwordEnabled,
        @Nullable String passwordHash,
        boolean bound,
        boolean initialized,
        int sizeX,
        int sizeY,
        int sizeBelowY,
        int sizeZ,
        CoreClearMode clearMode,
        boolean foundationEnabled,
        CoreFoundationMaterial foundationMaterial,
        Map<UUID, CoreResident> residents,
        Map<CoreUpgrade, Integer> upgradeFuelTicks
) {
    public static final String DEFAULT_BASE_NAME = "Portable Base";
    private static final UUID LEGACY_TEST_IDENTITY_UUID = UUID.fromString("3d8d2f6d-72c2-4a92-9f5b-4b46f7221001");
    private static final String LEGACY_TEST_IDENTITY_NAME = "测试身份1";

    public CoreStoredState {
        baseName = baseName == null ? DEFAULT_BASE_NAME : baseName;
        coreUUID = coreUUID == null ? UUID.randomUUID() : coreUUID;
        ownerName = ownerName == null || ownerName.isBlank() ? null : ownerName;
        clearMode = clearMode == null ? CoreClearMode.CLEAR : clearMode;
        foundationMaterial = foundationMaterial == null ? CoreFoundationMaterial.COBBLESTONE : foundationMaterial;
        residents = sanitizeResidents(residents);
        upgradeFuelTicks = sanitizeUpgradeFuelTicks(upgradeFuelTicks);
    }

    public CoreVolume volume() {
        return new CoreVolume(sizeX, sizeY, sizeBelowY, sizeZ);
    }

    public CoreStoredState withSize(int sizeX, int sizeY, int sizeBelowY, int sizeZ) {
        return new CoreStoredState(baseName, coreUUID, ownerUUID, ownerName, activeStorageUUID, passwordEnabled, passwordHash, bound, initialized, sizeX, sizeY, sizeBelowY, sizeZ, clearMode, foundationEnabled, foundationMaterial, residents, upgradeFuelTicks);
    }

    public CoreStoredState withName(String name) {
        return new CoreStoredState(name, coreUUID, ownerUUID, ownerName, activeStorageUUID, passwordEnabled, passwordHash, bound, initialized, sizeX, sizeY, sizeBelowY, sizeZ, clearMode, foundationEnabled, foundationMaterial, residents, upgradeFuelTicks);
    }

    public CoreStoredState withBinding(boolean bound, @Nullable UUID coreUuid) {
        return new CoreStoredState(baseName, coreUuid == null ? coreUUID : coreUuid, ownerUUID, ownerName, activeStorageUUID, passwordEnabled, passwordHash, bound, initialized, sizeX, sizeY, sizeBelowY, sizeZ, clearMode, foundationEnabled, foundationMaterial, residents, upgradeFuelTicks);
    }

    public CoreStoredState withInitialized(boolean initialized) {
        return new CoreStoredState(baseName, coreUUID, ownerUUID, ownerName, activeStorageUUID, passwordEnabled, passwordHash, bound, initialized, sizeX, sizeY, sizeBelowY, sizeZ, clearMode, foundationEnabled, foundationMaterial, residents, upgradeFuelTicks);
    }

    public CoreStoredState withOwner(@Nullable UUID ownerUuid, @Nullable String ownerName) {
        return new CoreStoredState(baseName, coreUUID, ownerUuid, ownerName, activeStorageUUID, passwordEnabled, passwordHash, bound, initialized, sizeX, sizeY, sizeBelowY, sizeZ, clearMode, foundationEnabled, foundationMaterial, residents, upgradeFuelTicks);
    }

    public CoreStoredState withActiveStorage(@Nullable UUID storageUuid) {
        return new CoreStoredState(baseName, coreUUID, ownerUUID, ownerName, storageUuid, passwordEnabled, passwordHash, bound, initialized, sizeX, sizeY, sizeBelowY, sizeZ, clearMode, foundationEnabled, foundationMaterial, residents, upgradeFuelTicks);
    }

    public CoreStoredState withPassword(boolean passwordEnabled, @Nullable String passwordHash) {
        return new CoreStoredState(baseName, coreUUID, ownerUUID, ownerName, activeStorageUUID, passwordEnabled, passwordHash, bound, initialized, sizeX, sizeY, sizeBelowY, sizeZ, clearMode, foundationEnabled, foundationMaterial, residents, upgradeFuelTicks);
    }

    public CoreStoredState withClearMode(CoreClearMode clearMode) {
        return new CoreStoredState(baseName, coreUUID, ownerUUID, ownerName, activeStorageUUID, passwordEnabled, passwordHash, bound, initialized, sizeX, sizeY, sizeBelowY, sizeZ, clearMode, foundationEnabled, foundationMaterial, residents, upgradeFuelTicks);
    }

    public CoreStoredState withFoundation(boolean foundationEnabled, CoreFoundationMaterial foundationMaterial) {
        return new CoreStoredState(baseName, coreUUID, ownerUUID, ownerName, activeStorageUUID, passwordEnabled, passwordHash, bound, initialized, sizeX, sizeY, sizeBelowY, sizeZ, clearMode, foundationEnabled, foundationMaterial, residents, upgradeFuelTicks);
    }

    public boolean hasResident(@Nullable UUID playerUuid) {
        return playerUuid != null && residents.containsKey(playerUuid);
    }

    public CoreStoredState withResidents(Map<UUID, CoreResident> residents) {
        return new CoreStoredState(baseName, coreUUID, ownerUUID, ownerName, activeStorageUUID, passwordEnabled, passwordHash, bound, initialized, sizeX, sizeY, sizeBelowY, sizeZ, clearMode, foundationEnabled, foundationMaterial, residents, upgradeFuelTicks);
    }

    public CoreStoredState withUpgradeFuelTicks(Map<CoreUpgrade, Integer> upgradeFuelTicks) {
        return new CoreStoredState(baseName, coreUUID, ownerUUID, ownerName, activeStorageUUID, passwordEnabled, passwordHash, bound, initialized, sizeX, sizeY, sizeBelowY, sizeZ, clearMode, foundationEnabled, foundationMaterial, residents, upgradeFuelTicks);
    }

    public CoreStoredState addResident(UUID residentUuid, @Nullable String residentName) {
        LinkedHashMap<UUID, CoreResident> updated = new LinkedHashMap<>(residents);
        updated.put(residentUuid, new CoreResident(residentUuid, residentName, CoreResident.DEFAULT_PERMISSION_BITS));
        return withResidents(updated);
    }

    public CoreStoredState removeResident(UUID residentUuid) {
        if (!residents.containsKey(residentUuid)) {
            return this;
        }

        LinkedHashMap<UUID, CoreResident> updated = new LinkedHashMap<>(residents);
        updated.remove(residentUuid);
        return withResidents(updated);
    }

    public CoreStoredState updateResidentPermission(UUID residentUuid, CorePermission permission, boolean enabled) {
        CoreResident resident = residents.get(residentUuid);
        if (resident == null) {
            return this;
        }

        LinkedHashMap<UUID, CoreResident> updated = new LinkedHashMap<>(residents);
        updated.put(residentUuid, resident.withPermission(permission, enabled));
        return withResidents(updated);
    }

    public boolean hasResidentPermission(@Nullable UUID residentUuid, CorePermission permission) {
        if (residentUuid == null) {
            return false;
        }

        CoreResident resident = residents.get(residentUuid);
        return resident != null && resident.hasPermission(permission);
    }

    public boolean hasUpgrade(CoreUpgrade upgrade) {
        return getUpgradeFuelTicks(upgrade) > 0;
    }

    public int getUpgradeFuelTicks(CoreUpgrade upgrade) {
        return upgradeFuelTicks.getOrDefault(upgrade, 0);
    }

    public int activeUpgradeCount() {
        int count = 0;
        for (CoreUpgrade upgrade : CoreUpgrade.values()) {
            if (hasUpgrade(upgrade)) {
                count++;
            }
        }
        return count;
    }

    public CoreStoredState addUpgradeFuel(CoreUpgrade upgrade) {
        EnumMap<CoreUpgrade, Integer> updated = new EnumMap<>(CoreUpgrade.class);
        updated.putAll(upgradeFuelTicks);
        updated.put(upgrade, upgrade.addFuel(getUpgradeFuelTicks(upgrade)));
        return withUpgradeFuelTicks(updated);
    }

    public CoreStoredState tickUpgradeFuel(int deltaTicks) {
        if (upgradeFuelTicks.isEmpty() || deltaTicks <= 0) {
            return this;
        }

        EnumMap<CoreUpgrade, Integer> updated = new EnumMap<>(CoreUpgrade.class);
        boolean changed = false;
        for (CoreUpgrade upgrade : CoreUpgrade.values()) {
            int remaining = getUpgradeFuelTicks(upgrade);
            if (remaining <= 0) {
                continue;
            }

            int nextRemaining = Math.max(0, remaining - deltaTicks);
            if (nextRemaining > 0) {
                updated.put(upgrade, nextRemaining);
            }
            changed |= nextRemaining != remaining;
        }
        return changed ? withUpgradeFuelTicks(updated) : this;
    }

    public @Nullable UUID findResidentByName(@Nullable String residentName) {
        if (residentName == null || residentName.isBlank()) {
            return null;
        }

        for (Map.Entry<UUID, CoreResident> entry : residents.entrySet()) {
            if (entry.getValue().name().equalsIgnoreCase(residentName.trim())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static Map<UUID, CoreResident> sanitizeResidents(@Nullable Map<UUID, CoreResident> residents) {
        if (residents == null || residents.isEmpty()) {
            return Map.of();
        }

        LinkedHashMap<UUID, CoreResident> sanitized = new LinkedHashMap<>();
        residents.forEach((uuid, resident) -> {
            if (uuid != null && resident != null && !isLegacyTestResident(uuid, resident.name())) {
                sanitized.put(uuid, new CoreResident(uuid, resident.name(), resident.permissionBits()));
            }
        });
        return sanitized.isEmpty() ? Map.of() : Collections.unmodifiableMap(sanitized);
    }

    private static boolean isLegacyTestResident(UUID uuid, @Nullable String name) {
        return LEGACY_TEST_IDENTITY_UUID.equals(uuid) || LEGACY_TEST_IDENTITY_NAME.equalsIgnoreCase(name == null ? "" : name.trim());
    }

    private static Map<CoreUpgrade, Integer> sanitizeUpgradeFuelTicks(@Nullable Map<CoreUpgrade, Integer> upgradeFuelTicks) {
        if (upgradeFuelTicks == null || upgradeFuelTicks.isEmpty()) {
            return Map.of();
        }

        EnumMap<CoreUpgrade, Integer> sanitized = new EnumMap<>(CoreUpgrade.class);
        upgradeFuelTicks.forEach((upgrade, ticks) -> {
            if (upgrade == null || ticks == null || ticks <= 0) {
                return;
            }
            sanitized.put(upgrade, Math.min(upgrade.maxDurationTicks(), ticks));
        });
        return sanitized.isEmpty() ? Map.of() : Collections.unmodifiableMap(sanitized);
    }
}
