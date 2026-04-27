package com.eddy1.easyadventure.block;

import com.eddy1.easyadventure.init.ModBlockEntities;
import com.eddy1.easyadventure.init.ModItems;
import com.eddy1.easyadventure.menu.BaseNameMenu;
import com.eddy1.easyadventure.menu.CoreSizeMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class BaseCoreBlock extends BaseEntityBlock {
    private final int defaultSizeX;
    private final int defaultSizeY;
    private final int defaultSizeZ;

    public BaseCoreBlock(Properties properties, int sizeX, int sizeY, int sizeZ) {
        super(properties);
        this.defaultSizeX = sizeX;
        this.defaultSizeY = sizeY;
        this.defaultSizeZ = sizeZ;
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
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
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

        boolean named = false;
        Player placingPlayer = placer instanceof Player player ? player : null;
        if (placingPlayer != null) {
            Player player = placingPlayer;
            core.setOwnerFromPlayer(player);
            if (stack.hasCustomHoverName() && level instanceof ServerLevel serverLevel) {
                named = trySetRegisteredName(serverLevel, pos, core, player, stack.getHoverName().getString());
            }
        }

        core.initializeFoundation(placingPlayer, core.getSizeX(), core.getSizeY(), 0, core.getSizeZ());
        if (!named && placingPlayer instanceof ServerPlayer serverPlayer) {
            openNameMenu(serverPlayer, pos, "");
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ModItems.BASE_KEY_ITEM.get())) {
            return InteractionResult.PASS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof BaseCoreBlockEntity core)) {
            return InteractionResult.PASS;
        }

        if (stack.getItem() == Items.NAME_TAG && stack.hasCustomHoverName()) {
            if (!level.isClientSide) {
                if (!core.canPlayerManage(player)) {
                    player.sendSystemMessage(Component.translatable("message.easyadventure.not_authorized_operation").withStyle(ChatFormatting.RED));
                    return InteractionResult.SUCCESS;
                }

                if (!(level instanceof ServerLevel serverLevel) || !trySetRegisteredName(serverLevel, pos, core, player, stack.getHoverName().getString())) {
                    return InteractionResult.SUCCESS;
                }
                level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (core.isBusy()) {
                player.displayClientMessage(Component.translatable("message.easyadventure.core_busy"), true);
                return InteractionResult.SUCCESS;
            }
            if (!core.canPlayerManage(player)) {
                player.sendSystemMessage(Component.translatable("message.easyadventure.not_authorized_operation").withStyle(ChatFormatting.RED));
                return InteractionResult.SUCCESS;
            }

            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                    (id, inventory, targetPlayer) -> new CoreSizeMenu(id, inventory, pos, false, core.isPasswordEnabled(), core.isBound()),
                    Component.translatable("gui.easyadventure.core_size_title")
            ), buffer -> {
                buffer.writeBlockPos(pos);
                buffer.writeBoolean(false);
                buffer.writeBoolean(core.isPasswordEnabled());
                buffer.writeBoolean(core.isBound());
            });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean trySetRegisteredName(ServerLevel level, BlockPos pos, BaseCoreBlockEntity core, Player player, String rawName) {
        return core.renameBase(player, rawName);
    }

    private static void openNameMenu(ServerPlayer player, BlockPos pos, String currentName) {
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (id, inventory, targetPlayer) -> new BaseNameMenu(id, inventory, pos, currentName),
                Component.translatable("gui.easyadventure.base_name_title")
        ), buffer -> {
            buffer.writeBlockPos(pos);
            buffer.writeUtf(currentName, 64);
        });
    }
}
