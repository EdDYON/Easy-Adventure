package com.eddy1.easyadventure.block;

import com.eddy1.easyadventure.block.core.CorePreview;
import com.eddy1.easyadventure.world.TerritoryManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BoundaryMarkerBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(6.0D, 0.0D, 6.0D, 10.0D, 13.0D, 10.0D),
            Block.box(4.0D, 13.0D, 4.0D, 12.0D, 16.0D, 12.0D)
    );

    public BoundaryMarkerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        BaseCoreBlockEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (BaseCoreBlockEntity core : TerritoryManager.getLoadedCores(serverLevel)) {
            if (!core.isTerritoryActive() || !core.canPlayerManage(serverPlayer)) {
                continue;
            }
            double distance = core.getBlockPos().distSqr(pos);
            if (distance < nearestDistance && distance <= 96.0D * 96.0D) {
                nearest = core;
                nearestDistance = distance;
            }
        }

        if (nearest == null) {
            player.displayClientMessage(Component.translatable("message.easyadventure.boundary_marker_no_core").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.SUCCESS;
        }

        CorePreview.show(serverLevel, nearest.getBlockPos(), nearest.getTerritoryVolume(), false);
        player.displayClientMessage(Component.translatable("message.easyadventure.boundary_marker_showing", nearest.getBaseName()).withStyle(ChatFormatting.AQUA), true);
        return InteractionResult.SUCCESS;
    }
}
