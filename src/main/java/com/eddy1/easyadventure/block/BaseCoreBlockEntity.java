package com.eddy1.easyadventure.block;

import com.eddy1.easyadventure.EasyAdventure;
import com.eddy1.easyadventure.block.core.CoreAccessControl;
import com.eddy1.easyadventure.block.core.CoreAreaCheckResult;
import com.eddy1.easyadventure.block.core.CoreEffects;
import com.eddy1.easyadventure.block.core.CoreEntityTransport;
import com.eddy1.easyadventure.block.core.CoreLifecycle;
import com.eddy1.easyadventure.block.core.CorePackager;
import com.eddy1.easyadventure.block.core.CorePasswordUtil;
import com.eddy1.easyadventure.block.core.CorePermission;
import com.eddy1.easyadventure.block.core.CorePersistence;
import com.eddy1.easyadventure.block.core.CorePhaseProcessor;
import com.eddy1.easyadventure.block.core.CorePreflight;
import com.eddy1.easyadventure.block.core.CorePreview;
import com.eddy1.easyadventure.block.core.CoreResident;
import com.eddy1.easyadventure.block.core.CoreRuntimeState;
import com.eddy1.easyadventure.block.core.CoreStoredState;
import com.eddy1.easyadventure.block.core.CoreStructureWorkspace;
import com.eddy1.easyadventure.block.core.CoreTerrainTracker;
import com.eddy1.easyadventure.block.core.CoreUpgrade;
import com.eddy1.easyadventure.block.core.CoreValidation;
import com.eddy1.easyadventure.block.core.CoreVolume;
import com.eddy1.easyadventure.init.ModBlockEntities;
import com.eddy1.easyadventure.storage.StructureSnapshot;
import com.eddy1.easyadventure.util.BlockPlacementUtil;
import com.eddy1.easyadventure.util.SavedBlockInfo;
import com.eddy1.easyadventure.world.BuildingStorageData;
import com.eddy1.easyadventure.world.TerritoryManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BaseCoreBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity implements Container {
    private static final int BASE_BLOCKS_PER_TICK = 200;
    private static final int BOOSTED_BLOCKS_PER_TICK = 280;

    public static final int MIN_SIZE_XZ = 3;
    public static final int MAX_SIZE_XZ = 64;
    public static final int MIN_SIZE_Y = 2;
    public static final int MAX_SIZE_Y = 320;
    public static final String DEFAULT_BASE_NAME = CoreStoredState.DEFAULT_BASE_NAME;
    private static final Comparator<SavedBlockInfo> PLACEMENT_ORDER = Comparator
            .comparingInt((SavedBlockInfo info) -> info.relativePos().getY())
            .thenComparingInt(info -> info.relativePos().getX())
            .thenComparingInt(info -> info.relativePos().getZ());
    private static final Comparator<BlockPos> RELATIVE_POSITION_ORDER = Comparator
            .comparingInt((BlockPos pos) -> pos.getY())
            .thenComparingInt(pos -> pos.getX())
            .thenComparingInt(pos -> pos.getZ());

    public enum State {
        IDLE,
        PACKING,
        RESTORING,
        CLEARING,
        GENERATING,
        UNPACKING,
        APPLYING_BLOCK_ENTITY_DATA
    }

    private final CoreRuntimeState runtime = new CoreRuntimeState();
    private final CoreStructureWorkspace workspace = new CoreStructureWorkspace();
    private final CoreTerrainTracker terrainTracker = new CoreTerrainTracker();
    private final NonNullList<ItemStack> upgradeFuelInventory = NonNullList.withSize(CoreUpgrade.values().length, ItemStack.EMPTY);

    private CoreStoredState storedState = new CoreStoredState(DEFAULT_BASE_NAME, UUID.randomUUID(), null, null, null, false, null, false, false, 9, 5, 9, Map.of(), Map.of());
    private State persistedRuntimeState = State.IDLE;

    public BaseCoreBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.BASE_CORE.get(), pos, blockState);
        updateRangeFromBlock();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BaseCoreBlockEntity blockEntity) {
        boolean passiveChanged = false;
        if (level instanceof ServerLevel serverLevel) {
            passiveChanged = blockEntity.tickUpgradeFuel(serverLevel);
        }

        if (level.isClientSide || blockEntity.runtime.isIdle()) {
            if (passiveChanged) {
                blockEntity.setChanged();
            }
            return;
        }

        if (blockEntity.runtime.incrementSoundTick() % 5 == 0) {
            float progress = blockEntity.runtime.progress();
            if (blockEntity.runtime.state() == State.PACKING
                    || blockEntity.runtime.state() == State.CLEARING
                    || blockEntity.runtime.state() == State.GENERATING
                    || blockEntity.runtime.state() == State.RESTORING) {
                level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.4F, 0.7F + progress);
            } else {
                level.playSound(null, pos, SoundEvents.CONDUIT_AMBIENT, SoundSource.BLOCKS, 0.4F, 1.4F - progress * 0.5F);
            }
        }

        if (level instanceof ServerLevel serverLevel) {
            CoreEffects.playCoreAmbience(serverLevel, pos);
            if (blockEntity.runtime.state() != State.APPLYING_BLOCK_ENTITY_DATA) {
                CoreEffects.playScannerEffect(serverLevel, pos, blockEntity.currentVolume(), blockEntity.runtime.currentProcessingY());
            }
        }

        boolean changed = false;
        int blocksPerTick = blockEntity.getBlocksPerTick();
        for (int i = 0; i < blocksPerTick && blockEntity.runtime.hasTasks(); i++) {
            BlockPos target = blockEntity.runtime.pollTask();
            if (target == null) {
                break;
            }

            blockEntity.runtime.markProcessed(pos, target);
            changed = true;
            switch (blockEntity.runtime.state()) {
                case PACKING -> blockEntity.processPacking(target);
                case RESTORING -> blockEntity.processRestoring(target);
                case CLEARING -> blockEntity.processClearing(target);
                case GENERATING -> blockEntity.processGenerating(target);
                case UNPACKING -> blockEntity.processUnpacking(target);
                case APPLYING_BLOCK_ENTITY_DATA -> blockEntity.processApplyingBlockEntityData(target);
                case IDLE -> {
                }
            }
        }

        if (changed || passiveChanged) {
            blockEntity.setChanged();
        }
        if (!blockEntity.runtime.hasTasks()) {
            blockEntity.finishTask();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        TerritoryManager.register(serverLevel, this);
        if (storedState.activeStorageUUID() != null) {
            BuildingStorageData.get(serverLevel).lockBuilding(storedState.activeStorageUUID(), storedState.coreUUID());
        }
        resumePersistedWork();
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) {
            TerritoryManager.unregister(serverLevel, worldPosition);
        }
        super.setRemoved();
    }

    public int getSizeX() {
        return storedState.sizeX();
    }

    public int getSizeY() {
        return storedState.sizeY();
    }

    public int getSizeZ() {
        return storedState.sizeZ();
    }

    public boolean isBound() {
        return storedState.bound();
    }

    public boolean isBusy() {
        return runtime.isBusy();
    }

    public UUID getCoreUUID() {
        return storedState.coreUUID();
    }

    public @Nullable UUID getOwnerUUID() {
        return storedState.ownerUUID();
    }

    public @Nullable String getOwnerName() {
        return storedState.ownerName();
    }

    public String getBaseName() {
        return storedState.baseName();
    }

    public boolean isPasswordEnabled() {
        return storedState.passwordEnabled();
    }

    public @Nullable String getPasswordHash() {
        return storedState.passwordHash();
    }

    public Map<UUID, CoreResident> getResidents() {
        return storedState.residents();
    }

    public List<CoreResident> getResidentList() {
        return storedState.residents().values().stream()
                .sorted(Comparator.comparing(CoreResident::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public boolean hasUpgrade(CoreUpgrade upgrade) {
        return storedState.hasUpgrade(upgrade);
    }

    public int getUpgradeFuelTicks(CoreUpgrade upgrade) {
        return storedState.getUpgradeFuelTicks(upgrade);
    }

    public Map<CoreUpgrade, Integer> getUpgradeFuelTicks() {
        return storedState.upgradeFuelTicks();
    }

    public int getActiveUpgradeCount() {
        return storedState.activeUpgradeCount();
    }

    public int getUpgradeFuelSlotIndex(CoreUpgrade upgrade) {
        return upgrade.ordinal();
    }

    public CoreVolume getTerritoryVolume() {
        return currentVolume();
    }

    public boolean isTerritoryActive() {
        return storedState.initialized();
    }

    public boolean canPlayerManage(@Nullable Player player) {
        return CoreAccessControl.canAccess(player, storedState.ownerUUID())
                || hasResidentPermission(player, CorePermission.RESIZE);
    }

    public boolean canResize(@Nullable Player player) {
        return hasResidentPermission(player, CorePermission.RESIZE);
    }

    public boolean isResident(@Nullable Player player) {
        if (CoreAccessControl.canAccess(player, storedState.ownerUUID())) {
            return true;
        }
        return player != null && storedState.hasResident(player.getUUID());
    }

    public boolean hasResidentPermission(@Nullable Player player, CorePermission permission) {
        if (CoreAccessControl.canAccess(player, storedState.ownerUUID())) {
            return true;
        }
        return player != null && storedState.hasResidentPermission(player.getUUID(), permission);
    }

    public boolean canEnterTerritory(@Nullable Player player) {
        return hasResidentPermission(player, CorePermission.ENTER);
    }

    public boolean canBuild(@Nullable Player player) {
        return hasResidentPermission(player, CorePermission.BUILD);
    }

    public boolean canUseStorage(@Nullable Player player) {
        return hasResidentPermission(player, CorePermission.STORAGE);
    }

    public boolean canUseDevices(@Nullable Player player) {
        return hasResidentPermission(player, CorePermission.USE_DEVICES);
    }

    public boolean canPlayerOperate(@Nullable Player player, @Nullable String password) {
        if (isResident(player)) {
            return true;
        }
        if (!storedState.passwordEnabled()) {
            return true;
        }
        return CorePasswordUtil.matches(storedState.coreUUID(), password, storedState.passwordHash());
    }

    public boolean isPasswordRequiredFor(@Nullable Player player) {
        return storedState.passwordEnabled() && !isResident(player);
    }

    public void setOwner(@Nullable UUID ownerUuid, @Nullable String ownerName) {
        applyStoredState(storedState.withOwner(ownerUuid, ownerName));
        setChanged();
    }

    public void setOwnerFromPlayer(@Nullable Player player) {
        if (player == null || storedState.ownerUUID() != null) {
            return;
        }
        setOwner(player.getUUID(), player.getGameProfile().getName());
    }

    public void setBaseName(String name) {
        applyStoredState(storedState.withName(name));
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public boolean updateSettings(@Nullable Player player, int newX, int newY, int newZ, boolean passwordEnabled, @Nullable String rawPassword) {
        boolean managerAccess = canPlayerManage(player);
        if (!managerAccess && !canResize(player)) {
            notifyPlayer(player, "message.easyadventure.not_authorized_operation");
            return false;
        }

        String normalizedPassword = CorePasswordUtil.normalize(rawPassword);
        if (!managerAccess) {
            if (passwordEnabled != storedState.passwordEnabled() || normalizedPassword != null) {
                notifyPlayer(player, "message.easyadventure.not_authorized_operation");
                return false;
            }
        } else if (!updatePasswordSettings(player, passwordEnabled, rawPassword)) {
            return false;
        }
        return initializeFoundation(player, newX, newY, newZ);
    }

    public boolean updatePasswordSettings(@Nullable Player player, boolean passwordEnabled, @Nullable String rawPassword) {
        if (!canPlayerManage(player)) {
            notifyPlayer(player, "message.easyadventure.not_authorized_operation");
            return false;
        }

        String normalizedPassword = CorePasswordUtil.normalize(rawPassword);
        String nextHash = storedState.passwordHash();
        if (normalizedPassword != null) {
            nextHash = CorePasswordUtil.hash(storedState.coreUUID(), normalizedPassword);
        }

        if (passwordEnabled && (nextHash == null || nextHash.isBlank())) {
            notifyPlayer(player, "message.easyadventure.password_required");
            return false;
        }

        applyStoredState(storedState.withPassword(passwordEnabled, nextHash));
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        return true;
    }

    public boolean initializeFoundation(int newX, int newY, int newZ) {
        return initializeFoundation(null, newX, newY, newZ);
    }

    public boolean initializeFoundation(@Nullable Player player, int newX, int newY, int newZ) {
        if (runtime.isBusy()) {
            notifyPlayer(player, "message.easyadventure.core_busy");
            return false;
        }
        if (!canResize(player)) {
            notifyPlayer(player, "message.easyadventure.not_authorized_operation");
            return false;
        }

        int clampedX = clamp(newX, MIN_SIZE_XZ, MAX_SIZE_XZ);
        int clampedY = clamp(newY, MIN_SIZE_Y, MAX_SIZE_Y);
        int clampedZ = clamp(newZ, MIN_SIZE_XZ, MAX_SIZE_XZ);
        int oldX = storedState.sizeX();
        int oldY = storedState.sizeY();
        int oldZ = storedState.sizeZ();
        boolean firstInitialization = !storedState.initialized();

        if (!firstInitialization && oldX == clampedX && oldY == clampedY && oldZ == clampedZ) {
            return true;
        }

        if (player != null) {
            setOwnerFromPlayer(player);
        }
        applyStoredState(storedState.withSize(clampedX, clampedY, clampedZ).withInitialized(true));
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            TerritoryManager.refresh(serverLevel, this);
        }

        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            level.playSound(null, worldPosition, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        if (firstInitialization) {
            terrainTracker.clear();
        }

        startSmartResizeTask(oldX, oldY, oldZ, getSizeX(), getSizeY(), getSizeZ(), firstInitialization);
        return true;
    }

    public boolean restoreFromTag(
            CompoundTag heavyData,
            String name,
            @Nullable UUID boundUUID,
            @Nullable UUID ownerUUID,
            @Nullable String ownerName,
            boolean passwordEnabled,
            @Nullable String passwordHash,
            @Nullable UUID storageUUID,
            Map<UUID, CoreResident> residents,
            Map<CoreUpgrade, Integer> upgradeFuelTicks,
            Map<CoreUpgrade, Integer> queuedUpgradeFuelCounts
    ) {
        return restoreFromSnapshot(
                StructureSnapshot.fromTag(heavyData),
                name,
                boundUUID,
                ownerUUID,
                ownerName,
                passwordEnabled,
                passwordHash,
                storageUUID,
                residents,
                upgradeFuelTicks,
                queuedUpgradeFuelCounts
        );
    }

    public boolean restoreFromSnapshot(
            StructureSnapshot snapshot,
            String name,
            @Nullable UUID boundUUID,
            @Nullable UUID ownerUUID,
            @Nullable String ownerName,
            boolean passwordEnabled,
            @Nullable String passwordHash,
            @Nullable UUID storageUUID,
            Map<UUID, CoreResident> residents,
            Map<CoreUpgrade, Integer> upgradeFuelTicks,
            Map<CoreUpgrade, Integer> queuedUpgradeFuelCounts
    ) {
        if (runtime.isBusy() || CoreValidation.containsNestedCore(snapshot)) {
            return false;
        }

        applyStoredState(storedState
                .withSize(snapshot.sizeX(), snapshot.sizeY(), snapshot.sizeZ())
                .withInitialized(true)
                .withName(name)
                .withBinding(true, boundUUID)
                .withOwner(ownerUUID, ownerName)
                .withPassword(passwordEnabled, passwordHash)
                .withActiveStorage(storageUUID)
                .withResidents(residents)
                .withUpgradeFuelTicks(upgradeFuelTicks));
        restoreQueuedUpgradeFuel(queuedUpgradeFuelCounts);
        if (level instanceof ServerLevel serverLevel) {
            TerritoryManager.refresh(serverLevel, this);
        }

        workspace.importIncomingSnapshot(snapshot);
        terrainTracker.clear();
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }

        startDeployment();
        return true;
    }

    public boolean updateResident(@Nullable Player player, ResidentAction action, @Nullable String residentValue) {
        if (runtime.isBusy()) {
            notifyPlayer(player, "message.easyadventure.core_busy");
            return false;
        }
        if (!canPlayerManage(player)) {
            notifyPlayer(player, "message.easyadventure.not_authorized_operation");
            return false;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        switch (action) {
            case ADD -> {
                String normalizedName = normalizeInput(residentValue);
                if (normalizedName == null) {
                    notifyPlayer(player, "message.easyadventure.resident_name_required");
                    return false;
                }

                Player target = serverLevel.getServer().getPlayerList().getPlayerByName(normalizedName);
                UUID targetUuid;
                String targetName;
                if (target != null) {
                    targetUuid = target.getUUID();
                    targetName = target.getGameProfile().getName();
                } else {
                    notifyPlayer(player, Component.translatable("message.easyadventure.resident_not_found", normalizedName));
                    return false;
                }
                if (storedState.ownerUUID() != null && storedState.ownerUUID().equals(targetUuid)) {
                    notifyPlayer(player, Component.translatable("message.easyadventure.resident_is_owner", targetName));
                    return false;
                }

                applyStoredState(storedState.addResident(targetUuid, targetName));
                notifyPlayer(player, Component.translatable("message.easyadventure.resident_added", targetName));
            }
            case REMOVE -> {
                UUID residentUuid = parseUuid(residentValue);
                if (residentUuid == null || !storedState.residents().containsKey(residentUuid)) {
                    notifyPlayer(player, "message.easyadventure.resident_missing");
                    return false;
                }

                String removedName = storedState.residents().get(residentUuid).name();
                applyStoredState(storedState.removeResident(residentUuid));
                notifyPlayer(player, Component.translatable("message.easyadventure.resident_removed", removedName));
            }
        }

        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        return true;
    }

    public boolean updateResidentPermission(@Nullable Player player, UUID residentUuid, CorePermission permission, boolean enabled) {
        if (runtime.isBusy()) {
            notifyPlayer(player, "message.easyadventure.core_busy");
            return false;
        }
        if (!canPlayerManage(player)) {
            notifyPlayer(player, "message.easyadventure.not_authorized_operation");
            return false;
        }
        if (!storedState.residents().containsKey(residentUuid)) {
            notifyPlayer(player, "message.easyadventure.resident_missing");
            return false;
        }
        UUID effectiveUuid = player == null ? null : player.getUUID();
        if (effectiveUuid != null && effectiveUuid.equals(residentUuid)) {
            notifyPlayer(player, "message.easyadventure.resident_self_permission_denied");
            return false;
        }

        applyStoredState(storedState.updateResidentPermission(residentUuid, permission, enabled));
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        return true;
    }

    public boolean startPacking(@Nullable Player player) {
        return startPacking(player, null);
    }

    public boolean startPacking(@Nullable Player player, @Nullable String password) {
        if (runtime.isBusy()) {
            notifyPlayer(player, "message.easyadventure.core_busy");
            return false;
        }
        if (!canPlayerOperate(player, password)) {
            notifyPlayer(player, storedState.passwordEnabled() ? "message.easyadventure.password_incorrect" : "message.easyadventure.not_authorized_operation");
            return false;
        }
        if (player != null) {
            setOwnerFromPlayer(player);
        }

        updateRangeFromBlock();
        if (level != null) {
            BlockPos nestedCorePos = CoreValidation.findNestedCore(level, worldPosition, currentVolume());
            if (nestedCorePos != null) {
                notifyPlayer(player, Component.translatable(
                        "message.easyadventure.nested_core",
                        nestedCorePos.getX(),
                        nestedCorePos.getY(),
                        nestedCorePos.getZ()
                ));
                return false;
            }

            CoreAreaCheckResult check = CorePreflight.checkPacking(level, worldPosition, currentVolume());
            if (!check.ok()) {
                notifyPlayer(player, check.reason());
                return false;
            }
        }

        workspace.clearPacked();
        captureEntitiesWithinBounds();
        CoreLifecycle.startPacking(runtime, worldPosition, currentVolume());
        setChanged();

        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0F, 0.5F);
        }
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        CorePersistence.save(tag, storedState, runtime.state(), terrainTracker, workspace, upgradeFuelInventory);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        applyStoredState(CorePersistence.loadStoredState(tag));
        CorePersistence.loadTransientData(tag, terrainTracker, workspace, upgradeFuelInventory);
        persistedRuntimeState = CorePersistence.loadRuntimeState(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        CorePersistence.writeUpdateTag(tag, storedState, upgradeFuelInventory);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void resumePersistedWork() {
        if (persistedRuntimeState == State.IDLE) {
            return;
        }

        CoreLifecycle.resume(
                runtime,
                persistedRuntimeState,
                worldPosition,
                currentVolume(),
                workspace,
                PLACEMENT_ORDER,
                RELATIVE_POSITION_ORDER
        );
        if (runtime.isBusy()) {
            EasyAdventure.LOGGER.warn("Resumed interrupted base operation {} at {}", persistedRuntimeState, worldPosition);
            notifyOwnerTransactionResumed();
            setChanged();
        }
        persistedRuntimeState = State.IDLE;
    }

    private void notifyOwnerTransactionResumed() {
        if (!(level instanceof ServerLevel serverLevel) || storedState.ownerUUID() == null) {
            return;
        }

        Player owner = serverLevel.getServer().getPlayerList().getPlayer(storedState.ownerUUID());
        if (owner != null) {
            owner.displayClientMessage(Component.translatable("message.easyadventure.transaction_resumed"), false);
        }
    }

    private void updateRangeFromBlock() {
        if (level == null) {
            return;
        }

        if (getBlockState().getBlock() instanceof BaseCoreBlock coreBlock && !storedState.initialized()) {
            applyStoredState(storedState.withSize(coreBlock.getSizeX(), coreBlock.getSizeY(), coreBlock.getSizeZ()));
        }
    }

    private void startSmartResizeTask(int oldX, int oldY, int oldZ, int newX, int newY, int newZ, boolean firstInitialization) {
        CoreLifecycle.startResize(
                runtime,
                worldPosition,
                new CoreVolume(oldX, oldY, oldZ),
                new CoreVolume(newX, newY, newZ),
                firstInitialization
        );
        setChanged();
    }

    private void startDeployment() {
        CoreLifecycle.startDeployment(runtime, worldPosition, currentVolume());
        setChanged();
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 2.0F, 0.5F);
        }
    }

    private void startBuildingPhase() {
        CoreLifecycle.startBuilding(runtime, worldPosition, currentVolume());
        setChanged();
    }

    private void startUnpackingPhase() {
        if (!CoreLifecycle.startUnpacking(runtime, worldPosition, workspace, PLACEMENT_ORDER)) {
            finishDeployment();
            return;
        }

        setChanged();
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.CONDUIT_ACTIVATE, SoundSource.BLOCKS, 2.0F, 1.0F);
        }
    }

    private void startApplyingBlockEntityDataPhase() {
        if (!CoreLifecycle.startApplyingBlockEntityData(runtime, worldPosition, workspace, RELATIVE_POSITION_ORDER)) {
            finishDeployment();
            return;
        }
        setChanged();
    }

    private void startRestoringPhase() {
        CoreLifecycle.startRestoring(runtime, worldPosition, currentVolume());
        setChanged();
    }

    private void processGenerating(BlockPos pos) {
        if (level == null) {
            return;
        }
        CorePhaseProcessor.processGenerating(level, worldPosition, currentVolume(), terrainTracker, pos, this::captureBlockEntityData);
    }

    private void processClearing(BlockPos pos) {
        if (level == null) {
            return;
        }
        CorePhaseProcessor.processClearing(level, worldPosition, terrainTracker, pos, this::captureBlockEntityData);
    }

    private void processUnpacking(BlockPos targetPos) {
        if (level == null) {
            return;
        }
        CorePhaseProcessor.processUnpacking(level, worldPosition, workspace, targetPos);
    }

    private void processApplyingBlockEntityData(BlockPos targetPos) {
        if (level == null) {
            return;
        }
        CorePhaseProcessor.processApplyingBlockEntityData(level, worldPosition, workspace, targetPos);
    }

    private void processPacking(BlockPos pos) {
        if (level == null) {
            return;
        }
        CorePhaseProcessor.processPacking(level, worldPosition, workspace, pos, this::captureBlockEntityData);
    }

    private void processRestoring(BlockPos pos) {
        if (level == null) {
            return;
        }
        terrainTracker.restore(level, worldPosition, pos);
    }

    private void finishTask() {
        switch (runtime.state()) {
            case CLEARING -> startBuildingPhase();
            case GENERATING -> startUnpackingPhase();
            case UNPACKING -> startApplyingBlockEntityDataPhase();
            case APPLYING_BLOCK_ENTITY_DATA -> finishDeployment();
            case PACKING -> startRestoringPhase();
            case RESTORING -> finishPacking();
            case IDLE -> {
            }
        }
    }

    private void finishPacking() {
        if (level == null) {
            runtime.finish();
            setChanged();
            return;
        }

        Map<CoreUpgrade, Integer> queuedUpgradeFuelCounts = getQueuedUpgradeFuelCounts();
        clearContent();
        runtime.finish();
        setChanged();
        CorePackager.finishPacking(
                level,
                worldPosition,
                storedState.baseName(),
                storedState.coreUUID(),
                storedState.ownerUUID(),
                storedState.ownerName(),
                storedState.passwordEnabled(),
                storedState.passwordHash(),
                workspace,
                currentVolume(),
                storedState.residents(),
                storedState.upgradeFuelTicks(),
                queuedUpgradeFuelCounts
        );
    }

    private void finishDeployment() {
        refreshPlacedBlocks();
        restoreIncomingEntities();

        if (storedState.activeStorageUUID() != null && level instanceof ServerLevel serverLevel) {
            BuildingStorageData.get(serverLevel).removeBuilding(storedState.activeStorageUUID());
        }

        runtime.finish();
        workspace.clearIncoming();
        applyStoredState(storedState.withActiveStorage(null));
        setChanged();
        performCelebration();
    }

    private void captureEntitiesWithinBounds() {
        if (level == null || level.isClientSide) {
            return;
        }

        CoreEntityTransport.capture(level, worldPosition, currentVolume(), workspace.packedEntities());
    }

    private void refreshPlacedBlocks() {
        if (level == null) {
            return;
        }

        for (SavedBlockInfo info : workspace.incomingBlocks()) {
            BlockPlacementUtil.refreshNeighbors(level, worldPosition.offset(info.relativePos()));
        }
    }

    private void restoreIncomingEntities() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        CoreEntityTransport.restore(serverLevel, worldPosition, workspace.incomingEntities());
    }

    private @Nullable CompoundTag captureBlockEntityData(BlockPos pos) {
        if (level == null) {
            return null;
        }
        return CorePackager.captureBlockEntityData(level, pos);
    }

    private void performCelebration() {
        if (level == null || level.isClientSide) {
            return;
        }
        CoreEffects.performCelebration(level, worldPosition);
    }

    public void dropUpgradeFuelInventory() {
        if (level == null || level.isClientSide || isEmpty()) {
            return;
        }

        Containers.dropContents(level, worldPosition, this);
        clearContent();
    }

    public Map<CoreUpgrade, Integer> getQueuedUpgradeFuelCounts() {
        EnumMap<CoreUpgrade, Integer> queuedCounts = new EnumMap<>(CoreUpgrade.class);
        for (CoreUpgrade upgrade : CoreUpgrade.values()) {
            ItemStack stack = getItem(getUpgradeFuelSlotIndex(upgrade));
            if (!stack.isEmpty()) {
                queuedCounts.put(upgrade, stack.getCount());
            }
        }
        return queuedCounts;
    }

    private void restoreQueuedUpgradeFuel(Map<CoreUpgrade, Integer> queuedUpgradeFuelCounts) {
        clearContent();
        if (queuedUpgradeFuelCounts == null || queuedUpgradeFuelCounts.isEmpty()) {
            return;
        }
        for (CoreUpgrade upgrade : CoreUpgrade.values()) {
            int count = Math.max(0, queuedUpgradeFuelCounts.getOrDefault(upgrade, 0));
            if (count <= 0) {
                continue;
            }
            upgradeFuelInventory.set(upgrade.ordinal(), new ItemStack(upgrade.material(), count));
        }
    }

    private void applyStoredState(CoreStoredState nextState) {
        storedState = new CoreStoredState(
                normalizeBaseName(nextState.baseName()),
                nextState.coreUUID(),
                nextState.ownerUUID(),
                normalizeOwnerName(nextState.ownerName()),
                nextState.activeStorageUUID(),
                nextState.passwordEnabled(),
                nextState.passwordHash(),
                nextState.bound(),
                nextState.initialized(),
                clamp(nextState.sizeX(), MIN_SIZE_XZ, MAX_SIZE_XZ),
                clamp(nextState.sizeY(), MIN_SIZE_Y, MAX_SIZE_Y),
                clamp(nextState.sizeZ(), MIN_SIZE_XZ, MAX_SIZE_XZ),
                nextState.residents(),
                nextState.upgradeFuelTicks()
        );
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String normalizeBaseName(@Nullable String name) {
        if (name == null) {
            return DEFAULT_BASE_NAME;
        }
        String trimmed = name.trim();
        return trimmed.isEmpty() ? DEFAULT_BASE_NAME : trimmed;
    }

    private static @Nullable String normalizeOwnerName(@Nullable String ownerName) {
        if (ownerName == null) {
            return null;
        }
        String trimmed = ownerName.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static @Nullable String normalizeInput(@Nullable String input) {
        if (input == null) {
            return null;
        }

        String trimmed = input.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int getBlocksPerTick() {
        return storedState.hasUpgrade(CoreUpgrade.FOLDING) ? BOOSTED_BLOCKS_PER_TICK : BASE_BLOCKS_PER_TICK;
    }

    private boolean tickUpgradeFuel(ServerLevel serverLevel) {
        CoreStoredState nextState = storedState.tickUpgradeFuel(1);
        boolean changed = nextState != storedState;
        boolean refueled = false;
        boolean expiredUpgrade = false;
        for (CoreUpgrade upgrade : CoreUpgrade.values()) {
            int previousTicks = storedState.getUpgradeFuelTicks(upgrade);
            if (previousTicks > 0 && nextState.getUpgradeFuelTicks(upgrade) == 0) {
                expiredUpgrade = true;
            }
            if (nextState.getUpgradeFuelTicks(upgrade) > 0) {
                continue;
            }

            int slot = getUpgradeFuelSlotIndex(upgrade);
            ItemStack fuelStack = getItem(slot);
            if (fuelStack.isEmpty() || fuelStack.getCount() < upgrade.materialCount() || !fuelStack.is(upgrade.material())) {
                continue;
            }

            fuelStack.shrink(upgrade.materialCount());
            if (fuelStack.isEmpty()) {
                upgradeFuelInventory.set(slot, ItemStack.EMPTY);
            }

            EnumMap<CoreUpgrade, Integer> updatedFuel = new EnumMap<>(CoreUpgrade.class);
            updatedFuel.putAll(nextState.upgradeFuelTicks());
            updatedFuel.put(upgrade, upgrade.durationPerFuelTicks());
            nextState = nextState.withUpgradeFuelTicks(updatedFuel);
            changed = true;
            refueled = true;
        }
        if (!changed) {
            return false;
        }

        applyStoredState(nextState);
        if (refueled || expiredUpgrade || serverLevel.getGameTime() % 20 == 0) {
            setChanged();
        }
        if (level != null && (refueled || expiredUpgrade || serverLevel.getGameTime() % 20 == 0)) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        return refueled || expiredUpgrade || serverLevel.getGameTime() % 20 == 0;
    }

    public static Component formatDuration(int ticks) {
        int totalSeconds = Math.max(0, ticks) / 20;
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        if (hours > 0) {
            return Component.translatable("gui.easyadventure.duration_hours_minutes", hours, minutes);
        }
        if (minutes > 0) {
            return Component.translatable("gui.easyadventure.duration_minutes_seconds", minutes, seconds);
        }
        return Component.translatable("gui.easyadventure.duration_seconds", seconds);
    }

    public boolean containsTerritoryPos(BlockPos pos) {
        return storedState.initialized() && currentVolume().contains(worldPosition, pos);
    }

    private static @Nullable UUID parseUuid(@Nullable String rawUuid) {
        String normalized = normalizeInput(rawUuid);
        if (normalized == null) {
            return null;
        }

        try {
            return UUID.fromString(normalized);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static void notifyPlayer(@Nullable Player player, String translationKey) {
        if (player != null) {
            MutableComponent message = Component.translatable(translationKey);
            boolean permissionWarning = isPermissionWarning(translationKey);
            if (isPermissionWarning(translationKey)) {
                message = message.withStyle(ChatFormatting.RED);
            }
            if (permissionWarning) {
                player.sendSystemMessage(message);
            } else {
                player.displayClientMessage(message, true);
            }
        }
    }

    private static void notifyPlayer(@Nullable Player player, @Nullable Component message) {
        if (player != null && message != null) {
            player.displayClientMessage(message, true);
        }
    }

    private static boolean isPermissionWarning(String translationKey) {
        return "message.easyadventure.not_authorized_operation".equals(translationKey)
                || "message.easyadventure.resident_self_permission_denied".equals(translationKey)
                || translationKey.startsWith("message.easyadventure.territory_");
    }

    @Override
    public int getContainerSize() {
        return upgradeFuelInventory.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : upgradeFuelInventory) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < upgradeFuelInventory.size() ? upgradeFuelInventory.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= upgradeFuelInventory.size() || amount <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack removed = getItem(slot).split(amount);
        if (!removed.isEmpty()) {
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= upgradeFuelInventory.size()) {
            return ItemStack.EMPTY;
        }

        ItemStack removed = upgradeFuelInventory.get(slot);
        upgradeFuelInventory.set(slot, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= upgradeFuelInventory.size()) {
            return;
        }
        if (!stack.isEmpty() && !canPlaceItem(slot, stack)) {
            return;
        }

        upgradeFuelInventory.set(slot, stack);
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= CoreUpgrade.values().length) {
            return false;
        }
        return stack.is(CoreUpgrade.fromId(slot).material());
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < upgradeFuelInventory.size(); i++) {
            upgradeFuelInventory.set(i, ItemStack.EMPTY);
        }
    }

    private CoreVolume currentVolume() {
        return storedState.volume();
    }

    public enum ResidentAction {
        ADD,
        REMOVE;

        public static ResidentAction fromId(int id) {
            ResidentAction[] values = values();
            if (id < 0 || id >= values.length) {
                return ADD;
            }
            return values[id];
        }
    }
}
