package com.eddy1.easyadventure.block.core;

import org.jetbrains.annotations.Nullable;

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
        int sizeZ
) {
    public static final String DEFAULT_BASE_NAME = "Portable Base";

    public CoreStoredState {
        baseName = baseName == null ? DEFAULT_BASE_NAME : baseName;
        coreUUID = coreUUID == null ? UUID.randomUUID() : coreUUID;
        ownerName = ownerName == null || ownerName.isBlank() ? null : ownerName;
    }

    public CoreVolume volume() {
        return new CoreVolume(sizeX, sizeY, sizeZ);
    }

    public CoreStoredState withSize(int sizeX, int sizeY, int sizeZ) {
        return new CoreStoredState(baseName, coreUUID, ownerUUID, ownerName, activeStorageUUID, passwordEnabled, passwordHash, bound, initialized, sizeX, sizeY, sizeZ);
    }

    public CoreStoredState withName(String name) {
        return new CoreStoredState(name, coreUUID, ownerUUID, ownerName, activeStorageUUID, passwordEnabled, passwordHash, bound, initialized, sizeX, sizeY, sizeZ);
    }

    public CoreStoredState withBinding(boolean bound, @Nullable UUID coreUuid) {
        return new CoreStoredState(baseName, coreUuid == null ? coreUUID : coreUuid, ownerUUID, ownerName, activeStorageUUID, passwordEnabled, passwordHash, bound, initialized, sizeX, sizeY, sizeZ);
    }

    public CoreStoredState withInitialized(boolean initialized) {
        return new CoreStoredState(baseName, coreUUID, ownerUUID, ownerName, activeStorageUUID, passwordEnabled, passwordHash, bound, initialized, sizeX, sizeY, sizeZ);
    }

    public CoreStoredState withOwner(@Nullable UUID ownerUuid, @Nullable String ownerName) {
        return new CoreStoredState(baseName, coreUUID, ownerUuid, ownerName, activeStorageUUID, passwordEnabled, passwordHash, bound, initialized, sizeX, sizeY, sizeZ);
    }

    public CoreStoredState withActiveStorage(@Nullable UUID storageUuid) {
        return new CoreStoredState(baseName, coreUUID, ownerUUID, ownerName, storageUuid, passwordEnabled, passwordHash, bound, initialized, sizeX, sizeY, sizeZ);
    }

    public CoreStoredState withPassword(boolean passwordEnabled, @Nullable String passwordHash) {
        return new CoreStoredState(baseName, coreUUID, ownerUUID, ownerName, activeStorageUUID, passwordEnabled, passwordHash, bound, initialized, sizeX, sizeY, sizeZ);
    }
}
