package com.eddy1.easyadventure.block;

import com.eddy1.easyadventure.init.ModBlockEntities;
import com.eddy1.easyadventure.init.ModItems;
import com.eddy1.easyadventure.menu.CoreSizeMenu;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.level.block.Block;
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

    public int getSizeX() {
        return defaultSizeX;
    }

    public int getSizeY() {
        return defaultSizeY;
    }

    public int getSizeZ() {
        return defaultSizeZ;
    }

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
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof BaseCoreBlockEntity core) {
                core.dropUpgradeFuelInventory();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof BaseCoreBlockEntity core)) {
            return;
        }

        if (placer instanceof Player player) {
            core.setOwnerFromPlayer(player);
        }
        if (stack.has(DataComponents.CUSTOM_NAME)) {
            core.setBaseName(stack.getHoverName().getString());
        }

        core.initializeFoundation(core.getSizeX(), core.getSizeY(), core.getSizeZ());
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (stack.getItem() != Items.NAME_TAG || !stack.has(DataComponents.CUSTOM_NAME)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }

        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof BaseCoreBlockEntity core) {
                if (!core.canPlayerManage(player)) {
                    player.sendSystemMessage(Component.translatable("message.easyadventure.not_authorized_operation").withStyle(ChatFormatting.RED));
                    return ItemInteractionResult.SUCCESS;
                }

                core.setBaseName(stack.getHoverName().getString());
                level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                player.displayClientMessage(Component.translatable("message.easyadventure.renamed"), true);
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
            }
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.getMainHandItem().is(ModItems.BASE_KEY_ITEM.get())) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof BaseCoreBlockEntity core) {
                if (core.isBusy()) {
                    player.displayClientMessage(Component.translatable("message.easyadventure.core_busy"), true);
                    return InteractionResult.SUCCESS;
                }
                if (!core.canPlayerManage(player)) {
                    player.sendSystemMessage(Component.translatable("message.easyadventure.not_authorized_operation").withStyle(ChatFormatting.RED));
                    return InteractionResult.SUCCESS;
                }

                serverPlayer.openMenu(new SimpleMenuProvider(
                        (id, inventory, targetPlayer) -> new CoreSizeMenu(id, inventory, pos, false, core.isPasswordEnabled(), core.isBound()),
                        Component.translatable("gui.easyadventure.core_size_title")
                ), buffer -> {
                    buffer.writeBlockPos(pos);
                    buffer.writeBoolean(false);
                    buffer.writeBoolean(core.isPasswordEnabled());
                    buffer.writeBoolean(core.isBound());
                });
            }
        }
        return InteractionResult.SUCCESS;
    }
}
