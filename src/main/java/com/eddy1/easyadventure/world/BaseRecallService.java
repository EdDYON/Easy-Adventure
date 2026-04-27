package com.eddy1.easyadventure.world;

import com.eddy1.easyadventure.block.BaseCoreBlockEntity;
import com.eddy1.easyadventure.block.core.CorePackager;
import com.eddy1.easyadventure.block.core.CoreResident;
import com.eddy1.easyadventure.block.core.CoreUpgrade;
import com.eddy1.easyadventure.block.core.CoreVolume;
import com.eddy1.easyadventure.block.core.CoreClearMode;
import com.eddy1.easyadventure.item.BaseKeyItem;
import com.eddy1.easyadventure.storage.StructureSnapshot;
import com.eddy1.easyadventure.util.KeyDataUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BaseRecallService {
    private BaseRecallService() {
    }

    public static void recall(ServerPlayer player, UUID coreUuid) {
        BaseRegistryData registry = BaseRegistryData.get(player.serverLevel());
        BaseRegistryData.BaseRecord record = registry.getRecord(coreUuid);
        if (record == null || !record.ownerUuid().equals(player.getUUID())) {
            player.displayClientMessage(Component.translatable("message.easyadventure.recall_not_found").withStyle(ChatFormatting.RED), true);
            return;
        }

        if (BaseRegistryData.STATE_PACKED.equals(record.state())) {
            recallPacked(player, record);
            return;
        }
        recallPlaced(player, record);
    }

    public static boolean operatorRecallRegistered(ServerPlayer player, UUID coreUuid) {
        BaseRegistryData.BaseRecord record = BaseRegistryData.get(player.serverLevel()).getRecord(coreUuid);
        if (record == null) {
            return false;
        }
        if (BaseRegistryData.STATE_PACKED.equals(record.state())) {
            recallPacked(player, record);
        } else {
            recallPlaced(player, record);
        }
        return true;
    }

    public static boolean operatorRecoverPacked(ServerPlayer player, UUID coreUuid, String rawBaseName) {
        String baseName = BaseRegistryData.normalizeDisplayName(rawBaseName);
        if (baseName.isBlank()) {
            player.displayClientMessage(Component.translatable("message.easyadventure.base_name_required").withStyle(ChatFormatting.RED), false);
            return false;
        }

        BaseRegistryData registry = BaseRegistryData.get(player.serverLevel());
        if (registry.isNameTaken(player.getUUID(), baseName, coreUuid)) {
            player.displayClientMessage(Component.translatable("message.easyadventure.base_name_duplicate", baseName).withStyle(ChatFormatting.RED), false);
            return false;
        }

        BuildingStorageData storage = BuildingStorageData.get(player.serverLevel());
        UUID storageUuid = storage.getActiveStorageId(coreUuid);
        if (storageUuid == null) {
            return false;
        }

        CompoundTag heavyData = storage.getBuilding(storageUuid);
        if (heavyData == null) {
            return false;
        }

        StructureSnapshot snapshot = StructureSnapshot.fromTag(heavyData);
        UUID keyUuid = UUID.randomUUID();
        removeInventoryKeysForBase(player, coreUuid);
        ItemStack restoredKey = CorePackager.createPackedKey(
                baseName,
                coreUuid,
                storageUuid,
                player.getUUID(),
                player.getGameProfile().getName(),
                false,
                null,
                new CoreVolume(snapshot.sizeX(), snapshot.sizeY(), snapshot.sizeBelowY(), snapshot.sizeZ()),
                CoreClearMode.CLEAR,
                Map.of(),
                Map.of(),
                Map.of(),
                snapshot,
                keyUuid
        );
        registry.registerPacked(
                coreUuid,
                player.getUUID(),
                player.getGameProfile().getName(),
                baseName,
                keyUuid,
                storageUuid,
                snapshot.sourceDimension(),
                restoredKey
        );
        giveDirectly(player, restoredKey);
        return true;
    }

    public static boolean operatorClaimPlaced(ServerPlayer player, BaseCoreBlockEntity core, BlockPos pos, String rawBaseName) {
        String baseName = BaseRegistryData.normalizeDisplayName(rawBaseName);
        if (baseName.isBlank()) {
            player.displayClientMessage(Component.translatable("message.easyadventure.base_name_required").withStyle(ChatFormatting.RED), false);
            return false;
        }

        BaseRegistryData registry = BaseRegistryData.get(player.serverLevel());
        if (registry.isNameTaken(player.getUUID(), baseName, core.getCoreUUID())) {
            player.displayClientMessage(Component.translatable("message.easyadventure.base_name_duplicate", baseName).withStyle(ChatFormatting.RED), false);
            return false;
        }

        core.setOwner(player.getUUID(), player.getGameProfile().getName());
        core.setBaseName(baseName);
        registry.registerPlaced(
                core.getCoreUUID(),
                player.getUUID(),
                player.getGameProfile().getName(),
                baseName,
                null,
                player.serverLevel().dimension().location(),
                pos
        );
        return true;
    }

    private static void recallPacked(ServerPlayer player, BaseRegistryData.BaseRecord record) {
        UUID storageUuid = record.storageUuid();
        if (storageUuid == null) {
            player.displayClientMessage(Component.translatable("message.easyadventure.recall_missing_storage").withStyle(ChatFormatting.RED), true);
            return;
        }

        BuildingStorageData storage = BuildingStorageData.get(player.serverLevel());
        if (storage.isLocked(storageUuid)) {
            player.displayClientMessage(Component.translatable("message.easyadventure.storage_locked").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (hasCurrentKeyInInventory(player, record)) {
            player.displayClientMessage(Component.translatable("message.easyadventure.key_already_in_inventory", record.baseName()).withStyle(ChatFormatting.YELLOW), true);
            return;
        }

        CompoundTag heavyData = storage.getBuilding(storageUuid);
        if (heavyData == null) {
            player.displayClientMessage(Component.translatable("message.easyadventure.storage_missing").withStyle(ChatFormatting.RED), true);
            return;
        }

        UUID newKeyUuid = UUID.randomUUID();
        removeInventoryKeysForBase(player, record.coreUuid());
        ItemStack recalledKey = createRecallKey(record, heavyData, newKeyUuid);
        BaseRegistryData.get(player.serverLevel()).registerPacked(
                record.coreUuid(),
                record.ownerUuid(),
                record.ownerName(),
                record.baseName(),
                newKeyUuid,
                storageUuid,
                record.dimension(),
                recalledKey
        );
        giveDirectly(player, recalledKey);
        player.level().playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.8F, 1.35F);
        player.displayClientMessage(Component.translatable("message.easyadventure.base_recalled_reissued", record.baseName()).withStyle(ChatFormatting.GREEN), true);
    }

    private static void recallPlaced(ServerPlayer player, BaseRegistryData.BaseRecord record) {
        if (record.dimension() == null || record.pos() == null) {
            player.displayClientMessage(Component.translatable("message.easyadventure.recall_missing_position").withStyle(ChatFormatting.RED), true);
            return;
        }

        ResourceLocation dimensionId = ResourceLocation.tryParse(record.dimension());
        if (dimensionId == null) {
            player.displayClientMessage(Component.translatable("message.easyadventure.recall_missing_position").withStyle(ChatFormatting.RED), true);
            return;
        }

        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionId);
        ServerLevel level = player.getServer().getLevel(dimensionKey);
        if (level == null) {
            player.displayClientMessage(Component.translatable("message.easyadventure.recall_missing_position").withStyle(ChatFormatting.RED), true);
            return;
        }

        BlockPos pos = record.pos();
        level.getChunk(pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof BaseCoreBlockEntity core) || !core.getCoreUUID().equals(record.coreUuid())) {
            player.displayClientMessage(Component.translatable("message.easyadventure.recall_core_missing").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!core.startPacking(player, null)) {
            return;
        }
        player.displayClientMessage(Component.translatable("message.easyadventure.remote_recall_started", record.baseName()).withStyle(ChatFormatting.AQUA), true);
    }

    private static ItemStack createRecallKey(BaseRegistryData.BaseRecord record, CompoundTag heavyData, UUID keyUuid) {
        if (record.keyStackTag() != null) {
            ItemStack stack = ItemStack.of(record.keyStackTag().copy());
            if (!stack.isEmpty()) {
                KeyDataUtil.setKeyUuid(stack, keyUuid);
                KeyDataUtil.setObsoleteKey(stack, false);
                return stack;
            }
        }

        StructureSnapshot snapshot = StructureSnapshot.fromTag(heavyData);
        return CorePackager.createPackedKey(
                record.baseName(),
                record.coreUuid(),
                record.storageUuid(),
                record.ownerUuid(),
                record.ownerName(),
                false,
                null,
                new CoreVolume(snapshot.sizeX(), snapshot.sizeY(), snapshot.sizeBelowY(), snapshot.sizeZ()),
                CoreClearMode.CLEAR,
                Map.<UUID, CoreResident>of(),
                Map.<CoreUpgrade, Integer>of(),
                Map.<CoreUpgrade, Integer>of(),
                snapshot,
                keyUuid
        );
    }

    private static boolean hasCurrentKeyInInventory(ServerPlayer player, BaseRegistryData.BaseRecord record) {
        if (record.keyUuid() == null) {
            return false;
        }
        return anyKeyMatches(player, record.coreUuid(), record.keyUuid(), false);
    }

    private static void removeInventoryKeysForBase(ServerPlayer player, UUID coreUuid) {
        forEachInventorySection(player, section -> {
            for (int i = 0; i < section.size(); i++) {
                ItemStack stack = section.get(i);
                if (!(stack.getItem() instanceof BaseKeyItem)) {
                    continue;
                }
                if (!coreUuid.equals(KeyDataUtil.getBoundUuid(stack))) {
                    continue;
                }
                section.set(i, ItemStack.EMPTY);
            }
        });
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    private static void giveDirectly(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    private static boolean anyKeyMatches(ServerPlayer player, UUID coreUuid, UUID keyUuid, boolean obsolete) {
        final boolean[] found = {false};
        forEachInventorySection(player, section -> {
            for (ItemStack stack : section) {
                if (!(stack.getItem() instanceof BaseKeyItem)) {
                    continue;
                }
                if (!coreUuid.equals(KeyDataUtil.getBoundUuid(stack))) {
                    continue;
                }
                if (!keyUuid.equals(KeyDataUtil.getKeyUuid(stack))) {
                    continue;
                }
                if (KeyDataUtil.isObsoleteKey(stack) != obsolete) {
                    continue;
                }
                found[0] = true;
                break;
            }
        });
        if (found[0]) {
            return true;
        }
        PlayerEnderChestContainer enderChest = player.getEnderChestInventory();
        for (int i = 0; i < enderChest.getContainerSize(); i++) {
            ItemStack stack = enderChest.getItem(i);
            if (!(stack.getItem() instanceof BaseKeyItem)) {
                continue;
            }
            if (!coreUuid.equals(KeyDataUtil.getBoundUuid(stack))) {
                continue;
            }
            if (!keyUuid.equals(KeyDataUtil.getKeyUuid(stack))) {
                continue;
            }
            if (KeyDataUtil.isObsoleteKey(stack) != obsolete) {
                continue;
            }
            return true;
        }
        return found[0];
    }

    private static void forEachInventorySection(ServerPlayer player, java.util.function.Consumer<List<ItemStack>> consumer) {
        consumer.accept(player.getInventory().items);
        consumer.accept(player.getInventory().offhand);
        consumer.accept(player.getInventory().armor);
    }
}
