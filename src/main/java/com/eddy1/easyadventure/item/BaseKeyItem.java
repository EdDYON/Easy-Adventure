package com.eddy1.easyadventure.item;

import com.eddy1.easyadventure.block.BaseCoreBlockEntity;
import com.eddy1.easyadventure.block.core.CoreAccessControl;
import com.eddy1.easyadventure.block.core.CoreAreaCheckResult;
import com.eddy1.easyadventure.block.core.CoreClearMode;
import com.eddy1.easyadventure.block.core.CorePasswordUtil;
import com.eddy1.easyadventure.block.core.CorePreflight;
import com.eddy1.easyadventure.block.core.CorePreview;
import com.eddy1.easyadventure.block.core.CoreValidation;
import com.eddy1.easyadventure.block.core.CoreVolume;
import com.eddy1.easyadventure.block.core.CoreUpgrade;
import com.eddy1.easyadventure.init.ModBlocks;
import com.eddy1.easyadventure.menu.KeyPasswordMenu;
import com.eddy1.easyadventure.network.KeyOperationAction;
import com.eddy1.easyadventure.storage.StructureSnapshot;
import com.eddy1.easyadventure.util.KeyDataUtil;
import com.eddy1.easyadventure.world.BaseRegistryData;
import com.eddy1.easyadventure.world.BuildingStorageData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Rotation;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BaseKeyItem extends Item {
    public BaseKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        migrateLegacyDisplayData(stack);
        if (!level.isClientSide && level instanceof ServerLevel serverLevel && entity instanceof Player && level.getGameTime() % 40L == 0L) {
            syncKeyValidity(serverLevel, stack);
        }
        registerKnownPackedKey(stack, level, entity);
    }

    @Override
    public Component getName(ItemStack stack) {
        String baseName = KeyDataUtil.getBaseName(stack);
        if (KeyDataUtil.isObsoleteKey(stack)) {
            if (baseName == null || baseName.isBlank()) {
                return Component.translatable("item.easyadventure.obsolete_base_key").withStyle(ChatFormatting.GRAY);
            }
            return Component.translatable("item.easyadventure.obsolete_base_key_named", getDisplayBaseName(baseName)).withStyle(ChatFormatting.GRAY);
        }
        if (baseName == null || baseName.isBlank()) {
            return super.getName(stack);
        }
        return getDisplayBaseName(baseName);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, level, tooltipComponents, tooltipFlag);
        if (KeyDataUtil.isObsoleteKey(stack)) {
            tooltipComponents.add(Component.translatable("tooltip.easyadventure.key_obsolete").withStyle(ChatFormatting.RED));
        }
        if (KeyDataUtil.hasStoredStructure(stack)) {
            tooltipComponents.add(Component.translatable("tooltip.easyadventure.key_contains").withStyle(ChatFormatting.GREEN));
        } else if (KeyDataUtil.hasBoundUuid(stack)) {
            tooltipComponents.add(Component.translatable("tooltip.easyadventure.key_bound").withStyle(ChatFormatting.YELLOW));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.easyadventure.key_empty").withStyle(ChatFormatting.GRAY));
        }

        String ownerName = KeyDataUtil.getOwnerName(stack);
        if (ownerName != null) {
            tooltipComponents.add(Component.translatable("tooltip.easyadventure.owner", ownerName).withStyle(ChatFormatting.GRAY));
        }
        if (KeyDataUtil.isPasswordEnabled(stack)) {
            tooltipComponents.add(Component.translatable("tooltip.easyadventure.password_enabled").withStyle(ChatFormatting.GOLD));
        }
        if (!KeyDataUtil.getResidents(stack).isEmpty()) {
            tooltipComponents.add(Component.translatable("tooltip.easyadventure.residents", KeyDataUtil.getResidents(stack).size()).withStyle(ChatFormatting.BLUE));
        }
        int activeUpgrades = (int) KeyDataUtil.getUpgradeFuelTicks(stack).values().stream().filter(ticks -> ticks > 0).count();
        if (activeUpgrades > 0) {
            tooltipComponents.add(Component.translatable("tooltip.easyadventure.upgrades", activeUpgrades).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (KeyDataUtil.hasStoredStructure(stack)) {
            tooltipComponents.add(Component.translatable(
                    "tooltip.easyadventure.key_size",
                    KeyDataUtil.getStoredSizeX(stack),
                    formatStoredYValue(KeyDataUtil.getStoredSizeY(stack), KeyDataUtil.getStoredSizeBelowY(stack)),
                    KeyDataUtil.getStoredSizeZ(stack)
            ).withStyle(ChatFormatting.AQUA));
            tooltipComponents.add(Component.translatable(
                    "tooltip.easyadventure.key_contents",
                    KeyDataUtil.getBlockCount(stack),
                    KeyDataUtil.getBlockEntityCount(stack),
                    KeyDataUtil.getEntityCount(stack)
            ).withStyle(ChatFormatting.DARK_AQUA));

            String packedAt = KeyDataUtil.getPackedAt(stack);
            if (packedAt != null) {
                tooltipComponents.add(Component.translatable("tooltip.easyadventure.packed_at", packedAt).withStyle(ChatFormatting.GRAY));
            }

            String sourceDimension = KeyDataUtil.getSourceDimension(stack);
            if (sourceDimension != null) {
                tooltipComponents.add(Component.translatable("tooltip.easyadventure.source_dimension", sourceDimension).withStyle(ChatFormatting.GRAY));
            }

            tooltipComponents.add(Component.translatable(
                    "tooltip.easyadventure.deploy_rotation",
                    Component.translatable(rotationLabelKey(KeyDataUtil.getDeployRotation(stack)))
            ).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        migrateLegacyDisplayData(stack);
        if (!ensureCurrentKey(player, stack)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!player.isShiftKeyDown() || !KeyDataUtil.hasStoredStructure(stack)) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide) {
            int nextRotation = (KeyDataUtil.getDeployRotation(stack) + 1) % 4;
            KeyDataUtil.setDeployRotation(stack, nextRotation);
            player.displayClientMessage(Component.translatable(
                    "message.easyadventure.rotation_changed",
                    Component.translatable(rotationLabelKey(nextRotation))
            ), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        ItemStack stack = context.getItemInHand();
        migrateLegacyDisplayData(stack);
        if (!ensureCurrentKey(player, stack)) {
            return InteractionResult.FAIL;
        }
        BlockPos clickedPos = context.getClickedPos();
        InteractionHand hand = context.getHand();
        Direction face = context.getClickedFace();
        BlockEntity blockEntity = level.getBlockEntity(clickedPos);
        if (blockEntity instanceof BaseCoreBlockEntity core) {
            if (!(level instanceof ServerLevel serverLevel)) {
                return InteractionResult.SUCCESS;
            }
            if (player.isShiftKeyDown() && !KeyDataUtil.hasStoredStructure(stack)) {
                return previewPacking(serverLevel, player, core);
            }
            if (requiresPackPassword(player, core)) {
                if (player instanceof ServerPlayer serverPlayer) {
                    openPasswordMenu(serverPlayer, KeyOperationAction.PACK, clickedPos, face, hand);
                }
                return InteractionResult.SUCCESS;
            }
            return packIntoKey(player, hand, stack, clickedPos, core, null);
        }

        if (player.isShiftKeyDown()) {
            return previewDeployment(context, player, stack);
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        if (requiresDeployPassword(player, stack)) {
            if (player instanceof ServerPlayer serverPlayer) {
                openPasswordMenu(serverPlayer, KeyOperationAction.DEPLOY, clickedPos, face, hand);
            }
            return InteractionResult.SUCCESS;
        }
        return deployStoredBase(serverLevel, player, stack, clickedPos, face, null);
    }

    public static void handlePasswordAction(ServerPlayer player, InteractionHand hand, KeyOperationAction action, BlockPos pos, Direction face, String password) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BaseKeyItem keyItem)) {
            return;
        }
        if (!ensureCurrentKey(player, stack)) {
            return;
        }

        if (action == KeyOperationAction.PACK) {
            BlockEntity blockEntity = player.level().getBlockEntity(pos);
            if (blockEntity instanceof BaseCoreBlockEntity core) {
                keyItem.packIntoKey(player, hand, stack, pos, core, password);
            }
            return;
        }

        keyItem.deployStoredBase(player.serverLevel(), player, stack, pos, face, password);
    }

    private InteractionResult previewPacking(ServerLevel level, Player player, BaseCoreBlockEntity core) {
        if (!core.canPlayerOperate(player, null)) {
            sendKeyAccessDenied(player, core.isPasswordEnabled());
            return InteractionResult.FAIL;
        }

        CoreVolume volume = new CoreVolume(core.getSizeX(), core.getSizeY(), core.getSizeBelowY(), core.getSizeZ());
        CoreAreaCheckResult result = CorePreflight.checkPacking(level, core.getBlockPos(), volume);
        CorePreview.show(level, core.getBlockPos(), volume, !result.ok());
        player.displayClientMessage(Component.translatable(
                "message.easyadventure.preview_summary",
                core.getSizeX(),
                formatStoredYValue(core.getSizeY(), core.getSizeBelowY()),
                core.getSizeZ(),
                result.occupiedBlocks(),
                result.blockedBlocks()
        ), false);
        player.displayClientMessage(Component.translatable(
                "message.easyadventure.preview_pack_details",
                result.blockEntityCount(),
                result.containerCount(),
                result.entityCount()
        ), false);
        if (!result.ok() && result.reason() != null) {
            player.displayClientMessage(result.reason(), false);
        }
        return InteractionResult.SUCCESS;
    }

    private InteractionResult packIntoKey(Player player, InteractionHand hand, ItemStack stack, BlockPos clickedPos, BaseCoreBlockEntity core, String password) {
        if (KeyDataUtil.hasStoredStructure(stack)) {
            player.displayClientMessage(Component.translatable("message.easyadventure.key_already_loaded"), true);
            return InteractionResult.FAIL;
        }

        UUID keyBoundId = KeyDataUtil.getBoundUuid(stack);
        if (core.isBound() && (keyBoundId == null || !keyBoundId.equals(core.getCoreUUID()))) {
            player.displayClientMessage(Component.translatable("message.easyadventure.key_mismatch"), true);
            return InteractionResult.FAIL;
        }

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return InteractionResult.FAIL;
        }
        KeyDataUtil.ensureKeyUuid(stack);
        if (!core.isBound()) {
            String requestedBaseName = BaseRegistryData.normalizeDisplayName(core.getBaseName());
            if (requestedBaseName.isBlank() || BaseCoreBlockEntity.DEFAULT_BASE_NAME.equals(requestedBaseName)) {
                player.displayClientMessage(Component.translatable("message.easyadventure.base_name_required"), true);
                return InteractionResult.FAIL;
            }
            BaseRegistryData registry = BaseRegistryData.get(serverLevel);
            if (registry.isNameTaken(player.getUUID(), requestedBaseName, core.getCoreUUID())) {
                player.displayClientMessage(Component.translatable("message.easyadventure.base_name_duplicate", requestedBaseName), true);
                return InteractionResult.FAIL;
            }
            core.setOwnerFromPlayer(player);
            core.setBaseName(requestedBaseName);
            registry.registerPlaced(
                    core.getCoreUUID(),
                    player.getUUID(),
                    player.getGameProfile().getName(),
                    requestedBaseName,
                    KeyDataUtil.getKeyUuid(stack),
                    serverLevel.dimension().location(),
                    core.getBlockPos()
            );
        }

        if (!core.startPacking(player, password)) {
            return InteractionResult.FAIL;
        }
        player.level().playSound(null, clickedPos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("message.easyadventure.packing_started"), true);
        consumeKeyFromHand(player, hand);
        return InteractionResult.SUCCESS;
    }

    private InteractionResult previewDeployment(UseOnContext context, Player player, ItemStack stack) {
        UUID storageUUID = KeyDataUtil.getStorageUuid(stack);
        if (storageUUID == null) {
            return InteractionResult.PASS;
        }
        if (!canUseKeyOperation(player, stack, null)) {
            sendKeyAccessDenied(player, KeyDataUtil.isPasswordEnabled(stack));
            return InteractionResult.FAIL;
        }
        if (!(context.getLevel() instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        BuildingStorageData storage = BuildingStorageData.get(serverLevel);
        var heavyData = storage.getBuilding(storageUUID);
        if (heavyData == null) {
            player.displayClientMessage(Component.translatable("message.easyadventure.storage_missing"), true);
            return InteractionResult.FAIL;
        }

        StructureSnapshot snapshot = StructureSnapshot.fromTag(heavyData).rotated(toRotation(KeyDataUtil.getDeployRotation(stack)));
        BlockPos placePos = context.getClickedPos().relative(context.getClickedFace());
        CoreAreaCheckResult result = CorePreflight.checkDeployment(serverLevel, placePos, snapshot);
        CorePreview.show(serverLevel, placePos, new CoreVolume(snapshot.sizeX(), snapshot.sizeY(), snapshot.sizeBelowY(), snapshot.sizeZ()), !result.ok());
        player.displayClientMessage(Component.translatable(
                "message.easyadventure.preview_summary",
                snapshot.sizeX(),
                formatStoredYValue(snapshot.sizeY(), snapshot.sizeBelowY()),
                snapshot.sizeZ(),
                result.occupiedBlocks(),
                result.blockedBlocks()
        ), false);
        player.displayClientMessage(Component.translatable(
                "message.easyadventure.preview_deploy_details",
                snapshot.blockCount(),
                snapshot.blockEntityCount(),
                snapshot.entityCount()
        ), false);
        if (!result.ok() && result.reason() != null) {
            player.displayClientMessage(result.reason(), false);
        }
        return InteractionResult.SUCCESS;
    }

    private InteractionResult deployStoredBase(ServerLevel serverLevel, Player player, ItemStack stack, BlockPos clickedPos, Direction face, String password) {
        UUID storageUUID = KeyDataUtil.getStorageUuid(stack);
        if (storageUUID == null) {
            return InteractionResult.PASS;
        }
        if (!canUseKeyOperation(player, stack, password)) {
            sendKeyAccessDenied(player, KeyDataUtil.isPasswordEnabled(stack));
            return InteractionResult.FAIL;
        }

        Level level = serverLevel;
        BlockPos placePos = clickedPos.relative(face);
        if (!level.getBlockState(placePos).canBeReplaced()) {
            return InteractionResult.FAIL;
        }

        BuildingStorageData storage = BuildingStorageData.get(serverLevel);
        UUID coreUuid = KeyDataUtil.getBoundUuid(stack);
        var heavyData = storage.getBuilding(storageUUID);
        if (heavyData == null) {
            player.displayClientMessage(Component.translatable("message.easyadventure.storage_missing"), true);
            return InteractionResult.FAIL;
        }

        StructureSnapshot snapshot = StructureSnapshot.fromTag(heavyData);
        if (CoreValidation.containsNestedCore(snapshot)) {
            player.displayClientMessage(Component.translatable("message.easyadventure.stored_nested_core"), true);
            return InteractionResult.FAIL;
        }

        snapshot = snapshot.rotated(toRotation(KeyDataUtil.getDeployRotation(stack)));
        CoreAreaCheckResult precheck = CorePreflight.checkDeployment(serverLevel, placePos, snapshot);
        if (!precheck.ok()) {
            CorePreview.show(serverLevel, placePos, new CoreVolume(snapshot.sizeX(), snapshot.sizeY(), snapshot.sizeBelowY(), snapshot.sizeZ()), true);
            if (precheck.reason() != null) {
                player.displayClientMessage(precheck.reason(), true);
            }
            return InteractionResult.FAIL;
        }

        UUID effectiveCoreUuid = coreUuid == null ? UUID.randomUUID() : coreUuid;
        if (!storage.lockBuilding(storageUUID, effectiveCoreUuid)) {
            player.displayClientMessage(Component.translatable("message.easyadventure.storage_locked"), true);
            return InteractionResult.FAIL;
        }

        level.setBlock(placePos, ModBlocks.BASE_CORE.get().defaultBlockState(), 3);
        BlockEntity blockEntity = level.getBlockEntity(placePos);
        if (!(blockEntity instanceof BaseCoreBlockEntity newCore)) {
            level.removeBlock(placePos, false);
            storage.unlockBuilding(storageUUID, effectiveCoreUuid);
            return InteractionResult.FAIL;
        }

        String baseName = getEffectiveBaseName(stack);
        if (!newCore.restoreFromTag(
                heavyData,
                baseName,
                effectiveCoreUuid,
                KeyDataUtil.getOwnerUuid(stack),
                KeyDataUtil.getOwnerName(stack),
                KeyDataUtil.isPasswordEnabled(stack),
                KeyDataUtil.getPasswordHash(stack),
                KeyDataUtil.getClearMode(stack),
                storageUUID,
                KeyDataUtil.getResidents(stack),
                KeyDataUtil.getUpgradeFuelTicks(stack),
                KeyDataUtil.getQueuedUpgradeFuelCounts(stack)
        )) {
            level.removeBlock(placePos, false);
            storage.unlockBuilding(storageUUID, effectiveCoreUuid);
            return InteractionResult.FAIL;
        }

        applyBoundKeyState(stack, newCore);
        BaseRegistryData.get(serverLevel).registerPlaced(
                newCore.getCoreUUID(),
                KeyDataUtil.getOwnerUuid(stack) == null ? player.getUUID() : KeyDataUtil.getOwnerUuid(stack),
                KeyDataUtil.getOwnerName(stack) == null ? player.getGameProfile().getName() : KeyDataUtil.getOwnerName(stack),
                newCore.getBaseName(),
                KeyDataUtil.ensureKeyUuid(stack),
                serverLevel.dimension().location(),
                placePos
        );
        level.playSound(null, placePos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, placePos.getX() + 0.5, placePos.getY() + 1.0, placePos.getZ() + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
        player.displayClientMessage(Component.translatable("message.easyadventure.unpacked"), true);
        return InteractionResult.SUCCESS;
    }

    private void openPasswordMenu(ServerPlayer player, KeyOperationAction action, BlockPos pos, Direction face, InteractionHand hand) {
        Component title = Component.translatable(
                action == KeyOperationAction.PACK ? "gui.easyadventure.password_pack_title" : "gui.easyadventure.password_deploy_title"
        );
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (containerId, inventory, targetPlayer) -> new KeyPasswordMenu(containerId, inventory, action, pos, face, hand),
                title
        ), buffer -> {
            buffer.writeVarInt(action.ordinal());
            buffer.writeBlockPos(pos);
            buffer.writeVarInt(face.get3DDataValue());
            buffer.writeVarInt(hand.ordinal());
        });
    }

    private boolean requiresPackPassword(Player player, BaseCoreBlockEntity core) {
        return core.isPasswordRequiredFor(player);
    }

    private boolean requiresDeployPassword(Player player, ItemStack stack) {
        return KeyDataUtil.isPasswordEnabled(stack)
                && !isResident(player, stack);
    }

    private boolean canUseKeyOperation(Player player, ItemStack stack, String password) {
        if (isResident(player, stack)) {
            return true;
        }
        if (!KeyDataUtil.isPasswordEnabled(stack)) {
            return true;
        }
        return CorePasswordUtil.matches(KeyDataUtil.getBoundUuid(stack), password, KeyDataUtil.getPasswordHash(stack));
    }

    private void applyBoundKeyState(ItemStack stack, BaseCoreBlockEntity core) {
        KeyDataUtil.setKeyData(
                stack,
                core.getCoreUUID(),
                null,
                core.getOwnerUUID(),
                core.getOwnerName(),
                core.getBaseName(),
                core.isPasswordEnabled(),
                core.getPasswordHash(),
                0,
                0,
                0,
                0,
                core.getClearMode(),
                core.getResidents(),
                core.getUpgradeFuelTicks(),
                Map.of()
        );
        if (stack.hasCustomHoverName()) {
            stack.resetHoverName();
        }
    }

    private static void registerKnownPackedKey(ItemStack stack, Level level, Entity entity) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer player)) {
            return;
        }
        if (level.getGameTime() % 200L != 0L || !KeyDataUtil.hasStoredStructure(stack) || KeyDataUtil.isObsoleteKey(stack)) {
            return;
        }

        UUID coreUuid = KeyDataUtil.getBoundUuid(stack);
        UUID storageUuid = KeyDataUtil.getStorageUuid(stack);
        UUID ownerUuid = KeyDataUtil.getOwnerUuid(stack);
        if (coreUuid == null || storageUuid == null || ownerUuid == null || !ownerUuid.equals(player.getUUID())) {
            return;
        }

        BaseRegistryData registry = BaseRegistryData.get(serverLevel);
        BaseRegistryData.BaseRecord record = registry.getRecord(coreUuid);
        if (record == null) {
            registry.registerPacked(
                    coreUuid,
                    ownerUuid,
                    KeyDataUtil.getOwnerName(stack) == null ? player.getGameProfile().getName() : KeyDataUtil.getOwnerName(stack),
                    getEffectiveBaseName(stack),
                    KeyDataUtil.ensureKeyUuid(stack),
                    storageUuid,
                    KeyDataUtil.getSourceDimension(stack),
                    stack
            );
            return;
        }
        if (!ownerUuid.equals(record.ownerUuid()) || !BaseRegistryData.STATE_PACKED.equals(record.state())) {
            return;
        }
        if (record.storageUuid() != null && !storageUuid.equals(record.storageUuid())) {
            return;
        }
        if (record.keyUuid() == null) {
            registry.registerPacked(
                    coreUuid,
                    ownerUuid,
                    KeyDataUtil.getOwnerName(stack) == null ? player.getGameProfile().getName() : KeyDataUtil.getOwnerName(stack),
                    getEffectiveBaseName(stack),
                    KeyDataUtil.ensureKeyUuid(stack),
                    storageUuid,
                    KeyDataUtil.getSourceDimension(stack),
                    stack
            );
        }
    }

    private static String getEffectiveBaseName(ItemStack stack) {
        String baseName = KeyDataUtil.getBaseName(stack);
        if (baseName == null || baseName.isBlank()) {
            return BaseCoreBlockEntity.DEFAULT_BASE_NAME;
        }
        return baseName;
    }

    private static Component formatStoredYValue(int sizeY, int sizeBelowY) {
        if (sizeBelowY <= 0) {
            return Component.literal(Integer.toString(sizeY));
        }
        return Component.literal(sizeY + "↑/" + sizeBelowY + "↓");
    }

    private static boolean ensureCurrentKey(Player player, ItemStack stack) {
        if (player.level().isClientSide || !(player.level() instanceof ServerLevel serverLevel)) {
            return true;
        }
        syncKeyValidity(serverLevel, stack);
        if (!KeyDataUtil.isObsoleteKey(stack)) {
            return true;
        }

        player.displayClientMessage(
                Component.translatable("message.easyadventure.key_obsolete", getEffectiveBaseName(stack)).withStyle(ChatFormatting.RED),
                true
        );
        return false;
    }

    private static void syncKeyValidity(ServerLevel serverLevel, ItemStack stack) {
        UUID coreUuid = KeyDataUtil.getBoundUuid(stack);
        if (coreUuid == null) {
            KeyDataUtil.setObsoleteKey(stack, false);
            return;
        }

        BaseRegistryData.BaseRecord record = BaseRegistryData.get(serverLevel).getRecord(coreUuid);
        if (record == null || record.keyUuid() == null) {
            if (!KeyDataUtil.isObsoleteKey(stack)) {
                KeyDataUtil.setObsoleteKey(stack, false);
            }
            return;
        }

        UUID keyUuid = KeyDataUtil.getKeyUuid(stack);
        if (keyUuid == null) {
            KeyDataUtil.setObsoleteKey(stack, true);
            return;
        }

        KeyDataUtil.setObsoleteKey(stack, !keyUuid.equals(record.keyUuid()));
    }

    private static Component getDisplayBaseName(String baseName) {
        if (BaseCoreBlockEntity.DEFAULT_BASE_NAME.equals(baseName)) {
            return Component.translatable("name.easyadventure.default_base");
        }
        return Component.literal(baseName);
    }

    private static void migrateLegacyDisplayData(ItemStack stack) {
        if (!KeyDataUtil.hasBoundUuid(stack)) {
            return;
        }

        Component customName = stack.hasCustomHoverName() ? stack.getHoverName() : null;
        String customNameText = customName == null ? null : customName.getString().trim();
        String baseName = KeyDataUtil.getBaseName(stack);
        boolean hasBaseName = baseName != null && !baseName.isBlank();
        if (!hasBaseName && customNameText != null && !customNameText.isBlank()) {
            UUID boundUuid = KeyDataUtil.getBoundUuid(stack);
            if (boundUuid != null) {
                KeyDataUtil.setKeyData(
                        stack,
                        boundUuid,
                        KeyDataUtil.getStorageUuid(stack),
                        KeyDataUtil.getOwnerUuid(stack),
                        KeyDataUtil.getOwnerName(stack),
                        customNameText,
                        KeyDataUtil.isPasswordEnabled(stack),
                        KeyDataUtil.getPasswordHash(stack),
                        KeyDataUtil.getStoredSizeX(stack),
                        KeyDataUtil.getStoredSizeY(stack),
                        KeyDataUtil.getStoredSizeBelowY(stack),
                        KeyDataUtil.getStoredSizeZ(stack),
                        KeyDataUtil.getClearMode(stack),
                        KeyDataUtil.getResidents(stack),
                        KeyDataUtil.getUpgradeFuelTicks(stack),
                        KeyDataUtil.getQueuedUpgradeFuelCounts(stack)
                );
            }
        }

        if (stack.hasCustomHoverName()) {
            stack.resetHoverName();
        }
    }

    private static void consumeKeyFromHand(Player player, InteractionHand hand) {
        player.setItemInHand(hand, ItemStack.EMPTY);
        player.getInventory().setChanged();
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.containerMenu.broadcastChanges();
        }
    }

    private static void sendKeyAccessDenied(Player player, boolean passwordProtected) {
        MutableComponent message = Component.translatable(
                passwordProtected ? "message.easyadventure.password_incorrect" : "message.easyadventure.not_authorized_operation"
        );
        boolean overlay = passwordProtected;
        if (!passwordProtected) {
            message = message.withStyle(ChatFormatting.RED);
        }
        if (overlay) {
            player.displayClientMessage(message, true);
        } else {
            player.sendSystemMessage(message);
        }
    }

    private static boolean isResident(Player player, ItemStack stack) {
        if (CoreAccessControl.canAccess(player, KeyDataUtil.getOwnerUuid(stack))) {
            return true;
        }
        return KeyDataUtil.getResidents(stack).containsKey(player.getUUID());
    }

    private static Rotation toRotation(int rotationId) {
        return switch (Math.floorMod(rotationId, 4)) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    private static String rotationLabelKey(int rotationId) {
        return switch (Math.floorMod(rotationId, 4)) {
            case 1 -> "tooltip.easyadventure.rotation_90";
            case 2 -> "tooltip.easyadventure.rotation_180";
            case 3 -> "tooltip.easyadventure.rotation_270";
            default -> "tooltip.easyadventure.rotation_0";
        };
    }
}
