package com.eddy1.easyadventure.block.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

public final class CorePreview {
    private static final double STEP = 2.0D;

    private CorePreview() {
    }

    public static void show(ServerLevel level, BlockPos center, CoreVolume volume, boolean blocked) {
        ParticleOptions particle = blocked ? ParticleTypes.SMOKE : ParticleTypes.END_ROD;

        double minX = center.getX() - volume.halfX() + 0.5D;
        double maxX = center.getX() + volume.halfX() + 0.5D;
        double minY = center.getY() + 0.5D;
        double maxY = center.getY() + volume.sizeY() + 0.5D;
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
    }

    private static void spawn(ServerLevel level, ParticleOptions particle, double x, double y, double z) {
        level.sendParticles(particle, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }
}
