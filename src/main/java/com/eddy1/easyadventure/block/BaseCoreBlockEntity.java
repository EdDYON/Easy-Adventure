package com.eddy1.easyadventure.block;

import com.eddy1.easyadventure.EasyAdventure;
import com.eddy1.easyadventure.init.ModBlockEntities;
import com.eddy1.easyadventure.util.SavedBlockInfo;
import com.eddy1.easyadventure.world.BuildingStorageData;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BaseCoreBlockEntity extends BlockEntity {

    public enum State { IDLE, PACKING, RESTORING, CLEARING, UNPACKING, GENERATING }
    private State currentState = State.IDLE;

    private final Queue<BlockPos> taskQueue = new LinkedList<>();

    private final List<SavedBlockInfo> packedDataCache = new ArrayList<>();
    private final List<SavedBlockInfo> incomingDataCache = new ArrayList<>();
    private final List<SavedBlockInfo> originalTerrainData = new ArrayList<>();

    private final List<CompoundTag> packedEntityData = new ArrayList<>();
    private final List<CompoundTag> incomingEntityData = new ArrayList<>();

    private String baseName = "便携基地";
    private UUID coreUUID = UUID.randomUUID();
    private boolean isBound = false;

    private static final int BLOCKS_PER_TICK = 200;

    private int sizeX = 9;
    private int sizeY = 5;
    private int sizeZ = 9;

    private boolean hasInitialized = false;
    private int soundTick = 0;

    private int currentProcessingY = 0;
    private int totalBlocksToProcess = 1;
    private int processedBlocks = 0;

    public BaseCoreBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.BASE_CORE.get(), pos, blockState);
        updateRangeFromBlock();
    }

    private void updateRangeFromBlock() {
        if (this.level != null) {
            Block block = this.getBlockState().getBlock();
            if (block instanceof BaseCoreBlock coreBlock) {
                if (this.sizeX == 9 && this.sizeZ == 9 && !hasInitialized) {
                    this.sizeX = coreBlock.getSizeX();
                    this.sizeY = coreBlock.getSizeY();
                    this.sizeZ = coreBlock.getSizeZ();
                }
            }
        }
    }

    public void setBaseName(String name) {
        this.baseName = name;
        this.setChanged();
    }

    public void initializeFoundation(int newX, int newY, int newZ) {
        if (this.currentState != State.IDLE) return;

        int oldX = this.sizeX;
        int oldY = this.sizeY;
        int oldZ = this.sizeZ;
        boolean isFirstTime = !this.hasInitialized;

        this.sizeX = Math.max(3, Math.min(64, newX));
        this.sizeY = Math.max(2, Math.min(320, newY));
        this.sizeZ = Math.max(3, Math.min(64, newZ));

        this.hasInitialized = true;
        this.setChanged();

        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.playSound(null, worldPosition, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 1.0f, 1.0f);
        }

        if (isFirstTime) {
            this.originalTerrainData.clear();
        }

        startSmartResizeTask(oldX, oldY, oldZ, this.sizeX, this.sizeY, this.sizeZ, isFirstTime);
    }

    private void startSmartResizeTask(int oldX, int oldY, int oldZ, int newX, int newY, int newZ, boolean isFirstTime) {
        this.taskQueue.clear();
        this.currentState = State.GENERATING;
        this.processedBlocks = 0;

        BlockPos center = this.worldPosition;
        int oldHalfX = oldX / 2; int oldHalfZ = oldZ / 2;
        int newHalfX = newX / 2; int newHalfZ = newZ / 2;

        if (!isFirstTime) {
            for (int y = oldY; y >= 0; y--) {
                for (int x = -oldHalfX; x <= oldHalfX; x++) {
                    for (int z = -oldHalfZ; z <= oldHalfZ; z++) {
                        boolean insideNew = (x >= -newHalfX && x <= newHalfX) &&
                                (z >= -newHalfZ && z <= newHalfZ) &&
                                (y <= newY);
                        if (!insideNew) {
                            BlockPos target = center.offset(x, y, z);
                            if (!target.equals(center)) this.taskQueue.add(target);
                        }
                    }
                }
            }
        }

        for (int x = -newHalfX; x <= newHalfX; x++) {
            for (int z = -newHalfZ; z <= newHalfZ; z++) {
                boolean isOldColumn = (x >= -oldHalfX && x <= oldHalfX) && (z >= -oldHalfZ && z <= oldHalfZ);
                int startY = newY;
                int endY = 0;

                if (!isFirstTime && isOldColumn) {
                    if (newY > oldY) {
                        startY = newY;
                        endY = oldY + 1;
                    } else {
                        continue;
                    }
                }

                for (int y = startY; y >= endY; y--) {
                    BlockPos target = center.offset(x, y, z);
                    if (!target.equals(center)) this.taskQueue.add(target);
                }
            }
        }
        this.totalBlocksToProcess = Math.max(1, this.taskQueue.size());
    }

    private void playScannerEffect(ServerLevel level, BlockPos center, int currentYOffset) {
        if (level.getGameTime() % 2 != 0) return;
        int halfX = this.sizeX / 2;
        int halfZ = this.sizeZ / 2;
        double y = center.getY() + currentYOffset + 0.5;

        for (int x = -halfX; x <= halfX; x+=2) {
            level.sendParticles(ParticleTypes.END_ROD, center.getX() + x + 0.5, y, center.getZ() - halfZ + 0.5, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.END_ROD, center.getX() + x + 0.5, y, center.getZ() + halfZ + 0.5, 1, 0, 0, 0, 0);
        }
        for (int z = -halfZ; z <= halfZ; z+=2) {
            level.sendParticles(ParticleTypes.END_ROD, center.getX() - halfX + 0.5, y, center.getZ() + z + 0.5, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.END_ROD, center.getX() + halfX + 0.5, y, center.getZ() + z + 0.5, 1, 0, 0, 0, 0);
        }
    }

    private void playCoreAmbience(ServerLevel level, BlockPos center) {
        if (level.getGameTime() % 5 == 0) {
            level.sendParticles(ParticleTypes.ENCHANT,
                    center.getX() + 0.5, center.getY() + 1.2, center.getZ() + 0.5,
                    2, 0.3, 0.5, 0.3, 0.05);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BaseCoreBlockEntity be) {
        if (level.isClientSide || be.currentState == State.IDLE) return;

        be.soundTick++;
        if (be.soundTick % 5 == 0) {
            float progress = (float) be.processedBlocks / be.totalBlocksToProcess;
            float pitch = 0.5f + progress * 1.5f;

            if (be.currentState == State.PACKING) {
                level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.5f, pitch);
            } else if (be.currentState == State.UNPACKING) {
                level.playSound(null, pos, SoundEvents.CONDUIT_AMBIENT, SoundSource.BLOCKS, 0.5f, 2.0f - pitch);
            }
        }

        if (level instanceof ServerLevel sl) {
            be.playCoreAmbience(sl, pos);
            if (be.currentState == State.PACKING || be.currentState == State.UNPACKING || be.currentState == State.CLEARING) {
                be.playScannerEffect(sl, pos, be.currentProcessingY);
            }
        }

        if (!be.taskQueue.isEmpty()) {
            for (int i = 0; i < BLOCKS_PER_TICK; i++) {
                if (be.taskQueue.isEmpty()) break;
                BlockPos target = be.taskQueue.poll();

                be.currentProcessingY = target.getY() - pos.getY();
                be.processedBlocks++;

                switch (be.currentState) {
                    case PACKING -> be.processPacking(target);
                    case RESTORING -> be.processRestoring(target);
                    case CLEARING -> be.processClearing(target);
                    case UNPACKING -> be.processUnpacking(target);
                    case GENERATING -> be.processGenerating(target);
                }
            }
        }

        if (be.taskQueue.isEmpty()) {
            be.finishTask();
        }
    }

    private void processGenerating(BlockPos pos) {
        if (level == null) return;
        int relY = pos.getY() - this.worldPosition.getY();
        int halfX = this.sizeX / 2;
        int halfZ = this.sizeZ / 2;
        int relX = Math.abs(pos.getX() - this.worldPosition.getX());
        int relZ = Math.abs(pos.getZ() - this.worldPosition.getZ());
        boolean insideCurrentBounds = (relX <= halfX) && (relZ <= halfZ) && (relY <= this.sizeY);

        if (!insideCurrentBounds) {
            restoreOriginalBlock(pos);
        } else {
            BlockState currentState = level.getBlockState(pos);

            if (currentState.getDestroySpeed(level, pos) < 0) return; // 保护基岩

            if (relY == 0) {
                if (!currentState.is(Blocks.COBBLESTONE)) {
                    CompoundTag nbt = null;
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be != null) {
                        nbt = be.saveWithFullMetadata(level.registryAccess());
                        Clearable.tryClear(be);
                    }
                    saveOriginalBlock(pos, currentState, nbt);
                    level.removeBlockEntity(pos);
                    level.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), 18);

                    if (level instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX()+0.5, pos.getY()+1, pos.getZ()+0.5, 1, 0, 0, 0, 0);
                    }
                }
            } else {
                if (!currentState.isAir()) {
                    CompoundTag nbt = null;
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be != null) {
                        nbt = be.saveWithFullMetadata(level.registryAccess());
                        Clearable.tryClear(be);
                    }
                    saveOriginalBlock(pos, currentState, nbt);
                    level.removeBlockEntity(pos);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
                }
            }
        }
    }

    private void saveOriginalBlock(BlockPos pos, BlockState state, @Nullable CompoundTag nbt) {
        BlockPos relative = pos.subtract(this.worldPosition);
        for (SavedBlockInfo info : originalTerrainData) {
            if (info.relativePos().equals(relative)) return;
        }
        originalTerrainData.add(new SavedBlockInfo(relative, state, nbt));
    }

    private void restoreOriginalBlock(BlockPos pos) {
        BlockPos relative = pos.subtract(this.worldPosition);
        BlockState restoreState = Blocks.AIR.defaultBlockState();
        CompoundTag restoreNbt = null;
        boolean found = false;

        for (SavedBlockInfo info : originalTerrainData) {
            if (info.relativePos().equals(relative)) {
                restoreState = info.state();
                restoreNbt = info.nbt();
                found = true;
                break;
            }
        }
        if (!found) {
            if (relative.getY() <= 0) restoreState = Blocks.DIRT.defaultBlockState();
        }

        level.removeBlockEntity(pos);
        level.setBlock(pos, restoreState, 18);

        if (restoreNbt != null) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                be.loadWithComponents(restoreNbt, level.registryAccess());
            }
        }
    }

    public int getSizeX() { return sizeX; }
    public int getSizeY() { return sizeY; }
    public int getSizeZ() { return sizeZ; }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.baseName != null) tag.putString("BaseName", this.baseName);
        tag.putUUID("CoreUUID", this.coreUUID);
        tag.putBoolean("IsBound", this.isBound);
        tag.putBoolean("HasInitialized", this.hasInitialized);
        tag.putInt("SizeX", this.sizeX);
        tag.putInt("SizeY", this.sizeY);
        tag.putInt("SizeZ", this.sizeZ);

        ListTag terrainList = new ListTag();
        for (SavedBlockInfo info : originalTerrainData) {
            terrainList.add(info.toTag());
        }
        tag.put("OriginalTerrain", terrainList);

        ListTag entityList = new ListTag();
        for (CompoundTag et : packedEntityData) {
            entityList.add(et);
        }
        tag.put("PackedEntities", entityList);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("BaseName")) this.baseName = tag.getString("BaseName");
        if (tag.contains("CoreUUID")) this.coreUUID = tag.getUUID("CoreUUID");
        if (tag.contains("IsBound")) this.isBound = tag.getBoolean("IsBound");
        if (tag.contains("HasInitialized")) this.hasInitialized = tag.getBoolean("HasInitialized");
        if (tag.contains("SizeX")) this.sizeX = tag.getInt("SizeX");
        if (tag.contains("SizeY")) this.sizeY = tag.getInt("SizeY");
        if (tag.contains("SizeZ")) this.sizeZ = tag.getInt("SizeZ");
        if (tag.contains("RangeXZ")) {
            int r = tag.getInt("RangeXZ");
            this.sizeX = r * 2 + 1; this.sizeZ = r * 2 + 1;
            this.hasInitialized = true;
        }
        if (tag.contains("RangeY")) this.sizeY = tag.getInt("RangeY");

        if (tag.contains("OriginalTerrain")) {
            this.originalTerrainData.clear();
            ListTag list = tag.getList("OriginalTerrain", Tag.TAG_COMPOUND);
            for (Tag t : list) {
                this.originalTerrainData.add(SavedBlockInfo.fromTag((CompoundTag) t));
            }
        }

        if (tag.contains("PackedEntities")) {
            this.packedEntityData.clear();
            ListTag list = tag.getList("PackedEntities", Tag.TAG_COMPOUND);
            for (Tag t : list) {
                this.packedEntityData.add((CompoundTag) t);
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("SizeX", this.sizeX);
        tag.putInt("SizeY", this.sizeY);
        tag.putInt("SizeZ", this.sizeZ);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public boolean isBound() { return this.isBound; }
    public UUID getCoreUUID() { return this.coreUUID; }


    public void restoreFromTag(CompoundTag heavyData, String name, UUID boundUUID) {
        if (this.currentState != State.IDLE) return;

        if (heavyData.contains("SavedSizeX")) {
            this.sizeX = heavyData.getInt("SavedSizeX");
            this.sizeY = heavyData.getInt("SavedSizeY");
            this.sizeZ = heavyData.getInt("SavedSizeZ");
            this.hasInitialized = true;
            this.setChanged();
        }

        List<SavedBlockInfo> dataList = new ArrayList<>();
        if (heavyData.contains("BaseData")) {
            ListTag list = heavyData.getList("BaseData", Tag.TAG_COMPOUND);
            for (Tag t : list) {
                dataList.add(SavedBlockInfo.fromTag((CompoundTag) t));
            }
        }

        this.incomingEntityData.clear();
        if (heavyData.contains("EntityData")) {
            ListTag entityList = heavyData.getList("EntityData", Tag.TAG_COMPOUND);
            for (Tag t : entityList) {
                this.incomingEntityData.add((CompoundTag) t);
            }
        }

        this.isBound = true;
        if (boundUUID != null) {
            this.coreUUID = boundUUID;
        }

        this.originalTerrainData.clear();

        startDeployment(dataList, name, boundUUID);
    }

    public void startDeployment(List<SavedBlockInfo> data, String name, UUID boundUUID) {
        if (this.currentState != State.IDLE) return;
        updateRangeFromBlock();

        this.baseName = name;
        this.incomingDataCache.clear();
        if (data != null) this.incomingDataCache.addAll(data);

        this.currentState = State.CLEARING;
        this.taskQueue.clear();
        this.soundTick = 0;
        this.processedBlocks = 0;

        BlockPos center = this.worldPosition;
        int halfX = sizeX / 2; int halfZ = sizeZ / 2;

        for (int y = this.sizeY; y >= 0; y--) {
            for (int x = -halfX; x <= halfX; x++) {
                for (int z = -halfZ; z <= halfZ; z++) {
                    BlockPos p = center.offset(x, y, z);
                    if (!p.equals(center)) this.taskQueue.add(p);
                }
            }
        }
        this.totalBlocksToProcess = Math.max(1, this.taskQueue.size());

        level.playSound(null, this.worldPosition, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 2.0f, 0.5f);
    }

    private void processClearing(BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (state.getDestroySpeed(level, pos) < 0) return; // 保护基岩

        if (!state.isAir()) {
            CompoundTag nbt = null;
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                nbt = be.saveWithFullMetadata(level.registryAccess());
                Clearable.tryClear(be);
            }
            saveOriginalBlock(pos, state, nbt);

            if (be != null) level.removeBlockEntity(pos);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.POOF, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 1, 0, 0, 0, 0.05);
            }
        }
    }

    private void startBuildingPhase() {
        this.taskQueue.clear();
        BlockPos center = this.worldPosition;
        int halfX = sizeX / 2; int halfZ = sizeZ / 2;

        this.currentState = State.GENERATING;
        this.processedBlocks = 0;

        for (int y = this.sizeY; y >= 0; y--) {
            for (int x = -halfX; x <= halfX; x++) {
                for (int z = -halfZ; z <= halfZ; z++) {
                    BlockPos p = center.offset(x, y, z);
                    if (!p.equals(center)) this.taskQueue.add(p);
                }
            }
        }
        this.totalBlocksToProcess = Math.max(1, this.taskQueue.size());
    }

    private void startUnpackingPhase() {
        this.taskQueue.clear();
        if (this.incomingDataCache.isEmpty()) {
            this.currentState = State.IDLE;
            return;
        }

        this.currentState = State.UNPACKING;
        this.packedDataCache.clear();
        this.packedDataCache.addAll(this.incomingDataCache);
        this.processedBlocks = 0;

        this.packedDataCache.sort(Comparator.comparingInt(info -> info.relativePos().getY()));

        for (SavedBlockInfo info : this.packedDataCache) {
            this.taskQueue.add(this.worldPosition.offset(info.relativePos()));
        }
        this.totalBlocksToProcess = Math.max(1, this.taskQueue.size());

        level.playSound(null, worldPosition, SoundEvents.CONDUIT_ACTIVATE, SoundSource.BLOCKS, 2.0f, 1.0f);
    }

    private void processUnpacking(BlockPos targetPos) {
        BlockPos relative = targetPos.subtract(this.worldPosition);
        for (SavedBlockInfo info : packedDataCache) {
            if (info.relativePos().equals(relative)) {
                BlockState state = info.state();
                level.setBlock(targetPos, state, 18);

                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.END_ROD, targetPos.getX()+0.5, targetPos.getY()+0.5, targetPos.getZ()+0.5, 1, 0, 0, 0, 0.01);
                    if (level.random.nextFloat() < 0.15f) {
                        sl.playSound(null, targetPos, SoundEvents.BAMBOO_WOOD_PLACE, SoundSource.BLOCKS, 0.5f, 1.5f);
                    }
                }

                if (info.nbt() != null) {
                    BlockEntity targetBe = level.getBlockEntity(targetPos);
                    if (targetBe != null) targetBe.loadWithComponents(info.nbt(), level.registryAccess());
                }
                break;
            }
        }
    }

    public void startPacking() {
        if (this.currentState != State.IDLE) return;
        updateRangeFromBlock();

        this.currentState = State.PACKING;
        this.packedDataCache.clear();
        this.packedEntityData.clear();
        this.taskQueue.clear();
        this.soundTick = 0;
        this.processedBlocks = 0;

        BlockPos center = this.worldPosition;
        int halfX = sizeX / 2; int halfZ = sizeZ / 2;

        // ★★★ 实体收纳 (带过滤器) ★★★
        if (level != null && !level.isClientSide) {
            // [修复] 手动提取坐标构造 AABB
            BlockPos minPos = center.offset(-halfX, 0, -halfZ);
            BlockPos maxPos = center.offset(halfX, sizeY, halfZ).offset(1, 1, 1);

            AABB aabb = new AABB(
                    minPos.getX(), minPos.getY(), minPos.getZ(),
                    maxPos.getX(), maxPos.getY(), maxPos.getZ()
            );

            List<Entity> entities = level.getEntitiesOfClass(Entity.class, aabb, e -> {
                if (e instanceof Player) return false;
                if (e instanceof ItemEntity || e instanceof FireworkRocketEntity) return false;

                ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
                if (id.getPath().contains("dragon") || id.getPath().contains("wither")) return false;

                if (e.hasPassenger(p -> p instanceof Player)) return false;

                return true;
            });

            for (Entity e : entities) {
                CompoundTag entityTag = new CompoundTag();
                if (e.saveAsPassenger(entityTag)) {
                    Vec3 relPos = e.position().subtract(center.getX(), center.getY(), center.getZ());
                    entityTag.putDouble("RelX", relPos.x);
                    entityTag.putDouble("RelY", relPos.y);
                    entityTag.putDouble("RelZ", relPos.z);
                    packedEntityData.add(entityTag);

                    e.discard();

                    if (level instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.POOF, e.getX(), e.getY()+0.5, e.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
                    }
                }
            }
        }

        for (int y = this.sizeY; y >= 0; y--) {
            for (int x = -halfX; x <= halfX; x++) {
                for (int z = -halfZ; z <= halfZ; z++) {
                    BlockPos p = center.offset(x, y, z);
                    if (!p.equals(center)) this.taskQueue.add(p);
                }
            }
        }
        this.totalBlocksToProcess = Math.max(1, this.taskQueue.size());

        level.playSound(null, worldPosition, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0f, 0.5f);
    }

    private void processPacking(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.isAir()) {
            if (state.getBlock() instanceof BaseCoreBlock) return;

            if (state.getDestroySpeed(level, pos) < 0) return; // 保护基岩

            if (level instanceof ServerLevel sl) {
                Vec3 center = new Vec3(this.worldPosition.getX()+0.5, this.worldPosition.getY()+0.5, this.worldPosition.getZ()+0.5);
                Vec3 blockPos = new Vec3(pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5);
                Vec3 direction = center.subtract(blockPos).normalize().scale(0.5);

                if (level.random.nextFloat() < 0.3f) {
                    sl.sendParticles(ParticleTypes.WITCH, blockPos.x, blockPos.y, blockPos.z, 0, direction.x, direction.y, direction.z, 1.0);
                }
            }

            CompoundTag nbt = null;
            BlockEntity targetBe = level.getBlockEntity(pos);
            if (targetBe != null) {
                nbt = targetBe.saveWithFullMetadata(level.registryAccess());
                Clearable.tryClear(targetBe);
            }
            packedDataCache.add(new SavedBlockInfo(pos.subtract(this.worldPosition), state, nbt));

            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
        }
    }

    private void startRestoringPhase() {
        this.currentState = State.RESTORING;
        this.taskQueue.clear();
        this.processedBlocks = 0;

        BlockPos center = this.worldPosition;
        int halfX = sizeX / 2; int halfZ = sizeZ / 2;

        for (int y = 0; y <= this.sizeY; y++) {
            for (int x = -halfX; x <= halfX; x++) {
                for (int z = -halfZ; z <= halfZ; z++) {
                    BlockPos p = center.offset(x, y, z);
                    if (!p.equals(center)) this.taskQueue.add(p);
                }
            }
        }
        this.totalBlocksToProcess = Math.max(1, this.taskQueue.size());
    }

    private void processRestoring(BlockPos pos) {
        restoreOriginalBlock(pos);
    }

    private void finishTask() {
        if (this.currentState == State.CLEARING) {
            startBuildingPhase();
        } else if (this.currentState == State.GENERATING) {
            startUnpackingPhase();
        } else if (this.currentState == State.UNPACKING) {

            // ★★★ 实体恢复 ★★★
            if (level instanceof ServerLevel sl && !incomingEntityData.isEmpty()) {
                for (CompoundTag entityTag : incomingEntityData) {
                    double relX = entityTag.getDouble("RelX");
                    double relY = entityTag.getDouble("RelY");
                    double relZ = entityTag.getDouble("RelZ");
                    double absX = this.worldPosition.getX() + relX;
                    double absY = this.worldPosition.getY() + relY;
                    double absZ = this.worldPosition.getZ() + relZ;

                    EntityType.create(entityTag, level).ifPresent(entity -> {
                        entity.load(entityTag);
                        entity.moveTo(absX, absY, absZ, entity.getYRot(), entity.getXRot());
                        sl.addFreshEntity(entity);
                    });
                }
            }

            this.currentState = State.IDLE;
            performCelebration();
        } else if (this.currentState == State.PACKING) {
            startRestoringPhase();
        } else if (this.currentState == State.RESTORING) {
            this.currentState = State.IDLE;
            this.soundTick = 0;

            if (level instanceof ServerLevel serverLevel) {
                CompoundTag heavyData = new CompoundTag();
                heavyData.putInt("SavedSizeX", this.sizeX);
                heavyData.putInt("SavedSizeY", this.sizeY);
                heavyData.putInt("SavedSizeZ", this.sizeZ);

                ListTag listTag = new ListTag();
                for (SavedBlockInfo info : packedDataCache) listTag.add(info.toTag());
                heavyData.put("BaseData", listTag);

                ListTag entityListTag = new ListTag();
                for (CompoundTag et : packedEntityData) entityListTag.add(et);
                heavyData.put("EntityData", entityListTag);

                UUID storageUUID = BuildingStorageData.get(serverLevel).saveBuilding(heavyData);

                ItemStack keyStack = new ItemStack(EasyAdventure.BASE_KEY_ITEM.get());
                String currentBaseName = this.baseName;
                if (currentBaseName == null || currentBaseName.isEmpty()) currentBaseName = "便携基地";
                String cleanName = currentBaseName.replace(" (已释放)", "").replace(" (已打包)", "");
                keyStack.set(DataComponents.CUSTOM_NAME, Component.literal(cleanName + " (已打包)"));
                CompoundTag keyTag = new CompoundTag();
                keyTag.putUUID("BoundUUID", this.coreUUID);
                keyTag.putUUID("StorageUUID", storageUUID);
                keyStack.set(DataComponents.CUSTOM_DATA, CustomData.of(keyTag));

                boolean given = false;
                Player nearestPlayer = level.getNearestPlayer(this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ(), 10, false);
                if (nearestPlayer != null) {
                    given = nearestPlayer.getInventory().add(keyStack);
                    if (given) {
                        nearestPlayer.playSound(SoundEvents.ITEM_PICKUP, 1.0f, 1.0f);
                        nearestPlayer.displayClientMessage(Component.literal("§d[系统] §f基地已安全收纳！"), true);
                    }
                }
                if (!given) spawnItemDrop(keyStack);
            }

            level.destroyBlock(this.worldPosition, false);
            level.playSound(null, this.worldPosition, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }

    private void spawnItemDrop(ItemStack stack) {
        ItemEntity entity = new ItemEntity(level, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.2, this.worldPosition.getZ() + 0.5, stack);
        entity.setDeltaMovement(0, 0.4, 0);
        entity.setNoPickUpDelay();
        level.addFreshEntity(entity);
    }

    private void performCelebration() {
        if (level == null || level.isClientSide) return;

        BlockPos pos = this.worldPosition;
        level.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0f, 1.0f);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8f, 0.8f);

        if (level instanceof ServerLevel serverLevel) {
            ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
            FireworkExplosion explosion = new FireworkExplosion(
                    FireworkExplosion.Shape.LARGE_BALL,
                    IntList.of(0xFF0000, 0xFFD700, 0xFFFFFF),
                    IntList.of(),
                    true,
                    false
            );
            Fireworks fireworksData = new Fireworks(1, List.of(explosion));
            rocket.set(DataComponents.FIREWORKS, fireworksData);

            FireworkRocketEntity entity = new FireworkRocketEntity(level,
                    pos.getX() + 0.5, pos.getY() + 2.5, pos.getZ() + 0.5, rocket);
            level.addFreshEntity(entity);

            for (int i = 0; i < 20; i++) {
                double angle = i * Math.PI * 2 / 20;
                double dx = Math.cos(angle) * 1.5;
                double dz = Math.sin(angle) * 1.5;
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        pos.getX() + 0.5 + dx, pos.getY() + 1.2, pos.getZ() + 0.5 + dz,
                        1, 0, 0, 0, 0.03);
            }
        }

        Player player = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 20, false);
        if (player instanceof ServerPlayer serverPlayer) {

            java.util.Random chaosRandom = new java.util.Random(System.nanoTime());

            List<Holder<MobEffect>> possibleEffects = new ArrayList<>();
            possibleEffects.add(MobEffects.MOVEMENT_SPEED);      // 速度
            possibleEffects.add(MobEffects.DIG_SPEED);           // 急迫
            possibleEffects.add(MobEffects.DAMAGE_RESISTANCE);   // 抗性
            possibleEffects.add(MobEffects.REGENERATION);        // 生命恢复
            possibleEffects.add(MobEffects.ABSORPTION);          // 伤害吸收 (金心)
            possibleEffects.add(MobEffects.JUMP);                // 跳跃提升
            possibleEffects.add(MobEffects.FIRE_RESISTANCE);     // 抗火
            possibleEffects.add(MobEffects.LUCK);                // 幸运
            possibleEffects.add(MobEffects.NIGHT_VISION);        // 夜视
            possibleEffects.add(MobEffects.SATURATION);          // 饱和 (回饱食度)
            possibleEffects.add(MobEffects.SLOW_FALLING);        // 缓降 (防止意外摔伤)
            possibleEffects.add(MobEffects.HERO_OF_THE_VILLAGE);// 村庄英雄

            Collections.shuffle(possibleEffects, chaosRandom);

            for (int i = 0; i < 2; i++) {
                serverPlayer.addEffect(new MobEffectInstance(possibleEffects.get(i), 1200, 1));
            }

            String[] quotes = {
                    "§b何处是吾乡？ §6此心安处是吾乡",
                    "§e既是起点，亦是归途",
                    "§d星辰指引归途，大地承载梦想",
                    "§6万水千山，不忘来路",
                    "§a风起之处，便是征程",
                    "§b世界虽大，总有一盏灯为你而亮",
                    "§5重铸秩序，安身立命",
                    "§c在此驻足，为了更好的出发",
                    "§6长路漫漫，幸有归处",
                    "§d遍历山河，终有归宿",
                    "§5以吾之名，铸就不朽",
                    "§6暂歇羽翼，静待风起",
                    "§b[系统] §f空间折叠矩阵：§a展开完成",
                    "§3量子纠缠态：§b已锁定当前坐标",
                    "§9维度投影稳定，§d欢迎回到主物质位面",
                    "§c苦力怕禁止入内！§e(大概吧...)",
                    "§a这次应该没忘带熔炉吧？",
                    "§b这里没有996，§a只有诗和方块",
                    "§eHome, §6Sweet Home.",
                    "§bJourney before destination.",
                    "§dおかえり。 §f(欢迎回家)",
                    "§5爆裂吧现实！§d粉碎吧精神！§b放逐这个世界！", // 中二病名台词
                    "§c吾之领土，§4神圣不可侵犯",
                    "§6契约已成，§e此地即为理想乡",
                    "§b以吾之名，§a重铸方块之秩序",
                    "§d封印解除！§5固有结界·无限剑制 (误)",
                    "§e颤抖吧，§6凡人！§f(只是个房子而已)",
                    "§3深渊凝视着你，§b而你在凝视着家",
                    "§a拔剑四顾心茫然，§e不如回家睡大觉",
                    "§5宿命的齿轮§d开始转动...",
                    "§c前方高能！§6安全屋已着陆",
                    "§9传说开始的地方，§b也是结束的地方",
                    "§aHey, you. §eYou're finally awake.",
                    "§eIt's dangerous to go alone! §aTake this.",
                    "§dThe Cake is a Lie. §f(但家是真的)",
                    "§b房屋已分配。 §a(泰拉瑞亚梗)",
                    "§6赞美太阳！§e(Praise the Sun!)",
                    "§c胜败乃兵家常事，§f大侠请重新来过",
                    "§b正在载入地图... §a99%",
                    "§d末影人偷走了你的草方块，§5但我还在这里",
                    "§a恭喜你，§e达成成就：【有房一族】",
                    "§3War... §bWar never changes.",
                    "§b虽然不大，§a但是抗揍",
                    "§e甚至比 §6村民的火柴盒 §e好一点点",
                    "§d苦力怕：§f这个家我看上了",
                    "§c警告：§f检测到大量“懒癌”晚期患者",
                    "§a这一刻，§b原本想去挖矿的念头消失了",
                    "§6这是魔法！§e牛顿管不了这个！",
                    "§b不仅省了材料，§d还省了脑子",
                    "§a别看了，§f再看也要收房租的",
                    "§5这里禁止随地大小便 §d(并没有)",
                    "§e如果不喜欢，§6请不要投诉开发者",
                    "§b这不仅仅是房子，§a这是你的快乐老家",
                    "§b方块的尽头，§3是圆吗？",
                    "§d我们在创造世界，§5还是世界在创造我们？",
                    "§a时间是静止的，§e移动的是我们",
                    "§6虚空在呼唤，§e大地在沉睡",
                    "§f白色的羊毛，§7灰色的石头，§8黑色的夜",
                    "§3孤独是§b一种艺术",
                    "§d梦境与现实的§5交界处",
                    "§a404 Not Found. §f(家还在)",
                    "§e意义本身§6没有意义",
                    "§f愿原力与你同在，§e还有这间屋子"
            };

            String randomQuote = quotes[chaosRandom.nextInt(quotes.length)];

            serverPlayer.connection.send(new ClientboundSetTitlesAnimationPacket(10, 100, 20));
            if (chaosRandom.nextBoolean()) {
                serverPlayer.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§e§l✦ 基地部署完毕 ✦")));
            } else {
                serverPlayer.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§b§l✦ SYSTEM ONLINE ✦")));
            }
            serverPlayer.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(randomQuote)));
        }
    }
}