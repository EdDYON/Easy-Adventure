package com.eddy1.easyadventure.world;

import com.eddy1.easyadventure.block.BaseCoreBlock;
import com.eddy1.easyadventure.block.BaseCoreBlockEntity;
import com.eddy1.easyadventure.block.core.CoreCompat;
import com.eddy1.easyadventure.block.core.CoreUpgrade;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class TerritoryEvents {
    private static final int DENY_MESSAGE_INTERVAL = 20;

    private static final Map<UUID, Long> LAST_DENY_MESSAGE_TICKS = new ConcurrentHashMap<>();

    private TerritoryEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(TerritoryEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(TerritoryEvents::onBreakBlock);
        NeoForge.EVENT_BUS.addListener(TerritoryEvents::onPlaceBlock);
        NeoForge.EVENT_BUS.addListener(TerritoryEvents::onMultiPlaceBlock);
        NeoForge.EVENT_BUS.addListener(TerritoryEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(TerritoryEvents::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(TerritoryEvents::onExplosionDetonate);
        NeoForge.EVENT_BUS.addListener(TerritoryEvents::onCropGrow);
        NeoForge.EVENT_BUS.addListener(TerritoryEvents::onFarmlandTrample);
        NeoForge.EVENT_BUS.addListener(TerritoryEvents::onMobSpawnCheck);
        NeoForge.EVENT_BUS.addListener(TerritoryEvents::onLevelTick);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        if (level.getGameTime() % 5 != 0) {
            return;
        }

        for (BaseCoreBlockEntity core : TerritoryManager.findContainingCores(level, player.blockPosition())) {
            if (core.canEnterTerritory(player)) {
                continue;
            }

            ejectPlayer(player, core);
            deny(player, "message.easyadventure.territory_entry_denied");
            return;
        }
    }

    private static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (requiresDenial(TerritoryManager.findContainingCores(level, event.getPos()), core -> !core.canBuild(event.getPlayer()))) {
            event.setCanceled(true);
            deny(event.getPlayer(), "message.easyadventure.territory_build_denied");
        }
    }

    private static void onPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Player player)) {
            return;
        }

        if (requiresDenial(TerritoryManager.findContainingCores(level, event.getPos()), core -> !core.canBuild(player))) {
            event.setCanceled(true);
            deny(player, "message.easyadventure.territory_build_denied");
            syncDeniedPlacement(player, List.of(event.getPos()));
        }
    }

    private static void onMultiPlaceBlock(BlockEvent.EntityMultiPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Player player)) {
            return;
        }

        for (var snapshot : event.getReplacedBlockSnapshots()) {
            if (requiresDenial(TerritoryManager.findContainingCores(level, snapshot.getPos()), core -> !core.canBuild(player))) {
                event.setCanceled(true);
                deny(player, "message.easyadventure.territory_build_denied");
                syncDeniedPlacement(player, event.getReplacedBlockSnapshots().stream().map(snapshot1 -> snapshot1.getPos()).toList());
                return;
            }
        }
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        BlockPos clickedPos = event.getPos();
        Collection<BaseCoreBlockEntity> clickedCores = TerritoryManager.findContainingCores(level, clickedPos);
        BlockPos placementPos = getPlacementTargetPos(level, clickedPos, event.getFace());
        Collection<BaseCoreBlockEntity> placementCores = placementPos == null
                ? List.of()
                : TerritoryManager.findContainingCores(level, placementPos);
        if (clickedCores.isEmpty() && placementCores.isEmpty()) {
            return;
        }

        Player player = event.getEntity();
        BlockState state = level.getBlockState(clickedPos);
        boolean buildAttempt = isBuildPlacementItem(event.getItemStack()) && placementPos != null;
        if (isStorageBlock(level, clickedPos, state)) {
            if (player.isShiftKeyDown() && buildAttempt) {
                if (requiresDenial(placementCores, core -> !core.canBuild(player))) {
                    cancelInteract(event);
                    deny(player, "message.easyadventure.territory_build_denied");
                    syncDeniedPlacement(player, List.of(placementPos));
                }
                return;
            }

            if (requiresDenial(clickedCores, core -> !core.canUseStorage(player))) {
                cancelInteract(event);
                deny(player, "message.easyadventure.territory_storage_denied");
            }
            return;
        }

        if (isDeviceBlock(level, clickedPos, state)) {
            if (player.isShiftKeyDown() && buildAttempt) {
                if (requiresDenial(placementCores, core -> !core.canBuild(player))) {
                    cancelInteract(event);
                    deny(player, "message.easyadventure.territory_build_denied");
                    syncDeniedPlacement(player, List.of(placementPos));
                }
                return;
            }

            if (requiresDenial(clickedCores, core -> !core.canUseDevices(player))) {
                cancelInteract(event);
                deny(player, "message.easyadventure.territory_device_denied");
            }
            return;
        }

        if (buildAttempt && requiresDenial(placementCores, core -> !core.canBuild(player))) {
            cancelInteract(event);
            deny(player, "message.easyadventure.territory_build_denied");
            syncDeniedPlacement(player, List.of(placementPos));
        }
    }

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !stack.is(CoreCompat.TERRITORY_BUILD_ITEMS)) {
            return;
        }

        Player player = event.getEntity();
        BlockPos playerPos = player.blockPosition();
        if (!requiresDenial(TerritoryManager.findContainingCores(level, playerPos), core -> !core.canBuild(player))) {
            return;
        }

        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
        deny(player, "message.easyadventure.territory_build_denied");
        syncDeniedPlacement(player, List.of(playerPos));
    }

    private static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        event.getAffectedBlocks().removeIf(pos ->
                TerritoryManager.findContainingCores(level, pos).stream().anyMatch(core -> core.hasUpgrade(CoreUpgrade.BLAST_SHIELD)));
    }

    private static void onCropGrow(CropGrowEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        BaseCoreBlockEntity core = findCoreWithUpgrade(level, event.getPos(), CoreUpgrade.GREENHOUSE);
        if (core != null && level.random.nextFloat() < 0.15F) {
            event.setResult(CropGrowEvent.Pre.Result.GROW);
        }
    }

    private static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (findCoreWithUpgrade(level, event.getPos(), CoreUpgrade.GREENHOUSE) != null) {
            event.setCanceled(true);
        }
    }

    private static void onMobSpawnCheck(MobSpawnEvent.PositionCheck event) {
        if (!(event.getLevel().getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (event.getEntity().getType().getCategory() != MobCategory.MONSTER) {
            return;
        }
        if (findCoreWithUpgrade(level, BlockPos.containing(event.getX(), event.getY(), event.getZ()), CoreUpgrade.PURIFICATION) == null) {
            return;
        }
        if (level.random.nextFloat() < 0.5F) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        long gameTime = level.getGameTime();
        if (gameTime % 40 == 0) {
            for (BaseCoreBlockEntity core : TerritoryManager.getLoadedCores(level)) {
                if (core.hasUpgrade(CoreUpgrade.PURIFICATION)) {
                    applyPurification(level, core);
                }
            }
        }

        if (gameTime % 20 == 0) {
            for (BaseCoreBlockEntity core : TerritoryManager.getLoadedCores(level)) {
                if (core.hasUpgrade(CoreUpgrade.GREENHOUSE)) {
                    applyGreenhouse(level, core);
                }
            }
        }
    }

    private static void applyPurification(ServerLevel level, BaseCoreBlockEntity core) {
        for (Mob mob : level.getEntitiesOfClass(Mob.class, core.getTerritoryVolume().createAabb(core.getBlockPos()))) {
            if (mob.getType().getCategory() != MobCategory.MONSTER) {
                continue;
            }

            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0, true, true));
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 0, true, true));
        }
    }

    private static void applyGreenhouse(ServerLevel level, BaseCoreBlockEntity core) {
        BlockPos center = core.getBlockPos();
        for (int i = 0; i < 6; i++) {
            BlockPos samplePos = center.offset(
                    level.random.nextInt(core.getTerritoryVolume().sizeX()) - core.getTerritoryVolume().halfX(),
                    level.random.nextInt(core.getTerritoryVolume().sizeY() + 1),
                    level.random.nextInt(core.getTerritoryVolume().sizeZ()) - core.getTerritoryVolume().halfZ()
            );
            BlockState state = level.getBlockState(samplePos);
            if (state.hasProperty(BlockStateProperties.LEVEL_HONEY) && state.getBlock() instanceof BeehiveBlock) {
                int honeyLevel = state.getValue(BlockStateProperties.LEVEL_HONEY);
                if (honeyLevel < 5 && level.random.nextFloat() < 0.25F) {
                    level.setBlock(samplePos, state.setValue(BlockStateProperties.LEVEL_HONEY, honeyLevel + 1), 3);
                }
            }
        }
    }

    private static @Nullable BaseCoreBlockEntity findCoreWithUpgrade(ServerLevel level, BlockPos pos, CoreUpgrade upgrade) {
        for (BaseCoreBlockEntity core : TerritoryManager.findContainingCores(level, pos)) {
            if (core.hasUpgrade(upgrade)) {
                return core;
            }
        }
        return null;
    }

    private static boolean requiresDenial(Collection<BaseCoreBlockEntity> cores, Predicate<BaseCoreBlockEntity> denialCheck) {
        for (BaseCoreBlockEntity core : cores) {
            if (denialCheck.test(core)) {
                return true;
            }
        }
        return false;
    }

    private static void cancelInteract(PlayerInteractEvent.RightClickBlock event) {
        event.setUseBlock(TriState.FALSE);
        event.setUseItem(TriState.FALSE);
        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
    }

    private static void deny(Player player, String translationKey) {
        long gameTime = player.level().getGameTime();
        long lastTick = LAST_DENY_MESSAGE_TICKS.getOrDefault(player.getUUID(), Long.MIN_VALUE);
        if (gameTime - lastTick < DENY_MESSAGE_INTERVAL) {
            return;
        }

        LAST_DENY_MESSAGE_TICKS.put(player.getUUID(), gameTime);
        player.sendSystemMessage(Component.translatable(translationKey).withStyle(ChatFormatting.RED));
    }

    private static void syncDeniedPlacement(Player player, Collection<BlockPos> positions) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ItemStack mainHand = serverPlayer.getMainHandItem().copy();
        ItemStack offHand = serverPlayer.getOffhandItem().copy();
        serverPlayer.getInventory().setChanged();
        serverPlayer.containerMenu.broadcastChanges();
        serverPlayer.inventoryMenu.broadcastChanges();
        serverPlayer.containerMenu.broadcastFullState();
        serverPlayer.inventoryMenu.broadcastFullState();
        serverPlayer.setItemInHand(InteractionHand.MAIN_HAND, mainHand);
        serverPlayer.setItemInHand(InteractionHand.OFF_HAND, offHand);

        ServerLevel level = serverPlayer.serverLevel();
        List<BlockPos> uniquePositions = new ArrayList<>(positions.size());
        for (BlockPos pos : positions) {
            if (pos != null && !uniquePositions.contains(pos)) {
                uniquePositions.add(pos);
            }
        }
        for (BlockPos pos : uniquePositions) {
            BlockState state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private static void ejectPlayer(ServerPlayer player, BaseCoreBlockEntity core) {
        BlockPos center = core.getBlockPos();
        var volume = core.getTerritoryVolume();
        double minX = center.getX() - volume.halfX();
        double maxX = center.getX() + volume.halfX() + 1.0D;
        double minZ = center.getZ() - volume.halfZ();
        double maxZ = center.getZ() + volume.halfZ() + 1.0D;
        double playerX = player.getX();
        double playerZ = player.getZ();

        double leftDistance = playerX - minX;
        double rightDistance = maxX - playerX;
        double frontDistance = playerZ - minZ;
        double backDistance = maxZ - playerZ;

        double targetX = playerX;
        double targetZ = playerZ;
        double smallest = Math.min(Math.min(leftDistance, rightDistance), Math.min(frontDistance, backDistance));
        if (smallest == leftDistance) {
            targetX = minX - 0.35D;
        } else if (smallest == rightDistance) {
            targetX = maxX + 0.35D;
        } else if (smallest == frontDistance) {
            targetZ = minZ - 0.35D;
        } else {
            targetZ = maxZ + 0.35D;
        }

        player.teleportTo(targetX, player.getY(), targetZ);
        player.setDeltaMovement(0.0D, Math.min(player.getDeltaMovement().y, 0.0D), 0.0D);
    }

    private static boolean isStorageBlock(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.is(CoreCompat.TERRITORY_STORAGE_BLOCKS)) {
            return true;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof Container) {
            return true;
        }

        return state.getBlock() instanceof ChestBlock
                || state.getBlock() instanceof BarrelBlock
                || state.getBlock() instanceof ShulkerBoxBlock
                || state.getBlock() instanceof HopperBlock
                || state.getBlock() instanceof DispenserBlock
                || state.getBlock() instanceof DropperBlock
                || state.getBlock() instanceof EnderChestBlock
                || hasItemHandler(level, pos)
                || hasFluidHandler(level, pos);
    }

    private static @Nullable BlockPos getPlacementTargetPos(ServerLevel level, BlockPos clickedPos, @Nullable net.minecraft.core.Direction face) {
        if (face == null) {
            return null;
        }
        return level.getBlockState(clickedPos).canBeReplaced() ? clickedPos : clickedPos.relative(face);
    }

    private static boolean isBuildPlacementItem(ItemStack stack) {
        return !stack.isEmpty() && (
                stack.getItem() instanceof BlockItem
                        || stack.getItem() instanceof BucketItem
                        || stack.is(CoreCompat.TERRITORY_BUILD_ITEMS)
        );
    }

    private static boolean isDeviceBlock(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof BaseCoreBlock) {
            return false;
        }
        if (state.is(CoreCompat.TERRITORY_DEVICE_BLOCKS)) {
            return true;
        }
        if (state.getMenuProvider(level, pos) != null) {
            return true;
        }

        return state.getBlock() instanceof DoorBlock
                || state.getBlock() instanceof TrapDoorBlock
                || state.getBlock() instanceof FenceGateBlock
                || state.getBlock() instanceof ButtonBlock
                || state.getBlock() instanceof LeverBlock
                || state.getBlock() instanceof BedBlock
                || state.getBlock() instanceof NoteBlock
                || hasEnergyStorage(level, pos)
                || hasItemHandler(level, pos)
                || hasFluidHandler(level, pos);
    }

    private static boolean hasItemHandler(ServerLevel level, BlockPos pos) {
        if (level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) != null) {
            return true;
        }
        for (Direction direction : Direction.values()) {
            if (level.getCapability(Capabilities.ItemHandler.BLOCK, pos, direction) != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasFluidHandler(ServerLevel level, BlockPos pos) {
        if (level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null) != null) {
            return true;
        }
        for (Direction direction : Direction.values()) {
            if (level.getCapability(Capabilities.FluidHandler.BLOCK, pos, direction) != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasEnergyStorage(ServerLevel level, BlockPos pos) {
        if (level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null) != null) {
            return true;
        }
        for (Direction direction : Direction.values()) {
            if (level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction) != null) {
                return true;
            }
        }
        return false;
    }
}
