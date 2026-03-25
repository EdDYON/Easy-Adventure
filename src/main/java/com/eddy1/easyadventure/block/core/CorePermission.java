package com.eddy1.easyadventure.block.core;

public enum CorePermission {
    ENTER("gui.easyadventure.permission_enter"),
    BUILD("gui.easyadventure.permission_build"),
    STORAGE("gui.easyadventure.permission_storage"),
    USE_DEVICES("gui.easyadventure.permission_use_devices"),
    RESIZE("gui.easyadventure.permission_resize");

    private final String translationKey;

    CorePermission(String translationKey) {
        this.translationKey = translationKey;
    }

    public int mask() {
        return 1 << ordinal();
    }

    public String translationKey() {
        return translationKey;
    }

    public boolean isEnabled(int permissionBits) {
        return (permissionBits & mask()) != 0;
    }

    public int apply(int permissionBits, boolean enabled) {
        return enabled ? permissionBits | mask() : permissionBits & ~mask();
    }

    public static CorePermission fromId(int id) {
        CorePermission[] values = values();
        if (id < 0 || id >= values.length) {
            return ENTER;
        }
        return values[id];
    }
}
