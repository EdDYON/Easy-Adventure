package com.eddy1.easyadventure.network;

public enum KeyOperationAction {
    PACK,
    DEPLOY;

    public static KeyOperationAction fromId(int id) {
        KeyOperationAction[] values = values();
        if (id < 0 || id >= values.length) {
            return DEPLOY;
        }
        return values[id];
    }
}
