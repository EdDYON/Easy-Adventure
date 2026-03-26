package com.eddy1.easyadventure.block.core;

import com.eddy1.easyadventure.block.BaseCoreBlockEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class CorePersistence {
    private CorePersistence() {
    }

    public static void save(
            CompoundTag tag,
            CoreStoredState state,
            BaseCoreBlockEntity.State runtimeState,
            CoreTerrainTracker terrainTracker,
            CoreStructureWorkspace workspace,
            NonNullList<ItemStack> upgradeFuelInventory
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
        if (!state.residents().isEmpty()) {
            tag.put("Residents", serializeResidents(state.residents()));
        }
        if (!state.upgradeFuelTicks().isEmpty()) {
            tag.put("UpgradeFuel", serializeUpgradeFuel(state.upgradeFuelTicks()));
        }
        tag.putString("RuntimeState", runtimeState.name());
        tag.put("OriginalTerrain", terrainTracker.toTag());

        CompoundTag workspaceTag = new CompoundTag();
        workspace.save(workspaceTag);
        tag.put("Workspace", workspaceTag);
        ContainerHelper.saveAllItems(tag, upgradeFuelInventory);
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
        Map<UUID, CoreResident> residents = tag.contains("Residents")
                ? deserializeResidents(tag.getList("Residents", Tag.TAG_COMPOUND))
                : tag.contains("Collaborators")
                ? deserializeLegacyCollaborators(tag.getList("Collaborators", Tag.TAG_COMPOUND))
                : Map.of();
        Map<CoreUpgrade, Integer> upgradeFuelTicks = tag.contains("UpgradeFuel")
                ? deserializeUpgradeFuel(tag.getList("UpgradeFuel", Tag.TAG_COMPOUND))
                : migrateLegacyUnlockedUpgrades(tag.contains("UnlockedUpgrades") ? tag.getInt("UnlockedUpgrades") : 0);

        if (tag.contains("RangeXZ")) {
            int range = tag.getInt("RangeXZ");
            sizeX = range * 2 + 1;
            sizeZ = range * 2 + 1;
            hasInitialized = true;
        }
        if (tag.contains("RangeY")) {
            sizeY = tag.getInt("RangeY");
        }

        return new CoreStoredState(
                baseName,
                coreUUID,
                ownerUUID,
                ownerName,
                activeStorageUUID,
                passwordEnabled,
                passwordHash,
                isBound,
                hasInitialized,
                sizeX,
                sizeY,
                sizeZ,
                residents,
                upgradeFuelTicks
        );
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

    public static void loadTransientData(CompoundTag tag, CoreTerrainTracker terrainTracker, CoreStructureWorkspace workspace, NonNullList<ItemStack> upgradeFuelInventory) {
        if (tag.contains("OriginalTerrain")) {
            terrainTracker.load(tag.getList("OriginalTerrain", Tag.TAG_COMPOUND));
        }

        if (tag.contains("Workspace")) {
            workspace.load(tag.getCompound("Workspace"));
        } else if (tag.contains("PackedEntities")) {
            workspace.clearAll();
            workspace.loadPackedEntities(tag.getList("PackedEntities", Tag.TAG_COMPOUND));
        }

        if (tag.contains("Items", Tag.TAG_LIST)) {
            for (int i = 0; i < upgradeFuelInventory.size(); i++) {
                upgradeFuelInventory.set(i, ItemStack.EMPTY);
            }
            ContainerHelper.loadAllItems(tag, upgradeFuelInventory);
        }
    }

    public static void writeUpdateTag(
            CompoundTag tag,
            CoreStoredState state,
            NonNullList<ItemStack> upgradeFuelInventory
    ) {
        tag.putString("BaseName", state.baseName());
        tag.putUUID("CoreUUID", state.coreUUID());
        if (state.ownerUUID() != null) {
            tag.putUUID("OwnerUUID", state.ownerUUID());
        }
        tag.putInt("SizeX", state.sizeX());
        tag.putInt("SizeY", state.sizeY());
        tag.putInt("SizeZ", state.sizeZ());
        if (state.ownerName() != null) {
            tag.putString("OwnerName", state.ownerName());
        }
        tag.putBoolean("IsBound", state.bound());
        tag.putBoolean("PasswordEnabled", state.passwordEnabled());
        if (!state.residents().isEmpty()) {
            tag.put("Residents", serializeResidents(state.residents()));
        }
        if (!state.upgradeFuelTicks().isEmpty()) {
            tag.put("UpgradeFuel", serializeUpgradeFuel(state.upgradeFuelTicks()));
        }
        ContainerHelper.saveAllItems(tag, upgradeFuelInventory);
    }

    private static ListTag serializeResidents(Map<UUID, CoreResident> residents) {
        ListTag list = new ListTag();
        residents.forEach((uuid, resident) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("UUID", uuid);
            entry.putString("Name", resident.name());
            entry.putInt("Permissions", resident.permissionBits());
            list.add(entry);
        });
        return list;
    }

    private static Map<UUID, CoreResident> deserializeResidents(ListTag residentList) {
        LinkedHashMap<UUID, CoreResident> residents = new LinkedHashMap<>();
        for (Tag entry : residentList) {
            CompoundTag residentTag = (CompoundTag) entry;
            if (!residentTag.hasUUID("UUID")) {
                continue;
            }

            UUID uuid = residentTag.getUUID("UUID");
            String name = residentTag.contains("Name") ? residentTag.getString("Name") : uuid.toString();
            int permissionBits = residentTag.contains("Permissions")
                    ? residentTag.getInt("Permissions")
                    : CoreResident.DEFAULT_PERMISSION_BITS;
            residents.put(uuid, new CoreResident(uuid, name, permissionBits));
        }
        return residents;
    }

    private static Map<UUID, CoreResident> deserializeLegacyCollaborators(ListTag collaboratorList) {
        LinkedHashMap<UUID, CoreResident> residents = new LinkedHashMap<>();
        for (Tag entry : collaboratorList) {
            CompoundTag collaboratorTag = (CompoundTag) entry;
            if (!collaboratorTag.hasUUID("UUID")) {
                continue;
            }

            UUID uuid = collaboratorTag.getUUID("UUID");
            String name = collaboratorTag.contains("Name") ? collaboratorTag.getString("Name") : uuid.toString();
            residents.put(uuid, new CoreResident(uuid, name, CoreResident.LEGACY_COLLABORATOR_PERMISSION_BITS));
        }
        return residents;
    }

    private static ListTag serializeUpgradeFuel(Map<CoreUpgrade, Integer> upgradeFuelTicks) {
        ListTag list = new ListTag();
        upgradeFuelTicks.forEach((upgrade, ticks) -> {
            if (ticks == null || ticks <= 0) {
                return;
            }

            CompoundTag entry = new CompoundTag();
            entry.putString("Upgrade", upgrade.name());
            entry.putInt("Ticks", ticks);
            list.add(entry);
        });
        return list;
    }

    private static Map<CoreUpgrade, Integer> deserializeUpgradeFuel(ListTag upgradeFuelList) {
        Map<CoreUpgrade, Integer> upgradeFuelTicks = new java.util.EnumMap<>(CoreUpgrade.class);
        for (Tag entry : upgradeFuelList) {
            CompoundTag upgradeTag = (CompoundTag) entry;
            if (!upgradeTag.contains("Upgrade") || !upgradeTag.contains("Ticks")) {
                continue;
            }

            try {
                CoreUpgrade upgrade = CoreUpgrade.valueOf(upgradeTag.getString("Upgrade"));
                int ticks = Math.max(0, upgradeTag.getInt("Ticks"));
                if (ticks > 0) {
                    upgradeFuelTicks.put(upgrade, Math.min(upgrade.maxDurationTicks(), ticks));
                }
            } catch (IllegalArgumentException exception) {
                // Ignore legacy or invalid upgrade ids.
            }
        }
        return upgradeFuelTicks;
    }

    private static Map<CoreUpgrade, Integer> migrateLegacyUnlockedUpgrades(int unlockedUpgradeBits) {
        Map<CoreUpgrade, Integer> upgradeFuelTicks = new java.util.EnumMap<>(CoreUpgrade.class);
        if (unlockedUpgradeBits == 0) {
            return upgradeFuelTicks;
        }

        for (int i = 0; i < CoreUpgrade.values().length; i++) {
            if ((unlockedUpgradeBits & (1 << i)) == 0) {
                continue;
            }
            CoreUpgrade upgrade = CoreUpgrade.fromId(i);
            upgradeFuelTicks.put(upgrade, upgrade.maxDurationTicks());
        }
        return upgradeFuelTicks;
    }
}
