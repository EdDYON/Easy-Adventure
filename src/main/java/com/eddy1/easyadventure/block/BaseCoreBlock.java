package com.eddy1.easyadventure.block;

import com.eddy1.easyadventure.EasyAdventure;
import com.eddy1.easyadventure.init.ModBlockEntities;
import com.eddy1.easyadventure.menu.CoreSizeMenu;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class BaseCoreBlock extends BaseEntityBlock {
    public static final MapCodec<BaseCoreBlock> CODEC = simpleCodec(properties -> new BaseCoreBlock(properties, 9, 5, 9));

    private final int defaultSizeX;
    private final int defaultSizeY;
    private final int defaultSizeZ;

    public BaseCoreBlock(Properties properties, int sizeX, int sizeY, int sizeZ) {
        super(properties);
        this.defaultSizeX = sizeX;
        this.defaultSizeY = sizeY;
        this.defaultSizeZ = sizeZ;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public int getSizeX() { return defaultSizeX; }
    public int getSizeY() { return defaultSizeY; }
    public int getSizeZ() { return defaultSizeZ; }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BaseCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.BASE_CORE.get(), BaseCoreBlockEntity::tick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BaseCoreBlockEntity core) {
                // 1. 检查物品是否有自定义名字
                if (stack.has(DataComponents.CUSTOM_NAME)) {
                    String name = stack.getHoverName().getString();
                    core.setBaseName(name);

                    if (placer instanceof Player p) {
                        p.displayClientMessage(Component.literal("§e[调试] 核心已命名为: " + name), true);
                    }
                }

                core.initializeFoundation(core.getSizeX(), core.getSizeY(), core.getSizeZ());
            }
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() == Items.NAME_TAG && stack.has(DataComponents.CUSTOM_NAME)) {
            if (!level.isClientSide) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof BaseCoreBlockEntity core) {
                    String newName = stack.getHoverName().getString();
                    core.setBaseName(newName);

                    level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
                    player.displayClientMessage(Component.literal("§e[调试] 核心已重命名为: " + newName), true);

                    if (!player.isCreative()) {
                        stack.shrink(1);
                    }
                }
            }
            return ItemInteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() == EasyAdventure.BASE_KEY_ITEM.get()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BaseCoreBlockEntity) {
                serverPlayer.openMenu(new SimpleMenuProvider(
                        (id, inv, p) -> new CoreSizeMenu(id, inv, pos),
                        Component.literal("设置基地大小")
                ), buffer -> buffer.writeBlockPos(pos));
            }
        }
        return InteractionResult.SUCCESS;
    }
}