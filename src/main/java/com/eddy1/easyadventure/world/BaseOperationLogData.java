package com.eddy1.easyadventure.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BaseOperationLogData extends SavedData {
    private static final String DATA_NAME = "easyadventure_base_operation_log";
    private static final int MAX_RECORDS = 512;

    private final List<Record> records = new ArrayList<>();

    public static BaseOperationLogData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        ServerLevel storageLevel = overworld == null ? level : overworld;
        return storageLevel.getDataStorage().computeIfAbsent(BaseOperationLogData::load, BaseOperationLogData::new, DATA_NAME);
    }

    public static BaseOperationLogData load(CompoundTag tag) {
        BaseOperationLogData data = new BaseOperationLogData();
        ListTag list = tag.getList("Records", Tag.TAG_COMPOUND);
        for (Tag entry : list) {
            Record record = Record.fromTag((CompoundTag) entry);
            if (record != null) {
                data.records.add(record);
            }
        }
        return data;
    }

    public void log(@Nullable Player actor, @Nullable UUID coreUuid, String baseName, String action, String result, String detail, @Nullable ResourceLocation dimension) {
        UUID actorUuid = actor == null ? null : actor.getUUID();
        String actorName = actor == null ? "System" : actor.getGameProfile().getName();
        records.add(new Record(
                System.currentTimeMillis(),
                actorUuid,
                actorName,
                coreUuid,
                baseName == null ? "" : baseName,
                action,
                result,
                detail == null ? "" : detail,
                dimension == null ? "" : dimension.toString()
        ));
        while (records.size() > MAX_RECORDS) {
            records.remove(0);
        }
        setDirty();
    }

    public List<Record> recent(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_RECORDS));
        int from = Math.max(0, records.size() - safeLimit);
        return List.copyOf(records.subList(from, records.size()));
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Record record : records) {
            list.add(record.toTag());
        }
        tag.put("Records", list);
        return tag;
    }

    public record Record(
            long timeMillis,
            @Nullable UUID actorUuid,
            String actorName,
            @Nullable UUID coreUuid,
            String baseName,
            String action,
            String result,
            String detail,
            String dimension
    ) {
        private static @Nullable Record fromTag(CompoundTag tag) {
            UUID actorUuid = tag.hasUUID("ActorUUID") ? tag.getUUID("ActorUUID") : null;
            UUID coreUuid = tag.hasUUID("CoreUUID") ? tag.getUUID("CoreUUID") : null;
            return new Record(
                    tag.getLong("Time"),
                    actorUuid,
                    tag.getString("ActorName"),
                    coreUuid,
                    tag.getString("BaseName"),
                    tag.getString("Action"),
                    tag.getString("Result"),
                    tag.getString("Detail"),
                    tag.getString("Dimension")
            );
        }

        private CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("Time", timeMillis);
            if (actorUuid != null) {
                tag.putUUID("ActorUUID", actorUuid);
            }
            tag.putString("ActorName", actorName);
            if (coreUuid != null) {
                tag.putUUID("CoreUUID", coreUuid);
            }
            tag.putString("BaseName", baseName);
            tag.putString("Action", action);
            tag.putString("Result", result);
            tag.putString("Detail", detail);
            tag.putString("Dimension", dimension);
            return tag;
        }
    }
}
