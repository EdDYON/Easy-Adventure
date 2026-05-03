package com.eddy1.easyadventure.block.core;

public enum CoreClearMode {
    CLEAR("gui.easyadventure.terrain_mode_clear"),
    KEEP("gui.easyadventure.terrain_mode_keep");

    private final String translationKey;

    CoreClearMode(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }

    public CoreClearMode next() {
        return this == CLEAR ? KEEP : CLEAR;
    }

    public static CoreClearMode fromName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return CLEAR;
        }

        for (CoreClearMode mode : values()) {
            if (mode.name().equalsIgnoreCase(rawName.trim())) {
                return mode;
            }
        }
        return CLEAR;
    }
}
