package com.eddy1.easyadventure.block.core;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record CoreResident(UUID uuid, String name, int permissionBits) {
    public static final int DEFAULT_PERMISSION_BITS =
            CorePermission.ENTER.mask()
                    | CorePermission.STORAGE.mask()
                    | CorePermission.USE_DEVICES.mask();
    public static final int LEGACY_COLLABORATOR_PERMISSION_BITS =
            DEFAULT_PERMISSION_BITS | CorePermission.RESIZE.mask();

    public CoreResident {
        uuid = uuid == null ? UUID.randomUUID() : uuid;
        name = sanitizeName(name, uuid);
    }

    public boolean hasFullAccess() {
        return CorePermission.RESIZE.isEnabled(permissionBits);
    }

    public boolean hasPermission(CorePermission permission) {
        return hasFullAccess() || permission.isEnabled(permissionBits);
    }

    public CoreResident withPermission(CorePermission permission, boolean enabled) {
        return new CoreResident(uuid, name, permission.apply(permissionBits, enabled));
    }

    public static String sanitizeName(@Nullable String name, UUID fallbackUuid) {
        if (name == null || name.isBlank()) {
            return fallbackUuid.toString();
        }
        return name.trim();
    }
}
