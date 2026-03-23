package com.eddy1.easyadventure.block;

import com.eddy1.easyadventure.EasyAdventure;
import com.eddy1.easyadventure.block.core.CoreAccessControl;
import com.eddy1.easyadventure.block.core.CoreAreaCheckResult;
import com.eddy1.easyadventure.block.core.CoreEffects;
import com.eddy1.easyadventure.block.core.CoreEntityTransport;
import com.eddy1.easyadventure.block.core.CoreLifecycle;
import com.eddy1.easyadventure.block.core.CorePackager;
import com.eddy1.easyadventure.block.core.CorePasswordUtil;
import com.eddy1.easyadventure.block.core.CorePersistence;
import com.eddy1.easyadventure.block.core.CorePhaseProcessor;
import com.eddy1.easyadventure.block.core.CorePreflight;
import com.eddy1.easyadventure.block.core.CoreRuntimeState;
import com.eddy1.easyadventure.block.core.CoreStoredState;
import com.eddy1.easyadventure.block.core.CoreStructureWorkspace;
import com.eddy1.easyadventure.block.core.CoreTerrainTracker;
import com.eddy1.easyadventure.block.core.CoreValidation;
import com.eddy1.easyadventure.block.core.CoreVolume;
import com.eddy1.easyadventure.init.ModBlockEntities;
import com.eddy1.easyadventure.storage.StructureSnapshot;
import com.eddy1.easyadventure.util.BlockPlacementUtil;
import com.eddy1.easyadventure.util.SavedBlockInfo;
import com.eddy1.easyadventure.world.BuildingStorageData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.UUID;

public class BaseCoreBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity {
    public static final int MIN_SIZE_XZ = 3;
    public static final int MAX_SIZE_XZ = 64;
    public static final int MIN_SIZE_Y = 2;
    public static final int MAX_SIZE_Y = 320;
    public static final String DEFAULT_BASE_NAME = CoreStoredState.DEFAULT_BASE_NAME;

    private static final int BLOCKS_PER_TICK = 200;
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

    private CoreStoredState storedState = new CoreStoredState(DEFAULT_BASE_NAME, UUID.randomUUID(), null, null, null, false, null, false, false, 9, 5, 9);
    private State persistedRuntimeState = State.IDLE;

    public BaseCoreBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.BASE_CORE.get(), pos, blockState);
        updateRangeFromBlock();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BaseCoreBlockEntity blockEntity) {
        if (level.isClientSide || blockEntity.runtime.isIdle()) {
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
        for (int i = 0; i < BLOCKS_PER_TICK && blockEntity.runtime.hasTasks(); i++) {
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

        if (changed) {
            blockEntity.setChanged();
        }
        if (!blockEntity.runtime.hasTasks()) {
            blockEntity.finishTask();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null || level.isClientSide) {
            return;
        }

        if (storedState.activeStorageUUID() != null && level instanceof ServerLevel serverLevel) {
            BuildingStorageData.get(serverLevel).lockBuilding(storedState.activeStorageUUID(), storedState.coreUUID());
        }
        resumePersistedWork();
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

    public boolean canPlayerAccess(@Nullable Player player) {
        return CoreAccessControl.canAccess(player, storedState.ownerUUID());
    }

    public boolean canPlayerOperate(@Nullable Player player, @Nullable String password) {
        if (CoreAccessControl.canAccess(player, storedState.ownerUUID())) {
            return true;
        }
        if (!storedState.passwordEnabled()) {
            return true;
        }
        return CorePasswordUtil.matches(storedState.coreUUID(), password, storedState.passwordHash());
    }

    public boolean isPasswordRequiredFor(@Nullable Player player) {
        return storedState.passwordEnabled() && !CoreAccessControl.canAccess(player, storedState.ownerUUID());
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
        if (CoreAccessControl.denyIfNoAccess(player, storedState.ownerUUID(), storedState.ownerName())) {
            return false;
        }

        if (!updatePasswordSettings(player, passwordEnabled, rawPassword)) {
            return false;
        }
        return initializeFoundation(player, newX, newY, newZ);
    }

    public boolean updatePasswordSettings(@Nullable Player player, boolean passwordEnabled, @Nullable String rawPassword) {
        if (CoreAccessControl.denyIfNoAccess(player, storedState.ownerUUID(), storedState.ownerName())) {
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
        if (CoreAccessControl.denyIfNoAccess(player, storedState.ownerUUID(), storedState.ownerName())) {
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
            @Nullable UUID storageUUID
    ) {
        return restoreFromSnapshot(StructureSnapshot.fromTag(heavyData), name, boundUUID, ownerUUID, ownerName, passwordEnabled, passwordHash, storageUUID);
    }

    public boolean restoreFromSnapshot(
            StructureSnapshot snapshot,
            String name,
            @Nullable UUID boundUUID,
            @Nullable UUID ownerUUID,
            @Nullable String ownerName,
            boolean passwordEnabled,
            @Nullable String passwordHash,
            @Nullable UUID storageUUID
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
                .withActiveStorage(storageUUID));

        workspace.importIncomingSnapshot(snapshot);
        terrainTracker.clear();
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }

        startDeployment();
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
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CorePersistence.save(tag, storedState, runtime.state(), terrainTracker, workspace);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        applyStoredState(CorePersistence.loadStoredState(tag));
        CorePersistence.loadTransientData(tag, terrainTracker, workspace);
        persistedRuntimeState = CorePersistence.loadRuntimeState(tag);
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        CorePersistence.writeUpdateTag(tag, storedState);
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
                currentVolume()
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
                clamp(nextState.sizeZ(), MIN_SIZE_XZ, MAX_SIZE_XZ)
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

    private static void notifyPlayer(@Nullable Player player, String translationKey) {
        if (player != null) {
            player.displayClientMessage(Component.translatable(translationKey), true);
        }
    }

    private static void notifyPlayer(@Nullable Player player, @Nullable Component message) {
        if (player != null && message != null) {
            player.displayClientMessage(message, true);
        }
    }

    private CoreVolume currentVolume() {
        return storedState.volume();
    }
}
