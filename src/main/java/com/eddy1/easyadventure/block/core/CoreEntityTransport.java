package com.eddy1.easyadventure.block.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Predicate;

public final class CoreEntityTransport {
    private CoreEntityTransport() {
    }

    public static void capture(Level level, BlockPos center, CoreVolume volume, List<CompoundTag> output) {
        if (level.isClientSide) {
            return;
        }

        AABB area = volume.createAabb(center);
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, area, capturableEntityPredicate());

        for (Entity entity : entities) {
            CompoundTag entityTag = new CompoundTag();
            if (!entity.saveAsPassenger(entityTag)) {
                continue;
            }

            Vec3 relativePos = entity.position().subtract(center.getX(), center.getY(), center.getZ());
            entityTag.putDouble("RelX", relativePos.x);
            entityTag.putDouble("RelY", relativePos.y);
            entityTag.putDouble("RelZ", relativePos.z);
            output.add(entityTag);
            entity.discard();

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.POOF, entity.getX(), entity.getY() + 0.5, entity.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
            }
        }
    }

    public static void restore(ServerLevel level, BlockPos center, List<CompoundTag> entities) {
        for (CompoundTag entityTag : entities) {
            double absX = center.getX() + entityTag.getDouble("RelX");
            double absY = center.getY() + entityTag.getDouble("RelY");
            double absZ = center.getZ() + entityTag.getDouble("RelZ");

            EntityType.create(entityTag, level).ifPresent(entity -> {
                entity.load(entityTag);
                entity.moveTo(absX, absY, absZ, entity.getYRot(), entity.getXRot());
                level.addFreshEntity(entity);
            });
        }
    }

    public static int countCapturable(Level level, BlockPos center, CoreVolume volume) {
        return level.getEntitiesOfClass(Entity.class, volume.createAabb(center), capturableEntityPredicate()).size();
    }

    private static Predicate<Entity> capturableEntityPredicate() {
        return entity -> {
            if (entity instanceof Player) {
                return false;
            }
            if (entity instanceof ItemEntity || entity instanceof FireworkRocketEntity) {
                return false;
            }
            if (entity.getType().is(CoreCompat.SKIP_ENTITY_CAPTURE)) {
                return false;
            }
            if (entity.isPassenger() || !entity.getPassengers().isEmpty()) {
                return false;
            }
            if (entity instanceof Mob mob && mob.isLeashed()) {
                return false;
            }

            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            if (id.getPath().contains("dragon") || id.getPath().contains("wither")) {
                return false;
            }

            return !entity.hasPassenger(passenger -> passenger instanceof Player);
        };
    }
}
