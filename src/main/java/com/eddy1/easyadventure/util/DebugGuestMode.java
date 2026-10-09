package com.eddy1.easyadventure.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class DebugGuestMode {
    private static final String TAG_KEY = "EasyAdventureDebugGuestMode";

    private DebugGuestMode() {
    }

    public static boolean isEnabled(@Nullable Player player) {
        return player != null && player.getPersistentData().getBoolean(TAG_KEY);
    }

    public static void setEnabled(Player player, boolean enabled) {
        CompoundTag persistentData = player.getPersistentData();
        if (enabled) {
            persistentData.putBoolean(TAG_KEY, true);
            return;
        }
        persistentData.remove(TAG_KEY);
    }
}
