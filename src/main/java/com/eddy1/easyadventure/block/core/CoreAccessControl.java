package com.eddy1.easyadventure.block.core;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class CoreAccessControl {
    private CoreAccessControl() {
    }

    public static boolean canAccess(@Nullable Player player, @Nullable UUID ownerUuid) {
        return ownerUuid == null
                || player == null
                || player.hasPermissions(2)
                || ownerUuid.equals(player.getUUID());
    }

    public static boolean denyIfNoAccess(@Nullable Player player, @Nullable UUID ownerUuid, @Nullable String ownerName) {
        if (canAccess(player, ownerUuid)) {
            return false;
        }

        if (player != null) {
            String displayName = ownerName == null || ownerName.isBlank() ? "Unknown" : ownerName;
            player.displayClientMessage(Component.translatable("message.easyadventure.not_owner", displayName), true);
        }
        return true;
    }
}
