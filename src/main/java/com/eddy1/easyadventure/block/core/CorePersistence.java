package com.eddy1.easyadventure.block.core;

import com.eddy1.easyadventure.block.BaseCoreBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class CorePersistence {
    private CorePersistence() {
    }

    public static void save(
            CompoundTag tag,
            CoreStoredState state,
            BaseCoreBlockEntity.State runtimeState,
            CoreTerrainTracker terrainTracker,
            CoreStructureWorkspace workspace
    ) {
        tag.putString("BaseName", state.baseName());
        tag.putUUID("CoreUUID", state.coreUUID());
        if (state.ownerUUID() != null) {
            tag.putUUID("OwnerUUID", state.ownerUUID());
        }
        if (state.ownerName() != null) {
            tag.putString("OwnerName", state.ownerName());
        }
        if (state.activeStorageUUID() != null) {
            tag.putUUID("ActiveStorageUUID", state.activeStorageUUID());
        }
        tag.putBoolean("PasswordEnabled", state.passwordEnabled());
        if (state.passwordHash() != null) {
            tag.putString("PasswordHash", state.passwordHash());
        }
        tag.putBoolean("IsBound", state.bound());
        tag.putBoolean("HasInitialized", state.initialized());
        tag.putInt("SizeX", state.sizeX());
        tag.putInt("SizeY", state.sizeY());
        tag.putInt("SizeZ", state.sizeZ());
        tag.putString("RuntimeState", runtimeState.name());
        tag.put("OriginalTerrain", terrainTracker.toTag());

        CompoundTag workspaceTag = new CompoundTag();
        workspace.save(workspaceTag);
        tag.put("Workspace", workspaceTag);
    }

    public static CoreStoredState loadStoredState(CompoundTag tag) {
        String baseName = tag.contains("BaseName") ? tag.getString("BaseName") : CoreStoredState.DEFAULT_BASE_NAME;
        UUID coreUUID = tag.contains("CoreUUID") ? tag.getUUID("CoreUUID") : UUID.randomUUID();
        UUID ownerUUID = tag.contains("OwnerUUID") ? tag.getUUID("OwnerUUID") : null;
        String ownerName = tag.contains("OwnerName") ? tag.getString("OwnerName") : null;
        UUID activeStorageUUID = tag.contains("ActiveStorageUUID") ? tag.getUUID("ActiveStorageUUID") : null;
        boolean passwordEnabled = tag.contains("PasswordEnabled") && tag.getBoolean("PasswordEnabled");
        String passwordHash = tag.contains("PasswordHash") ? tag.getString("PasswordHash") : null;
        boolean isBound = tag.contains("IsBound") && tag.getBoolean("IsBound");
        boolean hasInitialized = tag.contains("HasInitialized") && tag.getBoolean("HasInitialized");
        int sizeX = tag.contains("SizeX") ? tag.getInt("SizeX") : 9;
        int sizeY = tag.contains("SizeY") ? tag.getInt("SizeY") : 5;
        int sizeZ = tag.contains("SizeZ") ? tag.getInt("SizeZ") : 9;

        if (tag.contains("RangeXZ")) {
            int range = tag.getInt("RangeXZ");
            sizeX = range * 2 + 1;
            sizeZ = range * 2 + 1;
            hasInitialized = true;
        }
        if (tag.contains("RangeY")) {
            sizeY = tag.getInt("RangeY");
        }

        return new CoreStoredState(baseName, coreUUID, ownerUUID, ownerName, activeStorageUUID, passwordEnabled, passwordHash, isBound, hasInitialized, sizeX, sizeY, sizeZ);
    }

    public static BaseCoreBlockEntity.State loadRuntimeState(CompoundTag tag) {
        if (!tag.contains("RuntimeState")) {
            return BaseCoreBlockEntity.State.IDLE;
        }

        try {
            return BaseCoreBlockEntity.State.valueOf(tag.getString("RuntimeState"));
        } catch (IllegalArgumentException exception) {
            return BaseCoreBlockEntity.State.IDLE;
        }
    }

    public static void loadTransientData(CompoundTag tag, CoreTerrainTracker terrainTracker, CoreStructureWorkspace workspace) {
        if (tag.contains("OriginalTerrain")) {
            terrainTracker.load(tag.getList("OriginalTerrain", Tag.TAG_COMPOUND));
        } else {
            terrainTracker.clear();
        }

        if (tag.contains("Workspace")) {
            workspace.load(tag.getCompound("Workspace"));
            return;
        }

        workspace.clearAll();
        if (tag.contains("PackedEntities")) {
            workspace.loadPackedEntities(tag.getList("PackedEntities", Tag.TAG_COMPOUND));
        }
    }

    public static void writeUpdateTag(CompoundTag tag, CoreStoredState state) {
        tag.putInt("SizeX", state.sizeX());
        tag.putInt("SizeY", state.sizeY());
        tag.putInt("SizeZ", state.sizeZ());
        if (state.ownerName() != null) {
            tag.putString("OwnerName", state.ownerName());
        }
        tag.putBoolean("IsBound", state.bound());
        tag.putBoolean("PasswordEnabled", state.passwordEnabled());
    }
}
