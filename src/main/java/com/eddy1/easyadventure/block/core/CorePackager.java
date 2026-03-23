package com.eddy1.easyadventure.block.core;

import com.eddy1.easyadventure.init.ModItems;
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

import java.util.UUID;

public final class CorePackager {
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
            CoreVolume volume
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
                volume.sizeZ()
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

    public static UUID storePackedSnapshot(BuildingStorageData storage, UUID coreUuid, CoreStructureWorkspace workspace, CoreVolume volume) {
        return storage.saveBuilding(coreUuid, workspace.createPackedSnapshot(volume).toTag());
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
            CoreVolume volume
    ) {
        if (level instanceof ServerLevel serverLevel) {
            UUID storageUUID = storePackedSnapshot(BuildingStorageData.get(serverLevel), coreUuid, workspace, volume);
            giveOrDropPackedKey(
                    level,
                    origin,
                    createPackedKey(baseName, coreUuid, storageUUID, ownerUuid, ownerName, passwordEnabled, passwordHash, volume),
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
