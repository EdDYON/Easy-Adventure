package com.eddy1.easyadventure.block.core;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class CoreEffects {
    private CoreEffects() {
    }

    public static void playScannerEffect(ServerLevel level, BlockPos center, CoreVolume volume, int currentYOffset) {
        if (level.getGameTime() % 2 != 0) {
            return;
        }

        double y = center.getY() + currentYOffset + 0.5;
        for (int x = -volume.halfX(); x <= volume.halfX(); x += 2) {
            level.sendParticles(ParticleTypes.END_ROD, center.getX() + x + 0.5, y, center.getZ() - volume.halfZ() + 0.5, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.END_ROD, center.getX() + x + 0.5, y, center.getZ() + volume.halfZ() + 0.5, 1, 0, 0, 0, 0);
        }
        for (int z = -volume.halfZ(); z <= volume.halfZ(); z += 2) {
            level.sendParticles(ParticleTypes.END_ROD, center.getX() - volume.halfX() + 0.5, y, center.getZ() + z + 0.5, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.END_ROD, center.getX() + volume.halfX() + 0.5, y, center.getZ() + z + 0.5, 1, 0, 0, 0, 0);
        }
    }

    public static void playCoreAmbience(ServerLevel level, BlockPos center) {
        if (level.getGameTime() % 5 == 0) {
            level.sendParticles(ParticleTypes.ENCHANT, center.getX() + 0.5, center.getY() + 1.2, center.getZ() + 0.5, 2, 0.3, 0.5, 0.3, 0.05);
        }
    }

    public static void performCelebration(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }

        level.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8F, 0.8F);

        if (level instanceof ServerLevel serverLevel) {
            ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
            CompoundTag fireworksTag = rocket.getOrCreateTagElement("Fireworks");
            fireworksTag.putByte("Flight", (byte) 1);
            CompoundTag explosionTag = new CompoundTag();
            explosionTag.putByte("Type", (byte) 1);
            explosionTag.putIntArray("Colors", IntList.of(0xFF5A36, 0xFFD166, 0xF8F9FA).toIntArray());
            explosionTag.putBoolean("Flicker", true);
            ListTag explosions = new ListTag();
            explosions.add(explosionTag);
            fireworksTag.put("Explosions", explosions);
            level.addFreshEntity(new FireworkRocketEntity(level, pos.getX() + 0.5, pos.getY() + 2.5, pos.getZ() + 0.5, rocket));

            for (int i = 0; i < 20; i++) {
                double angle = i * Math.PI * 2 / 20;
                double dx = Math.cos(angle) * 1.5;
                double dz = Math.sin(angle) * 1.5;
                serverLevel.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5 + dx, pos.getY() + 1.2, pos.getZ() + 0.5 + dz, 1, 0, 0, 0, 0.03);
            }
        }

        Player nearestPlayer = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 20.0D, false);
        if (!(nearestPlayer instanceof ServerPlayer serverPlayer)) {
            return;
        }

        Random random = new Random(System.nanoTime());
        List<MobEffect> possibleEffects = new ArrayList<>(List.of(
                MobEffects.MOVEMENT_SPEED,
                MobEffects.DIG_SPEED,
                MobEffects.DAMAGE_RESISTANCE,
                MobEffects.REGENERATION,
                MobEffects.ABSORPTION,
                MobEffects.JUMP,
                MobEffects.FIRE_RESISTANCE,
                MobEffects.LUCK,
                MobEffects.NIGHT_VISION,
                MobEffects.SATURATION,
                MobEffects.SLOW_FALLING,
                MobEffects.HERO_OF_THE_VILLAGE
        ));
        Collections.shuffle(possibleEffects, random);
        serverPlayer.addEffect(new MobEffectInstance(possibleEffects.get(0), 1200, 1));
        serverPlayer.addEffect(new MobEffectInstance(possibleEffects.get(1), 1200, 1));
        serverPlayer.connection.send(new ClientboundSetTitlesAnimationPacket(10, 100, 20));
        serverPlayer.connection.send(new ClientboundSetTitleTextPacket(CoreCelebrationTexts.randomTitle(random)));
        serverPlayer.connection.send(new ClientboundSetSubtitleTextPacket(CoreCelebrationTexts.randomSubtitle(random)));
    }
}
