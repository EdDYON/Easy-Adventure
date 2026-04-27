package com.eddy1.easyadventure.block;

import com.eddy1.easyadventure.menu.KeyRecallMenu;
import com.eddy1.easyadventure.world.BaseRegistryData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
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
import net.minecraftforge.network.NetworkHooks;

public class KeyRecallTableBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1.0, 0.0, 1.0, 15.0, 2.0, 15.0),
            Block.box(3.0, 2.0, 3.0, 13.0, 10.0, 13.0),
            Block.box(0.0, 10.0, 0.0, 16.0, 13.0, 16.0),
            Block.box(2.0, 13.0, 2.0, 5.0, 16.0, 5.0),
            Block.box(11.0, 13.0, 2.0, 14.0, 16.0, 5.0),
            Block.box(2.0, 13.0, 11.0, 5.0, 16.0, 14.0),
            Block.box(11.0, 13.0, 11.0, 14.0, 16.0, 14.0),
            Block.box(6.0, 13.0, 6.0, 10.0, 16.0, 10.0)
    );

    public KeyRecallTableBlock(Properties properties) {
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
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        var records = BaseRegistryData.get(serverLevel).recordsForOwner(player.getUUID());
        NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                (containerId, inventory, targetPlayer) -> new KeyRecallMenu(containerId, inventory, pos, records.stream()
                        .map(record -> new KeyRecallMenu.RecallEntry(record.coreUuid(), record.baseName(), record.state()))
                        .toList()),
                Component.translatable("gui.easyadventure.key_recall_title")
        ), buffer -> {
            buffer.writeBlockPos(pos);
            KeyRecallMenu.writeEntries(buffer, records);
        });
        return InteractionResult.SUCCESS;
    }
}
