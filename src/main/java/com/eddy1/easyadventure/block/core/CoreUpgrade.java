package com.eddy1.easyadventure.block.core;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public enum CoreUpgrade {
    BLAST_SHIELD("gui.easyadventure.upgrade_blast_shield", "gui.easyadventure.upgrade_effect_blast_shield", Items.DIAMOND, 1, 30 * 60 * 20, 6 * 60 * 60 * 20),
    GREENHOUSE("gui.easyadventure.upgrade_greenhouse", "gui.easyadventure.upgrade_effect_greenhouse", Items.EMERALD, 1, 25 * 60 * 20, 6 * 60 * 60 * 20),
    PURIFICATION("gui.easyadventure.upgrade_purification", "gui.easyadventure.upgrade_effect_purification", Items.AMETHYST_SHARD, 1, 20 * 60 * 20, 4 * 60 * 60 * 20),
    FOLDING("gui.easyadventure.upgrade_folding", "gui.easyadventure.upgrade_effect_folding", Items.CHORUS_FRUIT, 1, 12 * 60 * 20, 2 * 60 * 60 * 20);

    private final String translationKey;
    private final String effectTranslationKey;
    private final Item material;
    private final int materialCount;
    private final int durationPerFuelTicks;
    private final int maxDurationTicks;

    CoreUpgrade(String translationKey, String effectTranslationKey, Item material, int materialCount, int durationPerFuelTicks, int maxDurationTicks) {
        this.translationKey = translationKey;
        this.effectTranslationKey = effectTranslationKey;
        this.material = material;
        this.materialCount = materialCount;
        this.durationPerFuelTicks = durationPerFuelTicks;
        this.maxDurationTicks = maxDurationTicks;
    }

    public String translationKey() {
        return translationKey;
    }

    public String effectTranslationKey() {
        return effectTranslationKey;
    }

    public Item material() {
        return material;
    }

    public int materialCount() {
        return materialCount;
    }

    public int durationPerFuelTicks() {
        return durationPerFuelTicks;
    }

    public int maxDurationTicks() {
        return maxDurationTicks;
    }

    public int addFuel(int currentTicks) {
        long nextTicks = (long) Math.max(0, currentTicks) + durationPerFuelTicks;
        return (int) Math.min(maxDurationTicks, nextTicks);
    }

    public boolean isActive(int remainingTicks) {
        return remainingTicks > 0;
    }

    public static CoreUpgrade fromId(int id) {
        CoreUpgrade[] values = values();
        if (id < 0 || id >= values.length) {
            return BLAST_SHIELD;
        }
        return values[id];
    }
}
