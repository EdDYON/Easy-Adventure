package com.eddy1.easyadventure.block.core;

import com.eddy1.easyadventure.init.ModItems;
import com.eddy1.easyadventure.storage.StructureSnapshot;
import com.eddy1.easyadventure.util.KeyDataUtil;
import com.eddy1.easyadventure.world.BuildingStorageData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

public final class CorePackager {
    private static final DateTimeFormatter PACKED_AT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private CorePackager() {
    }

    public static @Nullable CompoundTag captureBlockEntityData(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return null;
        }

        CompoundTag tag = blockEntity.saveWithFullMetadata(level.registryAccess());
        Clearable.tryClear(blockEntity);
        level.removeBlockEntity(pos);
        return tag;
    }

    public static ItemStack createPackedKey(
            String baseName,
            UUID coreUuid,
            UUID storageUuid,
            @Nullable UUID ownerUuid,
            @Nullable String ownerName,
            boolean passwordEnabled,
            @Nullable String passwordHash,
            CoreVolume volume,
            Map<UUID, CoreResident> residents,
            Map<CoreUpgrade, Integer> upgradeFuelTicks,
            Map<CoreUpgrade, Integer> queuedUpgradeFuelCounts,
            StructureSnapshot snapshot
    ) {
        ItemStack keyStack = new ItemStack(ModItems.BASE_KEY_ITEM.get());
        KeyDataUtil.setKeyData(
                keyStack,
                coreUuid,
                storageUuid,
                ownerUuid,
                ownerName,
                baseName,
                passwordEnabled,
                passwordHash,
                volume.sizeX(),
                volume.sizeY(),
                volume.sizeZ(),
                residents,
                upgradeFuelTicks,
                queuedUpgradeFuelCounts
        );
        KeyDataUtil.setStoredStructureReference(
                keyStack,
                storageUuid,
                snapshot.sizeX(),
                snapshot.sizeY(),
                snapshot.sizeZ(),
                snapshot.packedAt(),
                snapshot.sourceDimension(),
                snapshot.blockCount(),
                snapshot.blockEntityCount(),
                snapshot.entityCount()
        );
        return keyStack;
    }

    public static void giveOrDropPackedKey(Level level, BlockPos origin, ItemStack stack, @Nullable UUID preferredOwnerUuid) {
        boolean given = false;
        Player targetPlayer = findRecipient(level, origin, preferredOwnerUuid);
        if (targetPlayer != null) {
            given = targetPlayer.getInventory().add(stack);
            if (given) {
                targetPlayer.playSound(SoundEvents.ITEM_PICKUP, 1.0F, 1.0F);
                targetPlayer.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.easyadventure.packed"), true);
            }
        }

        if (!given) {
            ItemEntity itemEntity = new ItemEntity(level, origin.getX() + 0.5, origin.getY() + 1.2, origin.getZ() + 0.5, stack);
            itemEntity.setDeltaMovement(0, 0.4, 0);
            itemEntity.setNoPickUpDelay();
            level.addFreshEntity(itemEntity);
        }
    }

    public static UUID storePackedSnapshot(BuildingStorageData storage, UUID coreUuid, StructureSnapshot snapshot) {
        return storage.saveBuilding(coreUuid, snapshot.toTag());
    }

    public static void finishPacking(
            Level level,
            BlockPos origin,
            String baseName,
            UUID coreUuid,
            @Nullable UUID ownerUuid,
            @Nullable String ownerName,
            boolean passwordEnabled,
            @Nullable String passwordHash,
            CoreStructureWorkspace workspace,
            CoreVolume volume,
            Map<UUID, CoreResident> residents,
            Map<CoreUpgrade, Integer> upgradeFuelTicks,
            Map<CoreUpgrade, Integer> queuedUpgradeFuelCounts
    ) {
        if (level instanceof ServerLevel serverLevel) {
            StructureSnapshot snapshot = workspace.createPackedSnapshot(
                    volume,
                    PACKED_AT_FORMAT.format(Instant.now()),
                    serverLevel.dimension().location().toString()
            );
            UUID storageUUID = storePackedSnapshot(BuildingStorageData.get(serverLevel), coreUuid, snapshot);
            giveOrDropPackedKey(
                level,
                origin,
                    createPackedKey(baseName, coreUuid, storageUUID, ownerUuid, ownerName, passwordEnabled, passwordHash, volume, residents, upgradeFuelTicks, queuedUpgradeFuelCounts, snapshot),
                    ownerUuid
            );
        }

        level.destroyBlock(origin, false);
        level.playSound(null, origin, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private static @Nullable Player findRecipient(Level level, BlockPos origin, @Nullable UUID preferredOwnerUuid) {
        if (preferredOwnerUuid != null && level instanceof ServerLevel serverLevel) {
            Player owner = serverLevel.getServer().getPlayerList().getPlayer(preferredOwnerUuid);
            if (owner != null) {
                return owner;
            }
        }
        return level.getNearestPlayer(origin.getX(), origin.getY(), origin.getZ(), 10.0D, false);
    }
}
