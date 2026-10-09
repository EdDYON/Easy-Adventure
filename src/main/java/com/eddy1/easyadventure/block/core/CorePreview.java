package com.eddy1.easyadventure.block.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

public final class CorePreview {
    private static final double STEP = 2.0D;
    private static final double GROUND_STEP = 1.0D;

    private CorePreview() {
    }

    public static void show(ServerLevel level, BlockPos center, CoreVolume volume, boolean blocked) {
        ParticleOptions particle = blocked ? ParticleTypes.SMOKE : ParticleTypes.END_ROD;

        double minX = center.getX() - volume.halfX() + 0.5D;
        double maxX = center.getX() + volume.halfX() + 0.5D;
        double minY = center.getY() + volume.minYOffset() + 0.5D;
        double maxY = center.getY() + volume.maxYOffset() + 0.5D;
        double minZ = center.getZ() - volume.halfZ() + 0.5D;
        double maxZ = center.getZ() + volume.halfZ() + 0.5D;

        for (double x = minX; x <= maxX; x += STEP) {
            spawn(level, particle, x, minY, minZ);
            spawn(level, particle, x, minY, maxZ);
            spawn(level, particle, x, maxY, minZ);
            spawn(level, particle, x, maxY, maxZ);
        }
        for (double y = minY; y <= maxY; y += STEP) {
            spawn(level, particle, minX, y, minZ);
            spawn(level, particle, minX, y, maxZ);
            spawn(level, particle, maxX, y, minZ);
            spawn(level, particle, maxX, y, maxZ);
        }
        for (double z = minZ; z <= maxZ; z += STEP) {
            spawn(level, particle, minX, minY, z);
            spawn(level, particle, maxX, minY, z);
            spawn(level, particle, minX, maxY, z);
            spawn(level, particle, maxX, maxY, z);
        }

        ParticleOptions cornerParticle = blocked ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SOUL_FIRE_FLAME;
        spawnCornerColumn(level, cornerParticle, minX, minY, maxY, minZ);
        spawnCornerColumn(level, cornerParticle, minX, minY, maxY, maxZ);
        spawnCornerColumn(level, cornerParticle, maxX, minY, maxY, minZ);
        spawnCornerColumn(level, cornerParticle, maxX, minY, maxY, maxZ);

        ParticleOptions groundParticle = blocked ? ParticleTypes.SMOKE : ParticleTypes.WAX_ON;
        for (double x = minX; x <= maxX; x += GROUND_STEP) {
            spawn(level, groundParticle, x, center.getY() + 0.08D, minZ);
            spawn(level, groundParticle, x, center.getY() + 0.08D, maxZ);
        }
        for (double z = minZ; z <= maxZ; z += GROUND_STEP) {
            spawn(level, groundParticle, minX, center.getY() + 0.08D, z);
            spawn(level, groundParticle, maxX, center.getY() + 0.08D, z);
        }

        if (volume.sizeBelowY() > 0) {
            ParticleOptions belowParticle = blocked ? ParticleTypes.SMOKE : ParticleTypes.REVERSE_PORTAL;
            spawn(level, belowParticle, center.getX() + 0.5D, minY, center.getZ() + 0.5D);
            spawn(level, belowParticle, center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D);
            for (double y = minY; y <= center.getY() + 0.5D; y += 1.0D) {
                spawn(level, belowParticle, center.getX() + 0.5D, y, center.getZ() + 0.5D);
            }
        }
    }

    private static void spawn(ServerLevel level, ParticleOptions particle, double x, double y, double z) {
        level.sendParticles(particle, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static void spawnCornerColumn(ServerLevel level, ParticleOptions particle, double x, double minY, double maxY, double z) {
        for (double y = minY; y <= maxY; y += 1.0D) {
            spawn(level, particle, x, y, z);
        }
    }
}
