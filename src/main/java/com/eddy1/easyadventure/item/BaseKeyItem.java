package com.eddy1.easyadventure.item;

import com.eddy1.easyadventure.EasyAdventure;
import com.eddy1.easyadventure.block.BaseCoreBlockEntity;
import com.eddy1.easyadventure.world.BuildingStorageData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import java.util.UUID;

public class BaseKeyItem extends Item {
    public BaseKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && customData.contains("BoundUUID")) {
            if (customData.contains("StorageUUID")) {
                tooltipComponents.add(Component.literal("§a[已存储建筑数据]").withStyle(ChatFormatting.GREEN));
            } else {
                tooltipComponents.add(Component.literal("§e[已绑定核心 - 空]").withStyle(ChatFormatting.YELLOW));
            }
        } else {
            tooltipComponents.add(Component.literal("§7[未绑定 - 空白钥匙]").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(clickedPos);
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();

        if (be instanceof BaseCoreBlockEntity core) {
            if (!level.isClientSide) {
                CustomData customData = stack.get(DataComponents.CUSTOM_DATA);

                boolean hasData = customData != null && customData.contains("StorageUUID");
                if (hasData) {
                    player.displayClientMessage(Component.literal("§c错误：这把钥匙已经存有建筑了！请先释放。"), true);
                    return InteractionResult.FAIL;
                }

                UUID keyBoundID = (customData != null && customData.contains("BoundUUID")) ? customData.getUnsafe().getUUID("BoundUUID") : null;

                if (core.isBound()) {
                    if (keyBoundID == null || !keyBoundID.equals(core.getCoreUUID())) {
                        player.displayClientMessage(Component.literal("§c收纳失败：这把钥匙不匹配！该核心已认主。"), true);
                        return InteractionResult.FAIL;
                    }
                }

                core.startPacking();
                level.playSound(null, clickedPos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
                player.displayClientMessage(Component.literal("§a📦 开始收纳..."), true);
                stack.shrink(1);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.SUCCESS;
        }

        BlockPos placePos = clickedPos.relative(context.getClickedFace());
        if (!level.getBlockState(placePos).canBeReplaced()) return InteractionResult.FAIL;

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);

            if (customData != null && customData.contains("StorageUUID")) {
                CompoundTag keyTag = customData.copyTag();
                UUID storageUUID = keyTag.getUUID("StorageUUID");
                UUID boundUUID = keyTag.contains("BoundUUID") ? keyTag.getUUID("BoundUUID") : null;

                BuildingStorageData storage = BuildingStorageData.get(serverLevel);
                CompoundTag heavyData = storage.getBuilding(storageUUID);

                if (heavyData == null) {
                    player.displayClientMessage(Component.literal("§c错误：建筑数据丢失"), true);
                    return InteractionResult.FAIL;
                }

                Block blockToPlace = EasyAdventure.BASE_CORE.get();
                level.setBlock(placePos, blockToPlace.defaultBlockState(), 3);

                BlockEntity newBe = level.getBlockEntity(placePos);

                if (newBe instanceof BaseCoreBlockEntity newCore) {
                    String currentName = stack.getHoverName().getString();
                    String cleanName = currentName.replace(" (已打包)", "").replace(" (已释放)", "");

                    newCore.restoreFromTag(heavyData, cleanName, boundUUID);

                    storage.removeBuilding(storageUUID);

                    level.playSound(null, placePos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.0f);
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                            placePos.getX() + 0.5, placePos.getY() + 1.0, placePos.getZ() + 0.5,
                            1, 0.0, 0.0, 0.0, 0.0
                    );

                    CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                        tag.remove("StorageUUID");
                    });

                    stack.set(DataComponents.CUSTOM_NAME, Component.literal(cleanName + " (已绑定)"));
                    player.displayClientMessage(Component.literal("§e基地已释放！钥匙已保留绑定。"), true);
                }
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }
}